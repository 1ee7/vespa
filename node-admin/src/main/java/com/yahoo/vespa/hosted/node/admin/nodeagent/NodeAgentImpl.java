// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.nodeagent;

import com.yahoo.log.LogLevel;
import com.yahoo.vespa.applicationmodel.HostName;
import com.yahoo.vespa.hosted.node.admin.ContainerNodeSpec;
import com.yahoo.vespa.hosted.node.admin.docker.DockerImage;
import com.yahoo.vespa.hosted.node.admin.noderepository.NodeRepository;
import com.yahoo.vespa.hosted.node.admin.orchestrator.Orchestrator;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgentImpl.ContainerState.ABSENT;
import static com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgentImpl.ContainerState.RUNNING;
import static com.yahoo.vespa.hosted.node.admin.nodeagent.NodeAgentImpl.ContainerState.RUNNING_HOWEVER_RESUME_SCRIPT_NOT_RUN;

/**
 * @author dybis
 * @author bakksjo
 */
public class NodeAgentImpl implements NodeAgent {

    private AtomicBoolean isFrozen = new AtomicBoolean(false);
    private AtomicBoolean wantFrozen = new AtomicBoolean(false);
    private AtomicBoolean terminated = new AtomicBoolean(false);

    private boolean workToDoNow = true;

    private static final Logger logger = Logger.getLogger(NodeAgentImpl.class.getName());

    private DockerImage imageBeingDownloaded = null;

    private final String logPrefix;
    private final HostName hostname;

    private final NodeRepository nodeRepository;
    private final Orchestrator orchestrator;
    private final DockerOperations dockerOperations;

    private final Object monitor = new Object();

    private AtomicReference<String> debugString = new AtomicReference<>("not started");

    private long delaysBetweenEachTickMillis;

    private Thread loopThread;

    public enum ContainerState {
        ABSENT,
        RUNNING_HOWEVER_RESUME_SCRIPT_NOT_RUN,
        RUNNING
    }
    ContainerState containerState = ABSENT;

    // The attributes of the last successful noderepo attribute update for this node. Used to avoid redundant calls.
    private NodeAttributes lastAttributesSet = null;

    public NodeAgentImpl(
            final HostName hostName,
            final NodeRepository nodeRepository,
            final Orchestrator orchestrator,
            final DockerOperations dockerOperations) {
        this.logPrefix = "NodeAgent(" + hostName + "): ";
        this.nodeRepository = nodeRepository;
        this.orchestrator = orchestrator;
        this.hostname = hostName;
        this.dockerOperations = dockerOperations;
    }

    @Override
    public void freeze() {
        wantFrozen.set(true);
        signalWorkToBeDone();
    }

    @Override
    public void unfreeze() {
        wantFrozen.set(false);
        signalWorkToBeDone();
    }

    @Override
    public boolean isFrozen() {
        return isFrozen.get();
    }

    @Override
    public String debugInfo() {
        return debugString.get();
    }

    @Override
    public void start(int intervalMillis) {
        delaysBetweenEachTickMillis = intervalMillis;
        if (loopThread != null) {
            throw new RuntimeException("Can not restart a node agent.");
        }
        loopThread = new Thread(this::loop);
        loopThread.setName("loop-" + hostname.toString());
        loopThread.start();
    }

    @Override
    public void stop() {
        if (!terminated.compareAndSet(false, true)) {
            throw new RuntimeException("Can not re-stop a node agent.");
        }
        signalWorkToBeDone();
        try {
            loopThread.join(10000);
            if (loopThread.isAlive()) {
                logger.severe("Could not stop host thread " + hostname);
            }
        } catch (InterruptedException e1) {
            logger.severe("Interrupted; Could not stop host thread " + hostname);
        }
    }

    private void runLocalResumeScriptIfNeeded(final ContainerNodeSpec nodeSpec) {
        if (containerState != RUNNING_HOWEVER_RESUME_SCRIPT_NOT_RUN) {
            return;
        }
        logger.log(Level.INFO, logPrefix + "Starting optional node program resume command");
        dockerOperations.executeResume(nodeSpec.containerName);//, RESUME_NODE_COMMAND);
        containerState = RUNNING;
    }

    private void publishStateToNodeRepoIfChanged(final ContainerNodeSpec nodeSpec) throws IOException {
        final String containerVespaVersion = dockerOperations.getVespaVersionOrNull(nodeSpec.containerName);

        final NodeAttributes currentAttributes = new NodeAttributes(
                nodeSpec.wantedRestartGeneration.get(),
                nodeSpec.wantedDockerImage.get(),
                containerVespaVersion);
        if (!currentAttributes.equals(lastAttributesSet)) {
            logger.log(Level.INFO, logPrefix + "Publishing new set of attributes to node repo: "
                    + lastAttributesSet + " -> " + currentAttributes);
            nodeRepository.updateNodeAttributes(
                    nodeSpec.hostname,
                    currentAttributes.restartGeneration,
                    currentAttributes.dockerImage,
                    currentAttributes.vespaVersion);
            lastAttributesSet = currentAttributes;
        }

        logger.log(Level.INFO, logPrefix + "Call resume against Orchestrator");
    }

