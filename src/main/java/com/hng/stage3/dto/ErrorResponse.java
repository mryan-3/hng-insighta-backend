package com.hng.stage3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String status;
    private String message;

    public static ErrorResponse of(String message) {
        return new ErrorResponse("error", message);
    }
}
