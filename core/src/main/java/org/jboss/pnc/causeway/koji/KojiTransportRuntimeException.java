package org.jboss.pnc.causeway.koji;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Created by jdcasey on 11/13/15.
 */
public class KojiTransportRuntimeException
        extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private Object[] params;

    private transient String formattedMessage;

    public KojiTransportRuntimeException(String format, Throwable cause, Object... params) {
        super(format, cause);
        this.params = params;
    }

    public KojiTransportRuntimeException(String format, Object... params) {
        super(format);
        this.params = params;
    }

    @Override
    public synchronized String getMessage() {
        if (formattedMessage == null) {
            final String format = super.getMessage();
            if (params == null || params.length < 1) {
                formattedMessage = format;
            } else {
                final String original = super.getMessage();
                try {
                    formattedMessage = String.format(
                            format.replaceAll("\\{\\}", "%s"), params);
                } catch (final Error | Exception e) {
                }

                if (formattedMessage == null || original == formattedMessage) {
                    try {
                        formattedMessage = MessageFormat.format( format, params );
                    } catch (final Error | Exception e) {
                        formattedMessage = format;
                        throw e;
                    }
                }
            }
        }

        return formattedMessage;
    }

    /**
     * Stringify all parameters pre-emptively on serialization, to prevent
     * {@link NotSerializableException}. Since all parameters are used in
     * {@link String#format} or {@link MessageFormat#format}, flattening them to
     * strings is an acceptable way to provide this functionality without making
     * the use of {@link Serializable} viral.
     */
    private Object writeReplace() {
        final Object[] newParams = new Object[params.length];
        int i = 0;
        for (final Object object : params) {
            newParams[i] = String.valueOf(object);
            i++;
        }

        params = newParams;
        return this;
    }

}
