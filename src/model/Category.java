package model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a category for organizing tasks.
 * Categories help users organize their tasks by type (e.g., Work, Personal, Shopping).
 */
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private String description;

    /**
     * Creates a new category with a generated UUID.
     * @param name The name of the category
     */
    public Category(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = "";
    }

    /**
     * Creates a new category with a generated UUID and description.
     * @param name The name of the category
     * @param description The description of the category
     */
    public Category(String name, String description) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    /**
     * Creates a category with a specific ID (used for data loading).
     * @param id The unique identifier
     * @param name The name of the category
     * @param description The description of the category
     */
    public Category(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Category{id='%s', name='%s', description='%s'}",
                id, name, description);
    }
}
