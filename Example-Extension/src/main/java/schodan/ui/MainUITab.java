package schodan.ui;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Skeleton UI tab with example components (button, toggle, dropdown, input field, text area).
 */
public class MainUITab extends JPanel {
    private final MontoyaApi montoyaApi;

    public MainUITab(MontoyaApi montoyaApi) {
        super(new BorderLayout());
        this.montoyaApi = montoyaApi;
        setBorder(new EmptyBorder(12, 12, 12, 12));
        try {
            add(createContentPanel(), BorderLayout.NORTH);
        } catch (Exception e) {
            montoyaApi.logging().logToError("Error initializing UI tab: " + e.getMessage());
        }
    }

    private JPanel createContentPanel() {
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        // Button
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonRow.add(new JLabel("Button:"));
        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> montoyaApi.logging().logToOutput("[UI] Run clicked"));
        buttonRow.add(runButton);
        main.add(buttonRow);
        main.add(Box.createVerticalStrut(10));

        // Toggle button
        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toggleRow.add(new JLabel("Toggle:"));
        JToggleButton enableToggle = new JToggleButton("Enable", false);
        enableToggle.addActionListener(e -> montoyaApi.logging().logToOutput("[UI] Enable: " + enableToggle.isSelected()));
        toggleRow.add(enableToggle);
        main.add(toggleRow);
        main.add(Box.createVerticalStrut(10));

        // Dropdown
        JPanel dropdownRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dropdownRow.add(new JLabel("Dropdown:"));
        JComboBox<String> combo = new JComboBox<>(new String[]{"Option A", "Option B", "Option C"});
        combo.addActionListener(e -> montoyaApi.logging().logToOutput("[UI] Selected: " + combo.getSelectedItem()));
        dropdownRow.add(combo);
        main.add(dropdownRow);
        main.add(Box.createVerticalStrut(10));

        // Input field
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputRow.add(new JLabel("Input:"));
        JTextField textField = new JTextField(20);
        textField.setToolTipText("Single-line input");
        inputRow.add(textField);
        main.add(inputRow);
        main.add(Box.createVerticalStrut(10));

        // Input area
        JPanel areaRow = new JPanel(new BorderLayout(0, 4));
        areaRow.add(new JLabel("Notes:"), BorderLayout.NORTH);
        JTextArea textArea = new JTextArea(4, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        areaRow.add(new JScrollPane(textArea), BorderLayout.CENTER);
        main.add(areaRow);

        return main;
    }
}
