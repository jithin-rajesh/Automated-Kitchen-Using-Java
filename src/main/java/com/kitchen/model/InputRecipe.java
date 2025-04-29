package com.kitchen.model; 

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class InputRecipe {

    @JsonProperty("name") 
    private String name;

    @JsonProperty("url")
    private String url;

    @JsonProperty("ingredients") 
    private List<InputIngredient> ingredients;

    @JsonProperty("instructions") 
    private List<String> instructions;

    public InputRecipe() {
    }


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