package org.jboss.pnc.causeway.pncclient;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.jboss.pnc.causeway.pncclient.PncClient.ProductReleaseEndpoint;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

public class PncClientIT {
    private ResteasyClient client;
    private String pncUrl;
    private Integer productReleaseId;
    private Response response;

    @Before
    public void setUp() throws Exception {
        client = new ResteasyClientBuilder().build();
        pncUrl = "http://pnc-orch-master-nightly.cloud.pnc.devel.engineering.redhat.com/pnc-rest/rest";
        productReleaseId = 1;
    }

    @After
    public void after() {
        if (response != null) {
            response.close();
        }
    }

    @Test
    public void testReadProductRelease() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl + "/product-releases/" + productReleaseId);
        response = target.request().get();

        Singleton<ProductReleaseRest> responseEntity = response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {});

        assertEquals(productReleaseId, responseEntity.getContent().getId());
    }

    @Test
    public void testReadProductReleaseBuildConfigs() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl + "/product-releases/" + productReleaseId + "/distributed-build-records-ids");
        response = target.request().get();

        Page<BuildRecordRest> responseEntity = response.readEntity(new GenericType<Page<BuildRecordRest>>() {});

        assertArrayEquals(asList(1, 2).toArray(), extractIds(responseEntity.getContent()).toArray());
    }

    private List<Integer> extractIds(Collection<BuildRecordRest> content) {
        return content.stream().map(record -> record.getId()).collect(toList());
    }

    @Test
    public void testReadProductReleaseUsingProxy() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);

        response = endpoint.getSpecific(productReleaseId);
        ProductReleaseRest entity = ((Singleton<ProductReleaseRest>) response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {})).getContent();

        assertEquals(productReleaseId, entity.getId());
    }

    @Test
    public void testReadProductReleaseBuildConfigurationsUsingProxy() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);

        response = endpoint.getAllBuildsInDistributedRecordsetOfProductRelease(productReleaseId);
        Page<BuildRecordRest> ids = ((Page<BuildRecordRest>) response.readEntity(new GenericType<Page<BuildRecordRest>>() {}));

        assertArrayEquals(asList(1, 2).toArray(), extractIds(ids.getContent()).toArray());
    }

}