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

import static org.jboss.pnc.causeway.rest.CallbackMethod.POST;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
@Data
public class CallbackTarget {
    @NonNull
    private final String url;
    
    @NonNull
    private final CallbackMethod method;

    private final Map<String,String> headers;

    @JsonCreator
    public CallbackTarget(@JsonProperty("url") String url,
            @JsonProperty("method") CallbackMethod method,
            @JsonProperty("headers") Map<String,String> headers) {
        this.url = url;
        this.method = method == null ? POST : method;
	this.headers = headers == null ? Collections.emptyMap() : headers;
    }
    
    public CallbackTarget(String url, CallbackMethod method) {
        this(url, method, Collections.emptyMap());
    }
    
     @Override
    public String toString() {
         StringBuilder headersString = new StringBuilder();
         headers.entrySet().stream()
                 .map(e -> '"' + e.getKey() + ':'
                         + ("Authorization".equals(e.getKey()) ? "***" : e.getValue()) + '"')
                 .collect(Collectors.joining(","));

         return "CallbackTarget{" + "url=" + url + ", method=" + method + ", headers={"
                 + headersString + "}}";
     }

}
