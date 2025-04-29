package com.kitchen.model; // Or your package name

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitchen.model.processing.RecipeProcessor; // Adjust package name if needed

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
public class Main {

    private static final String RECIPE_JSON_FILENAME = "complete_indian_recipes.json"; // Corrected underscore
    private static final String NER_MODEL_FILENAME = "ner-model.ser.gz";
    private static RecipeProcessor processor;
    private static List<InputRecipe> recipes;

    public static void main(String[] args) {

        String recipeJsonPath = findFilePath(RECIPE_JSON_FILENAME);
        String nerModelPath = findFilePath(NER_MODEL_FILENAME);

        if (recipeJsonPath == null) {
            showErrorDialog("Could not find recipe file: " + RECIPE_JSON_FILENAME +
                            "\nPlace it next to the JAR or in src/main/resources and rebuild.");
            return;
        }
         if (nerModelPath == null) {
            showErrorDialog("Could not find NER model file: " + NER_MODEL_FILENAME +
                             "\nPlace it next to the JAR or in src/main/resources and rebuild.");
            return;
        }
        System.out.println("Using Recipe File: " + recipeJsonPath);
        System.out.println("Using NER Model File: " + nerModelPath);
        System.out.println("------------------------------------------");

        // 1. Load Recipes from JSON
        recipes = loadRecipes(recipeJsonPath);
        if (recipes == null || recipes.isEmpty()) {
            showErrorDialog("No recipes loaded or error loading recipes. Exiting.");
            return;
        }

        // 2. Initialize Recipe Processor (this loads the NER model)
        try {
            System.out.println("Loading NER model (this may take a moment)...");
            processor = new RecipeProcessor(nerModelPath);
            System.out.println("NER model loaded successfully.");
        } catch (RuntimeException e) {
            showErrorDialog("FATAL ERROR: Failed to initialize Recipe Processor:\n" + e.getMessage());
             e.printStackTrace(); // Print detailed error for debugging
            return; // Exit if processor fails to load
        }
        System.out.println("------------------------------------------");

        // 3. Launch the Swing GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Create and display the main recipe selection window
            RecipeSelectorWindow selectorWindow = new RecipeSelectorWindow(recipes, processor);
            selectorWindow.setVisible(true);
        });
    }

    // --- Helper methods remain mostly the same ---

    private static String findFilePath(String filename) {
        File fileInCurrentDir = new File(filename);
        if (fileInCurrentDir.exists() && fileInCurrentDir.isFile()) {
            return fileInCurrentDir.getAbsolutePath();
        }
        InputStream resourceStream = Main.class.getClassLoader().getResourceAsStream(filename);
        if (resourceStream != null) {
            try { resourceStream.close(); } catch (IOException e) { /* Ignore */ }
            return filename; // Will be loaded as resource
        }
        return null;
    }

    private static List<InputRecipe> loadRecipes(String pathOrResourceName) {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = null;
        try {
            File recipeFile = new File(pathOrResourceName);
            if (recipeFile.exists() && recipeFile.isFile()) {
                System.out.println("Reading recipes from file system: " + pathOrResourceName);
                return mapper.readValue(recipeFile, new TypeReference<List<InputRecipe>>() {});
            } else {
                System.out.println("Attempting to read recipes from classpath resource: " + pathOrResourceName);
                inputStream = Main.class.getClassLoader().getResourceAsStream(pathOrResourceName);
                if (inputStream != null) {
                    try (InputStream stream = inputStream) {
                        return mapper.readValue(stream, new TypeReference<List<InputRecipe>>() {});
                    }
                } else {
                    System.err.println("Recipe resource not found in classpath: " + pathOrResourceName);
                    return null;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading or parsing recipe file/resource: " + pathOrResourceName);
            e.printStackTrace();
            return null;
        }
    }

     // Helper to show error dialogs
    private static void showErrorDialog(String message) {
        // Ensure dialog runs on EDT if called from non-GUI thread, though here it's before GUI launch
        if (SwingUtilities.isEventDispatchThread()) {
             JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE));
        }
         System.err.println(message); // Also print to console
    }
}