package org.jboss.pnc.causeway;

import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.KojijiErrorInfo;
import com.redhat.red.build.koji.model.json.KojiJsonConstants;
import com.redhat.red.build.koji.model.json.VerificationException;
import org.jboss.pnc.api.causeway.dto.push.Build;
import org.jboss.pnc.causeway.brewclient.BrewClientImpl;
import org.jboss.pnc.causeway.brewclient.BuildTranslatorImpl;
import org.jboss.pnc.client.ClientException;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.RemoteResourceNotFoundException;
import org.jboss.pnc.enums.BuildType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.List;

import static org.jboss.pnc.api.constants.Attributes.BUILD_BREW_NAME;

public class ErrorMessages {
    private static final String KOJI_COMMUNICATION_FAILURE = "Failure while communicating with Koji: {0}";
    private static final String FAILURE_GETTING_TAG_INFORMATION = "Failure while getting tag information from build: {0}";
    private static final String FAILURE_IMPORTING_BUILDS = "Failure while importing builds to Koji: {0}";
    private static final String FAILURE_LOGGING_TO_KOJI = "Failure while logging to Koji: {0}";
    private static final String MISSING_TAG_PERMISSIONS = "Failure while communicating with Koji: This is most probably because of missing permissions. Ask RCM to add permissions for user ''{0}}'' to add packages to tag ''{1}'' and to tag builds into tag ''{2}''. Cause: {3}";
    private static final String FOUND_CONFLICTING_BREW_BUILD = "Found conflicting brew build {0} (build doesn''t have "
            + KojiJsonConstants.BUILD_SYSTEM + " set to " + BuildTranslatorImpl.PNC + ").";
    private static final String FAILURE_IMPORTING_ARTIFACTS = "Failure while importing artifacts.";
    private static final String NO_BUILD_INFO = "Import to koji failed for unknown reason. No build data.";
    private static final String FAILED_TO_IMPORT_ARTIFACT = "Failed to import artifact {0} ({1}): {2}";
    private static final String FAILED_TO_COMPUTE_BUILD_LOG_MD5 = "Failed to compute md5 sum of build log: {0}";
    private static final String UNKNOWN_ARTIFACT_TYPE = "Unknown artifact type.";
    private static final String UNKNOWN_SYSTEM_IMAGE_TYPE = "Unknown system image type.";
    private static final String FAILED_TO_PARSE_ARTIFACT_URL = "Failed to parse artifact URL: {0}";
    private static final String FAILED_TO_READ_LOG_FILE = "Failed to read log file: {0}";
    private static final String UNSUPPORTED_BUILD_CLASS = "Unsupported type of build ''{0}''";
    private static final String UNSUPPORTED_BUILD_TYPE = "Unsupported build type ''{0}''.";
    private static final String FAILED_TO_DOWNLOAD_SOURCES = "Failed to download sources: {0}";
    private static final String FAILURE_WHILE_BUILDING_KOJI_IMPORT = "Failure while building metadata for the import to Koji : {0}";
    private static final String MISSING_BREW_NAME_MAVEN = "The '" + BUILD_BREW_NAME
            + "' build attribute is missing. Add the attribute to the build with the 'GROUP_ID:ARTIFACT_ID' format. It will be used to name the build in Koji.";
    private static final String MISSING_BREW_NAME = "The ''" + BUILD_BREW_NAME
            + "'' build attribute is missing. Add the attribute to the build. It will be used to name the build in Koji.";
    private static final String ILLEGAL_MAVEN_BREW_NAME = BUILD_BREW_NAME
            + " attribute ''{0}'' doesn''t seem to be maven G:A.";
    private static final String BUILD_ENVIRONMENT_HAS_MULTIPLE_VERSIONS = "Build environment ({0}) has multiple versions for tool ''{1}''";
    private static final String BUILD_ENVIRONMENT_HAS_NO_VERSION = "Build environment ({0}) has no version for tool {1}";
    private static final String FAILED_TO_OBTAIN_ARTIFACT = "Failed to obtain artifact (status {0} {1})";
    private static final String CAUSEWAY_HAS_NOT_BEEN_CONFIGURED = "The Causeway system has not been configured. This is a sign that something is in the wrong order in the boot sequence.";
    private static final String CONFIG_VALUE_IS_NOT_URL = "Value of the ''{0}'' field in Causeway configuration is not parsable URL";
    private static final String FAILED_TO_READ_CONFIG_FILE = "Failed to read configuration: {0}. Reason: {1}";
    private static final String CONFIG_IS_NOT_COMPLETE = "Causeway configuration is not complete. Validation errors:\n{0}";
    public static final String FAILED_TO_IMPORT_BUILD = "Failed to import build {0}: {1}";
    public static final String ERROR_WHILE_IMPORTING_BUILD = "Error while importing build {0}: {1}";
    public static final String FAILED_TO_IMPORT_MILESTONE = "Failed to import milestone {0}: {1}";
    public static final String ERROR_WHILE_IMPORTING_MILESTONE = "Error while importing milestone {0}: {1}";
    public static final String FAILED_TO_UNTAG_BUILD = "Failed to untag build: {0}";
    public static final String ERROR_WHILE_UNTAGGING_BUILD = "Error while untagging build: {0}";
    public static final String BUILD_DOESN_T_CONTAIN_ANY_ARTIFACTS = "Build doesn't contain any artifacts.";
    public static final String MISSING_TAG_MESSAGE = "Proper brew tags don''t exist. Create them before importing builds.\n"
            + "Tag prefix: {0}\n" + "You should ask RCM to create at least following tags:\n" + " * {1}\n"
            + "   * {2}\n" + "in {3}\n" + "(Note that tag {1} should inherit from tag {2})";
    public static final String BREW_BUILD_WAS_NOT_FOUND = "Brew build with id {0} was not found.";
    public static final String MILESTONE_DOES_NOT_CONTAIN_ANY_BUILD = "Milestone {0} does not contain any build.";
    public static final String BAD_ARTIFACT_NOT_IMPORTED = "Failed to import artifact {0}: This artifact is blacklisted or deleted, so it was not imported.";
    public static final String COULD_NOT_CONNECT_TO_KOJI = "Couldn''t connect to Koji: {0}";
    public static final String MILESTONE_NOT_FOUND_WHEN_GETTING_TAG = "Can not read tag because the product milestone {0} was not found in PNC: {1}";
    public static final String ERROR_READING_TAG_FROM_MILESTONE = "Can not read tag from product milestone {0} because PNC responded with an {0} error: {2}";
    public static final String ERROR_COMMUNICATING_WHEN_GETTING_TAG = "Can not read tag because there was a problem communicating with PNC when getting product milestone {1}: {2}";
    public static final String ERROR_READING_BUILDS_FROM_MILESTONE = "Can not read builds for product milestone {0} because PNC responded with an {1} error: {2}";
    public static final String ERROR_READING_BUILD_LOG = "Can not read build log of build {0} because PNC responded with an {1} error: {2}";
    public static final String ERROR_READING_BUILD_SOURCES = "Can not read sources of build {0} because PNC responded with an {1} error: {2}";
    public static final String ERROR_READING_BUILD_SOURCES_NO_STATUS = "Can not read sources of build {0} because PNC responded with an error: {1}";
    public static final String ERROR_READING_BUILD_ARTIFACTS = "Can not read artifacts of build {0} because PNC responded with an {1} error: {2}";
    public static final String BUILD_LOG_IS_EMPTY = "Build log for build {0} is empty.";
    public static final String SOURCES_ARCHIVE_FILE_WAS_ALREADY_READ = "Sources archive file was already read.";
    public static final String THIS_IS_NOT_A_MAVEN_TYPE = "This is not a Maven type.";
    public static final String THIS_IS_NOT_AN_NPM_TYPE = "This is not an NPM type.";
    public static final String ERROR_REPACKING_ARCHIVE = "Error while repacking archive with changed root directory name: {0}";
    public static final String MISSING_MD5_SUPPORT = "The JVM does not support MD5 digest";
    public static final String NON_DIRECTORY_FILE_IN_ROOT_OF_THE_ARCHIVE = "There is a non-directory file in root of the archive.";
    public static final String MULTIPLE_DIRECTORIES_IN_ROOT_OF_THE_ARCHIVE = "There are multiple directories in root of the archive.";

