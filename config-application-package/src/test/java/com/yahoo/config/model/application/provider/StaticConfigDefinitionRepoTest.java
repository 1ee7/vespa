// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.config.model.application.provider;

import com.yahoo.config.model.application.provider.StaticConfigDefinitionRepo;
import com.yahoo.config.model.api.ConfigDefinitionRepo;
import com.yahoo.io.IOUtils;
import com.yahoo.vespa.config.ConfigDefinitionKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author lulf
 * @since 5.10
 */
public class StaticConfigDefinitionRepoTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testThatRepoIsCorrectlyInitialized() throws IOException {
        File topDir = folder.newFolder();
        File defDir = new File(topDir, "classes");
        defDir.mkdir();
        addFile(defDir, new ConfigDefinitionKey("foo", "foons"), "namespace=foons\nval int\n");
        addFile(defDir, new ConfigDefinitionKey("bar", "barns"), "namespace=barns\nval string\n");
        ConfigDefinitionRepo repo = new StaticConfigDefinitionRepo(defDir);
        assertThat(repo.getConfigDefinitions().size(), is(2));
    }

    private void addFile(File defDir, ConfigDefinitionKey key, String content) throws IOException {
        String fileName = key.getNamespace() + "." + key.getName() + ".def";
        File def = new File(defDir, fileName);
        IOUtils.writeFile(def, content, false);
    }
}
