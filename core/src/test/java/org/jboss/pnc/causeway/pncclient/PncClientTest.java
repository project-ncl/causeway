package org.jboss.pnc.causeway.pncclient;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.jboss.resteasy.util.HttpResponseCodes.SC_OK;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

import org.commonjava.util.jhttpc.HttpFactory;
import org.jboss.pnc.causeway.config.CausewayConfig;
import org.jboss.pnc.causeway.pncclient.PncClient.ProductReleaseEndpoint;
import org.jboss.pnc.rest.provider.collection.CollectionInfo;
import org.jboss.pnc.rest.restmodel.BuildRecordRest;
import org.jboss.pnc.rest.restmodel.ProductMilestoneRest;
import org.jboss.pnc.rest.restmodel.ProductReleaseRest;
import org.jboss.pnc.rest.restmodel.response.Page;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Random;

public class PncClientTest {

    private Random generator = new Random();

    private PncClient client;
    private CausewayConfig config;
    @Mock
    private PncClient.RestEndpointProxyFactory restEndpointProxyFactory;
    @Mock
    private HttpFactory httpFactory;
    @Mock
    private ProductReleaseEndpoint productReleaseEndpoint;
    @Mock
    private Response response ;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        client = new PncClient(config, httpFactory, restEndpointProxyFactory);
        when(restEndpointProxyFactory.createRestEndpoint(ProductReleaseEndpoint.class)).thenReturn(productReleaseEndpoint);
    }

    @Test
    public void testBuildIdsAreFoundInPNC() throws Exception {
        int productReleaseId = createRandomInt();
        Integer buildRecord1 = createRandomInt();
        Integer buildRecord2 = createRandomInt();
        when(response.getStatus()).thenReturn(SC_OK);
        Collection<Integer> expectedIds = asList(buildRecord1, buildRecord2);
        when(response.readEntity(new GenericType<Page<BuildRecordRest>>(){})).thenReturn(new Page<BuildRecordRest>(new CollectionInfo<BuildRecordRest>(0, expectedIds.size(), 1, expectedIds.stream().map(id -> createBuildRecordRest(id)).collect(toList()))));
        when(productReleaseEndpoint.getAllBuildsInDistributedRecordsetOfProductRelease(productReleaseId)).thenReturn(response);

        Collection<Integer> ids = client.findBuildIdsOfProductRelease(productReleaseId);

        assertArrayEquals(expectedIds.toArray(), ids.toArray());
    }

    public static BuildRecordRest createBuildRecordRest(Integer id) {
        BuildRecordRest record = new BuildRecordRest();
        record.setId(id);
        return record;
    }

    private ProductReleaseRest createProductRelease(int productReleaseId) {
        ProductReleaseRest entity = new ProductReleaseRest();
        entity.setId(productReleaseId);
        entity.setProductMilestoneId(createRandomInt());
        return entity;
    }

    private ProductMilestoneRest createProductMilestone(int productMilestoneId) {
        ProductMilestoneRest entity = new ProductMilestoneRest();
        entity.setId(productMilestoneId);
        return entity;
    }

    private Long createRandomLong() {
        return generator.nextLong();
    }
    private Integer createRandomInt() {
        return generator.nextInt();
    }
}