    /**
     * {@value KOJI_COMMUNICATION_FAILURE}
     */
    public static String kojiCommunicationFailure(KojiClientException ex) {
        return MessageFormat.format(KOJI_COMMUNICATION_FAILURE, ex);
    }

    /**
     * {@value FAILURE_GETTING_TAG_INFORMATION}
     */
    public static String failureWhileGettingTagInformation(KojiClientException ex) {
        return MessageFormat.format(FAILURE_GETTING_TAG_INFORMATION, ex);
    }

    /**
     * {@value FAILURE_IMPORTING_BUILDS}
     */
    public static String failureWhileImportingBuilds(KojiClientException ex) {
        return MessageFormat.format(FAILURE_IMPORTING_BUILDS, ex);
    }

    /**
     * {@value FAILURE_LOGGING_TO_KOJI}
     */
    public static String failureWhileLoggingToKoji(KojiClientException ex) {
        return MessageFormat.format(FAILURE_LOGGING_TO_KOJI, ex);
    }

    /**
     * {@value MISSING_TAG_PERMISSIONS}
     */
    public static String missingTagPermissions(String userName, String pkg, String tag, KojiClientException ex) {
        return MessageFormat.format(MISSING_TAG_PERMISSIONS, userName, pkg, tag, ex);

    }

