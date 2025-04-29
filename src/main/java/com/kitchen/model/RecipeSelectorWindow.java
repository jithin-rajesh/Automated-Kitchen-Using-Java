package com.kitchen.model; // Or your package name

import com.kitchen.model.processing.RecipeProcessor; // Adjust if needed

import javax.swing.*;
import javax.swing.event.DocumentEvent; // Import DocumentListener events
import javax.swing.event.DocumentListener; // Import DocumentListener interface
import java.awt.*;
import java.util.List;
// No longer need: import java.util.Vector;

public class RecipeSelectorWindow extends JFrame {

    private JList<InputRecipe> recipeList;
    private DefaultListModel<InputRecipe> listModel; // Use DefaultListModel
    private JButton viewButton;
    private JTextField searchField; // Added search field
    private RecipeProcessor processor;
    private List<InputRecipe> allRecipes; // Keep a reference to the full list

    public RecipeSelectorWindow(List<InputRecipe> recipes, RecipeProcessor processor) {
        this.processor = processor;
        this.allRecipes = recipes; // Store the original full list

        setTitle("Recipe Selector");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- Search Panel (New) ---
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);
        // Add listener to react to typing
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList(); // Plain text components don't fire this one
            }
        });
        add(searchPanel, BorderLayout.NORTH); // Add search panel to the top

        // --- Recipe List ---
        listModel = new DefaultListModel<>(); // Initialize the list model
        allRecipes.forEach(listModel::addElement); // Add all recipes initially

        recipeList = new JList<>(listModel); // Create JList with the dynamic model
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipeList.setCellRenderer(new DefaultListCellRenderer() {
             @Override
             public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof InputRecipe) {
                    ((JLabel) renderer).setText(((InputRecipe) value).getName());
                }
                return renderer;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(recipeList);
        add(listScrollPane, BorderLayout.CENTER);

        // --- View Button ---
        viewButton = new JButton("View Selected Recipe");
        viewButton.addActionListener(e -> viewSelectedRecipe());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(viewButton);
        add(buttonPanel, BorderLayout.SOUTH);

        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    // Method to filter the list based on search field text
    private void filterList() {
        String searchText = searchField.getText().trim().toLowerCase(); // Get search text, trim whitespace, lowercase

        listModel.clear(); // Clear the currently displayed list

        if (searchText.isEmpty()) {
            // If search is empty, show all recipes
            allRecipes.forEach(listModel::addElement);
        } else {
            // Filter the original list and add matching recipes to the model
            for (InputRecipe recipe : allRecipes) {
                if (recipe.getName().toLowerCase().contains(searchText)) {
                    listModel.addElement(recipe); // Add recipe if name contains search text (case-insensitive)
                }
            }
        }
    }

    // viewSelectedRecipe remains the same, it works on the JList's current selection
    private void viewSelectedRecipe() {
        InputRecipe selectedRecipe = recipeList.getSelectedValue();

        if (selectedRecipe == null) {
            JOptionPane.showMessageDialog(this, "Please select a recipe from the list.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            System.out.println("Processing '" + selectedRecipe.getName() + "' for display...");
            StructuredRecipe structuredRecipe = processor.processRecipe(selectedRecipe);
            System.out.println("Processing complete.");

            RecipeDisplayWindow displayWindow = new RecipeDisplayWindow(selectedRecipe, structuredRecipe);
            displayWindow.setVisible(true);

        } catch (Exception ex) {
            System.err.println("Error processing recipe: " + selectedRecipe.getName());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing recipe:\n" + ex.getMessage(), "Processing Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}