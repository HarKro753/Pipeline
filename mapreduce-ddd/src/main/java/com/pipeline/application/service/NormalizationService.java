package com.pipeline.application.service;

import java.util.Arrays;
import java.util.List;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.model.Tag;
import com.pipeline.domain.repository.ProductRepository;
import com.pipeline.domain.repository.TagRepository;
import com.pipeline.domain.valueobject.Barcode;
import com.pipeline.domain.valueobject.NutriScore;
import com.pipeline.domain.valueobject.NutrientInfo;

public class NormalizationService {

    private final ProductRepository productRepository;
    private final TagRepository tagRepository;

    public NormalizationService(ProductRepository productRepository, TagRepository tagRepository) {
        this.productRepository = productRepository;
        this.tagRepository = tagRepository;
    }

    public void normalizeAndPersist(String barcode, String name, String brand,
                                     int nutriScoreValue, String nutriGrade,
                                     double energyKcal, double fat, double saturatedFat,
                                     double sugars, double salt, double proteins, double fiber,
                                     String tagsRaw) {
        Barcode code = new Barcode(barcode);
        NutriScore nutriScore = new NutriScore(nutriScoreValue, nutriGrade);
        NutrientInfo nutrients = new NutrientInfo(energyKcal, fat, saturatedFat, sugars, salt, proteins, fiber);

        List<String> tagNames = Arrays.stream(tagsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        Product product = new Product(code, name, brand, nutriScore, nutrients, List.of());
        tagNames.forEach(product::addTag);

        productRepository.save(product);

        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            tagRepository.saveProductTagRelation(barcode, tag.getId());
        }
    }
}
