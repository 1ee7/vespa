// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.configchange;

import com.yahoo.config.model.api.ConfigChangeAction;
import com.yahoo.config.model.api.ConfigChangeRefeedAction;
import com.yahoo.config.model.api.ServiceInfo;

import java.util.*;

/**
 * Represents all actions to re-feed document types in order to handle config changes.
 *
 * @author geirst
 * @since 5.44
 */
public class RefeedActions {

    public static class Entry {

        private final String name;
        private final boolean allowed;
        private final String documentType;
        private final String clusterName;
        private final Set<ServiceInfo> services = new LinkedHashSet<>();
        private final Set<String> messages = new TreeSet<>();

        private Entry(String name, boolean allowed, String documentType, String clusterName) {
            this.name = name;
            this.allowed = allowed;
            this.documentType = documentType;
            this.clusterName = clusterName;
        }

        private Entry addService(ServiceInfo service) {
            services.add(service);
            return this;
        }

        private Entry addMessage(String message) {
            messages.add(message);
            return this;
        }

        public String name() { return name; }

        public boolean allowed() { return allowed; }

        public String getDocumentType() { return documentType; }

        public String getClusterName() { return clusterName; }

        public Set<ServiceInfo> getServices() { return services; }

        public Set<String> getMessages() { return messages; }

    }

    private Entry addEntry(String name, boolean allowed, String documentType, ServiceInfo service) {
        String clusterName = service.getProperty("clustername").orElse("");
        String entryId = name + "." + allowed + "." + clusterName + "." + documentType;
        Entry entry = actions.get(entryId);
        if (entry == null) {
            entry = new Entry(name, allowed, documentType, clusterName);
            actions.put(entryId, entry);
        }
        return entry;
    }

    private final Map<String, Entry> actions = new TreeMap<>();

    public RefeedActions() {
    }

    public RefeedActions(List<ConfigChangeAction> actions) {
        for (ConfigChangeAction action : actions) {
            if (action.getType().equals(ConfigChangeAction.Type.REFEED)) {
                ConfigChangeRefeedAction refeedAction = (ConfigChangeRefeedAction) action;
                for (ServiceInfo service : refeedAction.getServices()) {
                    addEntry(refeedAction.name(), refeedAction.allowed(), refeedAction.getDocumentType(), service).
                            addService(service).
                            addMessage(action.getMessage());
                }
            }
        }
    }

    public List<Entry> getEntries() {
        return new ArrayList<>(actions.values());
    }

    public String format() {
        return new RefeedActionsFormatter(this).format();
    }

    public boolean isEmpty() {
        return getEntries().isEmpty();
    }
}
