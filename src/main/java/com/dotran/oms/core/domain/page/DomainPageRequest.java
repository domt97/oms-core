package com.dotran.oms.core.domain.page;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DomainPageRequest {

    private int pageNumber;
    private int pageSize;
    private String sortBy;
    private String direction;
}
