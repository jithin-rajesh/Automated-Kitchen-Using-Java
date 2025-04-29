package com.kitchen.model; // Or your package name


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeDisplayWindow extends JFrame {

    private final InputRecipe inputRecipe; // Keep original for ingredients list
    private final StructuredRecipe structuredRecipe;
    private int currentStepIndex = -1; // Start before the first step

    private JTextArea ingredientsArea;
    private JTextArea stepDisplayArea;
    private JButton nextButton;
    private JLabel stepTitleLabel;

    public RecipeDisplayWindow(InputRecipe inputRecipe, StructuredRecipe structuredRecipe) {
        this.inputRecipe = inputRecipe;
        this.structuredRecipe = structuredRecipe;

        setTitle("Recipe: " + inputRecipe.getName());
        setSize(600, 700);
        // DISPOSE_ON_CLOSE so closing this window doesn't exit the whole app
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null); // Center relative to parent or screen
        setLayout(new BorderLayout(10, 10));

        // --- Ingredients Panel ---
        JPanel ingredientsPanel = new JPanel(new BorderLayout());
        ingredientsPanel.setBorder(BorderFactory.createTitledBorder("Ingredients"));
        ingredientsArea = new JTextArea(10, 40); // Rows, Columns
        ingredientsArea.setEditable(false);
        ingredientsArea.setLineWrap(true);
        ingredientsArea.setWrapStyleWord(true);
        displayIngredients(); // Populate the area
        JScrollPane ingredientsScrollPane = new JScrollPane(ingredientsArea);
        ingredientsPanel.add(ingredientsScrollPane, BorderLayout.CENTER);

        // --- Steps Panel ---
        JPanel stepsPanel = new JPanel(new BorderLayout(5, 5));
        stepsPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));

        stepTitleLabel = new JLabel("Press 'Next Step' to begin", SwingConstants.CENTER);
        stepTitleLabel.setFont(stepTitleLabel.getFont().deriveFont(Font.BOLD));
        stepsPanel.add(stepTitleLabel, BorderLayout.NORTH);

        stepDisplayArea = new JTextArea(15, 40);
        stepDisplayArea.setEditable(false);
        stepDisplayArea.setLineWrap(true);
        stepDisplayArea.setWrapStyleWord(true);
        stepDisplayArea.setBorder(new EmptyBorder(5, 5, 5, 5)); // Padding inside text area
        JScrollPane stepScrollPane = new JScrollPane(stepDisplayArea);
        stepsPanel.add(stepScrollPane, BorderLayout.CENTER);

        // --- Button Panel ---
        nextButton = new JButton("Next Step");
        nextButton.addActionListener(e -> displayNextStep());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(nextButton);
        stepsPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add panels to the main frame
        // Use a Split Pane to allow resizing between ingredients and steps
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ingredientsPanel, stepsPanel);
        splitPane.setResizeWeight(0.3); // Give ingredients less space initially
        add(splitPane, BorderLayout.CENTER);

        // Initial state check for the button
        if (structuredRecipe == null || structuredRecipe.getSteps() == null || structuredRecipe.getSteps().isEmpty()) {
            nextButton.setEnabled(false);
            stepTitleLabel.setText("No processed steps found for this recipe.");
        }

         // Add padding to the main content pane
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void displayIngredients() {
        StringBuilder sb = new StringBuilder();
        if (inputRecipe.getIngredients() != null) {
            for (InputIngredient ing : inputRecipe.getIngredients()) {
                if (ing.getAmount() != null && ing.getAmount() > 0) {
                    // Format amount nicely (avoid ".0")
                    String amountStr = (ing.getAmount() % 1 == 0)
                                        ? String.valueOf(ing.getAmount().intValue())
                                        : String.valueOf(ing.getAmount());
                    sb.append(amountStr);
                    sb.append(" ");
                }
                if (ing.getUnit() != null && !ing.getUnit().trim().isEmpty()) {
                    sb.append(ing.getUnit().trim());
                    sb.append(" ");
                }
                if (ing.getIngredientName() != null) {
                    sb.append(ing.getIngredientName());
                }
                sb.append("\n"); // New line for each ingredient
            }
        } else {
            sb.append("No ingredients listed.");
        }
        ingredientsArea.setText(sb.toString());
        ingredientsArea.setCaretPosition(0); // Scroll to top
    }

    private void displayNextStep() {
        currentStepIndex++;
        List<RecipeStep> steps = structuredRecipe.getSteps();

        if (steps != null && currentStepIndex < steps.size()) {
            displayStep(steps.get(currentStepIndex));
            if (currentStepIndex == steps.size() - 1) {
                nextButton.setText("End of Recipe");
                nextButton.setEnabled(false); // Disable after last step
            } else {
                 nextButton.setText("Next Step (" + (currentStepIndex + 2) + "/" + steps.size() + ")");
                 nextButton.setEnabled(true);
            }
        } else {
            // Should not happen if button is disabled correctly, but as a fallback:
             stepDisplayArea.setText("No more steps.");
             nextButton.setEnabled(false);
        }
    }

     private void displayStep(RecipeStep step) {
        stepTitleLabel.setText("Step " + step.getStep());
        StringBuilder sb = new StringBuilder();

        // Conditionally add attributes if they are not null or empty
        appendIfPresent(sb, "Action", step.getAction());

        if (step.getIngredients() != null && !step.getIngredients().isEmpty()) {
             String ingredientNames = step.getIngredients().stream()
                                       .map(ing -> ing.getName()) // Assuming StepIngredient has getName()
                                       .collect(Collectors.joining(", "));
             appendIfPresent(sb, "Ingredients", ingredientNames);
        }

        appendIfPresent(sb, "Time", step.getTime());
        appendIfPresent(sb, "Temperature", step.getTemperature());
        appendIfPresent(sb, "Tool", step.getTool());
        appendIfPresent(sb, "Quantity/Unit", step.getQuantityInfo());
        appendIfPresent(sb, "State/Descriptor", step.getStateInfo());
        appendIfPresent(sb, "Parameters", step.getParameters());

        stepDisplayArea.setText(sb.toString());
        stepDisplayArea.setCaretPosition(0); // Scroll to top
    }

    // Helper method to add a line only if the value is not null/empty
    private void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append(label).append(": ").append(value.trim()).append("\n\n"); // Add double newline for spacing
        }
    }
}