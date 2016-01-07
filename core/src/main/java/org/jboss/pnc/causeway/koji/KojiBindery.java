package org.jboss.pnc.causeway.koji;

import org.commonjava.rwx.binding.error.BindException;
import org.commonjava.rwx.binding.internal.reflect.ReflectionMapper;
import org.commonjava.rwx.binding.internal.xbr.XBRCompositionBindery;
import org.commonjava.rwx.binding.mapping.Mapping;
import org.jboss.pnc.causeway.koji.model.messages.AllPermissionsRequest;
import org.jboss.pnc.causeway.koji.model.messages.AllPermissionsResponse;
import org.jboss.pnc.causeway.koji.model.messages.ApiVersionRequest;
import org.jboss.pnc.causeway.koji.model.messages.ApiVersionResponse;
import org.jboss.pnc.causeway.koji.model.messages.LoggedInUserRequest;
import org.jboss.pnc.causeway.koji.model.messages.LoginRequest;
import org.jboss.pnc.causeway.koji.model.messages.LoginResponse;
import org.jboss.pnc.causeway.koji.model.messages.LogoutRequest;
import org.jboss.pnc.causeway.koji.model.messages.LogoutResponse;
import org.jboss.pnc.causeway.koji.model.messages.TagRequest;
import org.jboss.pnc.causeway.koji.model.messages.TagResponse;
import org.jboss.pnc.causeway.koji.model.messages.UserResponse;

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
                        LoggedInUserRequest.class, UserResponse.class, LogoutRequest.class, LogoutResponse.class,
                        TagRequest.class, TagResponse.class, AllPermissionsRequest.class, AllPermissionsResponse.class };

        return new ReflectionMapper().loadRecipes( classes );
    }
}
