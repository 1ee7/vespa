// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.core.config;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.yahoo.component.AbstractComponent;
import com.yahoo.component.ComponentSpecification;
import com.yahoo.component.provider.ComponentRegistry;
import com.yahoo.concurrent.ThreadFactoryFactory;
import com.yahoo.config.FileReference;
import com.yahoo.container.core.DiagnosticsConfig;
import com.yahoo.container.di.ComponentDeconstructor;
import com.yahoo.container.di.Container;
import com.yahoo.container.di.componentgraph.core.ComponentGraph;
import com.yahoo.container.di.componentgraph.core.DotGraph;
import com.yahoo.container.di.config.SubscriberFactory;
import com.yahoo.container.di.osgi.OsgiUtil;
import com.yahoo.container.handler.observability.OverviewHandler;
import com.yahoo.container.logging.AccessLog;
import com.yahoo.container.logging.AccessLogInterface;
import com.yahoo.jdisc.application.OsgiFramework;
import com.yahoo.jdisc.handler.RequestHandler;
import com.yahoo.jdisc.service.ClientProvider;
import com.yahoo.jdisc.service.ServerProvider;
import com.yahoo.language.Linguistics;
import com.yahoo.language.simple.SimpleLinguistics;
import com.yahoo.log.LogLevel;
import com.yahoo.osgi.OsgiImpl;
import com.yahoo.statistics.Statistics;

import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;
import scala.collection.immutable.Set;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static com.yahoo.collections.CollectionUtil.first;
import static com.yahoo.container.util.Util.quote;


/**
 * For internal use only.
 *
 * @author tonytv
 * @author gjoranv
 */
//TODO: rename
public class HandlersConfigurerDi {

    private static final Logger log = Logger.getLogger(HandlersConfigurerDi.class.getName());

    public static class RegistriesHack {

        @Inject
        public RegistriesHack(com.yahoo.container.Container vespaContainer,
                              ComponentRegistry<AbstractComponent> allComponents,
                              ComponentRegistry<RequestHandler> requestHandlerRegistry,
                              ComponentRegistry<ClientProvider> clientProviderRegistry,
                              ComponentRegistry<ServerProvider> serverProviderRegistry) {
            log.log(LogLevel.DEBUG, "RegistriesHack.init " + System.identityHashCode(this));

            vespaContainer.setComponentRegistry(allComponents);
            vespaContainer.setRequestHandlerRegistry(requestHandlerRegistry);
            vespaContainer.setClientProviderRegistry(clientProviderRegistry);
            vespaContainer.setServerProviderRegistry(serverProviderRegistry);
        }

    }

    private final com.yahoo.container.Container vespaContainer;
    private final OsgiWrapper osgiWrapper;
    private final Container container;

    private volatile ComponentGraph currentGraph = new ComponentGraph(0);

    public HandlersConfigurerDi(SubscriberFactory subscriberFactory,
                                com.yahoo.container.Container vespaContainer,
                                String configId,
                                ComponentDeconstructor deconstructor,
                                Injector discInjector,
                                OsgiFramework osgiFramework) {

        this.vespaContainer = vespaContainer;
        osgiWrapper = new OsgiWrapper(osgiFramework, vespaContainer.getBundleLoader());

        container = new Container(
                subscriberFactory,
                configId,
                deconstructor,
                osgiWrapper
        );
        try {
            runOnceAndEnsureRegistryHackRun(discInjector);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while setting up handlers for the first time.");
        }
    }

    private static class OsgiWrapper extends OsgiImpl implements com.yahoo.container.di.Osgi {

        private final OsgiFramework osgiFramework;
        private final BundleLoader bundleLoader;

        public OsgiWrapper(OsgiFramework osgiFramework, BundleLoader bundleLoader) {
            super(osgiFramework);
            this.osgiFramework = osgiFramework;
            this.bundleLoader = bundleLoader;
        }


        @Override
        public BundleClasses getBundleClasses(ComponentSpecification bundleSpec, Set<String> packagesToScan) {
            //Not written in an OO way since FelixFramework resides in JDisc core which for now is pure java,
            //and to load from classpath one needs classes from scalalib.

            //Temporary hack: Using class name since ClassLoaderOsgiFramework is not available at compile time in this bundle.
            if (osgiFramework.getClass().getName().equals("com.yahoo.application.container.impl.ClassLoaderOsgiFramework")) {
                Bundle syntheticClassPathBundle = first(osgiFramework.bundles());
                ClassLoader classLoader = syntheticClassPathBundle.adapt(BundleWiring.class).getClassLoader();

                return new BundleClasses(
                        syntheticClassPathBundle,
                        OsgiUtil.getClassEntriesForBundleUsingProjectClassPathMappings(classLoader, bundleSpec, packagesToScan));
            } else {
                Bundle bundle = getBundle(bundleSpec);
                if (bundle == null)
                    throw new RuntimeException("No bundle matching " + quote(bundleSpec));

                return new BundleClasses(bundle, OsgiUtil.getClassEntriesInBundleClassPath(bundle, packagesToScan));
            }
        }

        @Override
        public void useBundles(Collection<FileReference> bundles) {
            log.info("Installing bundles from the latest application");

            List<FileReference> fileReferences = new ArrayList<>(bundles);
            int bundlesRemovedOrInstalled = bundleLoader.use(fileReferences);

            if (bundlesRemovedOrInstalled > 0) {
                refreshPackages();
            }
        }
    }

    public void runOnceAndEnsureRegistryHackRun(Injector discInjector) throws InterruptedException {
        currentGraph = container.runOnce(currentGraph, createFallbackInjector(vespaContainer, discInjector));

        RegistriesHack registriesHack = currentGraph.getInstance(RegistriesHack.class);
        assert (registriesHack != null);
        injectDotGraph();
    }

    @SuppressWarnings("deprecation")
    private Injector createFallbackInjector(final com.yahoo.container.Container vespaContainer, Injector discInjector) {
        return discInjector.createChildInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(com.yahoo.container.Container.class).toInstance(vespaContainer);
                bind(com.yahoo.statistics.Statistics.class).toInstance(Statistics.nullImplementation);
                bind(Linguistics.class).toInstance(new SimpleLinguistics());
                bind(com.yahoo.container.protect.FreezeDetector.class).toInstance(
                        new com.yahoo.container.protect.FreezeDetector(
                                new DiagnosticsConfig(new DiagnosticsConfig.Builder().disabled(true))));
                bind(AccessLog.class).toInstance(new AccessLog(new ComponentRegistry<>()));
                bind(Executor.class).toInstance(Executors.newCachedThreadPool(ThreadFactoryFactory.getThreadFactory("HandlersConfigurerDI")));

                if (vespaContainer.getFileAcquirer() != null)
                    bind(com.yahoo.filedistribution.fileacquirer.FileAcquirer.class).toInstance(vespaContainer.getFileAcquirer());
            }
        });
    }

    private void injectDotGraph() {
        try {
            OverviewHandler overviewHandler = currentGraph.getInstance(OverviewHandler.class);
            overviewHandler.setDotGraph(DotGraph.generate(currentGraph));
        } catch (Exception e) {
            log.fine("No overview handler");
        }

    }

    public void reloadConfig(long generation) {
        container.reloadConfig(generation);
    }

    public <T> T getComponent(Class<T> componentClass) {
        return currentGraph.getInstance(componentClass);
    }

    public void shutdown(ComponentDeconstructor deconstructor) {
        container.shutdown(currentGraph, deconstructor);
    }

}
