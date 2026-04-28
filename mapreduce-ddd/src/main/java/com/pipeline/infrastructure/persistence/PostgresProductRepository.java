package com.pipeline.infrastructure.persistence;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.repository.ProductRepository;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.infrastructure.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PostgresProductRepository implements ProductRepository {

    private final DatabaseConfig config;

    public PostgresProductRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public void save(Product product) {
        String sql = """
                INSERT INTO products (barcode, name, brand, nutriscore_score, nutriscore_grade,
                    energy_kcal, fat, saturated_fat, sugars, salt, proteins, fiber)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (barcode) DO UPDATE SET
                    name = EXCLUDED.name, brand = EXCLUDED.brand
                """;

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, product.getBarcode().getValue());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getBrand());
            stmt.setInt(4, product.getNutriScore().getScore());
            stmt.setString(5, product.getNutriScore().getGrade());
            stmt.setDouble(6, product.getNutrientInfo().getEnergyKcal());
            stmt.setDouble(7, product.getNutrientInfo().getFat());
            stmt.setDouble(8, product.getNutrientInfo().getSaturatedFat());
            stmt.setDouble(9, product.getNutrientInfo().getSugars());
            stmt.setDouble(10, product.getNutrientInfo().getSalt());
            stmt.setDouble(11, product.getNutrientInfo().getProteins());
            stmt.setDouble(12, product.getNutrientInfo().getFiber());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product: " + product.getBarcode(), e);
        }
    }

    @Override
    public void saveAll(Iterable<Product> products) {
        products.forEach(this::save);
    }

    @Override
    public Optional<Product> findByBarcode(Barcode barcode) {
        String sql = "SELECT * FROM products WHERE barcode = ?";

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, barcode.getValue());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(ProductMapper.fromResultSet(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find product: " + barcode, e);
        }
    }
}
