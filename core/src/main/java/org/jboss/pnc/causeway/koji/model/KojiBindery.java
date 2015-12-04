package org.jboss.pnc.causeway.koji.model;

import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.rwx.binding.internal.reflect.ReflectionMapper;
import org.commonjava.rwx.binding.internal.xbr.XBRCompositionBindery;
import org.commonjava.rwx.binding.mapping.Mapping;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

/**
 * Created by jdcasey on 12/3/15.
 */
@ApplicationScoped
public class KojiBindery
        extends XBRCompositionBindery
{
    public KojiBindery()
            throws BindException
    {
        super( getRecipes() );
    }

    private static Map<Class<?>, Mapping<?>> getRecipes()
            throws BindException
    {
        Class<?>[] classes =
                { LoginRequest.class, LoginResponse.class, ApiVersionRequest.class, ApiVersionResponse.class,
                        LoggedInUserRequest.class, LoggedInUserResponse.class, LogoutRequest.class, LogoutResponse.class };

        return new ReflectionMapper().loadRecipes( classes );
    }
}