    private void startContainerIfNeeded(final ContainerNodeSpec nodeSpec) {
        if (dockerOperations.startContainerIfNeeded(nodeSpec)) {
            containerState = RUNNING_HOWEVER_RESUME_SCRIPT_NOT_RUN;
        } else {
            // In case container was already running on startup, we found the container, but should call
            if (containerState == ABSENT) {
                containerState = RUNNING_HOWEVER_RESUME_SCRIPT_NOT_RUN;
            }
        }
    }

    private void removeContainerIfNeededUpdateContainerState(ContainerNodeSpec nodeSpec) throws Exception {
        if (dockerOperations.removeContainerIfNeeded(nodeSpec, hostname, orchestrator)) {
            containerState = ABSENT;
        }
    }

    private void scheduleDownLoadIfNeeded(ContainerNodeSpec nodeSpec) {
        if (dockerOperations.shouldScheduleDownloadOfImage(nodeSpec.wantedDockerImage.get())) {
            if (imageBeingDownloaded == nodeSpec.wantedDockerImage.get()) {
                // Downloading already scheduled, but not done.
                return;
            }
            imageBeingDownloaded = nodeSpec.wantedDockerImage.get();
            // Create a signalWorkToBeDone when download is finished.
            dockerOperations.scheduleDownloadOfImage(nodeSpec, this::signalWorkToBeDone);
        } else {
            imageBeingDownloaded = null;
        }
    }

    @Override
    public void signalWorkToBeDone() {
        synchronized (monitor) {
            workToDoNow = true;
            monitor.notifyAll();
        }
    }

    private void loop() {
        while (! terminated.get()) {
            synchronized (monitor) {
                long waittimeLeft = delaysBetweenEachTickMillis;
                while (waittimeLeft > 1 && !workToDoNow) {
                    Instant start = Instant.now();
                    try {
                        monitor.wait(waittimeLeft);
                    } catch (InterruptedException e) {
                        logger.severe("Interrupted, but ignoring this: " + hostname);
                        continue;
                    }
                    waittimeLeft -= Duration.between(start, Instant.now()).toMillis();
                }
                workToDoNow = false;
            }
            isFrozen.set(wantFrozen.get());
            if (isFrozen.get()) {
                debugString.set(hostname + " frozen");
            } else {
                try {
                    tick();
                } catch (Exception e) {
                    logger.log(LogLevel.ERROR, logPrefix + "Unhandled exception, ignoring.", e);
                    debugString.set(hostname + " " + e.getMessage());
                } catch (Throwable t) {
                    logger.log(LogLevel.ERROR, logPrefix + "Unhandled throwable, taking down system.", t);
                    System.exit(234);
                }
            }
        }
    }

    // For testing
    public void tick() throws Exception {
        StringBuilder debugStringBuilder = new StringBuilder(hostname.toString());
        final ContainerNodeSpec nodeSpec = nodeRepository.getContainerNodeSpec(hostname)
                .orElseThrow(() ->
                        new IllegalStateException(String.format("Node '%s' missing from node repository.", hostname)));
        debugStringBuilder.append("Loaded node spec: ").append(nodeSpec.toString());
        switch (nodeSpec.nodeState) {
            case PROVISIONED:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                logger.log(LogLevel.INFO, logPrefix + "State is provisioned, will delete application storage and mark node as ready");
                dockerOperations.deleteContainerStorage(nodeSpec.containerName);
                nodeRepository.markAsReady(nodeSpec.hostname);
                break;
            case READY:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                break;
            case RESERVED:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                break;
            case ACTIVE:
                scheduleDownLoadIfNeeded(nodeSpec);
                if (imageBeingDownloaded != null) {
                    debugStringBuilder.append("Waiting for image to download " + imageBeingDownloaded.asString());
                    debugString.set(debugStringBuilder.toString());
                    return;
                }
                removeContainerIfNeededUpdateContainerState(nodeSpec);

                startContainerIfNeeded(nodeSpec);
                runLocalResumeScriptIfNeeded(nodeSpec);
                // Because it's more important to stop a bad release from rolling out in prod,
                // we put the resume call last. So if we fail after updating the node repo attributes
                // but before resume, the app may go through the tenant pipeline but will halt in prod.
                //
                // Note that this problem exists only because there are 2 different mechanisms
                // that should really be parts of a single mechanism:
                //  - The content of node repo is used to determine whether a new Vespa+application
                //    has been successfully rolled out.
                //  - Slobrok and internal orchestrator state is used to determine whether
                //    to allow upgrade (suspend).
                publishStateToNodeRepoIfChanged(nodeSpec);
                orchestrator.resume(nodeSpec.hostname);
                break;
            case INACTIVE:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                break;
            case DIRTY:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                logger.log(LogLevel.INFO, logPrefix + "State is dirty, will delete application storage and mark node as ready");
                dockerOperations.deleteContainerStorage(nodeSpec.containerName);
                nodeRepository.markAsReady(nodeSpec.hostname);
                break;
            case FAILED:
                removeContainerIfNeededUpdateContainerState(nodeSpec);
                break;
            default:
                throw new RuntimeException("UNKNOWN STATE " + nodeSpec.nodeState.name());
        }
        debugString.set(debugStringBuilder.toString());
    }
}