    /**
     * {@value FAILURE_IMPORTING_ARTIFACTS}
     */
    public static String failureWhileImportingArtifacts() {
        return FAILURE_IMPORTING_ARTIFACTS;
    }

    /**
     * {@value FOUND_CONFLICTING_BREW_BUILD}
     */
    public static String conflictingBrewBuild(int buildId) {
        return MessageFormat.format(FOUND_CONFLICTING_BREW_BUILD, buildId);
    }

    /**
     * {@value NO_BUILD_INFO}
     */
    public static String noBuildInfo() {
        return NO_BUILD_INFO;
    }

    /**
     * {@value FAILED_TO_IMPORT_ARTIFACT}
     */
    public static String failedToImportArtifact(String artifactId, String key, KojijiErrorInfo errorInfo) {
        return MessageFormat.format(FAILED_TO_IMPORT_ARTIFACT, artifactId, key, errorInfo);
    }

    /**
     * {@value FAILED_TO_COMPUTE_BUILD_LOG_MD5}
     */
    public static String failedToComputeBuildLogMD5(NoSuchAlgorithmException ex) {
        return MessageFormat.format(FAILED_TO_COMPUTE_BUILD_LOG_MD5, ex);
    }

    /**
     * {@value FAILED_TO_PARSE_ARTIFACT_URL}
     */
    public static String failedToParseArtifactURL(MalformedURLException ex) {
        return MessageFormat.format(FAILED_TO_PARSE_ARTIFACT_URL, ex);
    }

    /**
     * {@value FAILED_TO_PARSE_ARTIFACT_URL}
     */
    public static String failedToReadLogFile(Exception ex) {
        return MessageFormat.format(FAILED_TO_READ_LOG_FILE, ex);
    }

    /**
     * {@value FAILED_TO_DOWNLOAD_SOURCES}
     */
    public static String failedToDownloadSources(IOException ex) {
        return MessageFormat.format(FAILED_TO_DOWNLOAD_SOURCES, ex);
    }

