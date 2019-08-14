package org.jboss.pnc.causeway;

import lombok.Getter;
import org.jboss.pnc.dto.ArtifactImportError;

import java.util.List;

/**
 * This class indicates just a failure, which can usually be solved by user themselve.
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CausewayFailure extends CausewayException {

    @Getter
    private List<ArtifactImportError> artifactErrors;

    public CausewayFailure(String format, Throwable cause, Object... params) {
        super(format, cause, params);
    }

    public CausewayFailure(String format, Object... params) {
        super(format, params);
    }

    public CausewayFailure(List<ArtifactImportError> artifactErrors, String format, Throwable cause, Object... params) {
        super(format, cause, params);
        this.artifactErrors = artifactErrors;
    }

    public CausewayFailure(List<ArtifactImportError> artifactErrors, String format, Object... params) {
        super(format, params);
        this.artifactErrors = artifactErrors;
    }

}
