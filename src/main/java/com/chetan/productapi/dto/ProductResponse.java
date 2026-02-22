package com.chetan.productapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Integer id,
        String productName,
        String createdBy,
        LocalDateTime createdOn,
        String modifiedBy,
        LocalDateTime modifiedOn,
        List<ItemResponse> items
) {
}
