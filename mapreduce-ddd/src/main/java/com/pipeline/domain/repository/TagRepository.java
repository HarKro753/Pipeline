package com.pipeline.domain.repository;

import com.pipeline.domain.model.Tag;

import java.util.Optional;

public interface TagRepository {
    Tag save(Tag tag);
    Optional<Tag> findByName(String name);
    void saveProductTagRelation(String productCode, Long tagId);
}
