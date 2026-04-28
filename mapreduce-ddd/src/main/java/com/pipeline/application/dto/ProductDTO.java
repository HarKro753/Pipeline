package com.pipeline.application.dto;

import java.util.List;

public record ProductDTO(
        String barcode,
        String name,
        String brand,
        int nutriScore,
        String nutriGrade,
        double energyKcal,
        double fat,
        double saturatedFat,
        double sugars,
        double salt,
        double proteins,
        double fiber,
        List<String> tags
) {}
