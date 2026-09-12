package com.dotran.oms.core.common.id;

import java.util.UUID;

public class OrderItemId extends BaseId<UUID> {

    public OrderItemId(UUID value) {
        super(value);
    }

    public static OrderItemId of(UUID value) {
        return new OrderItemId(value);
    }

    public static OrderItemId newOrderItemId() {
        return new OrderItemId(UUID.randomUUID());
    }
}
