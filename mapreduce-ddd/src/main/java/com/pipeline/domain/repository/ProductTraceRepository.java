package com.pipeline.domain.repository;

public interface ProductTraceRepository {
    void save(String productBarcode, String value);
}
