package org.jboss.pnc.causeway.pncclient;

import static org.junit.Assert.assertEquals;

import org.jboss.pnc.rest.restmodel.ProductMilestoneRest;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SingletonJsonConversionIT {

    private final Random generator = new Random();

    @Test
    public void testGeneratedJson() throws Exception {
        ProductMilestoneRest productMilestone = createProductMilestone();

        String json = convertToJson(new Singleton<>(productMilestone));

        String expectedJson = "{\"content\":{\"id\":" + productMilestone.getId() + ",\"version\":null,\"endDate\":null,\"startingDate\":null,\"plannedEndDate\":null,\"downloadUrl\":null,\"issueTrackerUrl\":null,\"productVersionId\":null,\"performedBuilds\":null,\"distributedArtifactIds\":null,\"productReleaseId\":null}}";
        assertEquals(expectedJson, json);
    }

    private ProductMilestoneRest createProductMilestone() {
        ProductMilestoneRest productMilestone = new ProductMilestoneRest();
        productMilestone.setId(createRandomInt());
        return productMilestone;
    }

    private String convertToJson(Object object) throws IOException {
        OutputStream stream = new ByteArrayOutputStream();
        new ObjectMapper().writeValue(stream, object);
        stream.close();
        return stream.toString();
    }

    @Test
    public void testParsedJson() throws Exception {
        ProductMilestoneRest productMilestone = createProductMilestone();

        String json = "{\"content\":{\"id\":" + productMilestone.getId() + ",\"version\":null,\"endDate\":null,\"startingDate\":null,\"plannedEndDate\":null,\"downloadUrl\":null,\"issueTrackerUrl\":null,\"productVersionId\":null,\"performedBuildRecordSetId\":null,\"distributedBuildRecordSetId\":null,\"productReleaseId\":null}}";


        Singleton<ProductMilestoneRest> entity = convertFromJson(json, new TypeReference<Singleton<ProductMilestoneRest>>() { });
        ProductMilestoneRest parsed = entity.getContent();

        assertEquals(parsed.getId(), productMilestone.getId());
    }

    private <T> T convertFromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeReference) throws IOException {
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
