package com.kitchen.model.processing; // Adjust package name if needed (e.g., com.kitchen.processing)

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.kitchen.model.InputRecipe;
import com.kitchen.model.RecipeStep;
import com.kitchen.model.StepIngredient;
import com.kitchen.model.StructuredRecipe;

public class RecipeProcessor {

    private final CRFClassifier<CoreLabel> nerClassifier;
    private final StanfordCoreNLP sentencePipeline;

    /**
     * Constructor for RecipeProcessor.
     * Loads the custom Stanford NER model.
     * Sets up a basic CoreNLP pipeline for sentence splitting.
     *
     * @param modelPath Path to the serialized NER model file (.ser.gz).
     * @throws RuntimeException If the model cannot be loaded.
     */
    public RecipeProcessor(String modelPath) {
        try {
            // Attempt to load the classifier
            // This method can load from file path or classpath resource
            this.nerClassifier = CRFClassifier.getClassifier(modelPath);
            // Setup CoreNLP pipeline for sentence splitting ONLY
            // This is more robust than just splitting by "."
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit");
            // Optional: Handle multi-line sentences better if needed
            // props.setProperty("ssplit.newlineIsSentenceBreak", "always");
            this.sentencePipeline = new StanfordCoreNLP(props);

        } catch (IOException | ClassCastException | ClassNotFoundException e) {
            System.err.println("Error loading NER model from path/resource: " + modelPath);
            System.err.println("Make sure the model file exists and is compatible.");
            e.printStackTrace(); // Print detailed error
            throw new RuntimeException("Failed to load NER model", e);
        }
    }

