package com.pipeline.application.dto;

import java.util.List;

public record TagRelationDTO(
        String tagName,
        List<String> productCodes
) {}
