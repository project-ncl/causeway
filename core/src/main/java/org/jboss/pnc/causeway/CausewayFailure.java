package org.jboss.pnc.causeway;

import org.jboss.pnc.rest.restmodel.causeway.ArtifactImportError;

import java.util.List;

import lombok.Getter;

/**
 * This class indicates just a failure, which can usually be solved by user themselve.
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
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

    public CausewayFailure(List<ArtifactImportError> artifactErrors, String format, Object... params) {
        super(format, params);
        this.artifactErrors = artifactErrors;
    }

}
