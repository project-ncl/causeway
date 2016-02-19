package org.jboss.pnc.causeway.pncclient;

import static org.junit.Assert.assertEquals;

import org.jboss.pnc.causeway.pncclient.PncClient.ProductReleaseEndpoint;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

public class PncClientIT {
    private ResteasyClient client;
    private String pncUrl;
    private Integer productReleaseId;

    @Before
    public void setUp() throws Exception {
        client = new ResteasyClientBuilder().build();
        pncUrl = "http://ncl-nightly.stage.engineering.redhat.com/pnc-rest/rest";
        productReleaseId = 1;
    }

    @Test
    public void testReadProductRelease() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl + "/product-releases/" + productReleaseId);
        Response response = target.request().get();

        Singleton<ProductReleaseRest> responseEntity = response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {});

        assertEquals(productReleaseId, responseEntity.getContent().getId());
        response.close();
    }

    @Test
    public void testReadProductReleaseUsingProxy() throws Exception {
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);

        ProductReleaseRest productRelease = endpoint.getEntity(productReleaseId).getContent();
        assertEquals(productReleaseId, productRelease.getId());
    }


}