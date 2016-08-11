/*
 * Copyright 2016 Honza Brázdil <jbrazdil@redhat.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.causeway.brewclient;

import org.commonjava.indy.client.core.Indy;
import org.commonjava.indy.client.core.IndyClientException;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;


/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class IndyProducer {
    private final String indyHost = "http://pnc-indy-branch-nightly.cloud.pnc.devel.engineering.redhat.com/api";

    @Produces
    public Indy create() throws IndyClientException {
        return new Indy(indyHost).connect();
    }

    public void close(@Disposes Indy indy) {
        indy.close();
    }
}
