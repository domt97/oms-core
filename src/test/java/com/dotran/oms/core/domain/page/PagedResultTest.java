package com.dotran.oms.core.domain.page;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PagedResult")
class PagedResultTest {

    private final PagedResult<String> page = PagedResult.<String>builder()
            .content(List.of("a", "b")).pageNumber(1).pageSize(20).totalElements(61).totalPages(4).isLast(false).build();

    @Test
    @DisplayName("JSON keys stay content, pageNumber, pageSize, totalElements, totalPages, last (services return it; oms-web reads it)")
    void jsonShape() {
        JsonNode json = new ObjectMapper().valueToTree(page);

        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("content", "pageNumber", "pageSize", "totalElements", "totalPages", "last");
        assertThat(json.get("last").asBoolean()).isFalse();
        assertThat(json.get("totalElements").asLong()).isEqualTo(61);
    }

    @Test
    @DisplayName("map converts the content in order and keeps every paging value")
    void mapKeepsPaging() {
        PagedResult<Integer> mapped = page.map(String::length);

        assertThat(mapped.getContent()).containsExactly(1, 1);
        assertThat(mapped.getPageNumber()).isEqualTo(1);
        assertThat(mapped.getPageSize()).isEqualTo(20);
        assertThat(mapped.getTotalElements()).isEqualTo(61);
        assertThat(mapped.getTotalPages()).isEqualTo(4);
        assertThat(mapped.isLast()).isFalse();
    }
}
