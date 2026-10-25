package com.dotran.oms.core.domain.id;

import java.util.UUID;

public class MerchantId extends BaseId<UUID> {

    public MerchantId(UUID value) {
        super(value);
    }

    public static MerchantId of(UUID value) {
        return new MerchantId(value);
    }

    public static MerchantId newMerchantId() {
        return new MerchantId(UUID.randomUUID());
    }
}
