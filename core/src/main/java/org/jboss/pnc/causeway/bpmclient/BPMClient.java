/**
 * Copyright (C) 2015 Red Hat, Inc. (jbrazdil@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.bpmclient;

import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.causeway.rest.pnc.MilestoneReleaseResultRest;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Deprecated
public interface BPMClient {

    void error(Request callback, String callbackId, MilestoneReleaseResultRest result);

    void failure(Request callback, String callbackId, MilestoneReleaseResultRest result);

    void success(Request callback, String callbackId, MilestoneReleaseResultRest result);

}
