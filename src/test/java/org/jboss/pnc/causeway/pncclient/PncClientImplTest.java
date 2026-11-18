/*
 * SPDX-FileCopyrightText: Copyright © 2015 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.pncclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.jboss.pnc.common.scm.ScmUrlGeneratorProvider.SCMProvider;
import org.junit.jupiter.api.Test;

public class PncClientImplTest {

    @Test
    public void shouldUsePrivateTokenHeaderForGitlab() {
        Optional<Map.Entry<String, String>> header = PncClientImpl
                .authHeaderFor(SCMProvider.GITLAB, "github-token", Optional.of("gitlab-token"));

        assertTrue(header.isPresent());
        assertEquals("PRIVATE-TOKEN", header.get().getKey());
        assertEquals("gitlab-token", header.get().getValue());
    }

    @Test
    public void shouldUseBearerGithubTokenForGithub() {
        Optional<Map.Entry<String, String>> header = PncClientImpl
                .authHeaderFor(SCMProvider.GITHUB, "github-token", Optional.of("gitlab-token"));

        assertTrue(header.isPresent());
        assertEquals("Authorization", header.get().getKey());
        assertEquals("Bearer github-token", header.get().getValue());
    }

    @Test
    public void shouldUseBearerGithubTokenForGerrit() {
        Optional<Map.Entry<String, String>> header = PncClientImpl
                .authHeaderFor(SCMProvider.GERRIT, "github-token", Optional.empty());

        assertTrue(header.isPresent());
        assertEquals("Authorization", header.get().getKey());
        assertEquals("Bearer github-token", header.get().getValue());
    }

    @Test
    public void shouldNotSendHeaderForGitlabWhenTokenMissing() {
        assertTrue(PncClientImpl.authHeaderFor(SCMProvider.GITLAB, "github-token", Optional.empty()).isEmpty());
    }

    @Test
    public void shouldNotSendHeaderForGitlabWhenTokenBlank() {
        assertTrue(PncClientImpl.authHeaderFor(SCMProvider.GITLAB, "github-token", Optional.of("  ")).isEmpty());
    }

    @Test
    public void shouldNotSendHeaderForGithubWhenTokenBlank() {
        assertTrue(PncClientImpl.authHeaderFor(SCMProvider.GITHUB, "  ", Optional.of("gitlab-token")).isEmpty());
    }
}
