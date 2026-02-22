package com.chetan.productapi.service;

import com.chetan.productapi.dto.ItemResponse;
import com.chetan.productapi.dto.PageResponse;
import com.chetan.productapi.dto.ProductRequest;
import com.chetan.productapi.dto.ProductResponse;
import com.chetan.productapi.entity.Product;
import com.chetan.productapi.exception.ResourceNotFoundException;
import com.chetan.productapi.mapper.ProductMapper;
import com.chetan.productapi.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.ASC);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<ProductResponse> result = productRepository.findAll(pageRequest).map(ProductMapper::toResponse);
        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Integer id) {
        return ProductMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        Product product = Product.builder()
                .productName(request.productName())
                .createdBy(username)
                .createdOn(LocalDateTime.now())
                .build();
        product.setItems(ProductMapper.toItems(request));

        Product saved = productRepository.save(product);
        auditService.logProductAction("CREATE", saved.getId(), username);
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Integer id, ProductRequest request, String username) {
        Product product = findEntityById(id);
        product.setProductName(request.productName());
        product.setModifiedBy(username);
        product.setModifiedOn(LocalDateTime.now());
        product.setItems(ProductMapper.toItems(request));

        Product saved = productRepository.save(product);
        auditService.logProductAction("UPDATE", saved.getId(), username);
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProduct(Integer id, String username) {
        Product product = findEntityById(id);
        productRepository.delete(product);
        auditService.logProductAction("DELETE", id, username);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByProductId(Integer id) {
        Product product = findEntityById(id);
        return product.getItems().stream().map(item -> new ItemResponse(item.getId(), item.getQuantity())).toList();
    }

    private Product findEntityById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
