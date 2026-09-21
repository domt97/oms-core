package com.dotran.oms.core.domain.id;

import java.util.UUID;

public class OrderId extends BaseId<UUID> {

    public OrderId(UUID value) {
        super(value);
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId newOrderId() {
        return new OrderId(UUID.randomUUID());
    }
}
