package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecipeDataExtractor {
    public static void main(String[] args) {
        List<String[]> recipeLinks = extractRecipeLinks();
        JsonArray recipesJson = extractRecipeDataToJson(recipeLinks);
        saveToJson("complete_indian_recipes.json", recipesJson);
        System.out.println("Complete recipe data saved successfully in complete_indian_recipes.json");
    }
    
    public static List<String[]> extractRecipeLinks() {
        String baseUrl = "https://www.kannammacooks.com/category/recipes/south-indian/page/";
        List<String[]> recipeLinks = new ArrayList<>();
        int maxPages = 1;
        
        try {
            for (int i = 1; i <= maxPages; i++) {
                String url = baseUrl + i;
                Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
                Elements recipes = doc.select("h2.entry-title.ast-blog-single-element a");
                
                for (Element recipe : recipes) {
                    recipeLinks.add(new String[]{recipe.text(), recipe.absUrl("href")});
                }
            }
            return recipeLinks;
        } catch (IOException e) {
            e.printStackTrace();
            return recipeLinks;
        }
    }
    
    public static JsonArray extractRecipeDataToJson(List<String[]> recipeLinks) {
        JsonArray recipesArray = new JsonArray();
        
        for (String[] linkData : recipeLinks) {
            String recipeName = linkData[0];
            String recipeLink = linkData[1];
            
            try {
                Document doc = Jsoup.connect(recipeLink).userAgent("Mozilla/5.0").timeout(10000).get();
                JsonObject recipeJson = new JsonObject();
                recipeJson.addProperty("name", recipeName);
                recipeJson.addProperty("url", recipeLink);
                
                JsonArray ingredientsJsonArray = extractIngredients(doc);
                JsonArray instructionsJsonArray = extractInstructions(doc);
                
                recipeJson.add("ingredients", ingredientsJsonArray);
                recipeJson.add("instructions", instructionsJsonArray);
                
                recipesArray.add(recipeJson);
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing " + recipeName + ": " + e.getMessage());
            }
        }
        return recipesArray;
    }
    
    public static JsonArray extractIngredients(Document doc) {
        JsonArray ingredientsJsonArray = new JsonArray();
        Element ingredientsSection = doc.selectFirst("div.tasty-recipes-ingredients");
        
        if (ingredientsSection != null) {
            // Select spans that have both data-amount and data-unit
            Elements ingredientSpans = ingredientsSection.select("span[data-amount][data-unit]");
            
            for (Element span : ingredientSpans) {
                String amount = span.attr("data-amount");
                String unit = span.attr("data-unit");
                
                String ingredientName = "";
                if (span.nextSibling() != null) {
                    ingredientName = span.nextSibling().toString().trim();
                }
    
                ingredientName = ingredientName.replaceAll("\\s+", " ").trim();
                
                JsonObject ingredientJson = new JsonObject();
                try {
                    ingredientJson.addProperty("amount", Double.parseDouble(amount));
                } catch (NumberFormatException e) {
                    ingredientJson.addProperty("amount", amount);
                }
                ingredientJson.addProperty("unit", unit);
                ingredientJson.addProperty("ingredient", ingredientName);
                
                ingredientsJsonArray.add(ingredientJson);
            }
        }
        return ingredientsJsonArray;
    }
    
    
    public static JsonArray extractInstructions(Document doc) {
        JsonArray instructionsJsonArray = new JsonArray();
        Element instructionsSection = doc.selectFirst("div.tasty-recipes-instructions");
        
        if (instructionsSection != null) {
            Elements steps = instructionsSection.select("p");
            
            for (Element step : steps) {
                instructionsJsonArray.add(step.text().trim());
            }
        }
        return instructionsJsonArray;
    }
    
    public static void saveToJson(String filename, JsonArray recipesArray) {
        try (FileWriter writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(recipesArray, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
