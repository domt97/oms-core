package com.dotran.oms.core.common.id;

import java.util.UUID;

public class InventoryId extends BaseId<UUID> {

    public InventoryId(UUID value) {
        super(value);
    }

    public static InventoryId of(UUID value) {
        return new InventoryId(value);
    }

    public static InventoryId newInventoryId() {
        return new InventoryId(UUID.randomUUID());
    }
}
