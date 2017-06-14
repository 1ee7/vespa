// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.docprocs.indexing;

import com.yahoo.document.Document;
import com.yahoo.document.DocumentUpdate;
import com.yahoo.document.Field;
import com.yahoo.document.annotation.SpanTrees;
import com.yahoo.document.datatypes.Array;
import com.yahoo.document.datatypes.FieldValue;
import com.yahoo.document.datatypes.MapFieldValue;
import com.yahoo.document.datatypes.StringFieldValue;
import com.yahoo.document.datatypes.Struct;
import com.yahoo.document.datatypes.StructuredFieldValue;
import com.yahoo.document.datatypes.WeightedSet;
import com.yahoo.document.fieldpathupdate.AssignFieldPathUpdate;
import com.yahoo.document.fieldpathupdate.FieldPathUpdate;
import com.yahoo.document.update.FieldUpdate;
import com.yahoo.document.update.MapValueUpdate;
import com.yahoo.document.update.ValueUpdate;
import com.yahoo.vespa.indexinglanguage.AdapterFactory;
import com.yahoo.vespa.indexinglanguage.expressions.Expression;

import java.util.*;

/**
 * @author <a href="mailto:simon@yahoo-inc.com">Simon Thoresen</a>
 */
public class DocumentScript {

    private final String documentType;
    private final Set<String> inputFields;
    private final Expression expression;

    public DocumentScript(String documentType, Collection<String> inputFields, Expression expression) {
        this.documentType = documentType;
        this.inputFields = new HashSet<>(inputFields);
        this.expression = expression;
    }

    public Expression getExpression() { return expression; }
    public Document execute(AdapterFactory adapterFactory, Document document) {
        for (Iterator<Map.Entry<Field, FieldValue>> it = document.iterator(); it.hasNext(); ) {
            Map.Entry<Field, FieldValue> entry = it.next();
            requireThatFieldIsDeclaredInDocument(entry.getKey());
            removeAnyLinguisticsSpanTree(entry.getValue());
        }
        return expression.execute(adapterFactory, document);
    }

    public DocumentUpdate execute(AdapterFactory adapterFactory, DocumentUpdate update) {
        for (FieldUpdate fieldUpdate : update.getFieldUpdates()) {
            requireThatFieldIsDeclaredInDocument(fieldUpdate.getField());
            for (ValueUpdate<?> valueUpdate : fieldUpdate.getValueUpdates()) {
                removeAnyLinguisticsSpanTree(valueUpdate);
            }
        }
        for (FieldPathUpdate fieldUpdate : update.getFieldPathUpdates()) {
            requireThatFieldIsDeclaredInDocument(fieldUpdate.getFieldPath().get(0).getFieldRef());
            if (fieldUpdate instanceof AssignFieldPathUpdate) {
                removeAnyLinguisticsSpanTree(((AssignFieldPathUpdate)fieldUpdate).getFieldValue());
            }
        }
        return Expression.execute(expression, adapterFactory, update);
    }

    private void requireThatFieldIsDeclaredInDocument(Field field) {
        if (field != null && !inputFields.contains(field.getName())) {
            throw new IllegalArgumentException("Field '" + field.getName() + "' is not part of the declared document " +
                                               "type '" + documentType + "'.");
        }
    }

    private void removeAnyLinguisticsSpanTree(ValueUpdate<?> valueUpdate) {
        if (valueUpdate instanceof MapValueUpdate) {
            removeAnyLinguisticsSpanTree(((MapValueUpdate)valueUpdate).getUpdate());
        } else {
            removeAnyLinguisticsSpanTree(valueUpdate.getValue());
        }
    }

    private void removeAnyLinguisticsSpanTree(FieldValue value) {
        if (value instanceof StringFieldValue) {
            ((StringFieldValue)value).removeSpanTree(SpanTrees.LINGUISTICS);
        } else if (value instanceof Array) {
            Array<?> arr = (Array)value;
            for (Object obj : arr.getValues()) {
                removeAnyLinguisticsSpanTree((FieldValue)obj);
            }
        } else if (value instanceof WeightedSet) {
            WeightedSet<?> wset = (WeightedSet)value;
            for (Object obj : wset.keySet()) {
                removeAnyLinguisticsSpanTree((FieldValue)obj);
            }
        } else if (value instanceof MapFieldValue) {
            MapFieldValue<?,?> map = (MapFieldValue)value;
            for (Map.Entry<?,?> entry : map.entrySet()) {
                removeAnyLinguisticsSpanTree((FieldValue)entry.getKey());
                removeAnyLinguisticsSpanTree((FieldValue)entry.getValue());
            }
        } else if (value instanceof StructuredFieldValue) {
            StructuredFieldValue struct = (StructuredFieldValue)value;
            for (Iterator<Map.Entry<Field, FieldValue>> it = struct.iterator(); it.hasNext();) {
                removeAnyLinguisticsSpanTree(it.next().getValue());
            }
        }
    }
}
