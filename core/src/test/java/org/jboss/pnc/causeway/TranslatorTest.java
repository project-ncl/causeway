package org.jboss.pnc.causeway;

import org.jboss.pnc.causeway.pncclient.*;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static org.apache.commons.lang.StringUtils.join;
import static org.mockito.Mockito.when;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.Indy;
import org.jboss.pnc.causeway.brewclient.BuildTranslator;
import org.jboss.pnc.causeway.brewclient.IndyProducer;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.rest.BrewNVR;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.head;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.util.KojiObjectMapper;

public class TranslatorTest {
    public static final String CONTEXT_URL = "/pnc-rest/rest";
    public static final String INDY_CONTEXT_URL = "/api";
    private ResteasyClient client;
    private String pncUrl;

    public static int port = Integer.valueOf(System.getProperty("test.port", "8089"));

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(port);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;
    private PncClient pncClient;
    @Mock
    private CausewayConfig config;

    private Indy indy;

    private BuildTranslator bt;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pncClient = new PncClient(config, null);
        client = new ResteasyClientBuilder().build();
        indy = new Indy("http://localhost:"  + port + INDY_CONTEXT_URL).connect();
        pncUrl = "http://localhost:"  + port + CONTEXT_URL;

        bt = new BuildTranslator(pncClient, indy);
        when(config.getPnclURL()).thenReturn(pncUrl);
    }

    @After
    public void tearDown() throws Exception {
        if(indy != null){
            indy.close();
        }
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
    public void testReadBuildArtifacts() throws Exception {
        Integer buildId = 61;

        String relativeBuiltUrl = "/build-records/" + buildId + "/built-artifacts?pageIndex=0&pageSize=" + PncClient.MAX_ARTIFACTS + "&sort=&q=";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeBuiltUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("pncclient/build-records-61-built-artifacts-1.json"))));

        String relativeDependencyUrl = "/build-records/" + buildId + "/dependency-artifacts?pageIndex=0&pageSize=" + PncClient.MAX_ARTIFACTS + "&sort=&q=";
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeDependencyUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("pncclient/build-records-61-dependency-artifacts-1.json"))));

        String relativeBuildRecordUrl = "/build-records/" + buildId;
        stubFor(get(urlEqualTo(CONTEXT_URL + relativeBuildRecordUrl))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readResponseBodyFromTemplate("pncclient/build-records-61-1.json"))));

        stubIndy("/remote/EAP7_MEAD/org/apache/maven/maven-project/2.0.6/maven-project-2.0.6.pom", "2637");
        stubIndy("/remote/EAP7_MEAD/org/apache/maven/shared/maven-shared-io/1.1/maven-shared-io-1.1.jar", "39480");
        stubIndy("/remote/EAP7_MEAD/xml-apis/xml-apis/1.0.b2/xml-apis-1.0.b2.jar", "109318");
        stubIndy("/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.pom", "4369");
        stubIndy("/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1.jar", "13387");
        stubIndy("/hosted/build_geronimo-annotation_1-0_spec-1-1-1_20160804.0721/org/apache/geronimo/specs/geronimo-annotation_1.0_spec/1.1.1.redhat-1/geronimo-annotation_1.0_spec-1.1.1.redhat-1-project-sources.tar.gz", "6992");

        PncBuild pncBuild = pncClient.findBuild(buildId);

        KojiImport out = bt.translate(new BrewNVR("a", "1.2.3", "1"), pncBuild);

        KojiObjectMapper mapper = new KojiObjectMapper();
        String json = mapper.writeValueAsString(out);
        System.out.println("RESULT:\n" + json);
    }

    private void stubIndy(final String path, final String size) {
        stubFor(head(urlEqualTo(INDY_CONTEXT_URL + path))
                .willReturn(aResponse()
                        .withHeader("CONTENT-LENGTH", size)
                        .withHeader("Content-Type", "application/octet-stream")
                        .withHeader("LAST-MODIFIED", "Tue, 06 Jan 2009 18:34:05 GMT")));
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