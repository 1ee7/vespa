// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.search.pagetemplates.test;

import com.yahoo.search.Query;
import com.yahoo.search.pagetemplates.PageTemplate;
import com.yahoo.search.pagetemplates.PageTemplateRegistry;
import com.yahoo.search.pagetemplates.PageTemplateSearcher;
import com.yahoo.search.pagetemplates.config.PageTemplateXMLReader;
import com.yahoo.search.searchchain.Execution;

/**
 * @author bratseth
 */
public class SourceParametersTestCase extends junit.framework.TestCase {

    private static final String root="src/test/java/com/yahoo/search/pagetemplates/test/";

    public void testSourceParametersWithSourcesDeterminedByTemplate() {
        // Create the page template
        PageTemplateRegistry pageTemplateRegistry=new PageTemplateRegistry();
        PageTemplate page=importPage("SourceParameters.xml");
        pageTemplateRegistry.register(page);
        PageTemplateSearcher s=new PageTemplateSearcher(pageTemplateRegistry);
        Query query=new Query("?query=foo&page.id=SourceParameters&page.resolver=native.deterministic");
        new Execution(s, Execution.Context.createContextStub()).search(query);
        assertEquals("source1p1Value",query.properties().get("source.source1.p1"));
        assertEquals("source1p1Value",query.properties().get("source.source1.p1"));
        assertEquals("source2p1Value",query.properties().get("source.source2.p1"));
        assertEquals("source2p3Value",query.properties().get("source.source2.p3"));
        assertEquals("source3p1Value",query.properties().get("source.source3.p1"));
        assertEquals("We get the correct number of parameters",5,query.properties().listProperties("source").size());
    }

    public void testSourceParametersWithSourcesDeterminedByParameter() {
        // Create the page template
        PageTemplateRegistry pageTemplateRegistry=new PageTemplateRegistry();
        PageTemplate page=importPage("SourceParameters.xml");
        pageTemplateRegistry.register(page);
        PageTemplateSearcher s=new PageTemplateSearcher(pageTemplateRegistry);
        Query query=new Query("?query=foo&page.id=SourceParameters&model.sources=source1,source3&page.resolver=native.deterministic");
        new Execution(s, Execution.Context.createContextStub()).search(query);
        assertEquals("source1p1Value",query.properties().get("source.source1.p1"));
        assertEquals("source1p1Value",query.properties().get("source.source1.p1"));
        assertEquals("source3p1Value",query.properties().get("source.source3.p1"));
        assertEquals("We get the correct number of parameters",3,query.properties().listProperties("source").size());
    }

    protected PageTemplate importPage(String name) {
        PageTemplate template=new PageTemplateXMLReader().readFile(root + name);
        assertNotNull("Could look up read template '" + name + "'",template);
        return template;
    }

}
