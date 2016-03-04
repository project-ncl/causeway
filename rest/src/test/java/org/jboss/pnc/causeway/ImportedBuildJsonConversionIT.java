package org.jboss.pnc.causeway;

import static org.junit.Assert.assertEquals;

import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.ImportedBuild;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        BrewBuild brewBuild = new BrewBuild(createRandomLong(), brewNvr);
        return new ImportedBuild(createRandomLong(), brewBuild);
    }

    private String convertToJson(Object object) throws IOException {
        OutputStream stream = new ByteArrayOutputStream();
        new ObjectMapper().writeValue(stream, object);
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
