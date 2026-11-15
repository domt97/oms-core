package com.dotran.oms.core.domain.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results, framework-free (the domain has no Spring imports). Adapters build it from their own paging type,
 * e.g. {@code PageUtils.toPagedResult(Page)} for Spring Data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;

    /**
     * Converts this PagedResult's content to a different type using the provided converter function.
     * Pagination metadata (pageNumber, pageSize, totalElements, etc.) is preserved.
     *
     * @param converter the function to convert each element from type T to type U
     * @param <U>       target content type
     * @return new PagedResult with converted content
     */
    public <U> PagedResult<U> map(Function<T, U> converter) {
        List<U> convertedContent = this.content.stream()
                .map(converter)
                .toList();

        return PagedResult.<U>builder()
                .content(convertedContent)
                .pageNumber(this.pageNumber)
                .pageSize(this.pageSize)
                .totalElements(this.totalElements)
                .totalPages(this.totalPages)
                .isLast(this.isLast)
                .build();
    }
}
