package com.kitchen.model;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeStep {
    private int step;
    private String action;
    private List<StepIngredient> ingredients;
    private String time; // Extracted B-TIME/I-TIME entities
    private String temperature; // Extracted B-TEMP entities
    private String tool; // Extracted B-TOOL/I-TOOL entities
    private String quantityInfo; // Extracted B-QUANTITY/B-UNIT/I-QUANTITY entities
    private String stateInfo; // Extracted B-STATE entities
    private String parameters; // Other relevant 'O' tagged words

    // --- Constructor ---
    public RecipeStep(int step, String action, List<StepIngredient> ingredients, String time, String temperature, String tool, String quantityInfo, String stateInfo, String parameters) {
        this.step = step;
        this.action = action; // This might be null if inferred
        this.ingredients = ingredients;
        this.time = time;
        this.temperature = temperature;
        this.tool = tool;
        this.quantityInfo = quantityInfo;
        this.stateInfo = stateInfo;
        this.parameters = parameters;
    }

    // --- Getters ---
    // Needed for Jackson serialization

    public int getStep() {
        return step;
    }

    public String getAction() {
        return action;
    }

    public List<StepIngredient> getIngredients() {
        return ingredients;
    }

    public String getTime() {
        return time;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getTool() {
        return tool;
    }

    public String getQuantityInfo() {
        return quantityInfo;
    }

    public String getStateInfo() {
        return stateInfo;
    }

    public String getParameters() {
        return parameters;
    }

    // --- toString() for readable console output ---
    @Override
    public String toString() {
         String ingredientStr = (ingredients == null || ingredients.isEmpty()) ? "[]" :
                                ingredients.stream()
                                           .map(StepIngredient::getName)
                                           .collect(Collectors.joining(", ", "[", "]")); // Format as list

         // Helper to format optional fields
         java.util.function.Function<String, String> format = s -> (s != null && !s.isEmpty()) ? s : "N/A";

         return String.format("  Step %d:\n    Action: %s\n    Ingredients: %s\n    Time: %s\n    Temp: %s\n    Tool: %s\n    Quantity: %s\n    State: %s\n    Params: %s",
                              step,
                              format.apply(action),
                              ingredientStr,
                              format.apply(time),
                              format.apply(temperature),
                              format.apply(tool),
                              format.apply(quantityInfo),
                              format.apply(stateInfo),
                              format.apply(parameters));
    }
}