package org.jboss.pnc.causeway.pncclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.PncClient.ProductReleaseEndpoint;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collection;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;


public class PncClientWIT {
    public static final String CONTEXT_URL = "/pnc-rest/rest";
    private ResteasyClient client;
    private String pncUrl;
    private Integer productReleaseId;

    public static int port = Integer.valueOf(System.getProperty("test.port", "8089"));

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(port);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;
    private PncClient pncClient;
    @Mock
    private CausewayConfig config;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pncClient = new PncClient(config, null);
        client = new ResteasyClientBuilder().build();
        pncUrl = "http://localhost:"  + port + CONTEXT_URL;
        productReleaseId = 1;
        when(config.getPnclURL()).thenReturn(pncUrl);
    }

    @AfterClass
    public static void after () {
        if (wireMockRule != null)
            wireMockRule.shutdown();
    }

    @Test
    public void testReadProductRelease() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId;
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\":{\"id\":1,\"version\":\"1.0.0.GA\",\"releaseDate\":null,\"downloadUrl\":null,\"issueTrackerUrl\":null,\"productVersionId\":1,\"productMilestoneId\":1,\"supportLevel\":\"EARLYACCESS\"}}")));

        ResteasyWebTarget target = client.target(pncUrl + relativeUrl);
        Response response = target.request().get();

        Singleton<ProductReleaseRest> responseEntity = response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {});

        assertEquals(productReleaseId, responseEntity.getContent().getId());
        response.close();
    }

    @Test
    public void testReadProductReleaseUsingProxy() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId;
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"content\":{\"id\":1,\"version\":\"1.0.0.GA\",\"releaseDate\":null,\"downloadUrl\":null,\"issueTrackerUrl\":null,\"productVersionId\":1,\"productMilestoneId\":1,\"supportLevel\":\"EARLYACCESS\"}}")));
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);
        Response response = endpoint.getSpecific(productReleaseId);
        ProductReleaseRest entity = ((Singleton<ProductReleaseRest>) response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {})).getContent();

        assertEquals(productReleaseId, entity.getId());
    }

    @Test
    public void testReadProductReleaseBuildConfigIds() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId + "/distributed-build-records-ids";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageIndex\":0,\"pageSize\":1,\"totalPages\":1,\"content\":[1]}")));
        ResteasyWebTarget target = client.target(pncUrl + relativeUrl);
        Response response = target.request().get();

        Page<Integer> responseEntity = response.readEntity(new GenericType<Page<Integer>>() {});

        assertArrayEquals(asList(1).toArray(), responseEntity.getContent().toArray());
        response.close();
    }

    @Test
    public void testReadProductReleaseBuildConfigurationsUsingProxy() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId + "/distributed-build-records-ids";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageIndex\":0,\"pageSize\":1,\"totalPages\":1,\"content\":[1]}")));
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);

        Response response = endpoint.getAllBuildsInDistributedRecordsetOfProductRelease(productReleaseId);
        Page<Integer> ids = ((Page<Integer>) response.readEntity(new GenericType<Page<Integer>>() {}));

        assertArrayEquals(asList(1).toArray(), ids.getContent().toArray());
        target.getResteasyClient().close();
    }

    @Test
    public void testClientReadProductReleaseBuildConfigurationsUsingProxy() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId + "/distributed-build-records-ids";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageIndex\":0,\"pageSize\":1,\"totalPages\":1,\"content\":[1]}")));
        ResteasyWebTarget target = client.target(pncUrl);
        ProductReleaseEndpoint endpoint = target.proxy(ProductReleaseEndpoint.class);

        Collection<Integer> ids = pncClient.findBuildIdsOfProductRelease(productReleaseId);

        assertArrayEquals(asList(1).toArray(), ids.toArray());
        target.getResteasyClient().close();
    }
}