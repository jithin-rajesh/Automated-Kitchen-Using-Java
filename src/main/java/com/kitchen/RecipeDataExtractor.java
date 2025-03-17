package com.kitchen;

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
        System.out.println("Starting recipe extraction process...");
        List<String[]> recipeLinks = extractRecipeLinks();
        System.out.println("Total recipe links extracted: " + recipeLinks.size());
        JsonArray recipesJson = extractRecipeDataToJson(recipeLinks);
        saveToJson("complete_indian_recipes.json", recipesJson);
        System.out.println("Complete recipe data saved successfully in complete_indian_recipes.json");
    }
    
    public static List<String[]> extractRecipeLinks() {
        String baseUrl = "https://www.kannammacooks.com/category/recipes/south-indian/page/";
        List<String[]> recipeLinks = new ArrayList<>();
        int maxPages = 50;
        
        try {
            for (int i = 1; i <= maxPages; i++) {
                System.out.println("Processing page: " + i);
                String url = baseUrl + i;
                Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();
                Elements recipes = doc.select("h2.entry-title.ast-blog-single-element a");
                
                for (Element recipe : recipes) {
                    String name = recipe.text();
                    String link = recipe.absUrl("href");
                    recipeLinks.add(new String[]{name, link});
                    System.out.println("Found recipe: " + name + " | " + link);
                }
            }
            return recipeLinks;
        } catch (IOException e) {
            System.err.println("Error while extracting recipe links: " + e.getMessage());
            e.printStackTrace();
            return recipeLinks;
        }
    }
    
    public static JsonArray extractRecipeDataToJson(List<String[]> recipeLinks) {
        JsonArray recipesArray = new JsonArray();
        
        for (int i = 0; i < recipeLinks.size(); i++) {
            String[] linkData = recipeLinks.get(i);
            String recipeName = linkData[0];
            String recipeLink = linkData[1];
            System.out.println("Processing recipe (" + (i+1) + "/" + recipeLinks.size() + "): " + recipeName);
            
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
                System.out.println("Successfully extracted data for recipe: " + recipeName);
                Thread.sleep(1000); // pause between requests
            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing " + recipeName + ": " + e.getMessage());
            }
        }
        return recipesArray;
    }
    
    public static JsonArray extractIngredients(Document doc) {
        JsonArray ingredientsJsonArray = new JsonArray();
        System.out.println("Extracting ingredients...");
        Element ingredientsSection = doc.selectFirst("div.tasty-recipes-ingredients");
        
        if (ingredientsSection != null) {
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
                System.out.println("Found ingredient: " + ingredientName + " (" + amount + " " + unit + ")");
            }
        } else {
            System.out.println("No ingredients section found.");
        }
        return ingredientsJsonArray;
    }
    
    public static JsonArray extractInstructions(Document doc) {
        JsonArray instructionsJsonArray = new JsonArray();
        System.out.println("Extracting instructions...");
        Element instructionsSection = doc.selectFirst("div.tasty-recipes-instructions");
        
        if (instructionsSection != null) {
            Elements steps = instructionsSection.select("p");
            
            for (Element step : steps) {
                String instruction = step.text().trim();
                instructionsJsonArray.add(instruction);
                System.out.println("Found instruction: " + instruction);
            }
        } else {
            System.out.println("No instructions section found.");
        }
        return instructionsJsonArray;
    }
    
    public static void saveToJson(String filename, JsonArray recipesArray) {
        System.out.println("Saving data to " + filename);
        try (FileWriter writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(recipesArray, writer);
        } catch (IOException e) {
            System.err.println("Error while saving to JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
