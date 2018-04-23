package org.jboss.pnc.causeway.rest;

import static org.junit.Assert.assertFalse;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Honza Brázdil &lt;jbrazdil@redhat.com&gt;
 */
public class CallbackTargetTest {

    @Test
    public void testSecurityCensoring() {
        Map<String, String> headers = new HashMap<>();
        headers.put("foo", "bar");
        headers.put("Authorization", "top-secret-token");
        CallbackTarget ct = new CallbackTarget("http://foo.bar/", CallbackMethod.POST, headers);
        String toString = ct.toString();
        assertFalse("Secret token should be censored", toString.contains("top-secret-token"));
    }
}