    /**
     * Processes a raw InputRecipe object into a StructuredRecipe.
     * Splits instructions into sentences, applies NER, and extracts structured steps.
     *
     * @param inputRecipe The raw recipe object loaded from JSON.
     * @return A StructuredRecipe object with processed steps.
     */
    public StructuredRecipe processRecipe(InputRecipe inputRecipe) {
        if (inputRecipe == null || inputRecipe.getInstructions() == null || inputRecipe.getInstructions().isEmpty()) { // Check if list is empty
            System.err.println("Warning: Recipe '" + (inputRecipe != null ? inputRecipe.getName() : "Unknown") + "' has no instructions list to process.");
            return new StructuredRecipe(inputRecipe != null ? inputRecipe.getName() : "Unknown", new ArrayList<>());
        }
    
        List<RecipeStep> structuredSteps = new ArrayList<>();
        // --- JOIN THE LIST OF INSTRUCTIONS INTO ONE STRING ---
        String instructionsText = String.join(" ", inputRecipe.getInstructions());
        // --- END JOIN ---
    
    
        // 1. Split instructions into sentences using Stanford CoreNLP
        Annotation document = new Annotation(instructionsText);
        sentencePipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        int stepCounter = 1;
        for (CoreMap sentence : sentences) {
            String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class).trim();
            if (sentenceText.isEmpty()) {
                continue; // Skip empty sentences
            }
            // System.out.println("Processing Sentence: " + sentenceText); // Debugging

            // 2. Apply NER classification to the sentence
            List<CoreLabel> classifiedTokens = nerClassifier.classify(sentenceText).get(0); 
            // 3. Extract entities and structure the step
            String action = null;
            List<StepIngredient> ingredients = new ArrayList<>();
            List<String> timeParts = new ArrayList<>();
            List<String> tempParts = new ArrayList<>();
            List<String> toolParts = new ArrayList<>();
            List<String> quantityParts = new ArrayList<>();
            List<String> stateParts = new ArrayList<>();
            List<String> paramParts = new ArrayList<>(); // For 'O' tagged words

            StringBuilder currentPhrase = null;
            String currentTagType = null; // To track B-TAG type (e.g., "INGREDIENT", "TIME")

            for (CoreLabel token : classifiedTokens) {
                String word = token.originalText();
                String nerTag = token.get(CoreAnnotations.AnswerAnnotation.class); // The predicted tag
                String tagPrefix = nerTag.startsWith("B-") ? "B" : (nerTag.startsWith("I-") ? "I" : "O");
                String tagType = nerTag.length() > 2 ? nerTag.substring(2) : null; // e.g., INGREDIENT, ACTION, TIME

                 // --- Logic to finalize previous multi-word entity if tag changes ---
                // Finalize if:
                // - Current tag is 'O' or 'B-' (starts a new entity or non-entity)
                // - Current tag is 'I-' but belongs to a *different* entity type than the one being built
                if (!tagPrefix.equals("I") || (currentTagType != null && !currentTagType.equals(tagType))) {
                    if (currentPhrase != null && currentTagType != null) {
                        String phrase = currentPhrase.toString().trim();
                        // Add the completed phrase to the correct list based on its type
                        switch (currentTagType) {
                            case "INGREDIENT": ingredients.add(new StepIngredient(phrase)); break;
                            case "TIME":       timeParts.add(phrase); break;
                            case "TEMP":       tempParts.add(phrase); break;
                            case "TOOL":       toolParts.add(phrase); break;
                            case "QUANTITY":   // Fall-through: Combine Quantity and Unit for simplicity now
                            case "UNIT":       quantityParts.add(phrase); break;
                            case "STATE":      stateParts.add(phrase); break;
                            // case "ACTION": // B-ACTION handled below, I-ACTION can be added here if needed
                            // default: System.err.println("Warning: Unhandled tag type for phrase ending: " + currentTagType); break;
                        }
                        // Reset phrase builder
                        currentPhrase = null;
                        currentTagType = null;
                    }
                }

                // --- Logic to process current token ---
                if (tagPrefix.equals("B")) {
                    currentPhrase = new StringBuilder(word); // Start a new phrase
                    currentTagType = tagType;
                    if ("ACTION".equals(tagType)) {
                        // Usually treat B-ACTION as the main action for the step
                        action = word; // Overwrite previous action if multiple B-ACTION in sentence
                        currentPhrase = null; // Reset phrase for action, assume single word
                        currentTagType = null;
                    }
                } else if (tagPrefix.equals("I")) {
                    // Append to the current phrase if it's the same entity type
                    if (currentPhrase != null && currentTagType != null && currentTagType.equals(tagType)) {
                        currentPhrase.append(" ").append(word);
                    } else {
                        // Handle unexpected I- tag (e.g., I-ING without B-ING before)
                        // Treat it as a new B- tag for robustness
                        System.err.printf("Warning: Unexpected I-%s tag for '%s' without preceding B-%s. Treating as B-%s.\n",
                                          tagType, word, tagType, tagType);
                        currentPhrase = new StringBuilder(word);
                        currentTagType = tagType;
                         if ("ACTION".equals(tagType)) { // Handle unexpected I-ACTION
                             action = word; // Treat as a B-ACTION
                             currentPhrase = null;
                             currentTagType = null;
                         }
                    }
                } else { // O tag
                    // Collect potentially relevant 'O' words as parameters, excluding simple punctuation and common stopwords
                    if (!word.matches("[.,;!?:]+") && !word.toLowerCase().matches("\\b(a|an|the|is|are|was|were|in|on|at|to|of|and|or|but|it|this|that|they|with|for|will|be|can|i|you|he|she)\\b")) {
                        paramParts.add(word);
                    }
                }
            } // End token loop

            // --- Finalize any trailing entity after the loop ---
            if (currentPhrase != null && currentTagType != null) {
                 String phrase = currentPhrase.toString().trim();
                 switch (currentTagType) {
                      case "INGREDIENT": ingredients.add(new StepIngredient(phrase)); break;
                      case "TIME":       timeParts.add(phrase); break;
                      case "TEMP":       tempParts.add(phrase); break;
                      case "TOOL":       toolParts.add(phrase); break;
                      case "QUANTITY":
                      case "UNIT":       quantityParts.add(phrase); break;
                      case "STATE":      stateParts.add(phrase); break;
                      // case "ACTION": // Should have been handled
                 }
             }

            // --- Create the RecipeStep object ---
            // Create a step only if an action was identified or if other significant information exists
            boolean hasInfo = action != null || !ingredients.isEmpty() || !timeParts.isEmpty() ||
                              !tempParts.isEmpty() || !toolParts.isEmpty() || !quantityParts.isEmpty() ||
                              !stateParts.isEmpty() || !paramParts.isEmpty();

            if (hasInfo) {
                // Join multi-word parts with a separator (e.g., "; ") if multiple instances found
                String timeStr = timeParts.isEmpty() ? null : String.join("; ", timeParts);
                String tempStr = tempParts.isEmpty() ? null : String.join("; ", tempParts);
                String toolStr = toolParts.isEmpty() ? null : String.join("; ", toolParts);
                String quantityStr = quantityParts.isEmpty() ? null : String.join("; ", quantityParts);
                String stateStr = stateParts.isEmpty() ? null : String.join("; ", stateParts);
                String paramStr = paramParts.isEmpty() ? null : String.join(" ", paramParts); // Join params with space

                RecipeStep step = new RecipeStep(
                    stepCounter++,
                    action, // This might be null if no B-ACTION was found but other info existed
                    ingredients,
                    timeStr,
                    tempStr,
                    toolStr,
                    quantityStr,
                    stateStr,
                    paramStr
                );
                structuredSteps.add(step);
            } else {
                 // Optional: Log sentences that didn't yield any structured info
                 // System.out.println("Info: Skipping sentence with no extracted entities: " + sentenceText);
            }
        } // End sentence loop

        return new StructuredRecipe(inputRecipe.getName(), structuredSteps);
    }
}