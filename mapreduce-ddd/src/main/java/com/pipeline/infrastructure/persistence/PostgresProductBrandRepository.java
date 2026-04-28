package com.pipeline.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.pipeline.domain.repository.ProductBrandRepository;
import com.pipeline.infrastructure.config.DatabaseConfig;

public class PostgresProductBrandRepository implements ProductBrandRepository {

    private static final String SQL =
            "INSERT INTO product_brands (product_barcode, value) VALUES (?, ?) ON CONFLICT DO NOTHING";

    private final DatabaseConfig config;

    public PostgresProductBrandRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public void save(String productBarcode, String value) {
        try {
            Connection conn = config.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
                stmt.setString(1, productBarcode);
                stmt.setString(2, value.trim().toLowerCase());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save to product_brands: " + value, e);
        }
    }
}