    /**
     * {@value FAILURE_WHILE_BUILDING_KOJI_IMPORT}
     */
    public static String failureWhileBuildingKojiImport(VerificationException ex) {
        return MessageFormat.format(FAILURE_WHILE_BUILDING_KOJI_IMPORT, ex);
    }

    /**
     * {@value UNKNOWN_ARTIFACT_TYPE}
     */
    public static String unknownArtifactType() {
        return UNKNOWN_ARTIFACT_TYPE;
    }

    /**
     * {@value UNKNOWN_SYSTEM_IMAGE_TYPE}
     */
    public static String unknownSystemImageType() {
        return UNKNOWN_SYSTEM_IMAGE_TYPE;
    }

    /**
     * {@value UNSUPPORTED_BUILD_CLASS}
     */
    public static String unsupportedBuildClass(Class<? extends Build> buildClass) {
        return MessageFormat.format(UNSUPPORTED_BUILD_CLASS, buildClass);
    }

    /**
     * {@value UNSUPPORTED_BUILD_TYPE}
     */
    public static String unsupportedBuildType(BuildType buildType) {
        return MessageFormat.format(UNSUPPORTED_BUILD_TYPE, buildType);
    }

    /**
     * {@value MISSING_BREW_NAME_MAVEN}
     */
    public static String missingBrewNameAttributeInMavenBuild() {
        return MISSING_BREW_NAME_MAVEN;
    }

    /**
     * {@value MISSING_BREW_NAME}
     */
    public static String missingBrewNameAttributeInBuild() {
        return MISSING_BREW_NAME;
    }

    /**
     * {@value ILLEGAL_MAVEN_BREW_NAME}
     */
    public static String illegalMavenBrewName(String attributeValue) {
        return MessageFormat.format(ILLEGAL_MAVEN_BREW_NAME, attributeValue);
    }

    /**
     * {@value BUILD_ENVIRONMENT_HAS_MULTIPLE_VERSIONS}
     */
    public static String environmentWithMultipleVersions(String environmentId, String toolName) {
        return MessageFormat.format(BUILD_ENVIRONMENT_HAS_MULTIPLE_VERSIONS, environmentId, toolName);
    }

    /**
     * {@value BUILD_ENVIRONMENT_HAS_NO_VERSION}
     */
    public static String environmentWithoutVersion(String environmentId, String toolName) {
        return MessageFormat.format(BUILD_ENVIRONMENT_HAS_NO_VERSION, environmentId, toolName);
    }

    /**
     * {@value FAILED_TO_OBTAIN_ARTIFACT}
     */
    public static String failedToObtainArtifact(int responseCode, String responseMessage) {
        return MessageFormat.format(FAILED_TO_OBTAIN_ARTIFACT, responseCode, responseMessage);
    }

    /**
     * {@value CAUSEWAY_HAS_NOT_BEEN_CONFIGURED}
     */
    public static String causewayNotConfigured() {
        return CAUSEWAY_HAS_NOT_BEEN_CONFIGURED;
    }

    /**
     * {@value CONFIG_VALUE_IS_NOT_URL}
     */
    public static String configurationValueIsNotURL(String valueName) {
        return MessageFormat.format(CONFIG_VALUE_IS_NOT_URL, valueName);
    }

    /**
     * {@value FAILED_TO_READ_CONFIG_FILE}
     */
    public static String failedToReadConfigFile(File configFile, Exception ex) {
        return MessageFormat.format(FAILED_TO_READ_CONFIG_FILE, configFile, ex);
    }

    /**
     * {@value CONFIG_IS_NOT_COMPLETE}
     */
    public static String configIsNotComplete(List<String> validationErrors) {
        String formattedValidationErrors = String.join("\n", validationErrors);
        return MessageFormat.format(CONFIG_IS_NOT_COMPLETE, formattedValidationErrors);
    }

    /**
     * {@value FAILED_TO_IMPORT_BUILD}
     */
    public static String failedToImportBuild(String buildId, CausewayFailure ex) {
        return MessageFormat.format(FAILED_TO_IMPORT_BUILD, buildId, ex);
    }

