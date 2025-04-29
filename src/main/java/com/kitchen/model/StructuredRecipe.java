package com.kitchen.model;


import java.util.List;

public class StructuredRecipe {
    private String name; // Name of the recipe
    private List<RecipeStep> steps; // List of structured steps

    // --- Constructor ---
    public StructuredRecipe(String name, List<RecipeStep> steps) {
        this.name = name;
        this.steps = steps;
    }

    // --- Getters ---
    // Needed for Jackson serialization

    public String getName() {
        return name;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }


    // --- toString() for readable console output ---
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe: ").append(name != null ? name : "Unnamed Recipe").append("\n");
        sb.append("Steps:\n");
        if (steps != null && !steps.isEmpty()) {
            for (RecipeStep step : steps) {
                sb.append(step).append("\n"); // Relies on RecipeStep.toString()
            }
        } else {
            sb.append("  (No steps processed)\n");
        }
        return sb.toString();
    }
}