package com.pipeline.domain.repository;

import java.util.Optional;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.valueobject.Barcode;

public interface ProductRepository {
    void save(Product product);
    void saveAll(Iterable<Product> products);
    Optional<Product> findByBarcode(Barcode barcode);
}
