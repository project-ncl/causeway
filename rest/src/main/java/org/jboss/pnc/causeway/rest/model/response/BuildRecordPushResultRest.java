/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.rest.model.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

import lombok.Data;
import lombok.NonNull;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@AllArgsConstructor
@Data
@Builder
@JsonDeserialize(builder = BuildRecordPushResultRest.BuildRecordPushResultRestBuilder.class)
public class BuildRecordPushResultRest {
    public enum Status {
        SUCCESS, FAILED, SYSTEM_ERROR;
    }

    private final Integer id;

    private final int buildRecordId;

    @NonNull
    private final BuildRecordPushResultRest.Status status;

    @NonNull
    private final String log;

    /**
     * list of errors for artifact imports
     */
    private final List<ArtifactImportError> artifactImportErrors;

    /**
     * build id assigned by brew
     */
    private final Integer brewBuildId;

    /**
     * link to brew
     */
    private final String brewBuildUrl;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class BuildRecordPushResultRestBuilder {
    }

}
