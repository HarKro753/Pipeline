package com.pipeline.infrastructure.persistence;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class ProductMapper {

    static Product fromResultSet(ResultSet rs) throws SQLException {
        return new Product(
                new Barcode(rs.getString("barcode")),
                rs.getString("name"),
                rs.getString("brand"),
                new NutriScore(rs.getInt("nutriscore_score"), rs.getString("nutriscore_grade")),
                new NutrientInfo(
                        rs.getDouble("energy_kcal"),
                        rs.getDouble("fat"),
                        rs.getDouble("saturated_fat"),
                        rs.getDouble("sugars"),
                        rs.getDouble("salt"),
                        rs.getDouble("proteins"),
                        rs.getDouble("fiber")
                ),
                List.of()
        );
    }
}
