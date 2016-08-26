package org.jboss.pnc.causeway.rest;


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;


/**
 *
 * @author Jozef Mrazek <jmrazek@redhat.com>
 *
 */
@ApplicationPath("/rest")
public class ReportsRestActivator extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        addProjectResources(resources);
        return resources;
    }

    /**
     * Add all JAX-RS classes here to get activated!
     * @param resources
     */
    public void addProjectResources(Set<Class<?>> resources) {
        resources.add(Root.class);
    }
}
