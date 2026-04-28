package com.hng.stage3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
    
    @JsonProperty("total_pages")
    private Integer totalPages;
    
    private Map<String, String> links;
    private List<T> data;

    public static <T> PagedResponse<T> success(List<T> data, int page, int limit, long total, int totalPages, Map<String, String> links) {
        return PagedResponse.<T>builder()
                .status("success")
                .page(page)
                .limit(limit)
                .total(total)
                .totalPages(totalPages)
                .links(links)
                .data(data)
                .build();
    }
}
