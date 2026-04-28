package com.hng.stage3.dto;

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
public class PagedResponse<T> {
    private String status;
    private Integer page;
    private Integer limit;
    private Long total;
    private List<T> data;

    public static <T> PagedResponse<T> success(List<T> data, int page, int limit, long total) {
        return PagedResponse.<T>builder()
                .status("success")
                .page(page)
                .limit(limit)
                .total(total)
                .data(data)
                .build();
    }
}
