/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.brewclient;

import com.redhat.red.build.koji.model.json.KojiImport;
import org.jboss.pnc.api.causeway.dto.push.PushResult;
import org.jboss.pnc.causeway.CausewayException;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface BrewClient {

    /**
     * Find Brew build with given name:version:release that was imported by Causeway. If there isn't any such build,
     * returns null. If there is build with the NVR but it wasn't imported by causeway, CausewayFailure exception is
     * thrown.
     *
     * @throws CausewayException when build with given NVR exists but wasn't imported by Causeway.
     */
    BrewBuild findBrewBuildOfNVR(BrewNVR nvr) throws CausewayException;

    /**
     * Find Brew build with given id that was imported by Causeway. If there isn't such build, returns null. If there is
     * build with the id but it wasn't imported by causeway, CausewayFailure exception is thrown.
     *
     * @throws CausewayException when build with given id exists but wasn't imported by Causeway.
     */
    BrewBuild findBrewBuild(int id) throws CausewayException;

    BrewBuild importBuild(BrewNVR nvr, KojiImport kojiImport, ImportFileGenerator importFiles) throws CausewayException;

    public String getBuildUrl(int id);

    public boolean tagsExists(String tag) throws CausewayException;

    /**
     * Check if build is tagged in given tag.
     *
     * @param tag Tag name to be checked.
     * @param build Brew build to get tag information.
     * @return True if the build is tagged in the given tag.
     * @throws CausewayException when there was problem communicating with Brew.
     */
    boolean isBuildTagged(String tag, BrewBuild build) throws CausewayException;

    /**
     * Tag build into given tag.
     *
     * @param tag Tag name.
     * @param build Build to be tagegd.
     * @throws CausewayException when there was problem communicating with Brew or problems with permisions.
     */
    void tagBuild(String tag, BrewBuild build) throws CausewayException;

    boolean isBuildDeleted(BrewBuild build) throws CausewayException;

    void untagBuild(String tag, BrewNVR nvr) throws CausewayException;

}
