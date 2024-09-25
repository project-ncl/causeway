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
package org.jboss.pnc.causeway.rest.pnc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * Author: Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com Date: 8/24/16 Time: 3:42 PM
 */
@Data
@Deprecated
public class MilestoneReleaseRest {

    private final int milestoneId;
    private String userInitiator;

    @JsonCreator
    public MilestoneReleaseRest(
            @JsonProperty("milestoneId") int milestoneId,
            @JsonProperty("userInitiator") String userInitiator) {
        this.milestoneId = milestoneId;
        this.userInitiator = userInitiator;
    }
}
