package com.chetan.productapi.service;

import com.chetan.productapi.dto.ItemRequest;
import com.chetan.productapi.dto.PageResponse;
import com.chetan.productapi.dto.ProductRequest;
import com.chetan.productapi.dto.ProductResponse;
import com.chetan.productapi.entity.Item;
import com.chetan.productapi.entity.Product;
import com.chetan.productapi.exception.ResourceNotFoundException;
import com.chetan.productapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductShouldPersistAndReturnResponse() {
        ProductRequest request = new ProductRequest("Laptop", List.of(new ItemRequest(2)));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0, Product.class);
            saved.setId(1);
            saved.getItems().forEach(item -> item.setId(11));
            return saved;
        });

        ProductResponse response = productService.createProduct(request, "admin");

        assertEquals(1, response.id());
        assertEquals("Laptop", response.productName());
        assertEquals("admin", response.createdBy());
        assertEquals(1, response.items().size());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        verify(auditService).logProductAction("CREATE", 1, "admin");
        assertEquals(1, captor.getValue().getItems().size());
    }

    @Test
    void getAllProductsShouldReturnPageResponse() {
        Product product = Product.builder()
                .id(7)
                .productName("Phone")
                .createdBy("admin")
                .createdOn(LocalDateTime.now())
                .items(List.of(Item.builder().id(1).quantity(3).build()))
                .build();

        when(productRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1));

        PageResponse<ProductResponse> response = productService.getAllProducts(0, 10, "id", "asc");

        assertEquals(1, response.content().size());
        assertEquals(7, response.content().get(0).id());
        assertEquals(1, response.totalElements());
    }

    @Test
    void deleteProductShouldThrowWhenNotFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(99, "admin"));
    }
}
