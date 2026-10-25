package com.dotran.oms.core.cloud.dynamodb;

import com.dotran.oms.core.annotation.PersistenceAdapter;
import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.id.MerchantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.Optional;

@PersistenceAdapter
@RequiredArgsConstructor
@Slf4j
public class DynamoDbMerchantInfoRepository {

    private final DynamoDbTable<MerchantInfoItem> merchantInfoTable;
    private final MerchantInfoConverter merchantInfoConverter;

    public Optional<MerchantInfo> findByMerchantId(MerchantId merchantId) {
        try {
            MerchantInfoItem key = new MerchantInfoItem();
            key.setPk(merchantId.getValue());
            
            MerchantInfoItem item = merchantInfoTable.getItem(key);

            if (item == null) {
                log.debug("MerchantInfo not found for merchantId: {}", merchantId.getValue());
                return Optional.empty();
            }

            return Optional.of(merchantInfoConverter.toMerchantInfo(item));
        } catch (Exception e) {
            log.error("Error fetching merchant info from DynamoDB for merchantId: {}", merchantId.getValue(), e);
            throw new RuntimeException("Failed to fetch merchant info", e);
        }
    }

    public void save(MerchantInfo merchantInfo) {
        try {
            MerchantInfoItem item = merchantInfoConverter.toMerchantInfoItem(merchantInfo);
            merchantInfoTable.putItem(item);
            log.info("MerchantInfo saved successfully for merchantId: {}", merchantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error saving merchant info to DynamoDB for merchantId: {}", merchantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to save merchant info", e);
        }
    }

    public void update(MerchantInfo merchantInfo) {
        try {
            MerchantInfoItem item = merchantInfoConverter.toMerchantInfoItem(merchantInfo);
            merchantInfoTable.updateItem(item);
            log.info("MerchantInfo updated successfully for merchantId: {}", merchantInfo.getId().getValue());
        } catch (Exception e) {
            log.error("Error updating merchant info in DynamoDB for merchantId: {}", merchantInfo.getId().getValue(), e);
            throw new RuntimeException("Failed to update merchant info", e);
        }
    }

    public void delete(MerchantId merchantId) {
        try {
            MerchantInfoItem key = new MerchantInfoItem();
            key.setPk(merchantId.getValue());
            merchantInfoTable.deleteItem(key);
            log.info("MerchantInfo deleted successfully for merchantId: {}", merchantId.getValue());
        } catch (Exception e) {
            log.error("Error deleting merchant info from DynamoDB for merchantId: {}", merchantId.getValue(), e);
            throw new RuntimeException("Failed to delete merchant info", e);
        }
    }
}
