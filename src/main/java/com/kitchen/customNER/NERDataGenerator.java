package com.kitchen.customNER;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

public class NERDataGenerator {

    static class Ingredient {
        double amount;
        String unit;
        String ingredient;
    }
    
    static class Recipe {
        String name;
        String url;
        List<Ingredient> ingredients;
        List<String> instructions;
    }
    
    static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            "chop", "stir", "sauté", "boil", "mix", "heat", "cook", "add", "grind", "fry",
            "bake", "blend", "whisk", "roast", "pour", "serve", "simmer", "knead", "soak",
            "sprinkle", "reduce", "cover", "drain", "steam"
    ));
    
    static final Set<String> TOOLS = new HashSet<>(Arrays.asList(
            "pan", "blender", "pressure cooker", "knife", "spatula", "bowl", "oven", "pot",
            "stove", "tongs", "mixer", "microwave", "ladle", "whisk", "strainer", "tray",
            "plate", "grinder", "steamer", "peeler"
    ));
    
    static final Set<String> TIMES = new HashSet<>(Arrays.asList(
            "seconds", "minutes", "hours", "overnight"
    ));
    
    static final Set<String> TEMPERATURES = new HashSet<>(Arrays.asList(
            "low", "medium", "high", "simmer", "350f", "400f", "hot", "warm", "cold"
    ));
    
    static final Set<String> QUANTITIES = new HashSet<>(Arrays.asList(
            "cup", "cups", "tablespoon", "tablespoons", "teaspoon", "teaspoons",
            "gram", "grams", "ml", "liter", "pinch", "handful", "quart", "pint", "ounce", "ounces"
    ));
    
    public static List<Recipe> loadRecipes(String filename) {
        try (Reader reader = new FileReader(filename)) {
            Gson gson = new Gson();
            Type recipeListType = new TypeToken<List<Recipe>>() {}.getType();
            return gson.fromJson(reader, recipeListType);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    // ----------------------
    // Build a global ingredient list from all recipes.
    // For each ingredient, add the full cleaned string.
    // For multi-word ingredients, add the last word (after checking its POS).
    // ----------------------
    public static List<String> buildGlobalIngredientList(List<Recipe> recipes) {
        Set<String> ingredientSet = new HashSet<>();
        // Pipeline for tokenizing ingredients (tokenize, ssplit, pos)
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos");
        StanfordCoreNLP ingredientPipeline = new StanfordCoreNLP(props);
        
        for (Recipe recipe : recipes) {
            if (recipe.ingredients != null) {
                for (Ingredient ing : recipe.ingredients) {
                    // Clean the ingredient text: lowercase and remove non-letter characters (except spaces)
                    String fullIngredient = ing.ingredient.toLowerCase().trim();
                    fullIngredient = fullIngredient.replaceAll("[^a-z\\s]", "").trim();
                    if (!fullIngredient.isEmpty()) {
                        ingredientSet.add(fullIngredient);
                        
                        // Tokenize the full ingredient text using CoreNLP
                        Annotation annotation = new Annotation(fullIngredient);
                        ingredientPipeline.annotate(annotation);
                        List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);
                        if (sentences != null && !sentences.isEmpty()) {
                            List<CoreLabel> tokens = sentences.get(0).get(CoreAnnotations.TokensAnnotation.class);
                            if (tokens != null && !tokens.isEmpty() && tokens.size() > 1) {
                                // Get the last token.
                                CoreLabel lastToken = tokens.get(tokens.size() - 1);
                                String pos = lastToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                                // Skip if it's a preposition (IN, TO) or any verb (starting with VB)
                                if (!pos.equals("IN") && !pos.equals("TO") && !pos.startsWith("VB")) {
                                    String lastWord = lastToken.originalText().toLowerCase().trim();
                                    lastWord = lastWord.replaceAll("[^a-z]", "");
                                    if (!lastWord.isEmpty()) {
                                        ingredientSet.add(lastWord);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(ingredientSet);
    }
    
    // ----------------------
    // Annotate instructions using Stanford CoreNLP.
    // For each token, first check for multi-word ingredient matches from the global ingredient list.
    // Then, if no ingredient match is found (label remains "O"), check the token against additional keyword sets
    // to tag actions, tools, times, temperatures, and quantities.
    // ----------------------
    public static void annotateInstructions(List<String> instructions, List<String> ingredientList,
                                            BufferedWriter bw, StanfordCoreNLP pipeline) throws IOException {
        for (String instruction : instructions) {
            Annotation document = new Annotation(instruction);
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
            if (sentences == null) continue;
            
            for (CoreMap sentence : sentences) {
                List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
                for (int i = 0; i < tokens.size(); i++) {
                    CoreLabel token = tokens.get(i);
                    String tokenText = token.originalText();
                    // Clean the token text for matching.
                    String cleanedToken = tokenText.replaceAll("[^a-zA-Z]", "").toLowerCase();
                    String label = "O"; // Default
                    
                    boolean multiWordMatched = false;
                    
                    // First, check for multi-word ingredients in the global ingredient list.
                    for (String ingredient : ingredientList) {
                        if (ingredient.contains(" ")) {
                            String[] ingredientWords = ingredient.split("\\s+");
                            if (i <= tokens.size() - ingredientWords.length) {
                                boolean match = true;
                                for (int j = 0; j < ingredientWords.length; j++) {
                                    String wordToMatch = tokens.get(i + j).originalText()
                                            .replaceAll("[^a-zA-Z]", "").toLowerCase();
                                    if (!wordToMatch.equals(ingredientWords[j])) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    for (int j = 0; j < ingredientWords.length; j++) {
                                        String outToken = tokens.get(i + j).originalText();
                                        String nerLabel = (j == 0) ? "B-INGREDIENT" : "I-INGREDIENT";
                                        bw.write(outToken + " " + nerLabel + "\n");
                                    }
                                    i += ingredientWords.length - 1;
                                    multiWordMatched = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // If no multi-word ingredient match, check for single-word ingredient.
                    if (!multiWordMatched) {
                        for (String ingredient : ingredientList) {
                            if (!ingredient.contains(" ")) {  // single-word ingredient
                                if (cleanedToken.equals(ingredient)) {
                                    label = "B-INGREDIENT";
                                    break;
                                }
                            }
                        }
                        
                        // If still "O", check additional keyword sets.
                        if (label.equals("O")) {
                            if (ACTIONS.contains(cleanedToken)) {
                                label = "B-ACTION";
                            } else if (TOOLS.contains(cleanedToken)) {
                                label = "B-TOOL";
                            } else if (TIMES.contains(cleanedToken)) {
                                label = "B-TIME";
                            } else if (TEMPERATURES.contains(cleanedToken)) {
                                label = "B-TEMP";
                            } else if (QUANTITIES.contains(cleanedToken)) {
                                label = "B-QUANTITY";
                            }
                        }
                        
                        bw.write(tokenText + " " + label + "\n");
                    }
                }
                bw.write("\n"); // Sentence separator
            }
        }
    }
    
    // ----------------------
    // Main method
    // ----------------------
    public static void main(String[] args) {
        // Adjust file paths as needed.
        String inputFilename = "complete_indian_recipes.json";
        String outputFilename = "ner_training_data.txt";
        
        List<Recipe> recipes = loadRecipes(inputFilename);
        System.out.println("Loaded " + recipes.size() + " recipes.");
        
        // Build global ingredient list.
        List<String> globalIngredientList = buildGlobalIngredientList(recipes);
        System.out.println("Global ingredient list size: " + globalIngredientList.size());
        
        // Set up a CoreNLP pipeline for instructions (tokenize, ssplit).
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilename))) {
            for (Recipe recipe : recipes) {
                System.out.println("Processing recipe: " + recipe.name);
                bw.write("# Recipe: " + recipe.name + "\n");
                if (recipe.instructions != null) {
                    annotateInstructions(recipe.instructions, globalIngredientList, bw, pipeline);
                }
                bw.write("\n"); // Separate recipes.
            }
            System.out.println("Annotated NER training data written to " + outputFilename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
