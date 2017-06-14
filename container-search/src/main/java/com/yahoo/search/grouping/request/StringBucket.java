// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.grouping.request;

/**
 * This class represents a {@link String} bucket in a {@link PredefinedFunction}.
 *
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class StringBucket extends BucketValue {

    /**
     * Get the next distinct value.
     *
     * @param value The base value.
     * @return the next value.
     */
    public static StringValue nextValue(StringValue value) {
        return new StringValue(value.getValue() + " ");
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param from          The from-value to assign to this.
     * @param to            The to-value to assign to this.
     */
    public StringBucket(String from, String to) {
        super(new StringValue(from), new StringValue(to));
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param from          The from-value to assign to this.
     * @param to            The to-value to assign to this.
     */
    public StringBucket(ConstantValue<?> from, ConstantValue<?> to) {
        super(from, to);
    }
}
