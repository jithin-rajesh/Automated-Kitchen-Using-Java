package com.kitchen.model;

public class StepIngredient {
    private String name;
    // Potential future fields: quantity, unit (if reliably extractable per step)

    public StepIngredient(String name) {
        this.name = name;
    }

    // --- Getters ---
    // Necessary for Jackson serialization (e.g., when converting StructuredRecipe to JSON)

    public String getName() {
        return name;
    }

    // Setter might be needed if you construct objects differently
    public void setName(String name) {
       this.name = name;
    }


    // Optional: toString() for debugging
    @Override
    public String toString() {
        return name; // Keep it simple for list output
    }
}