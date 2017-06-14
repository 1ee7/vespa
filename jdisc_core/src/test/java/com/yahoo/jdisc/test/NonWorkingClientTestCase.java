// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.jdisc.test;

import com.yahoo.jdisc.service.ClientProvider;
import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class NonWorkingClientTestCase {

    @Test
    public void requireThatHandleRequestThrowsException() {
        ClientProvider client = new NonWorkingClientProvider();
        try {
            client.handleRequest(null, null);
            fail();
        } catch (UnsupportedOperationException e) {

        }
    }

    @Test
    public void requireThatHandleTimeoutThrowsException() {
        ClientProvider client = new NonWorkingClientProvider();
        try {
            client.handleTimeout(null, null);
            fail();
        } catch (UnsupportedOperationException e) {

        }
    }

    @Test
    public void requireThatStartDoesNotThrow() {
        ClientProvider client = new NonWorkingClientProvider();
        client.start();
    }

    @Test
    public void requireThatRetainDoesNotThrow() {
        ClientProvider client = new NonWorkingClientProvider();
        client.release();
    }

    @Test
    public void requireThatReleaseDoesNotThrow() {
        ClientProvider client = new NonWorkingClientProvider();
        client.release();
    }
}
