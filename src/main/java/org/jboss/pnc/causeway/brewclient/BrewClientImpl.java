/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import com.redhat.red.build.koji.KojiClient;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.KojijiErrorInfo;
import com.redhat.red.build.koji.model.KojiImportResult;
import com.redhat.red.build.koji.model.json.KojiImport;
import com.redhat.red.build.koji.model.json.KojiJsonConstants;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiNVR;
import com.redhat.red.build.koji.model.xmlrpc.KojiSessionInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiTagInfo;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.pnc.causeway.CausewayConfig;
import org.jboss.pnc.causeway.CausewayException;
import org.jboss.pnc.causeway.CausewayFailure;
import org.jboss.pnc.causeway.ErrorMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class BrewClientImpl implements BrewClient {
    private static final Logger userLog = LoggerFactory.getLogger("org.jboss.pnc._userlog_.brew-push");

    public static final String BUILD_TAG_SUFIX = "-candidate";

    private final KojiClient koji;

    private final String brewUrl;

    @Inject
    public BrewClientImpl(KojiClient koji, CausewayConfig config) {
        this.koji = koji;
        brewUrl = config.koji().webURL();
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BrewBuild findBrewBuildOfNVR(BrewNVR nvr) throws CausewayException {
        KojiSessionInfo session = login();
        try {
            KojiNVR knvr = new KojiNVR(nvr.getKojiName(), nvr.getVersion(), nvr.getRelease());
            log.info("Get build info of build {} from user '{}'.", knvr, session.getUserInfo().getUserName());
            KojiBuildInfo bi = koji.getBuildInfo(knvr, session); // returns null if missing

            if (bi == null) {
                return null;
            }
            checkPNCImportedBuild(bi);
            return toBrewBuild(bi, nvr);
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.kojiCommunicationFailure(ex), ex);
        } finally {
            logout(session);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BrewBuild findBrewBuild(int id) throws CausewayException {
        KojiBuildInfo buildInfo;

        KojiSessionInfo session = login();
        try {
            log.info("Get build info of build id {} from user '{}'.", id, session.getUserInfo().getUserName());
            buildInfo = koji.getBuildInfo(id, session);
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.kojiCommunicationFailure(ex), ex);
        } finally {
            logout(session);
        }

        if (buildInfo == null) {
            return null;
        }
        checkPNCImportedBuild(buildInfo);
        return toBrewBuild(buildInfo);
    }

    /**
     * Checks if the brew build is imported by PNC. If not, throws an exception.
     *
     * @throws CausewayException when the brew build is not imporeted by PNC.
     */
    private void checkPNCImportedBuild(KojiBuildInfo bi) throws CausewayException {
        final Map<String, Object> extra = bi.getExtra();
        Object buildSystem = extra == null ? null : extra.get(KojiJsonConstants.BUILD_SYSTEM);
        if (!BuildTranslatorImpl.PNC.equals(buildSystem)) {
            throw new CausewayFailure(ErrorMessages.conflictingBrewBuild(bi.getId()));
        }
    }

    private static BrewBuild toBrewBuild(KojiBuildInfo bi, BrewNVR nvr) throws CausewayException {
        return new BrewBuild(bi.getId(), nvr);
    }

    private static BrewBuild toBrewBuild(KojiBuildInfo bi) throws CausewayException {
        return new BrewBuild(bi.getId(), new BrewNVR(bi.getName(), bi.getVersion(), bi.getRelease()));
    }

    @Retry
    @ExponentialBackoff
    @Override
    public void tagBuild(String pkg, BrewBuild build) throws CausewayException {
        String tag = pkg + BUILD_TAG_SUFIX;
        KojiSessionInfo session = login();
        log.info(
                "Applying tag {} in package {} on build {} from user '{}'.",
                tag,
                pkg,
                build.getNVR(),
                session.getUserInfo().getUserName());

        try {
            koji.addPackageToTag(pkg, build.getKojiName(), session);
            koji.tagBuild(tag, build.getNVR(), session);
        } catch (KojiClientException ex) {
            if (ex.getMessage().contains("policy violation")) {
                String userName = session.getUserInfo().getUserName();
                throw new CausewayFailure(ErrorMessages.missingTagPermissions(userName, pkg, tag, ex), ex);
            } else {
                throw new CausewayException(ErrorMessages.kojiCommunicationFailure(ex), ex);
            }
        } finally {
            logout(session);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public boolean isBuildTagged(String tag, BrewBuild build) throws CausewayException {
        KojiSessionInfo session = login();
        String tagName = tag + BUILD_TAG_SUFIX;
        try {
            log.info("Listing tags of build id {} from user '{}'.", build.getId(), session.getUserInfo().getUserName());
            List<KojiTagInfo> tags = koji.listTags(build.getId(), session);
            return tags.stream().map(KojiTagInfo::getName).anyMatch(tagName::equals);
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.failureWhileGettingTagInformation(ex), ex);
        } finally {
            logout(session);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public void untagBuild(String tag, BrewNVR nvr) throws CausewayException {
        String tagToRemove = tag + BUILD_TAG_SUFIX;
        KojiSessionInfo session = login();
        log.info(
                "Removing tag {} from build {} from user '{}'.",
                tagToRemove,
                nvr.getNVR(),
                session.getUserInfo().getUserName());
        try {
            koji.untagBuild(tagToRemove, nvr.getNVR(), session);
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.kojiCommunicationFailure(ex), ex);
        } finally {
            logout(session);
        }
    }

    @Retry
    @ExponentialBackoff
    @Override
    public BrewBuild importBuild(BrewNVR nvr, KojiImport kojiImport, ImportFileGenerator importFiles)
            throws CausewayException {
        KojiSessionInfo session = login();
        KojiImportResult result;
        try {
            log.info(
                    "Importing build {} from user '{}'.",
                    kojiImport.getBuildNVR(),
                    session.getUserInfo().getUserName());
            result = koji.importBuild(kojiImport, importFiles, session);
        } catch (KojiClientException ex) {
            checkImportErrors(null, importFiles); // to ensure errors are logged for users
            throw new CausewayFailure(ErrorMessages.failureWhileImportingBuilds(ex), ex);
        } finally {
            logout(session);
        }

        if (checkImportErrors(result, importFiles)) {
            throw new CausewayFailure(ErrorMessages.failureWhileImportingArtifacts());
        }

        KojiBuildInfo bi = result.getBuildInfo();
        if (bi == null) {
            throw new CausewayException(ErrorMessages.noBuildInfo());
        }
        return toBrewBuild(bi, nvr);
    }

    private boolean checkImportErrors(KojiImportResult result, ImportFileGenerator importFiles) {
        boolean errorsPresent = false;
        Map<String, KojijiErrorInfo> kojiErrors = result == null ? null : result.getUploadErrors();
        if (kojiErrors != null) {
            for (Map.Entry<String, KojijiErrorInfo> e : kojiErrors.entrySet()) {
                String artifactId = importFiles.getId(e.getKey());
                if (userLog.isWarnEnabled()) {
                    KojijiErrorInfo errorInfo = e.getValue();
                    String message = ErrorMessages.failedToImportArtifact(artifactId, e.getKey(), errorInfo);
                    userLog.warn(message, errorInfo.getError());
                }
                errorsPresent = true;
            }
        }
        return errorsPresent;
    }

    @Override
    public String getBuildUrl(int id) {
        return brewUrl + id;
    }

    @Retry
    @ExponentialBackoff
    @Override
    public boolean tagsExists(String tag) throws CausewayException {
        boolean packageTag, buildTag;
        KojiSessionInfo session = login();
        try {
            log.info("Checking if tag {} exists from user '{}'.", tag, session.getUserInfo().getUserName());

            packageTag = koji.getTag(tag, session) != null;
            buildTag = koji.getTag(tag + BUILD_TAG_SUFIX, session) != null;
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.kojiCommunicationFailure(ex), ex);
        } finally {
            logout(session);
        }
        return packageTag && buildTag;
    }

    private KojiSessionInfo login() throws CausewayException {
        try {
            KojiSessionInfo session = koji.login();
            log.info("Login to koji done successfully from user '{}'.", session.getUserInfo().getUserName());
            return session;
        } catch (KojiClientException ex) {
            throw new CausewayException(ErrorMessages.failureWhileLoggingToKoji(ex), ex);
        }
    }

    private void logout(KojiSessionInfo session) {
        String username = session.getUserInfo().getUserName();
        koji.logout(session);
        log.info("Logout from to koji done successfully from user '{}'.", username);
    }

}
