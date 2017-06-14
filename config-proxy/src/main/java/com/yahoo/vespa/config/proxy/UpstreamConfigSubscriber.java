// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.proxy;

import com.yahoo.config.subscription.ConfigSource;
import com.yahoo.config.subscription.ConfigSourceSet;
import com.yahoo.config.subscription.impl.GenericConfigHandle;
import com.yahoo.config.subscription.impl.GenericConfigSubscriber;
import com.yahoo.config.subscription.impl.JRTConfigRequester;
import com.yahoo.log.LogLevel;
import com.yahoo.yolean.Exceptions;
import com.yahoo.vespa.config.RawConfig;
import com.yahoo.vespa.config.TimingValues;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hmusum
 * @since 5.5
 */
public class UpstreamConfigSubscriber implements Subscriber {
    private final static Logger log = Logger.getLogger(UpstreamConfigSubscriber.class.getName());

    private final RawConfig config;
    private final ClientUpdater clientUpdater;
    private final ConfigSource configSourceSet;
    private final TimingValues timingValues;
    private Map<ConfigSourceSet, JRTConfigRequester> requesterPool;
    private GenericConfigSubscriber subscriber;
    private GenericConfigHandle handle;

    UpstreamConfigSubscriber(RawConfig config,
                             ClientUpdater clientUpdater,
                             ConfigSource configSourceSet,
                             TimingValues timingValues,
                             Map<ConfigSourceSet, JRTConfigRequester> requesterPool) {
        this.config = config;
        this.clientUpdater = clientUpdater;
        this.configSourceSet = configSourceSet;
        this.timingValues = timingValues;
        this.requesterPool = requesterPool;
    }

    void subscribe() {
        subscriber = new GenericConfigSubscriber(requesterPool);
        handle = subscriber.subscribe(config.getKey(), config.getDefContent(), configSourceSet, timingValues);
    }

    @Override
    public void run() {
        do {
            if (! subscriber.nextGeneration()) continue;

            try {
                updateWithNewConfig(handle);
            } catch (Exception e) {  // To avoid thread throwing exception and loop never running this again
                log.log(LogLevel.WARNING, "Got exception: " + Exceptions.toMessageString(e));
            } catch (Throwable e) {
                com.yahoo.protect.Process.logAndDie("Got error, exiting: " + Exceptions.toMessageString(e));
            }
        } while (!subscriber.isClosed());
    }

    private void updateWithNewConfig(GenericConfigHandle handle) {
        final RawConfig newConfig = handle.getRawConfig();
        if (log.isLoggable(LogLevel.DEBUG)) {
            log.log(LogLevel.DEBUG, "config to be returned for '" + newConfig.getKey() +
                    "', generation=" + newConfig.getGeneration() +
                    ", payload=" + newConfig.getPayload());
        }
        // memoryCache.put(); TODO: Add here later and remove in ClientUpdater
        clientUpdater.updateSubscribers(newConfig);
    }

    @Override
    public void cancel() {
        if (subscriber != null) {
            subscriber.close();
        }
    }
}
