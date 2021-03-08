package org.jboss.pnc.causeway.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(value = "npm")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(builder = NpmBuild.NpmBuildBuilder.class)
public class NpmBuild extends Build {

    @NonNull
    private final String name;

    private final String version;

    @Builder
    private NpmBuild(
            String name,
            String version,
            String buildName,
            String buildVersion,
            String externalBuildSystem,
            int externalBuildID,
            String externalBuildURL,
            Date startTime,
            Date endTime,
            String scmURL,
            String scmRevision,
            String scmTag,
            BuildRoot buildRoot,
            Set<Logfile> logs,
            String sourcesURL,
            Set<Dependency> dependencies,
            Set<BuiltArtifact> builtArtifacts,
            String tagPrefix) {
        super(
                buildName,
                buildVersion,
                externalBuildSystem,
                externalBuildID,
                externalBuildURL,
                startTime,
                endTime,
                scmURL,
                scmRevision,
                scmTag,
                buildRoot,
                logs,
                sourcesURL,
                dependencies,
                builtArtifacts,
                tagPrefix);
        this.name = Objects.requireNonNull(name);
        this.version = version;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class NpmBuildBuilder {
    }
}
