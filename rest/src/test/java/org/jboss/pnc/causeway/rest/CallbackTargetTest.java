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
package org.jboss.pnc.causeway.rest;

import static org.junit.Assert.assertFalse;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CallbackTargetTest {

    @Test
    public void testSecurityCensoring() {
        Map<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        headers.put("Authorization", "top-secret-token");
        CallbackTarget ct = new CallbackTarget("http://foo.bar/", CallbackMethod.POST, headers);
        String toString = ct.toString();
        assertFalse("Secret token should be censored", toString.contains("top-secret-token"));
    }
}
