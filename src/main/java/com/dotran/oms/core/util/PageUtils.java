package com.dotran.oms.core.util;

import com.dotran.oms.core.domain.page.DomainPageRequest;
import com.dotran.oms.core.domain.page.PagedResult;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

/** Bridge between the framework-free paging types in {@code domain.page} and Spring Data. */
@UtilityClass
public class PageUtils {

    public static PageRequest toPageRequest(DomainPageRequest domainPageRequest) {
        if (StringUtils.isNullOrEmpty(domainPageRequest.getSortBy())) {
            return PageRequest.of(domainPageRequest.getPageNumber(), domainPageRequest.getPageSize());
        }

        Sort.Direction sortDirection = Optional.ofNullable(domainPageRequest.getDirection())
                .map(String::toUpperCase)
                .map(Sort.Direction::valueOf)
                .orElse(Sort.Direction.ASC);

        return PageRequest.of(
                domainPageRequest.getPageNumber(),
                domainPageRequest.getPageSize(),
                sortDirection,
                domainPageRequest.getSortBy());
    }

    /**
     * Spring Data page → {@link PagedResult} (content and paging values as Spring computed them). To convert the
     * elements too, map the page first: {@code toPagedResult(page.map(mapper::toDomain))}.
     */
    public static <T> PagedResult<T> toPagedResult(Page<T> page) {
        return PagedResult.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
