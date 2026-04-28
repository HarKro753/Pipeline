package com.pipeline.application.service;

import java.util.List;
import java.util.Map;

import com.pipeline.domain.model.Product;
import com.pipeline.domain.repository.*;

public class NormalizationService {

    private final ProductRepository productRepository;
    private final Map<String, ProductRelationRepository> relationRepositories;

    public NormalizationService(ProductRepository productRepository,
                                Map<String, ProductRelationRepository> relationRepositories) {
        this.productRepository = productRepository;
        this.relationRepositories = relationRepositories;
    }

    public void normalize(Product product) {
        productRepository.save(product);

        String barcode = product.getBarcode().getValue();

        for (Map.Entry<String, List<String>> entry : product.getRelations().entrySet()) {
            String category = entry.getKey();
            List<String> values = entry.getValue();

            ProductRelationRepository repo = relationRepositories.get(category);
            if (repo == null) {
                throw new IllegalStateException("No repository registered for category: " + category);
            }

            for (String value : values) {
                repo.save(barcode, value);
            }
        }
    }
}
