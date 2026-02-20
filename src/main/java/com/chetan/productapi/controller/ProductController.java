package com.chetan.productapi.controller;

import com.chetan.productapi.entity.Product;
import com.chetan.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductRepository productRepository;

    @GetMapping
    public List<Product> getAll() {

        return productRepository.findAll();
    }


    @PostMapping
    public Product create(@RequestBody Product product) {

        return productRepository.save(product);
    }
}
