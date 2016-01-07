/**
 * Copyright (C) 2015 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.anno.DataKey;
import org.commonjava.rwx.binding.anno.KeyRefs;
import org.commonjava.rwx.binding.anno.StructPart;

/**
 * Created by jdcasey on 1/6/16.
 */
@StructPart
public class KojiTagInfo
{

    @DataKey( "id" )
    private final int id;

    @DataKey( "name" )
    private final String name;

    @DataKey( "perm" )
    private final String permission;

    @DataKey( "perm_id" )
    private final int permissionId;

    @DataKey( "arches" )
    private final String arches;

    @DataKey( "locked" )
    private final boolean locked;

    @DataKey( "maven_support" )
    private final boolean mavenSupport;

    @DataKey( "maven_include_all" )
    private final boolean mavenIncludeAll;

    @KeyRefs( {"id", "name", "perm", "perm_id", "arches", "locked", "maven_support", "maven_include_all" } )
    public KojiTagInfo( int id, String name, String permission, int permissionId, String arches, boolean locked,
                        boolean mavenSupport, boolean mavenIncludeAll )
    {
        this.id = id;
        this.name = name;
        this.permission = permission;
        this.permissionId = permissionId;
        this.arches = arches;
        this.locked = locked;
        this.mavenSupport = mavenSupport;
        this.mavenIncludeAll = mavenIncludeAll;
    }

    public int getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getPermission()
    {
        return permission;
    }

    public int getPermissionId()
    {
        return permissionId;
    }

    public String getArches()
    {
        return arches;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public boolean isMavenSupport()
    {
        return mavenSupport;
    }

    public boolean isMavenIncludeAll()
    {
        return mavenIncludeAll;
    }
}
