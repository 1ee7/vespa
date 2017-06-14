// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.container.handler;


/**
 * <p>A wrapper for timing of events in the course of a query evaluation. Advanced
 * database searches and similar could use these structures as well.</p>
 *
 * <p>Not adding this object will lead to less exact entries in the query
 * log. It is legal to set only queryStartTime and set the other values
 * to zero.</p>
 *
 * <p>If you do not understand the fields, just avoid creating this object
 * in you handler.</p>
 *
 * @author <a href="mailto:steinar@yahoo-inc.com">Steinar Knutsen</a>
 */
public class Timing {

    protected long summaryStartTime;

    protected long queryStartTime;

    protected long timeout;

    /**
     * Do consider using
     * com.yahoo.search.handler.SearchResponse.createTiming(Query, Result) if
     * instead of this constructor if you are creating a Timing instance in a
     * search context.
     *
     * @param summaryStartTime when fetching of document contents started
     * @param queryStartTime when the request started
     * @param timeout maximum allowed lifetime of the request
     */
    public Timing(long summaryStartTime, long ignored, long queryStartTime, long timeout) {
        super();
        this.summaryStartTime = summaryStartTime;
        this.queryStartTime = queryStartTime;
        this.timeout = timeout;
    }

    /**
     * Summary start time is when the fetching of hit/document contents
     * start. (As opposed to just analyzing hit relevancies.)
     *
     * @return the start time of summary fetching or 0
     */
    public long getSummaryStartTime() {
        return summaryStartTime;
    }

    /**
     * This is the start of the server's evaluation of a query
     * or request, after full reception of it through the network.
     * It will usually be intialized implicitly from the value
     * generated by the com.yahoo.search.Query constructor.
     *
     * @return the starting time of query construction
     */
    public long getQueryStartTime() {
        return queryStartTime;
    }

    /**
     * This is the timeout that was given to this query.
     *
     * @return The timeout given allowed to the query.
     */
    public long getTimeout() {
        return timeout;
    }
}
