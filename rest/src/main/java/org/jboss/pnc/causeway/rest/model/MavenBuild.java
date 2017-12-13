package org.jboss.pnc.causeway.rest.model;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(value = "maven")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = MavenBuild.MavenBuildBuilder.class)
public class MavenBuild extends Build {

    @NonNull
    private final String groupId;
    @NonNull
    private final String artifactId;
    @NonNull
    private final String version;

    @Builder
    private MavenBuild(String groupId, String artifactId, String version, String buildName,
            String buildVersion, String externalBuildSystem, int externalBuildID,
            String externalBuildURL, Date startTime, Date endTime, String scmURL,
            String scmRevision, BuildRoot buildRoot, Set<Logfile> logs,
            Set<Dependency> dependencies, Set<BuiltArtifact> builtArtifacts, String tagPrefix) {
        super(buildName, buildVersion, externalBuildSystem, externalBuildID, externalBuildURL,
                startTime, endTime, scmURL, scmRevision, buildRoot, logs, dependencies,
                builtArtifacts, tagPrefix);
        this.groupId = Objects.requireNonNull(groupId);
        this.artifactId = Objects.requireNonNull(artifactId);
        this.version = Objects.requireNonNull(version);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class MavenBuildBuilder {
    }
}
