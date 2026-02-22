package com.chetan.productapi.mapper;

import com.chetan.productapi.dto.ItemRequest;
import com.chetan.productapi.dto.ItemResponse;
import com.chetan.productapi.dto.ProductRequest;
import com.chetan.productapi.dto.ProductResponse;
import com.chetan.productapi.entity.Item;
import com.chetan.productapi.entity.Product;

import java.util.Collections;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        List<ItemResponse> itemResponses = product.getItems() == null
                ? Collections.emptyList()
                : product.getItems().stream()
                .map(item -> new ItemResponse(item.getId(), item.getQuantity()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getCreatedBy(),
                product.getCreatedOn(),
                product.getModifiedBy(),
                product.getModifiedOn(),
                itemResponses
        );
    }

    public static List<Item> toItems(ProductRequest request) {
        if (request.items() == null) {
            return Collections.emptyList();
        }
        return request.items().stream().map(ProductMapper::toItem).toList();
    }

    private static Item toItem(ItemRequest request) {
        return Item.builder().quantity(request.quantity()).build();
    }
}
