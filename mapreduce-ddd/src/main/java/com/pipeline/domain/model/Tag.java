package com.pipeline.domain.model;

import java.util.Objects;

public class Tag {

    private Long id;
    private final String name;

    public Tag(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be empty");
        }
        this.name = name.trim().toLowerCase();
    }

    public Tag(Long id, String name) {
        this(name);
        this.id = id;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Tag{name='" + name + "'}";
    }
}
