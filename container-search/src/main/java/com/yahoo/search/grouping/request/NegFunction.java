// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.grouping.request;

import java.util.Arrays;

/**
 * This class represents a negate-function in a {@link GroupingExpression}. It evaluates to a number that equals the
 * negative of the results of the argument.
 *
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class NegFunction extends FunctionNode {

    /**
     * Constructs a new instance of this class.
     *
     * @param exp The expression to evaluate, must evaluate to a number.
     */
    public NegFunction(GroupingExpression exp) {
        super("neg", Arrays.asList(exp));
    }
}

