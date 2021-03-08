package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.api.causeway.dto.CallbackTarget;
import org.jboss.pnc.api.causeway.dto.push.Build;
import org.jboss.pnc.api.causeway.dto.untag.TaggedBuild;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface ImportController {

    void importBuild(Build build, CallbackTarget callback, String username, boolean reimport);

    void untagBuild(TaggedBuild build, CallbackTarget callback);
}
