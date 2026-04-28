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

    public void saveRaw(String barcode, String name, String genericName, String quantity,
                        int nutriscoreScore, String nutriscoreGrade, int novaGroup,
                        double energyKcal, double fat, double saturatedFat,
                        double sugars, double salt, double proteins, double fiber) {
        String sql = """
                INSERT INTO products (barcode, name, generic_name, quantity,
                    nutriscore_score, nutriscore_grade, nova_group,
                    energy_kcal, fat, saturated_fat, sugars, salt, proteins, fiber)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (barcode) DO UPDATE SET
                    name = EXCLUDED.name, generic_name = EXCLUDED.generic_name,
                    quantity = EXCLUDED.quantity, nutriscore_score = EXCLUDED.nutriscore_score,
                    nutriscore_grade = EXCLUDED.nutriscore_grade, nova_group = EXCLUDED.nova_group,
                    energy_kcal = EXCLUDED.energy_kcal, fat = EXCLUDED.fat,
                    saturated_fat = EXCLUDED.saturated_fat, sugars = EXCLUDED.sugars,
                    salt = EXCLUDED.salt, proteins = EXCLUDED.proteins, fiber = EXCLUDED.fiber
                """;

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, barcode);
            stmt.setString(2, name);
            stmt.setString(3, genericName);
            stmt.setString(4, quantity);
            stmt.setInt(5, nutriscoreScore);
            stmt.setString(6, nutriscoreGrade);
            stmt.setInt(7, novaGroup);
            stmt.setDouble(8, energyKcal);
            stmt.setDouble(9, fat);
            stmt.setDouble(10, saturatedFat);
            stmt.setDouble(11, sugars);
            stmt.setDouble(12, salt);
            stmt.setDouble(13, proteins);
            stmt.setDouble(14, fiber);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product: " + barcode, e);
        }
    }

    @Override
    public void save(Product product) {
        saveRaw(
                product.getBarcode().getValue(),
                product.getName(),
                product.getGenericName(),
                product.getQuantity(),
                product.getNutriScore().getScore(),
                product.getNutriScore().getGrade(),
                product.getNovaGroup(),
                product.getNutrientInfo().getEnergyKcal(),
                product.getNutrientInfo().getFat(),
                product.getNutrientInfo().getSaturatedFat(),
                product.getNutrientInfo().getSugars(),
                product.getNutrientInfo().getSalt(),
                product.getNutrientInfo().getProteins(),
                product.getNutrientInfo().getFiber()
        );
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
