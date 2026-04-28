package com.pipeline.domain.model;

import com.pipeline.domain.valueobject.Barcode;

import java.util.Objects;

public final class ProductTag {

    private final Barcode productCode;
    private final String tagName;

    public ProductTag(Barcode productCode, String tagName) {
        if (tagName == null || tagName.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be empty");
        }
        this.productCode = productCode;
        this.tagName = tagName.trim().toLowerCase();
    }

    public Barcode getProductCode() { return productCode; }
    public String getTagName() { return tagName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductTag other)) return false;
        return productCode.equals(other.productCode) && tagName.equals(other.tagName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productCode, tagName);
    }

    @Override
    public String toString() {
        return "ProductTag{" + productCode + " -> " + tagName + "}";
    }
}
