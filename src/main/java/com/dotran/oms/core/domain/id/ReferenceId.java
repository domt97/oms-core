package com.dotran.oms.core.domain.id;

import java.util.UUID;

public class ReferenceId extends BaseId<UUID> {

    public ReferenceId(UUID value) {
        super(value);
    }

    public static ReferenceId of(UUID value) {
        return new ReferenceId(value);
    }

    public static ReferenceId newReferenceId() {
        return new ReferenceId(UUID.randomUUID());
    }
}
