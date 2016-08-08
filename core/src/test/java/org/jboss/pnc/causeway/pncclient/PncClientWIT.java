package org.jboss.pnc.causeway.pncclient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static java.util.Arrays.asList;

import static org.apache.commons.lang.StringUtils.join;
import static org.jboss.pnc.causeway.pncclient.PncClient.extractIds;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.PncClient.ProductReleaseEndpoint;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.jboss.pnc.rest.restmodel.response.Singleton;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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

    @After
    public void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
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
                        .withBody(readResponseBodyFromTemplate("product-releases-1.json"))));

        Response response = client.target(pncUrl + relativeUrl).request().get();
        Singleton<ProductReleaseRest> responseEntity = response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {});

        assertEquals(productReleaseId, responseEntity.getContent().getId());
    }

    @Test
    public void testReadProductReleaseUsingProxy() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId;
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("product-releases-1.json"))));

        ProductReleaseEndpoint endpoint = client.target(pncUrl).proxy(ProductReleaseEndpoint.class);
        Response response = endpoint.getSpecific(productReleaseId);
        ProductReleaseRest entity = ((Singleton<ProductReleaseRest>) response.readEntity(new GenericType<Singleton<ProductReleaseRest>>() {})).getContent();

        assertEquals(productReleaseId, entity.getId());
    }

    @Test
    public void testReadProductReleaseBuildConfigurations() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId + "/distributed-build-records-ids";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("product-release-distributed-build-records-ids-1.json"))));

        ProductReleaseEndpoint endpoint = client.target(pncUrl).proxy(ProductReleaseEndpoint.class);
        Response response = endpoint.getAllBuildsInDistributedRecordsetOfProductRelease(productReleaseId);
        Page<BuildRecordRest> ids = ((Page<BuildRecordRest>) response.readEntity(new GenericType<Page<BuildRecordRest>>() {}));

        assertArrayEquals(asList(1, 2).toArray(), extractIds(ids.getContent()).toArray());
    }

    @Test
    public void testReadProductReleaseBuildIds() throws Exception {
        String relativeUrl = "/product-releases/" + productReleaseId + "/distributed-build-records-ids";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("product-release-distributed-build-records-ids-1.json"))));

        Collection<Integer> ids = pncClient.findBuildIdsOfProductRelease(productReleaseId);

        assertArrayEquals(asList(1, 2).toArray(), ids.toArray());
    }


    @Test
    public void testReadBuildArtifacts() throws Exception {
        Integer buildId = 61;

        String relativeBuiltUrl = "/build-records/" + buildId + "/built-artifacts?pageIndex=0&pageSize=" + PncClient.MAX_ARTIFACTS + "&sort=&q=";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeBuiltUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("build-records-61-built-artifacts-1.json"))));

        String relativeDependencyUrl = "/build-records/" + buildId + "/dependency-artifacts?pageIndex=0&pageSize=" + PncClient.MAX_ARTIFACTS + "&sort=&q=";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeDependencyUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("build-records-61-dependency-artifacts-1.json"))));

        PncBuild pncBuild = pncClient.findBuild(buildId);

        assertEquals(3, pncBuild.buildArtifacts.size());
        assertEquals(3, pncBuild.dependencies.size());
        //FIXME nvr, build environment, actual files
    }

    private String readResponseBodyFromTemplate(String name) throws IOException {
        String folderName = getClass().getPackage().getName().replace(".", "/");
        try (InputStream inputStream = getContextClassLoader().getResourceAsStream(folderName + "/" + name)) {
            return join(IOUtils.readLines(inputStream, Charset.forName("utf-8")), "\n");
        }
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}