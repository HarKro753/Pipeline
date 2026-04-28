package com.pipeline.infrastructure.persistence;

import com.pipeline.domain.model.Tag;
import com.pipeline.domain.repository.TagRepository;
import com.pipeline.infrastructure.config.DatabaseConfig;

import java.sql.*;
import java.util.Optional;

public class PostgresTagRepository implements TagRepository {

    private final DatabaseConfig config;

    public PostgresTagRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public Tag save(Tag tag) {
        String sql = "INSERT INTO tags (name) VALUES (?) ON CONFLICT (name) DO NOTHING RETURNING id";

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tag.getName());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Tag(rs.getLong("id"), tag.getName());
            }
            return findByName(tag.getName()).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save tag: " + tag.getName(), e);
        }
    }

    @Override
    public Optional<Tag> findByName(String name) {
        String sql = "SELECT id, name FROM tags WHERE name = ?";

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim().toLowerCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Tag(rs.getLong("id"), rs.getString("name")));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find tag: " + name, e);
        }
    }

    @Override
    public void saveProductTagRelation(String productCode, Long tagId) {
        String sql = "INSERT INTO product_tags (product_barcode, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, productCode);
            stmt.setLong(2, tagId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save product-tag relation", e);
        }
    }
}
