package com.kitchen.model; // Using package from your screenshots

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InputIngredient {

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("unit") 
    private String unit;

    @JsonProperty("ingredient")
    private String ingredientName;

    public InputIngredient() {
    }

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

    @Override
    public String toString() {
        return "InputIngredient{" +
               "amount=" + amount +
               ", unit='" + unit + '\'' +
               ", ingredientName='" + ingredientName + '\'' +
               '}';
    }
}