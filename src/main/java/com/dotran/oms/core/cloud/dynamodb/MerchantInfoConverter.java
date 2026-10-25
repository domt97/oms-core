package com.dotran.oms.core.cloud.dynamodb;

import com.dotran.oms.core.domain.MerchantInfo;
import com.dotran.oms.core.domain.MerchantSetting;
import com.dotran.oms.core.domain.id.MerchantId;
import org.springframework.stereotype.Component;

@Component
public class MerchantInfoConverter {

    public MerchantInfo toMerchantInfo(MerchantInfoItem item) {
        if (item == null) {
            return null;
        }

        return MerchantInfo.builder()
                .id(MerchantId.of(item.getPk()))
                .code(item.getCode())
                .name(item.getName())
                .status(item.getStatus())
                .settings(toMerchantSetting(item.getSettings()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private MerchantSetting toMerchantSetting(MerchantSettingsItem item) {
        if ( item == null ) {
            return null;
        }

        MerchantSetting.MerchantSettingBuilder merchantSetting = MerchantSetting.builder();

        merchantSetting.timeZone( item.getTimeZone() );
        merchantSetting.currency( item.getCurrency() );
        merchantSetting.locale( item.getLocale() );

        return merchantSetting.build();
    }

    public MerchantInfoItem toMerchantInfoItem(MerchantInfo merchantInfo) {
        if ( merchantInfo == null ) {
            return null;
        }

        MerchantInfoItem merchantInfoItem = new MerchantInfoItem();

        MerchantId merchantId = merchantInfo.getId();

        merchantInfoItem.setPk( merchantId != null ? merchantId.getValue() : null );
        merchantInfoItem.setCode( merchantInfo.getCode() );
        merchantInfoItem.setName( merchantInfo.getName() );
        merchantInfoItem.setStatus( merchantInfo.getStatus() );
        merchantInfoItem.setSettings( toMerchantSettingsItem( merchantInfo.getSettings() ) );
        merchantInfoItem.setCreatedAt( merchantInfo.getCreatedAt() );
        merchantInfoItem.setUpdatedAt( merchantInfo.getUpdatedAt() );

        return merchantInfoItem;
    }

    private MerchantSettingsItem toMerchantSettingsItem(MerchantSetting settings) {
        if ( settings == null ) {
            return null;
        }

        MerchantSettingsItem merchantSettingsItem = new MerchantSettingsItem();

        merchantSettingsItem.setTimeZone( settings.getTimeZone() );
        merchantSettingsItem.setCurrency( settings.getCurrency() );
        merchantSettingsItem.setLocale( settings.getLocale() );

        return merchantSettingsItem;
    }
}
