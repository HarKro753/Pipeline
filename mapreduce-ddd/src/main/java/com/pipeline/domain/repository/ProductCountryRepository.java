package com.pipeline.domain.repository;

public interface ProductCountryRepository {
    void save(String productBarcode, String value);
}
