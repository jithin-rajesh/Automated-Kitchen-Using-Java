package com.kitchen.model; // Using package from your screenshots

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List; // Required for List type

/**
 * Represents a complete recipe object as found in the root level of the input JSON array.
 * Contains basic recipe info, a list of ingredients, and a list of instruction steps.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore fields in JSON not defined here
public class InputRecipe {

    @JsonProperty("name") // Maps JSON key "name"
    private String name;

    @JsonProperty("url") // Maps JSON key "url"
    private String url;

    @JsonProperty("ingredients") // Maps JSON key "ingredients"
    private List<InputIngredient> ingredients; // Expects a JSON array of ingredient objects

    @JsonProperty("instructions") // Maps JSON key "instructions"
    private List<String> instructions; // Expects a JSON array of strings

    // Default constructor (needed by Jackson)
    public InputRecipe() {
    }

    // --- Getters and Setters for all fields ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<InputIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<InputIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }

    // Optional: toString() for debugging (truncates long instructions)
    @Override
    public String toString() {
        String instrPreview = (instructions != null && !instructions.isEmpty())
                              ? String.join(" ", instructions).substring(0, Math.min(String.join(" ", instructions).length(), 60)) + "..."
                              : "[]";
        return "InputRecipe{" +
               "name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", ingredients=" + (ingredients != null ? ingredients.size() : 0) + " items" +
               ", instructions='" + instrPreview +
               '}';
    }
}