    /**
     * {@value ERROR_WHILE_IMPORTING_BUILD}
     */
    public static String errorImportingBuild(String buildId, Exception ex) {
        return MessageFormat.format(ERROR_WHILE_IMPORTING_BUILD, buildId, ex);
    }

    /**
     * {@value FAILED_TO_IMPORT_MILESTONE}
     */
    public static String failedToImportMilestone(int milestoneId, CausewayFailure ex) {
        return MessageFormat.format(FAILED_TO_IMPORT_MILESTONE, milestoneId, ex);
    }

    /**
     * {@value ERROR_WHILE_IMPORTING_MILESTONE}
     */
    public static String errorImportingMilestone(int milestoneId, Exception ex) {
        return MessageFormat.format(ERROR_WHILE_IMPORTING_MILESTONE, milestoneId, ex);
    }

    /**
     * {@value FAILED_TO_UNTAG_BUILD}
     */
    public static String failedToUntagBuild(CausewayFailure ex) {
        return MessageFormat.format(FAILED_TO_UNTAG_BUILD, ex);
    }

    /**
     * {@value ERROR_WHILE_UNTAGGING_BUILD}
     */
    public static String errorUntaggingBuild(Exception ex) {
        return MessageFormat.format(ERROR_WHILE_UNTAGGING_BUILD, ex);
    }

    /**
     * {@value BUILD_DOESN_T_CONTAIN_ANY_ARTIFACTS}
     */
    public static String buildHasNoArtifacts() {
        return BUILD_DOESN_T_CONTAIN_ANY_ARTIFACTS;
    }

    /**
     * {@value MISSING_TAG_MESSAGE}
     */
    public static String tagsAreMissingInKoji(String tagPrefix, String kojiURL) {
        final String parent = tagPrefix;
        final String child = tagPrefix + BrewClientImpl.BUILD_TAG_SUFIX;
        return MessageFormat.format(MISSING_TAG_MESSAGE, tagPrefix, child, parent, kojiURL);
    }

    /**
     * {@value BREW_BUILD_WAS_NOT_FOUND}
     */
    public static String brewBuildNotFound(int kojiBuildId) {
        return MessageFormat.format(BREW_BUILD_WAS_NOT_FOUND, kojiBuildId);
    }

    /**
     * {@value MILESTONE_DOES_NOT_CONTAIN_ANY_BUILD}
     */
    public static String messageMilestoneWithoutBuilds(long milestoneId) {
        return MessageFormat.format(MILESTONE_DOES_NOT_CONTAIN_ANY_BUILD, milestoneId);
    }

    /**
     * {@value BAD_ARTIFACT_NOT_IMPORTED}
     */
    public static String badArtifactNotImported(String artifactId) {
        return MessageFormat.format(BAD_ARTIFACT_NOT_IMPORTED, artifactId);
    }

    /**
     * {@value COULD_NOT_CONNECT_TO_KOJI}
     */
    public static String canNotConnectToKoji(KojiClientException ex) {
        return MessageFormat.format(COULD_NOT_CONNECT_TO_KOJI, ex);
    }

    /**
     * {@value MILESTONE_NOT_FOUND_WHEN_GETTING_TAG}
     */
    public static String milestoneNotFoundWhenGettingTag(int milestoneId, RemoteResourceNotFoundException ex) {
        return MessageFormat.format(MILESTONE_NOT_FOUND_WHEN_GETTING_TAG, milestoneId, ex);
    }

    /**
     * {@value ERROR_READING_TAG_FROM_MILESTONE}
     */
    public static String errorReadingTagFromMilestone(int milestoneId, RemoteResourceException ex) {
        return MessageFormat.format(ERROR_READING_TAG_FROM_MILESTONE, milestoneId, ex.getStatus(), ex);
    }

