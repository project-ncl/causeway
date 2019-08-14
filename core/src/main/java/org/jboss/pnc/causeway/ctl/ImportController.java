package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.causeway.rest.CallbackTarget;
import org.jboss.pnc.causeway.rest.model.Build;
import org.jboss.pnc.causeway.rest.model.TaggedBuild;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface ImportController {

    void importBuild(Build build, CallbackTarget callback, String username);

    void untagBuild(TaggedBuild build, CallbackTarget callback);
}
