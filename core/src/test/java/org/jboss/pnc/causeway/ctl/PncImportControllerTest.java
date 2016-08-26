package org.jboss.pnc.causeway.ctl;

import static org.jboss.pnc.causeway.ctl.PncImportControllerImpl.messageBuildNotFound;
import static org.jboss.pnc.causeway.ctl.PncImportControllerImpl.messageReleaseWithoutBuildConfigurations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.pncclient.PncBuild;
import org.jboss.pnc.causeway.pncclient.PncClientImpl;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.BuildImportResult;
import org.jboss.pnc.causeway.rest.ProductReleaseImportResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

public class PncImportControllerTest {

    private PncImportControllerImpl importController;
    @Mock
    private PncClientImpl pncClient;
    @Mock
    private BrewClient brewClient;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        importController = new PncImportControllerImpl(pncClient, brewClient);
    }

    @Test
    public void testProductReleaseWithBuildConfiguratiuonsIsImported() throws Exception {
        Long releaseId = createRandomLong();
        Integer buildId = createRandomInt();

        Collection<Integer> buildIds = new HashSet<>();
        buildIds.add(buildId);
        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenReturn(buildIds);
        PncBuild pncBuild = new PncBuild(0);
        when(pncClient.findBuild(buildId)).thenReturn(pncBuild);
        BrewBuild brewBuild = new BrewBuild(createRandomLong(), new BrewNVR(createRandomString(), createRandomString(), createRandomString()));
        BuildImportResult buildImportResult = new BuildImportResult(brewBuild, null);
        when(brewClient.importBuild(pncBuild.createNVR(), pncBuild)).thenReturn(buildImportResult);

        ProductReleaseImportResult importResult = importController.importProductRelease(releaseId, false);

        assertEquals(0, importResult.getImportErrors().size());
    }

    @Test
    public void testProductReleaseImportsFailsWhenBuildConfigurationsNotFound() throws Exception {
        Long releaseId = createRandomLong();
        Integer buildId = createRandomInt();

        Collection<Integer> buildIds = new HashSet<>();
        buildIds.add(buildId);
        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenReturn(buildIds);

        ProductReleaseImportResult importResult = importController.importProductRelease(releaseId, false);

        assertEquals(1, importResult.getImportErrors().size());
        assertEquals(messageBuildNotFound(buildId), importResult.getImportErrors().get(buildId.longValue()));
    }

    @Test
    public void testProductReleaseWithoutBuildConfigurationsResultsInError() throws Exception {
        Long releaseId = createRandomLong();

        Collection<Integer> buildIds = new HashSet<>();
        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenReturn(buildIds);

        try {
            importController.importProductRelease(releaseId, false);
            fail("Expected exception");
        } catch (Exception e) {
            assertEquals(CausewayException.class, e.getClass());
            assertEquals(messageReleaseWithoutBuildConfigurations(releaseId), e.getMessage());
        }
    }

    @Test
    public void testUknownProductReleaseImportReturnsError() throws Exception {
        Long releaseId = createRandomLong();

        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenThrow(new IllegalArgumentException("Not found")); //FIXME no idea ATM what will be thrown if configuration will not be found
        try {
            importController.importProductRelease(releaseId, false);
            fail("Expected exception");
        } catch (Exception e) {
            assertEquals(CausewayException.class, e.getClass());
        }
    }

    public static Integer createRandomInt() {
        return new Random().nextInt();
    }
    public static Long createRandomLong() {
        return new Random().nextLong();
    }
    public static String createRandomString() {
        return "" + createRandomLong();
    }
}