package org.jboss.pnc.causeway.rest.model;

import org.jboss.pnc.causeway.rest.CallbackTarget;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 *
 * @author Honza Brázdil <janinko.g@gmail.com>
 */
@Data
@Builder
public class BuildImportRequest {

    @NonNull
    private final CallbackTarget callback;
    @NonNull
    private final Build build;

}
