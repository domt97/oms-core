package com.dotran.oms.core.logging;

import com.dotran.oms.core.logging.MdcProperties.Format;
import com.dotran.oms.core.logging.MdcProperties.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MdcPropertiesTest {

    private static MdcProperties bind(Map<String, String> yaml) {
        MdcProperties properties = new MdcProperties();
        new Binder(new MapConfigurationPropertySource(yaml)).bind("app.logging.mdc", Bindable.ofInstance(properties));
        properties.validate();
        return properties;
    }

    @Test
    @DisplayName("defaults: the common ids from the path; requestId is the only propagated key")
    void defaults() {
        MdcProperties properties = bind(Map.of());

        assertThat(properties.fieldsFrom(Source.PATH)).containsOnlyKeys(
                "merchantId", "storeId", "productId", "inventoryId", "collectionId", "storeAvailabilityId");
        assertThat(properties.fieldsFrom(Source.QUERY)).isEmpty();
        assertThat(properties.propagatedKeys()).containsExactly("requestId");
    }

    @Test
    @DisplayName("a service's entries are merged into the defaults; an overridden key replaces its entry")
    void serviceEntriesMerge() {
        MdcProperties properties = bind(Map.of(
                "app.logging.mdc.fields.sku.format", "code",
                "app.logging.mdc.fields.sku.sources", "path,query",
                "app.logging.mdc.fields.merchantId.format", "uuid",
                "app.logging.mdc.fields.merchantId.propagate", "true"));

        assertThat(properties.getFields()).containsKeys("storeId", "productId", "sku", "merchantId");
        assertThat(properties.getFields().get("sku").getFormat()).isEqualTo(Format.CODE);
        assertThat(properties.fieldsFrom(Source.QUERY)).containsOnlyKeys("sku");
        assertThat(properties.propagatedKeys()).containsExactly("merchantId", "requestId");
    }

    @Test
    @DisplayName("a broken entry fails at startup with the key in the message")
    void brokenEntriesFail() {
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.orderNo.format", "pattern")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("orderNo").hasMessageContaining("pattern");
        assertThatThrownBy(() -> bind(Map.of(
                "app.logging.mdc.fields.orderNo.format", "pattern",
                "app.logging.mdc.fields.orderNo.pattern", "^(ORD")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("invalid pattern");
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.requestId.sources", "query")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("requestId");
    }

    @Test
    @DisplayName("an invalid key name fails at startup")
    void invalidKeyName() {
        MdcProperties properties = new MdcProperties();
        properties.getFields().put("bad key", MdcProperties.Field.of(Format.CODE, false, Source.PATH));

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class).hasMessageContaining("bad key");
    }

    @Test
    @DisplayName("formats: uuid, code (1-64 safe chars) and pattern (length capped)")
    void formats() {
        MdcProperties.Field uuid = MdcProperties.Field.of(Format.UUID, false, Source.PATH);
        MdcProperties.Field code = MdcProperties.Field.of(Format.CODE, false, Source.PATH);
        MdcProperties.Field pattern = MdcProperties.Field.of(Format.PATTERN, false, Source.PATH);
        pattern.setPattern("^[a-z]+$");
        pattern.validate("p");

        assertThat(uuid.accepts("3f1c2a9e-7d4b-4c1e-9a0b-1234567890ab")).isTrue();
        assertThat(uuid.accepts("TS-M")).isFalse();
        assertThat(code.accepts("TS-M.01")).isTrue();
        assertThat(code.accepts("a b")).isFalse();
        assertThat(code.accepts(null)).isFalse();
        assertThat(pattern.accepts("abc")).isTrue();
        assertThat(pattern.accepts("a".repeat(MdcProperties.MAX_PATTERN_VALUE_LENGTH + 1))).isFalse();
    }

    @Test
    @DisplayName("message keys are reserved: they take propagate but never a request source")
    void reservedKeys() {
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.eventId.sources", "path")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("eventId");

        MdcProperties properties = bind(Map.of(
                "app.logging.mdc.fields.eventId.sources", "",
                "app.logging.mdc.fields.eventId.propagate", "true"));
        assertThat(properties.propagatedKeys()).contains("eventId");
        assertThat(properties.fieldsFrom(Source.PATH)).doesNotContainKey("eventId");
    }

    @Test
    @DisplayName("keys that differ only by case (e.g. from an environment variable) fail at startup")
    void caseCollisions() {
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.merchantid.format", "uuid")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("merchantId");
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.eventid.sources", "")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("eventId");
    }

    @Test
    @DisplayName("a pattern without format: pattern fails instead of being ignored")
    void patternWithoutPatternFormat() {
        assertThatThrownBy(() -> bind(Map.of("app.logging.mdc.fields.orderNo.pattern", "^ORD-[0-9]+$")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("only used with format: pattern");
    }

    @Test
    @DisplayName("accepts never throws, also on an entry that was never validated")
    void acceptsNullFormat() {
        MdcProperties.Field field = new MdcProperties.Field();
        field.setFormat(null);

        assertThat(field.accepts("x")).isFalse();
    }
}
