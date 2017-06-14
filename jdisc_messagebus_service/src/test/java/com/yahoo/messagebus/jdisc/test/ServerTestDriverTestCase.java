// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus.jdisc.test;

import com.yahoo.jdisc.test.NonWorkingRequestHandler;
import com.yahoo.jrt.ListenFailedException;
import com.yahoo.jrt.slobrok.server.Slobrok;
import com.yahoo.messagebus.test.SimpleProtocol;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class ServerTestDriverTestCase {

    @Test
    public void requireThatFactoryMethodsWork() throws ListenFailedException {
        ServerTestDriver driver = ServerTestDriver.newInstance(new NonWorkingRequestHandler());
        assertNotNull(driver);
        assertTrue(driver.close());

        driver = ServerTestDriver.newInstanceWithProtocol(new SimpleProtocol(), new NonWorkingRequestHandler());
        assertNotNull(driver);
        assertTrue(driver.close());

        Slobrok slobrok = new Slobrok();
        driver = ServerTestDriver.newInstanceWithExternSlobrok(slobrok.configId(), new NonWorkingRequestHandler());
        assertNotNull(driver);
        assertTrue(driver.close());
    }
}
