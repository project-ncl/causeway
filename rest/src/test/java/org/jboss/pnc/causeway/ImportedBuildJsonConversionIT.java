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
package org.jboss.pnc.causeway;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.ImportedBuild;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class ImportedBuildJsonConversionIT {

    private final Random generator = new Random();

    @Test
    public void testGeneratedJson() throws Exception {
        ImportedBuild importedBuild = createImportedBuild();

        String json = convertToJson(importedBuild);

        String expectedJson = "{\"buildId\":" + importedBuild.getBuildId() + ",\"brewBuild\":{\"id\":" + importedBuild.getBrewBuild().getId() + ",\"name\":\"" + importedBuild.getBrewBuild().getName() + "\",\"version\":\"" + importedBuild.getBrewBuild().getVersion() + "\",\"release\":\"" + importedBuild.getBrewBuild().getRelease() + "\"}}";
        assertEquals(expectedJson, json);
    }

    private ImportedBuild createImportedBuild() {
        BrewNVR brewNvr = new BrewNVR(createRandomString(), createRandomString(), createRandomString());
        BrewBuild brewBuild = new BrewBuild(createRandomInt(), brewNvr);
        return new ImportedBuild(createRandomLong(), brewBuild);
    }

    private String convertToJson(Object object) throws IOException {
        OutputStream stream = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE));

        mapper.writeValue(stream, object);
        stream.close();
        return stream.toString();
    }

    @Test
    public void testParsedJson() throws Exception {
        ImportedBuild importedBuild = createImportedBuild();

        String json = "{\"buildId\":" + importedBuild.getBuildId() + ",\"brewBuild\":{\"name\":\"" + importedBuild.getBrewBuild().getName() + "\",\"version\":\"" + importedBuild.getBrewBuild().getVersion() + "\",\"release\":\"" + importedBuild.getBrewBuild().getRelease() + "\",\"id\":" + importedBuild.getBrewBuild().getId() + "}}";

        ImportedBuild parsed = convertFromJson(json, new TypeReference<ImportedBuild>() { });

        assertEquals(parsed.getBuildId(), importedBuild.getBuildId());
    }

    private <T> T convertFromJson(String json, TypeReference<T> typeReference) throws IOException {
        return new ObjectMapper().readValue(json, typeReference);
    }


    private Integer createRandomInt() {
        return generator.nextInt();
    }
    private Long createRandomLong() {
        return generator.nextLong();
    }
    private String createRandomString() {
        return Long.toString(createRandomLong());
    }
}
