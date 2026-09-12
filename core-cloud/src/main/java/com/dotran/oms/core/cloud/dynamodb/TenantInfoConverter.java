package com.dotran.oms.core.cloud.dynamodb;

import com.dotran.oms.core.common.domain.TenantInfo;
import com.dotran.oms.core.common.domain.TenantSetting;
import com.dotran.oms.core.common.id.TenantId;
import org.springframework.stereotype.Component;

@Component
public class TenantInfoConverter {

    public TenantInfo toTenantInfo(TenantInfoItem item) {
        if (item == null) {
            return null;
        }

        return TenantInfo.builder()
                .id(TenantId.of(item.getPk()))
                .code(item.getCode())
                .name(item.getName())
                .status(item.getStatus())
                .settings(toTenantSetting(item.getSettings()))
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private TenantSetting toTenantSetting(TenantSettingsItem item) {
        if ( item == null ) {
            return null;
        }

        TenantSetting.TenantSettingBuilder tenantSetting = TenantSetting.builder();

        tenantSetting.timeZone( item.getTimeZone() );
        tenantSetting.currency( item.getCurrency() );
        tenantSetting.locale( item.getLocale() );

        return tenantSetting.build();
    }

    public TenantInfoItem toTenantInfoItem(TenantInfo tenantInfo) {
        if ( tenantInfo == null ) {
            return null;
        }

        TenantInfoItem tenantInfoItem = new TenantInfoItem();

        TenantId tenantId = tenantInfo.getId();

        tenantInfoItem.setPk( tenantId != null ? tenantId.getValue() : null );
        tenantInfoItem.setCode( tenantInfo.getCode() );
        tenantInfoItem.setName( tenantInfo.getName() );
        tenantInfoItem.setStatus( tenantInfo.getStatus() );
        tenantInfoItem.setSettings( toTenantSettingsItem( tenantInfo.getSettings() ) );
        tenantInfoItem.setCreatedAt( tenantInfo.getCreatedAt() );
        tenantInfoItem.setUpdatedAt( tenantInfo.getUpdatedAt() );

        return tenantInfoItem;
    }

    private TenantSettingsItem toTenantSettingsItem(TenantSetting settings) {
        if ( settings == null ) {
            return null;
        }

        TenantSettingsItem tenantSettingsItem = new TenantSettingsItem();

        tenantSettingsItem.setTimeZone( settings.getTimeZone() );
        tenantSettingsItem.setCurrency( settings.getCurrency() );
        tenantSettingsItem.setLocale( settings.getLocale() );

        return tenantSettingsItem;
    }
}
