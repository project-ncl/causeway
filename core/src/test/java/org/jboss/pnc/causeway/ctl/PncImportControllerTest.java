package org.jboss.pnc.causeway.ctl;

import static org.jboss.pnc.causeway.ctl.PncImportController.messageBuildNotFound;
import static org.jboss.pnc.causeway.ctl.PncImportController.messageReleaseWithoutBuildConfigurations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.brewclient.BrewClient;
import org.jboss.pnc.causeway.rest.BrewBuild;
import org.jboss.pnc.causeway.rest.BrewNVR;
import org.jboss.pnc.causeway.rest.BuildImportResult;
import org.jboss.pnc.causeway.rest.ProductReleaseImportResult;
import org.jboss.pnc.causeway.pncclient.PncBuild;
import org.jboss.pnc.causeway.pncclient.PncClient;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PncImportControllerTest {

    private PncImportController importController;
    @Mock
    private PncClient pncClient;
    @Mock
    private BrewClient brewClient;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        importController = new PncImportController(pncClient, brewClient);
    }

    @Test
    public void testProductReleaseWithBuildConfiguratiuonsIsImported() throws Exception {
        Long releaseId = createRandomLong();
        Long buildId = createRandomLong();

        Set<Long> buildIds = new HashSet<>();
        buildIds.add(buildId);
        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenReturn(buildIds);
        PncBuild pncBuild = new PncBuild();
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
        Long buildId = createRandomLong();

        Set<Long> buildIds = new HashSet<>();
        buildIds.add(buildId);
        when(pncClient.findBuildIdsOfProductRelease(releaseId.intValue())).thenReturn(buildIds);

        ProductReleaseImportResult importResult = importController.importProductRelease(releaseId, false);

        assertEquals(1, importResult.getImportErrors().size());
        assertEquals(messageBuildNotFound(buildId), importResult.getImportErrors().get(buildId));
    }

    @Test
    public void testProductReleaseWithoutBuildConfigurationsResultsInError() throws Exception {
        Long releaseId = createRandomLong();

        Set<Long> buildIds = new HashSet<>();
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

    public static Long createRandomLong() {
        return new Random().nextLong();
    }
    public static String createRandomString() {
        return "" + createRandomLong();
    }
}