package com.krypto.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private int numberOfElements;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean empty;
    private String sortBy;
    private String sortDirection;

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return of(content, page, size, totalElements, content.size(), null, null);
    }

    public static <T> PageResponse<T> of(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int numberOfElements,
            String sortBy,
            String sortDirection
    ) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .numberOfElements(numberOfElements)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(first)
                .last(last)
                .hasNext(!last)
                .hasPrevious(!first)
                .empty(numberOfElements == 0)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}
