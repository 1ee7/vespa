// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.indexinglanguage;

import com.yahoo.document.Document;
import com.yahoo.document.DocumentUpdate;

import java.util.List;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public interface AdapterFactory {

    public DocumentAdapter newDocumentAdapter(Document doc);

    public List<UpdateAdapter> newUpdateAdapterList(DocumentUpdate upd);
}
