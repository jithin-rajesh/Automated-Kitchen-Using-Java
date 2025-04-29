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
           this.nerClassifier = CRFClassifier.getClassifier(modelPath);
            Properties props = new Properties();
            props.setProperty("annotators", "tokenize, ssplit");
            this.sentencePipeline = new StanfordCoreNLP(props);

        } catch (IOException | ClassCastException | ClassNotFoundException e) {
            System.err.println("Error loading NER model from path/resource: " + modelPath);
            System.err.println("Make sure the model file exists and is compatible.");
            e.printStackTrace(); // Print detailed error
            throw new RuntimeException("Failed to load NER model", e);
        }
    }

    
    public StructuredRecipe processRecipe(InputRecipe inputRecipe) {
        if (inputRecipe == null || inputRecipe.getInstructions() == null || inputRecipe.getInstructions().isEmpty()) { // Check if list is empty
            System.err.println("Warning: Recipe '" + (inputRecipe != null ? inputRecipe.getName() : "Unknown") + "' has no instructions list to process.");
            return new StructuredRecipe(inputRecipe != null ? inputRecipe.getName() : "Unknown", new ArrayList<>());
        }
    
        List<RecipeStep> structuredSteps = new ArrayList<>();
        String instructionsText = String.join(" ", inputRecipe.getInstructions());
       
        Annotation document = new Annotation(instructionsText);
        sentencePipeline.annotate(document);
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        int stepCounter = 1;
        for (CoreMap sentence : sentences) {
            String sentenceText = sentence.get(CoreAnnotations.TextAnnotation.class).trim();
            if (sentenceText.isEmpty()) {
                continue; 
            }
            List<CoreLabel> classifiedTokens = nerClassifier.classify(sentenceText).get(0); 
            String action = null;
            List<StepIngredient> ingredients = new ArrayList<>();
            List<String> timeParts = new ArrayList<>();
            List<String> tempParts = new ArrayList<>();
            List<String> toolParts = new ArrayList<>();
            List<String> quantityParts = new ArrayList<>();
            List<String> stateParts = new ArrayList<>();
            List<String> paramParts = new ArrayList<>();

            StringBuilder currentPhrase = null;
            String currentTagType = null;

            for (CoreLabel token : classifiedTokens) {
                String word = token.originalText();
                String nerTag = token.get(CoreAnnotations.AnswerAnnotation.class); // The predicted tag
                String tagPrefix = nerTag.startsWith("B-") ? "B" : (nerTag.startsWith("I-") ? "I" : "O");
                String tagType = nerTag.length() > 2 ? nerTag.substring(2) : null; // e.g., INGREDIENT, ACTION, TIME

                if (!tagPrefix.equals("I") || (currentTagType != null && !currentTagType.equals(tagType))) {
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
                            }
                        currentPhrase = null;
                        currentTagType = null;
                    }
                }
                if (tagPrefix.equals("B")) {
                    currentPhrase = new StringBuilder(word); 
                    currentTagType = tagType;
                    if ("ACTION".equals(tagType)) {
                        action = word; 
                        currentPhrase = null;
                        currentTagType = null;
                    }
                } else if (tagPrefix.equals("I")) {
                    if (currentPhrase != null && currentTagType != null && currentTagType.equals(tagType)) {
                        currentPhrase.append(" ").append(word);
                    } else {
                        System.err.printf("Warning: Unexpected I-%s tag for '%s' without preceding B-%s. Treating as B-%s.\n",
                                          tagType, word, tagType, tagType);
                        currentPhrase = new StringBuilder(word);
                        currentTagType = tagType;
                         if ("ACTION".equals(tagType)) { 
                             action = word; 
                             currentPhrase = null;
                             currentTagType = null;
                         }
                    }
                } else { if (!word.matches("[.,;!?:]+") && !word.toLowerCase().matches("\\b(a|an|the|is|are|was|were|in|on|at|to|of|and|or|but|it|this|that|they|with|for|will|be|can|i|you|he|she)\\b")) {
                        paramParts.add(word);
                    }
                }
            } 
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
                 }
             }

            boolean hasInfo = action != null || !ingredients.isEmpty() || !timeParts.isEmpty() ||
                              !tempParts.isEmpty() || !toolParts.isEmpty() || !quantityParts.isEmpty() ||
                              !stateParts.isEmpty() || !paramParts.isEmpty();

            if (hasInfo) {
                String timeStr = timeParts.isEmpty() ? null : String.join("; ", timeParts);
                String tempStr = tempParts.isEmpty() ? null : String.join("; ", tempParts);
                String toolStr = toolParts.isEmpty() ? null : String.join("; ", toolParts);
                String quantityStr = quantityParts.isEmpty() ? null : String.join("; ", quantityParts);
                String stateStr = stateParts.isEmpty() ? null : String.join("; ", stateParts);
                String paramStr = paramParts.isEmpty() ? null : String.join(" ", paramParts); 

                RecipeStep step = new RecipeStep(
                    stepCounter++,
                    action, 
                    ingredients,
                    timeStr,
                    tempStr,
                    toolStr,
                    quantityStr,
                    stateStr,
                    paramStr
                );
                structuredSteps.add(step);
            } 
        }

        return new StructuredRecipe(inputRecipe.getName(), structuredSteps);
    }
}