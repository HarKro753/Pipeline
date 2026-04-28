package com.pipeline.domain.repository;

public interface ProductPackagingRepository {
    void save(String productBarcode, String value);
}
