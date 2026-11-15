package com.dotran.oms.core.util;

import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageUtils")
class PageUtilsTest {

    private static void assertPaging(PagedResult<?> r, int number, int size, long total, int pages, boolean last) {
        assertThat(r.getPageNumber()).as("pageNumber").isEqualTo(number);
        assertThat(r.getPageSize()).as("pageSize").isEqualTo(size);
        assertThat(r.getTotalElements()).as("totalElements").isEqualTo(total);
        assertThat(r.getTotalPages()).as("totalPages").isEqualTo(pages);
        assertThat(r.isLast()).as("last").isEqualTo(last);
    }

    @Test
    @DisplayName("toPagedResult: a middle page keeps content in order and every paging value")
    void middlePage() {
        PagedResult<String> r = PageUtils.toPagedResult(new PageImpl<>(List.of("b", "a"), PageRequest.of(1, 20), 61));

        assertThat(r.getContent()).containsExactly("b", "a");
        assertPaging(r, 1, 20, 61, 4, false);
    }

    @Test
    @DisplayName("toPagedResult: the last page is marked last")
    void lastPage() {
        assertPaging(PageUtils.toPagedResult(new PageImpl<>(List.of("x"), PageRequest.of(3, 20), 61)), 3, 20, 61, 4, true);
    }

    @Test
    @DisplayName("toPagedResult: an empty page keeps Spring's values")
    void emptyPage() {
        Page<String> empty = Page.empty();
        PagedResult<String> r = PageUtils.toPagedResult(empty);

        assertThat(r.getContent()).isEmpty();
        assertPaging(r, empty.getNumber(), empty.getSize(), 0, empty.getTotalPages(), true);
        assertPaging(PageUtils.toPagedResult(new PageImpl<String>(List.of(), PageRequest.of(0, 20), 0)), 0, 20, 0, 0, true);
        // a page past the end: no content, the totals stay, it counts as the last page
        assertPaging(PageUtils.toPagedResult(new PageImpl<String>(List.of(), PageRequest.of(5, 20), 61)), 5, 20, 61, 4, true);
    }

    @Test
    @DisplayName("toPageRequest: no sort, a missing direction is ASC, a lower-case direction is read")
    void toPageRequest() {
        assertThat(PageUtils.toPageRequest(DomainPageRequest.builder().pageNumber(2).pageSize(5).build()))
                .isEqualTo(PageRequest.of(2, 5));
        assertThat(PageUtils.toPageRequest(DomainPageRequest.builder().pageNumber(0).pageSize(5).sortBy("name").build()))
                .isEqualTo(PageRequest.of(0, 5, Sort.Direction.ASC, "name"));
        assertThat(PageUtils.toPageRequest(DomainPageRequest.builder().pageNumber(0).pageSize(5).sortBy("name").direction("desc").build()))
                .isEqualTo(PageRequest.of(0, 5, Sort.Direction.DESC, "name"));
    }
}
