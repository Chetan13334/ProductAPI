package com.chetan.productapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditService {

    @Async
    public void logProductAction(String action, Integer productId, String username) {
        log.info("Product action={} productId={} by={}", action, productId, username);
    }
}
