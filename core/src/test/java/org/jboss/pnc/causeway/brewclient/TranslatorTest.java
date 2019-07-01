/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.brewclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.util.KojiObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.BuildArtifacts;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.MavenBuild;
import org.jboss.pnc.causeway.rest.model.MavenBuiltArtifact;
import org.jboss.pnc.enums.ArtifactQuality;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class TranslatorTest {
    private static final CausewayConfig config = new CausewayConfig();
    private static final BuildTranslator bt = new BuildTranslatorImpl(config);
    private static final ObjectMapper mapper = new KojiObjectMapper();

    @BeforeClass
    public static void setUp() {
        config.setPnclBuildsURL("http://example.com/build-records/");
        mapper.registerSubtypes(MavenBuild.class, MavenBuiltArtifact.class);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    public void testReadBuildArtifacts() throws Exception {
        String json = readResponseBodyFromTemplate("build-dto-1.json");

        org.jboss.pnc.dto.Build build = mapper.readValue(json, org.jboss.pnc.dto.Build.class);
        BuildArtifacts artifacts = new BuildArtifacts();
        artifacts.buildArtifacts.add(newArtifact(2369, "org.apache.geronimo.specs", "geronimo-annotation_1.0_spec", "1.1.1.redhat-1", "pom"));
        artifacts.buildArtifacts.add(newArtifact(2370, "org.apache.geronimo.specs", "geronimo-annotation_1.0_spec", "1.1.1.redhat-1", "jar"));
        artifacts.buildArtifacts.add(newArtifact(2371, "org.apache.geronimo.specs", "geronimo-annotation_1.0_spec", "1.1.1.redhat-1", "tar.gz", "project-sources"));

        artifacts.dependencies.add(newArtifact(7, "org.apache.maven", "maven-project", "2.0.6", "pom"));
        artifacts.dependencies.add(newArtifact(9, "org.apache.maven.shared", "maven-shared-io", "1.1", "jar"));
        artifacts.dependencies.add(newArtifact(10, "xml-apis", "xml-apis", "1.0.b2", "jar")); 

        KojiImport out = bt.translate(new BrewNVR("g:a", "1.2.3", "1"), build, artifacts, "foo-bar-logs", "joe");

        mapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        String jsonOut = mapper.writeValueAsString(out);
        System.out.println("RESULTA:\n" + jsonOut);
    }

    @Test
    public void testReadBuild() throws Exception {
        String json = readResponseBodyFromTemplate("build.json");

        Build build = mapper.readValue(json, Build.class);

        KojiImport out = bt.translate(new BrewNVR("g:a", "1.2.3", "1"), build, "joe");

        mapper.enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
        String jsonOut = mapper.writeValueAsString(out);
        System.out.println("RESULTA:\n" + jsonOut);
    }

    private static org.jboss.pnc.causeway.pncclient.BuildArtifacts.PncArtifact newArtifact(int id, String groupId, String artifactId, String version, String type) {
        return newArtifact(id, groupId, artifactId, version, type, null);
    }

    private static BuildArtifacts.PncArtifact newArtifact(int id, String groupId, String artifactId, String version, String type, String specifier) {
        final String filename;
        final String identifier;
        if (specifier == null) {
            filename = artifactId+"-"+version+"."+type;
            identifier = groupId+":"+artifactId+":"+type+":"+version;
        }else{
            filename = artifactId+"-"+version + "-" + specifier+"."+type;
            identifier = groupId+":"+artifactId+":"+type+":"+version+ ":" + specifier;
        }
        final String path = groupId.replace('.', '/')+"/"+artifactId+"/"+version+"/"+filename;
        return new BuildArtifacts.PncArtifact(id,
                identifier, filename,
                "bedf8af1b107b36c72f52009e6fcc768",
                "http://ulozto.cz/api/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/"+path,
                13245,
                ArtifactQuality.NEW);
    }

    private String readResponseBodyFromTemplate(String name) throws IOException {
        String folderName = getClass().getPackage().getName().replace(".", "/");
        try (InputStream inputStream = getContextClassLoader().getResourceAsStream(folderName + "/" + name)) {
            return StringUtils.join(IOUtils.readLines(inputStream, Charset.forName("utf-8")), "\n");
        }
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
