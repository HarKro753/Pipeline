package com.pipeline.domain.repository;

import com.pipeline.domain.model.Product;

public interface ProductRepository {
    void save(Product product);
    void saveAll(Iterable<Product> products);
}
