package com.dotran.oms.core.cloud.dynamodb;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.TenantInfo;
import com.dotran.oms.core.domain.id.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class DynamoDbTenantInfoRepository {

    private final DynamoDbTable<TenantInfoItem> tenantInfoTable;
    private final TenantInfoConverter tenantInfoConverter;

    public Optional<TenantInfo> findByTenantId(TenantId tenantId) {
        try {
            TenantInfoItem key = new TenantInfoItem();
            key.setPk(tenantId.getValue());
            
            TenantInfoItem item = tenantInfoTable.getItem(key);

            if (item == null) {
                log.debug("TenantInfo not found for tenantId: {}", tenantId.getValue());
                return Optional.empty();
            }

            return Optional.of(tenantInfoConverter.toTenantInfo(item));
        } catch (Exception e) {
            log.error("Error fetching tenant info from DynamoDB for tenantId: {}", tenantId.getValue(), e);
            throw new RuntimeException("Failed to fetch tenant info", e);
        }
    }

    public void save(TenantInfo tenantInfo) {
        try {
            TenantInfoItem item = tenantInfoConverter.toTenantInfoItem(tenantInfo);
            tenantInfoTable.putItem(item);
            log.info("TenantInfo saved successfully for tenantId: {}", tenantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error saving tenant info to DynamoDB for tenantId: {}", tenantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to save tenant info", e);
        }
    }

    public void update(TenantInfo tenantInfo) {
        try {
            TenantInfoItem item = tenantInfoConverter.toTenantInfoItem(tenantInfo);
            tenantInfoTable.updateItem(item);
            log.info("TenantInfo updated successfully for tenantId: {}", tenantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error updating tenant info in DynamoDB for tenantId: {}", tenantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to update tenant info", e);
        }
    }

    public void delete(TenantId tenantId) {
        try {
            TenantInfoItem key = new TenantInfoItem();
            key.setPk(tenantId.getValue());
            tenantInfoTable.deleteItem(key);
            log.info("TenantInfo deleted successfully for tenantId: {}", tenantId.getValue());
        } catch (Exception e) {
            log.error("Error deleting tenant info from DynamoDB for tenantId: {}", tenantId.getValue(), e);
            throw new RuntimeException("Failed to delete tenant info", e);
        }
    }
}
