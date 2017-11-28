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
package org.jboss.pnc.causeway.pncclient;

import org.jboss.pnc.causeway.CausewayException;

/**
 * Created by jdcasey on 11/10/15.
 */
@Deprecated
public class PncClientException
        extends CausewayException
{
    private static final long serialVersionUID = 1L;

    public PncClientException(String format, Throwable cause, Object... params) {
        super(format, cause, params);
    }

    public PncClientException(String format, Object... params) {
        super(format, params);
    }

}
