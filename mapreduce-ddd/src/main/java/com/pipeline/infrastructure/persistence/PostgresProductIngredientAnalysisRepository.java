package com.pipeline.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.pipeline.domain.repository.ProductIngredientAnalysisRepository;
import com.pipeline.infrastructure.config.DatabaseConfig;

public class PostgresProductIngredientAnalysisRepository implements ProductIngredientAnalysisRepository {

    private static final String TABLE = "product_ingredients_analysis";

    private final DatabaseConfig config;

    public PostgresProductIngredientAnalysisRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public void save(String productBarcode, String value) {
        String sql = "INSERT INTO " + TABLE
                + " (product_barcode, value) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productBarcode);
            stmt.setString(2, value.trim().toLowerCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save to " + TABLE + ": " + value, e);
        }
    }
}
