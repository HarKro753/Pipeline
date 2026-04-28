package com.pipeline.domain.valueobject;

import java.util.Objects;

public final class NutrientInfo {

    private final double energyKcal;
    private final double fat;
    private final double saturatedFat;
    private final double sugars;
    private final double salt;
    private final double proteins;
    private final double fiber;

    public NutrientInfo(double energyKcal, double fat, double saturatedFat,
                        double sugars, double salt, double proteins, double fiber) {
        this.energyKcal = energyKcal;
        this.fat = fat;
        this.saturatedFat = saturatedFat;
        this.sugars = sugars;
        this.salt = salt;
        this.proteins = proteins;
        this.fiber = fiber;
    }

    public double getEnergyKcal() { return energyKcal; }
    public double getFat() { return fat; }
    public double getSaturatedFat() { return saturatedFat; }
    public double getSugars() { return sugars; }
    public double getSalt() { return salt; }
    public double getProteins() { return proteins; }
    public double getFiber() { return fiber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NutrientInfo other)) return false;
        return Double.compare(energyKcal, other.energyKcal) == 0
                && Double.compare(fat, other.fat) == 0
                && Double.compare(saturatedFat, other.saturatedFat) == 0
                && Double.compare(sugars, other.sugars) == 0
                && Double.compare(salt, other.salt) == 0
                && Double.compare(proteins, other.proteins) == 0
                && Double.compare(fiber, other.fiber) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(energyKcal, fat, saturatedFat, sugars, salt, proteins, fiber);
    }

    @Override
    public String toString() {
        return "NutrientInfo{kcal=" + energyKcal + ", fat=" + fat + ", sugar=" + sugars + "}";
    }
}
