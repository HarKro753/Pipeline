package com.pipeline.domain.repository;

public interface ProductLabelRepository {
    void save(String productBarcode, String value);
}
