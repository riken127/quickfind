package com.riken.product_service.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riken.product_service.model.Product;
import com.riken.product_service.repository.ProductRepository;

import io.nats.client.Connection;
import io.nats.client.Nats;



@Service
@EnableCaching
public class ProductService {
    private final ProductRepository productRepository;
    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    public ProductService(ProductRepository productRepository) throws Exception {
        this.productRepository = productRepository;
        this.natsConnection = Nats.connect("nats://localhost:4222");
        this.objectMapper = new ObjectMapper();
    }

    public List<Product> getAllProducts() {
        return this.productRepository.findAll();
    }

    @Cacheable(value = "hotels", key = "#id")
    public Optional<Product> getProductById(String id) {
        return this.productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        publishProductEvent("product.created", savedProduct);
        return savedProduct;
    }

    private void publishProductEvent(String subject, Product product) {
        try {
            String message = objectMapper.writeValueAsString(product);
            natsConnection.publish(subject, message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String serializeProduct(Product product) {
        try {
            return objectMapper.writeValueAsString(product);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}  