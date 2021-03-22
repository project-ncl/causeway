package org.jboss.pnc.causeway.ctl;

import org.jboss.pnc.api.causeway.dto.push.Build;
import org.jboss.pnc.api.causeway.dto.untag.TaggedBuild;
import org.jboss.pnc.api.dto.Request;

/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public interface ImportController {

    void importBuild(Build build, Request callback, String username, boolean reimport);

    void untagBuild(TaggedBuild build, Request callback);
}
