package com.pipeline.domain.valueobject;

import java.util.Objects;
import java.util.Set;

public final class NutriScore {

    private static final Set<String> VALID_GRADES = Set.of("a", "b", "c", "d", "e");

    private final int score;
    private final String grade;

    public NutriScore(int score, String grade) {
        if (grade != null && !VALID_GRADES.contains(grade.toLowerCase())) {
            throw new IllegalArgumentException("NutriScore grade must be a-e, got: " + grade);
        }
        this.score = score;
        this.grade = grade != null ? grade.toLowerCase() : null;
    }

    public int getScore() {
        return score;
    }

    public String getGrade() {
        return grade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NutriScore other)) return false;
        return score == other.score && Objects.equals(grade, other.grade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(score, grade);
    }

    @Override
    public String toString() {
        return "NutriScore{score=" + score + ", grade='" + grade + "'}";
    }
}