    /**
     * {@value ERROR_COMMUNICATING_WHEN_GETTING_TAG}
     */
    public static String errorCommunicatingWhenGettingTag(int milestoneId, ClientException ex) {
        return MessageFormat.format(ERROR_COMMUNICATING_WHEN_GETTING_TAG, milestoneId, ex);
    }

    /**
     * {@value ERROR_READING_BUILDS_FROM_MILESTONE}
     */
    public static String errorReadingBuildsFromMilestone(int milestoneId, RemoteResourceException ex) {
        return MessageFormat.format(ERROR_READING_BUILDS_FROM_MILESTONE, milestoneId, ex.getStatus(), ex);
    }

    /**
     * {@value BUILD_LOG_IS_EMPTY}
     */
    public static String buildLogIsEmpty(String buildId) {
        return MessageFormat.format(BUILD_LOG_IS_EMPTY, buildId);
    }

    /**
     * {@value ERROR_READING_BUILD_LOG}
     */
    public static String errorReadingBuildLog(String buildId, RemoteResourceException ex) {
        return MessageFormat.format(ERROR_READING_BUILD_LOG, buildId, ex.getStatus(), ex);
    }

    /**
     * {@value ERROR_READING_BUILD_SOURCES}
     */
    public static String errorReadingBuildSources(String buildId, RemoteResourceException ex) {
        return MessageFormat.format(ERROR_READING_BUILD_SOURCES, buildId, ex.getStatus(), ex);
    }

    /**
     * {@value ERROR_READING_BUILD_SOURCES}
     */
    public static String errorReadingBuildSources(String buildId, int responseStatus, String message) {
        return MessageFormat.format(ERROR_READING_BUILD_SOURCES, buildId, responseStatus, message);
    }

    /**
     * {@value ERROR_READING_BUILD_SOURCES_NO_STATUS}
     */
    public static String errorReadingBuildSources(String buildId, RuntimeException ex) {
        return MessageFormat.format(ERROR_READING_BUILD_SOURCES_NO_STATUS, buildId, ex);
    }

    /**
     * {@value ERROR_READING_BUILD_ARTIFACTS}
     */
    public static String errorReadingBuildArtifacts(String buildId, RemoteResourceException ex) {
        return MessageFormat.format(ERROR_READING_BUILD_ARTIFACTS, buildId, ex.getStatus(), ex);
    }

    /**
     * {@value SOURCES_ARCHIVE_FILE_WAS_ALREADY_READ}
     */
    public static String sourcesFileWasAlreadyRead() {
        return SOURCES_ARCHIVE_FILE_WAS_ALREADY_READ;
    }

    /**
     * {@value THIS_IS_NOT_A_MAVEN_TYPE}
     */
    public static String notMavenType() {
        return THIS_IS_NOT_A_MAVEN_TYPE;
    }

    /**
     * {@value THIS_IS_NOT_AN_NPM_TYPE}
     */
    public static String notNPMType() {
        return THIS_IS_NOT_AN_NPM_TYPE;
    }

    /**
     * {@value ERROR_REPACKING_ARCHIVE}
     */
    public static String errorRepackingArchive(Exception ex) {
        return MessageFormat.format(ERROR_REPACKING_ARCHIVE, ex);
    }

    /**
     * {@value MISSING_MD5_SUPPORT}
     */
    public static String missingMD5Support(Exception ex) {
        return MessageFormat.format(MISSING_MD5_SUPPORT, ex);
    }

    /**
     * {@value NON_DIRECTORY_FILE_IN_ROOT_OF_THE_ARCHIVE}
     */
    public static String nonDirectoryInArchiveRoot() {
        return NON_DIRECTORY_FILE_IN_ROOT_OF_THE_ARCHIVE;
    }

    /**
     * {@value MULTIPLE_DIRECTORIES_IN_ROOT_OF_THE_ARCHIVE}
     */
    public static String multipleDirectoriseInArchiveRoot() {
        return MULTIPLE_DIRECTORIES_IN_ROOT_OF_THE_ARCHIVE;
    }
}
