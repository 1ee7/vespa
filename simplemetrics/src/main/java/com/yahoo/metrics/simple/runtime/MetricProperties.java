// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.metrics.simple.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Constants used by Vespa to make the simple metrics implementation available
 * to other components.
 *
 * @author <a href="mailto:steinar@yahoo-inc.com">Steinar Knutsen</a>
 */
public final class MetricProperties {
    private MetricProperties() {
    }

    public static final String BUNDLE_SYMBOLIC_NAME = "simplemetrics";
}
