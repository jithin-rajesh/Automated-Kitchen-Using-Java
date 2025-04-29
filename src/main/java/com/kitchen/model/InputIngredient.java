package com.kitchen.model; // Using package from your screenshots

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single ingredient item as found in the input JSON file.
 * Uses Jackson annotations to map JSON keys to Java fields.
 */
// Allows Jackson to ignore any fields in the JSON that are not defined here
@JsonIgnoreProperties(ignoreUnknown = true)
public class InputIngredient {

    // Use Double to handle potential decimals like 1.5 cups
    @JsonProperty("amount") // Maps JSON key "amount" to this field
    private Double amount;

    @JsonProperty("unit") // Maps JSON key "unit" to this field
    private String unit;

    // Maps the "ingredient" field from JSON to this variable name
    @JsonProperty("ingredient") // Maps JSON key "ingredient" to this field
    private String ingredientName;

    // Default constructor (needed by Jackson, usually implicit)
    public InputIngredient() {
    }

    // --- Getters and Setters ---
    // Necessary for Jackson to serialize/deserialize and for other classes to access data

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    // Optional: toString() for debugging purposes
    @Override
    public String toString() {
        return "InputIngredient{" +
               "amount=" + amount +
               ", unit='" + unit + '\'' +
               ", ingredientName='" + ingredientName + '\'' +
               '}';
    }
}