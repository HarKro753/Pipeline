package com.pipeline.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.repository.ProductRepository;
import com.pipeline.infrastructure.config.DatabaseConfig;

public class PostgresProductRepository implements ProductRepository {

    private static final String SQL = """
            INSERT INTO products (barcode, name, generic_name, quantity,
                nutriscore_score, nutriscore_grade, nova_group,
                energy_kcal, fat, saturated_fat, sugars, salt, proteins, fiber)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (barcode) DO UPDATE SET
                name = COALESCE(EXCLUDED.name, products.name),
                generic_name = COALESCE(EXCLUDED.generic_name, products.generic_name),
                quantity = COALESCE(EXCLUDED.quantity, products.quantity),
                nutriscore_score = EXCLUDED.nutriscore_score,
                nutriscore_grade = COALESCE(EXCLUDED.nutriscore_grade, products.nutriscore_grade),
                nova_group = EXCLUDED.nova_group,
                energy_kcal = EXCLUDED.energy_kcal, fat = EXCLUDED.fat,
                saturated_fat = EXCLUDED.saturated_fat, sugars = EXCLUDED.sugars,
                salt = EXCLUDED.salt, proteins = EXCLUDED.proteins, fiber = EXCLUDED.fiber
            """;

    private final DatabaseConfig config;

    public PostgresProductRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public void save(Product product) {
        try {
            Connection conn = config.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(SQL)) {
                stmt.setString(1, product.getBarcode().getValue());
                stmt.setString(2, product.getName());
                stmt.setString(3, product.getGenericName());
                stmt.setString(4, product.getQuantity());
                stmt.setInt(5, product.getNutriScore().getScore());
                stmt.setString(6, product.getNutriScore().getGrade());
                stmt.setInt(7, product.getNovaGroup());
                stmt.setDouble(8, product.getNutrientInfo().getEnergyKcal());
                stmt.setDouble(9, product.getNutrientInfo().getFat());
                stmt.setDouble(10, product.getNutrientInfo().getSaturatedFat());
                stmt.setDouble(11, product.getNutrientInfo().getSugars());
                stmt.setDouble(12, product.getNutrientInfo().getSalt());
                stmt.setDouble(13, product.getNutrientInfo().getProteins());
                stmt.setDouble(14, product.getNutrientInfo().getFiber());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product: " + product.getBarcode(), e);
        }
    }

    @Override
    public void saveAll(Iterable<Product> products) {
        products.forEach(this::save);
    }
}
