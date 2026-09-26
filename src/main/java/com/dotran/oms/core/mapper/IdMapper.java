package com.dotran.oms.core.mapper;

import com.dotran.oms.core.domain.id.CategoryId;
import com.dotran.oms.core.domain.id.CustomerId;
import com.dotran.oms.core.domain.id.InventoryId;
import com.dotran.oms.core.domain.id.OrderId;
import com.dotran.oms.core.domain.id.OrderItemId;
import com.dotran.oms.core.domain.id.ProductId;
import com.dotran.oms.core.domain.id.ReferenceId;
import com.dotran.oms.core.domain.id.SKU;
import com.dotran.oms.core.domain.id.StoreId;
import com.dotran.oms.core.domain.id.TenantId;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

public interface IdMapper {

    @Named("toStoreId")
    default StoreId toStoreId(UUID id) {
        return id == null ? null : new StoreId(id);
    }


    @Named("toSKU")
    default SKU toSKU(String id) {
        return id == null ? null : new SKU(id);
    }

    @Named("toSKUList")
    default List<SKU> toSKUList(List<String> skus) {
        return skus == null ? null : skus.stream().map(this::toSKU).toList();
    }

    @Named("toProductId")
    default ProductId toProductId(UUID id) {
        return id == null ? null : new ProductId(id);
    }

    @Named("toCategoryId")
    default CategoryId toCategoryId(UUID id) {
        return id == null ? null : new CategoryId(id);
    }

    @Named("toTenantId")
    default TenantId toTenantId(UUID id) {
        return id == null ? null : new TenantId(id);
    }

    @Named("toCustomerId")
    default CustomerId toCustomerId(UUID id) {
        return id == null ? null : new CustomerId(id);
    }

    @Named("toInventoryId")
    default InventoryId toInventoryId(UUID id) {
        return id == null ? null : new InventoryId(id);
    }

    @Named("toReferenceId")
    default ReferenceId toReferenceId(UUID id) {
        return id == null ? null : new ReferenceId(id);
    }

    @Named("toOrderId")
    default OrderId toOrderId(UUID id) {
        return id == null ? null : new OrderId(id);
    }

    @Named("toOrderItemId")
    default OrderItemId toOrderItemId(UUID id) {
        return id == null ? null : new OrderItemId(id);
    }
}
