package com.dbwb.platform.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list, and enough about the whole list to ask for the rest.
 *
 * Deliberately not Spring Data's Page, which serialises its Pageable and its
 * Sort as well - a dozen fields a client never reads, and a shape the Spring
 * team warn changes between versions. This is the four numbers a caller
 * actually uses.
 *
 * `total` is what makes the count callers cheap: asking for one row and
 * reading it costs a single row over the wire, where they used to fetch every
 * item a website had in order to call .length on it.
 */
public record PagedResponse<T>(List<T> items, int page, int size, long total, boolean hasMore) {

    public static <E, T> PagedResponse<T> from(Page<E> page, Function<E, T> map) {
        return new PagedResponse<>(
                page.getContent().stream().map(map).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.hasNext());
    }
}
