package com.kitchen.model;


import java.util.List;

public class StructuredRecipe {
    private String name; 
    private List<RecipeStep> steps;

    public StructuredRecipe(String name, List<RecipeStep> steps) {
        this.name = name;
        this.steps = steps;
    }


    public String getName() {
        return name;
    }

    public List<RecipeStep> getSteps() {
        return steps;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe: ").append(name != null ? name : "Unnamed Recipe").append("\n");
        sb.append("Steps:\n");
        if (steps != null && !steps.isEmpty()) {
            for (RecipeStep step : steps) {
                sb.append(step).append("\n"); 
            }
        } else {
            sb.append("  (No steps processed)\n");
        }
        return sb.toString();
    }
}