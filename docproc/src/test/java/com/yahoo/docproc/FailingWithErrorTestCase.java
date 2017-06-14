// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.docproc;

import com.yahoo.document.DataType;
import com.yahoo.document.Document;
import com.yahoo.document.DocumentId;
import com.yahoo.document.DocumentOperation;
import com.yahoo.document.DocumentPut;
import com.yahoo.document.DocumentType;

/**
 * @author <a href="mailto:einarmr@yahoo-inc.com">Einar M R Rosenvinge</a>
 */
public class FailingWithErrorTestCase extends junit.framework.TestCase {

    public void testErrors() {
        DocprocService service = new DocprocService("failing");
        DocumentProcessor first = new ErrorThrowingProcessor();
        service.setCallStack(new CallStack().addLast(first));
        service.setInService(true);

        DocumentType type = new DocumentType("test");
        type.addField("test", DataType.STRING);
        DocumentPut put = new DocumentPut(type, new DocumentId("doc:failing:test:1"));
        put.getDocument().setFieldValue("test", "foobar");

        service.process(put);
        assertEquals(1, service.getQueueSize());
        try {
            while (service.doWork()) { }
            fail("Should have gotten OOME here");
        } catch (Throwable t) {
            //we don't want a finally block in doWork()!
            assertEquals(0, service.getQueueSize());
        }
        assertEquals(0, service.getQueueSize());

    }

    private class ErrorThrowingProcessor extends DocumentProcessor {
        @Override
        public Progress process(Processing processing) {
            throw new OutOfMemoryError("Einar is out of mem");
        }
    }

}
