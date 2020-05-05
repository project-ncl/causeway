package org.jboss.pnc.causeway;

/**
 * This class indicates just a failure, which can usually be solved by user themselve.
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CausewayFailure extends CausewayException {

    public CausewayFailure(String format, Throwable cause, Object... params) {
        super(format, cause, params);
    }

    public CausewayFailure(String format, Object... params) {
        super(format, params);
    }

}
