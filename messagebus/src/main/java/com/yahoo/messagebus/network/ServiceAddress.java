// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.messagebus.network;

/**
 * This interface represents an abstract network service; i.e. somewhere to send messages. An instance of this is
 * retrieved by calling {@link Network#allocServiceAddress(com.yahoo.messagebus.routing.RoutingNode)}.
 *
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public interface ServiceAddress {
    // empty
}


