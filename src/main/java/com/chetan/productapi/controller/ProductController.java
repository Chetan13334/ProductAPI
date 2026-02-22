package com.chetan.productapi.controller;

import com.chetan.productapi.dto.ItemResponse;
import com.chetan.productapi.dto.PageResponse;
import com.chetan.productapi.dto.ProductRequest;
import com.chetan.productapi.dto.ProductResponse;
import com.chetan.productapi.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page cannot be negative") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be at least 1") @Max(value = 100, message = "Size cannot exceed 100") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        return productService.getAllProducts(page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Integer id) {
        return productService.getProductById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, Authentication authentication) {
        return productService.createProduct(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication
    ) {
        return productService.updateProduct(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        productService.deleteProduct(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/items")
    public List<ItemResponse> getItems(@PathVariable Integer id) {
        return productService.getItemsByProductId(id);
    }
}
