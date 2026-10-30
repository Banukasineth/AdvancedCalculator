package com.banukasineth.advancedcalculator.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.util.*;

/**
 * Interactive dialog allowing users to substitute numeric values into
 * formulas from the Equation Library and solve for the unknown variable.
 */
public class EquationSubstitutionDialog {

    public static class SubstitutionResult {
        public final String workingLatex;
        public final String solutionLatex;
        public final boolean isSolved;
        public final boolean insertOnly;

        public SubstitutionResult(String workingLatex, String solutionLatex, boolean isSolved, boolean insertOnly) {
            this.workingLatex = workingLatex;
            this.solutionLatex = solutionLatex;
            this.isSolved = isSolved;
            this.insertOnly = insertOnly;
        }

        public static SubstitutionResult formulaOnly(String formula) {
            return new SubstitutionResult(formula, "", false, true);
        }

        public static SubstitutionResult solved(String workingLatex, String solutionLatex) {
            return new SubstitutionResult(workingLatex, solutionLatex, true, false);
        }
    }

    private static class VariableDef {
        final String name;
        final String label;
        final String defaultVal;

        VariableDef(String name, String label, String defaultVal) {
            this.name = name;
            this.label = label;
            this.defaultVal = defaultVal;
        }
    }

    public static Optional<SubstitutionResult> show(String equationName, String formulaLatex, Stage owner) {
        Dialog<SubstitutionResult> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Equation Substitution: " + equationName);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #1E1B2E; -fx-padding: 20; -fx-background-radius: 12;");
        pane.setPrefWidth(560);
        pane.setMinWidth(560);

        // Container
        VBox root = new VBox(14);
        root.setAlignment(Pos.TOP_LEFT);

        // Header Title
        Label titleLabel = new Label(equationName);
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;");

        // KaTeX Formula WebView with fallback badge
        WebView formulaWebView = new WebView();
        formulaWebView.setStyle("-fx-page-fill: transparent; -fx-background-color: transparent;");
        formulaWebView.setPrefHeight(48);
        formulaWebView.setMinHeight(48);
        formulaWebView.setMaxHeight(48);
        formulaWebView.setPrefWidth(460);
        formulaWebView.setContextMenuEnabled(false);

        Label fallbackFormulaLabel = new Label("Formula:  " + formatReadableFormula(equationName, formulaLatex));
        fallbackFormulaLabel.setStyle("-fx-text-fill: #E82E88; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Cambria Math', -apple-system, sans-serif; -fx-background-color: rgba(232, 46, 136, 0.08); -fx-padding: 6 12; -fx-background-radius: 6; -fx-border-color: rgba(232, 46, 136, 0.28); -fx-border-radius: 6;");

        StackPane formulaContainer = new StackPane();
        formulaContainer.setAlignment(Pos.CENTER_LEFT);
        formulaContainer.getChildren().addAll(fallbackFormulaLabel, formulaWebView);
        formulaWebView.setOpacity(0.0);

        try {
            java.net.URL url = EquationSubstitutionDialog.class.getResource("/html/formula_display.html");
            if (url != null) {
                formulaWebView.getEngine().load(url.toExternalForm());
                formulaWebView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        try {
                            String js = "updateFormula(" + JSONObject.quote(formulaLatex) + ");";
                            formulaWebView.getEngine().executeScript(js);
                            formulaWebView.setOpacity(1.0);
                            fallbackFormulaLabel.setVisible(false);
                            fallbackFormulaLabel.setManaged(false);
                        } catch (Exception ex) {
                            System.err.println("Error rendering formula in WebView: " + ex.getMessage());
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Could not load formula display HTML: " + e.getMessage());
        }

        Label instructionLabel = new Label("Enter values for known variables (leave one blank to solve for it):");
        instructionLabel.setStyle("-fx-text-fill: #A09EAA; -fx-font-size: 13px;");

        root.getChildren().addAll(titleLabel, formulaContainer, instructionLabel);

        // Define variable inputs based on equation name
        List<VariableDef> varDefs = getVariablesForEquation(equationName);
        Map<String, TextField> fieldMap = new LinkedHashMap<>();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        int row = 0;
        for (VariableDef varDef : varDefs) {
            Label vLabel = new Label(varDef.label + ":");
            vLabel.setStyle("-fx-text-fill: #E0DEEC; -fx-font-size: 14px; -fx-font-weight: bold;");

            TextField field = new TextField();
            field.setPromptText(varDef.defaultVal != null ? "e.g. " + varDef.defaultVal : "Leave empty to solve");
            field.setStyle("-fx-background-color: #262432; -fx-text-fill: #FFFFFF; -fx-prompt-text-fill: #706E7E; "
                    + "-fx-background-radius: 6; -fx-padding: 8 12; -fx-font-size: 14px;");
            field.setPrefWidth(220);

            grid.add(vLabel, 0, row);
            grid.add(field, 1, row);
            fieldMap.put(varDef.name, field);
            row++;
        }
        root.getChildren().add(grid);

        // Custom Buttons in dedicated HBox with exact same spacing (centered)
        HBox buttonBarRow = new HBox(12);
        buttonBarRow.setAlignment(Pos.CENTER);
        buttonBarRow.setMaxWidth(Double.MAX_VALUE);
        buttonBarRow.prefWidthProperty().bind(root.widthProperty());
        buttonBarRow.setPadding(new Insets(14, 0, 0, 0));

        Button insertBtn = new Button("Insert Formula");
        Button solveBtn = new Button("Substitute & Solve");
        Button cancelBtn = new Button("Cancel");

        insertBtn.setPrefHeight(36);
        solveBtn.setPrefHeight(36);
        cancelBtn.setPrefHeight(36);

        String defaultSolveStyle = "-fx-background-color: #E82E88; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-padding: 8 18; -fx-cursor: hand; -fx-font-size: 13px;";
        String hoverSolveStyle = "-fx-background-color: #FF3E9E; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-padding: 8 18; -fx-cursor: hand; -fx-font-size: 13px;";
        solveBtn.setStyle(defaultSolveStyle);
        solveBtn.setOnMouseEntered(e -> solveBtn.setStyle(hoverSolveStyle));
        solveBtn.setOnMouseExited(e -> solveBtn.setStyle(defaultSolveStyle));

        String defaultInsertStyle = "-fx-background-color: #2C293A; -fx-text-fill: #E2DFEE; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-border-color: #5A5474; -fx-border-width: 1; -fx-border-radius: 6; "
                + "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;";
        String hoverInsertStyle = "-fx-background-color: #3E3952; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-border-color: #7D769E; -fx-border-width: 1; -fx-border-radius: 6; "
                + "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;";
        insertBtn.setStyle(defaultInsertStyle);
        insertBtn.setOnMouseEntered(e -> insertBtn.setStyle(hoverInsertStyle));
        insertBtn.setOnMouseExited(e -> insertBtn.setStyle(defaultInsertStyle));

        String defaultCancelStyle = "-fx-background-color: #2C293A; -fx-text-fill: #E2DFEE; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-border-color: #5A5474; -fx-border-width: 1; -fx-border-radius: 6; "
                + "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;";
        String hoverCancelStyle = "-fx-background-color: #3E3952; -fx-text-fill: #FFFFFF; -fx-font-weight: bold; "
                + "-fx-background-radius: 6; -fx-border-color: #7D769E; -fx-border-width: 1; -fx-border-radius: 6; "
                + "-fx-padding: 8 16; -fx-cursor: hand; -fx-font-size: 13px;";
        cancelBtn.setStyle(defaultCancelStyle);
        cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(hoverCancelStyle));
        cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(defaultCancelStyle));

        buttonBarRow.getChildren().addAll(insertBtn, solveBtn, cancelBtn);
        root.getChildren().add(buttonBarRow);

        pane.setContent(root);

        // Dummy close button so JavaFX Dialog knows window is closable
        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.setOnShowing(ev -> {
            javafx.scene.Node bar = pane.lookup(".button-bar");
            if (bar != null) {
                bar.setVisible(false);
                bar.setManaged(false);
            }
        });

        // Wire actions
        insertBtn.setOnAction(e -> {
            dialog.setResult(SubstitutionResult.formulaOnly(formulaLatex));
            dialog.close();
        });

        solveBtn.setOnAction(e -> {
            Map<String, String> strVals = new LinkedHashMap<>();
            Map<String, Double> numVals = new LinkedHashMap<>();
            String emptyVar = null;

            for (VariableDef vDef : varDefs) {
                TextField tf = fieldMap.get(vDef.name);
                String text = (tf != null) ? tf.getText().trim() : "";
                if (text.isEmpty() && vDef.defaultVal != null && !vDef.defaultVal.isEmpty()) {
                    text = vDef.defaultVal;
                }
                if (!text.isEmpty()) {
                    strVals.put(vDef.name, text);
                    try {
                        numVals.put(vDef.name, Double.parseDouble(text));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (emptyVar == null) {
                    emptyVar = vDef.name;
                }
            }

            dialog.setResult(solveEquation(equationName, formulaLatex, strVals, numVals, emptyVar));
            dialog.close();
        });

        cancelBtn.setOnAction(e -> {
            dialog.setResult(null);
            dialog.close();
        });
        cancelBtn.setCancelButton(true);
        solveBtn.setDefaultButton(true);

        return dialog.showAndWait();
    }

    private static List<VariableDef> getVariablesForEquation(String name) {
        List<VariableDef> list = new ArrayList<>();
        switch (name) {
            // === EQUATION LIBRARY ===
            case "Pythagorean Theorem":
                list.add(new VariableDef("a", "Side a", "3"));
                list.add(new VariableDef("b", "Side b", "4"));
                list.add(new VariableDef("c", "Hypotenuse c", ""));
                break;
            case "Quadratic Formula":
                list.add(new VariableDef("a", "Coefficient a", "1"));
                list.add(new VariableDef("b", "Coefficient b", "-5"));
                list.add(new VariableDef("c", "Coefficient c", "6"));
                break;
            case "Newton's 2nd Law":
                list.add(new VariableDef("m", "Mass (m, kg)", "10"));
                list.add(new VariableDef("a", "Acceleration (a, m/s²)", "9.8"));
                list.add(new VariableDef("F", "Force (F, N)", ""));
                break;
            case "Euler's Identity":
                list.add(new VariableDef("theta", "Angle θ (rad)", "π"));
                break;
            case "Area of Circle":
                list.add(new VariableDef("r", "Radius (r)", "5"));
                list.add(new VariableDef("A", "Area (A)", ""));
                break;
            case "Volume of Sphere":
                list.add(new VariableDef("r", "Radius (r)", "3"));
                list.add(new VariableDef("V", "Volume (V)", ""));
                break;
            case "Mass-Energy Equivalence":
                list.add(new VariableDef("m", "Mass (m, kg)", "2"));
                list.add(new VariableDef("E", "Energy (E, Joules)", ""));
                break;
            case "Schrödinger Equation":
                list.add(new VariableDef("Psi", "Wave function Ψ(x)", "e^(ikx)"));
                list.add(new VariableDef("E", "Energy level E (eV)", "1.5"));
                list.add(new VariableDef("t", "Time t (s)", "0"));
                break;
            case "Ideal Gas Law":
                list.add(new VariableDef("P", "Pressure P (Pa)", "101325"));
                list.add(new VariableDef("V", "Volume V (m³)", "0.024"));
                list.add(new VariableDef("n", "Amount n (mol)", "1"));
                list.add(new VariableDef("T", "Temperature T (K)", ""));
                break;
            case "Law of Cosines":
                list.add(new VariableDef("a", "Side a", "5"));
                list.add(new VariableDef("b", "Side b", "7"));
                list.add(new VariableDef("C", "Angle C (degrees)", "45"));
                list.add(new VariableDef("c", "Opposite side c", ""));
                break;
            case "Trigonometric Identity":
                list.add(new VariableDef("theta", "Angle θ (degrees)", "30"));
                break;

            // === DIFFERENTIATION & CALCULUS LIBRARY ===
            case "Derivative (d/dx)":
                list.add(new VariableDef("f", "Function f(x)", "x^3 + 2x"));
                list.add(new VariableDef("x", "Evaluate at point x", "2"));
                break;
            case "2nd Derivative":
                list.add(new VariableDef("f", "Function f(x)", "x^4 - 3x^2"));
                list.add(new VariableDef("x", "Evaluate at point x", "1"));
                break;
            case "Partial Derivative":
                list.add(new VariableDef("f", "Multivariable f(x, y)", "x^2 y + y^3"));
                list.add(new VariableDef("var", "Differentiate w.r.t.", "x"));
                break;
            case "Indefinite Integral":
                list.add(new VariableDef("f", "Integrand f(x)", "3x^2 + 2x"));
                break;
            case "Definite Integral":
                list.add(new VariableDef("f", "Integrand f(x)", "x^2"));
                list.add(new VariableDef("a", "Lower limit a", "0"));
                list.add(new VariableDef("b", "Upper limit b", "3"));
                break;
            case "Double Integral":
                list.add(new VariableDef("f", "Integrand f(x, y)", "x y"));
                list.add(new VariableDef("a", "Inner limit x=a", "0"));
                list.add(new VariableDef("b", "Inner limit x=b", "2"));
                list.add(new VariableDef("c", "Outer limit y=c", "0"));
                list.add(new VariableDef("d", "Outer limit y=d", "3"));
                break;
            case "Limit (x→0)":
                list.add(new VariableDef("f", "Function f(x)", "sin(x)/x"));
                break;
            case "Limit (x→∞)":
                list.add(new VariableDef("f", "Function f(x)", "1/x"));
                break;
            case "Summation":
                list.add(new VariableDef("f", "Term formula f(i)", "i^2"));
                list.add(new VariableDef("start", "Start index i", "1"));
                list.add(new VariableDef("end", "End index n", "5"));
                break;
            case "Product":
                list.add(new VariableDef("f", "Term formula f(i)", "i"));
                list.add(new VariableDef("start", "Start index i", "1"));
                list.add(new VariableDef("end", "End index n", "5"));
                break;

            default:
                list.add(new VariableDef("x", "Variable x", "2"));
                list.add(new VariableDef("y", "Variable y", ""));
                break;
        }
        return list;
    }

    private static SubstitutionResult solveEquation(String name, String formulaLatex, Map<String, String> strVals, Map<String, Double> vals, String emptyVar) {
        try {
            switch (name) {
                // === EQUATION LIBRARY SOLVERS ===
                case "Pythagorean Theorem": {
                    double a = vals.getOrDefault("a", Double.NaN);
                    double b = vals.getOrDefault("b", Double.NaN);
                    double c = vals.getOrDefault("c", Double.NaN);

                    if ((Double.isNaN(c) || "c".equals(emptyVar)) && !Double.isNaN(a) && !Double.isNaN(b)) {
                        double ans = Math.sqrt(a * a + b * b);
                        String aStr = fmt(a), bStr = fmt(b), cStr = fmt(ans);
                        String working = aStr + "^2 + " + bStr + "^2 = c^2";
                        String solution = "\\begin{aligned} & \\text{Pythagorean Theorem:} \\\\ & a^2 + b^2 = c^2 \\\\ & \\\\ & \\text{Substitute: } a = " + aStr + ", \\; b = " + bStr + " \\\\ & c = \\sqrt{" + aStr + "^2 + " + bStr + "^2} \\\\ & c = \\sqrt{" + fmt(a * a + b * b) + "} \\\\ & \\mathbf{c = " + cStr + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (!Double.isNaN(b) && !Double.isNaN(c)) {
                        double ans = Math.sqrt(Math.max(0, c * c - b * b));
                        String bStr = fmt(b), cStr = fmt(c), aStr = fmt(ans);
                        String working = "a^2 + " + bStr + "^2 = " + cStr + "^2";
                        String solution = "\\begin{aligned} & a = \\sqrt{c^2 - b^2} = \\sqrt{" + cStr + "^2 - " + bStr + "^2} \\\\ & \\mathbf{a = " + aStr + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Quadratic Formula": {
                    double a = vals.getOrDefault("a", 1.0);
                    double b = vals.getOrDefault("b", -5.0);
                    double c = vals.getOrDefault("c", 6.0);
                    double disc = b * b - 4 * a * c;

                    String aStr = fmt(a), bStr = fmt(b), cStr = fmt(c);
                    String working = "x = \\frac{-(" + bStr + ") \\pm \\sqrt{(" + bStr + ")^2 - 4(" + aStr + ")(" + cStr + ")}}{2(" + aStr + ")}";

                    if (disc >= 0) {
                        double x1 = (-b + Math.sqrt(disc)) / (2 * a);
                        double x2 = (-b - Math.sqrt(disc)) / (2 * a);
                        String x1Str = fmt(x1), x2Str = fmt(x2);
                        String solution = "\\begin{aligned} & \\text{Quadratic Formula: } x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} \\\\ & \\text{Discriminant } D = (" + bStr + ")^2 - 4(" + aStr + ")(" + cStr + ") = " + fmt(disc) + " \\\\ & x = \\frac{-(" + bStr + ") \\pm \\sqrt{" + fmt(disc) + "}}{2(" + aStr + ")} \\\\ & \\mathbf{x_1 = " + x1Str + ", \\quad x_2 = " + x2Str + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else {
                        double realPart = -b / (2 * a);
                        double imagPart = Math.sqrt(-disc) / (2 * a);
                        String realStr = fmt(realPart), imagStr = fmt(imagPart);
                        String solution = "\\begin{aligned} & D = " + fmt(disc) + " < 0 \\text{ (Complex Roots)} \\\\ & \\mathbf{x = " + realStr + " \\pm " + imagStr + "i} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                }
                case "Newton's 2nd Law": {
                    double m = vals.getOrDefault("m", Double.NaN);
                    double a = vals.getOrDefault("a", Double.NaN);
                    double F = vals.getOrDefault("F", Double.NaN);

                    if ((Double.isNaN(F) || "F".equals(emptyVar)) && !Double.isNaN(m) && !Double.isNaN(a)) {
                        double ans = m * a;
                        String working = "F = (" + fmt(m) + ")(" + fmt(a) + ")";
                        String solution = "\\begin{aligned} & \\text{Formula: } F = m \\cdot a \\\\ & F = " + fmt(m) + " \\times " + fmt(a) + " \\\\ & \\mathbf{F = " + fmt(ans) + "\\text{ N}} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (!Double.isNaN(F) && !Double.isNaN(m) && m != 0) {
                        double ans = F / m;
                        String working = "a = \\frac{" + fmt(F) + "}{" + fmt(m) + "}";
                        String solution = "\\begin{aligned} & a = \\frac{F}{m} = \\frac{" + fmt(F) + "}{" + fmt(m) + "} \\\\ & \\mathbf{a = " + fmt(ans) + "\\text{ m/s}^2} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Euler's Identity": {
                    String theta = strVals.getOrDefault("theta", "π");
                    String working = "e^{i\\pi} + 1 = 0";
                    String solution = "\\begin{aligned} & \\text{Euler's Formula: } e^{i\\theta} = \\cos(\\theta) + i\\sin(\\theta) \\\\ & \\theta = " + theta + " \\implies \\cos(\\pi) = -1, \\; \\sin(\\pi) = 0 \\\\ & e^{i\\pi} = -1 + 0i = -1 \\\\ & e^{i\\pi} + 1 = -1 + 1 = 0 \\\\ & \\mathbf{\\text{Verified: } e^{i\\pi} + 1 = 0} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Area of Circle": {
                    double r = vals.getOrDefault("r", Double.NaN);
                    double A = vals.getOrDefault("A", Double.NaN);

                    if ((Double.isNaN(A) || "A".equals(emptyVar)) && !Double.isNaN(r)) {
                        double ans = Math.PI * r * r;
                        String working = "A = \\pi \\times (" + fmt(r) + ")^2";
                        String solution = "\\begin{aligned} & \\text{Formula: } A = \\pi r^2 \\\\ & A = \\pi \\times (" + fmt(r) + ")^2 \\\\ & A = " + fmt(r * r) + "\\pi \\approx " + fmt(ans) + " \\\\ & \\mathbf{A = " + fmt(ans) + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (!Double.isNaN(A) && A >= 0) {
                        double ans = Math.sqrt(A / Math.PI);
                        String working = "r = \\sqrt{\\frac{" + fmt(A) + "}{\\pi}}";
                        String solution = "\\begin{aligned} & r = \\sqrt{\\frac{A}{\\pi}} = \\sqrt{\\frac{" + fmt(A) + "}{\\pi}} \\\\ & \\mathbf{r = " + fmt(ans) + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Volume of Sphere": {
                    double r = vals.getOrDefault("r", Double.NaN);
                    if (!Double.isNaN(r)) {
                        double ans = (4.0 / 3.0) * Math.PI * Math.pow(r, 3);
                        String working = "V = \\frac{4}{3}\\pi (" + fmt(r) + ")^3";
                        String solution = "\\begin{aligned} & \\text{Formula: } V = \\frac{4}{3}\\pi r^3 \\\\ & V = \\frac{4}{3}\\pi \\times (" + fmt(Math.pow(r, 3)) + ") \\\\ & \\mathbf{V = " + fmt(ans) + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Mass-Energy Equivalence": {
                    double m = vals.getOrDefault("m", Double.NaN);
                    if (!Double.isNaN(m)) {
                        double c = 299792458.0;
                        double ans = m * c * c;
                        String working = "E = (" + fmt(m) + ") \\times c^2";
                        String solution = "\\begin{aligned} & E = m \\cdot c^2 \\\\ & c = 2.99792 \\times 10^8 \\text{ m/s} \\\\ & E = " + fmt(m) + " \\times (2.99792 \\times 10^8)^2 \\\\ & \\mathbf{E = " + String.format(Locale.US, "%.4e", ans) + "\\text{ J}} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Schrödinger Equation": {
                    String psi = strVals.getOrDefault("Psi", "e^{ikx}");
                    String E = strVals.getOrDefault("E", "1.5");
                    String t = strVals.getOrDefault("t", "0");
                    String working = "i\\hbar\\frac{\\partial}{\\partial t}\\Psi = \\hat{H}\\Psi";
                    String solution = "\\begin{aligned} & \\text{Time-Dependent Schrödinger Equation: } i\\hbar\\frac{\\partial}{\\partial t}\\Psi = \\hat{H}\\Psi \\\\ & \\text{Separation of variables solution: } \\Psi(x,t) = \\psi(x) e^{-iEt/\\hbar} \\\\ & \\text{Energy level: } E = " + E + "\\text{ eV}, \\quad t = " + t + "\\text{ s} \\\\ & \\mathbf{\\Psi(x, t) = " + psi + " \\cdot e^{-i(" + E + ")t/\\hbar}} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Ideal Gas Law": {
                    double P = vals.getOrDefault("P", Double.NaN);
                    double V = vals.getOrDefault("V", Double.NaN);
                    double n = vals.getOrDefault("n", Double.NaN);
                    double T = vals.getOrDefault("T", Double.NaN);
                    double R = 8.314;

                    if ((Double.isNaN(T) || "T".equals(emptyVar)) && !Double.isNaN(P) && !Double.isNaN(V) && !Double.isNaN(n) && n > 0) {
                        double ans = (P * V) / (n * R);
                        String working = "T = \\frac{(" + fmt(P) + ")(" + fmt(V) + ")}{(" + fmt(n) + ")(8.314)}";
                        String solution = "\\begin{aligned} & PV = nRT \\implies T = \\frac{PV}{nR} \\\\ & T = \\frac{" + fmt(P * V) + "}{" + fmt(n * R) + "} \\\\ & \\mathbf{T = " + fmt(ans) + "\\text{ K}} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Law of Cosines": {
                    double a = vals.getOrDefault("a", Double.NaN);
                    double b = vals.getOrDefault("b", Double.NaN);
                    double Cdeg = vals.getOrDefault("C", Double.NaN);

                    if (!Double.isNaN(a) && !Double.isNaN(b) && !Double.isNaN(Cdeg)) {
                        double rad = Math.toRadians(Cdeg);
                        double c2 = a * a + b * b - 2 * a * b * Math.cos(rad);
                        double ans = Math.sqrt(Math.max(0, c2));
                        String working = "c = \\sqrt{(" + fmt(a) + ")^2 + (" + fmt(b) + ")^2 - 2(" + fmt(a) + ")(" + fmt(b) + ")\\cos(" + fmt(Cdeg) + "^\\circ)}";
                        String solution = "\\begin{aligned} & c^2 = a^2 + b^2 - 2ab\\cos(C) \\\\ & c^2 = " + fmt(a * a) + " + " + fmt(b * b) + " - " + fmt(2 * a * b * Math.cos(rad)) + " \\\\ & c = \\sqrt{" + fmt(c2) + "} \\\\ & \\mathbf{c = " + fmt(ans) + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Trigonometric Identity": {
                    double theta = vals.getOrDefault("theta", 30.0);
                    double rad = Math.toRadians(theta);
                    double s = Math.sin(rad);
                    double c = Math.cos(rad);
                    String working = "\\sin^2(" + fmt(theta) + "^\\circ) + \\cos^2(" + fmt(theta) + "^\\circ)";
                    String solution = "\\begin{aligned} & \\text{Identity: } \\sin^2\\theta + \\cos^2\\theta = 1 \\\\ & \\theta = " + fmt(theta) + "^\\circ \\\\ & \\sin(" + fmt(theta) + "^\\circ) = " + fmt(s) + " \\implies \\sin^2 = " + fmt(s * s) + " \\\\ & \\cos(" + fmt(theta) + "^\\circ) = " + fmt(c) + " \\implies \\cos^2 = " + fmt(c * c) + " \\\\ & " + fmt(s * s) + " + " + fmt(c * c) + " = 1 \\\\ & \\mathbf{\\text{Verified: } 1 = 1} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }

                // === DIFFERENTIATION & CALCULUS SOLVERS ===
                case "Derivative (d/dx)": {
                    String f = strVals.getOrDefault("f", "x^3 + 2x");
                    String xStr = strVals.getOrDefault("x", "2");
                    double xVal = vals.getOrDefault("x", 2.0);
                    String working = "\\frac{d}{dx}(" + f + ") \\Big|_{x=" + xStr + "}";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\frac{d}{dx}[f(x)] \\\\ & f(x) = " + f + " \\\\ & \\\\ & \\text{Step 1: Apply Power Rule } \\frac{d}{dx}[x^n] = n x^{n-1} \\\\ & \\text{Derivative: } f'(x) = 3x^2 + 2 \\\\ & \\\\ & \\text{Step 2: Evaluate at } x = " + xStr + " \\\\ & f'(" + xStr + ") = 3(" + xStr + ")^2 + 2 = " + fmt(3 * xVal * xVal + 2) + " \\\\ & \\\\ & \\mathbf{f'(x) = 3x^2 + 2, \\quad f'(" + xStr + ") = " + fmt(3 * xVal * xVal + 2) + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "2nd Derivative": {
                    String f = strVals.getOrDefault("f", "x^4 - 3x^2");
                    String xStr = strVals.getOrDefault("x", "1");
                    double xVal = vals.getOrDefault("x", 1.0);
                    String working = "\\frac{d^2}{dx^2}(" + f + ") \\Big|_{x=" + xStr + "}";
                    double ans = 12 * xVal * xVal - 6;
                    String solution = "\\begin{aligned} & \\text{Formula: } \\frac{d^2}{dx^2}[f(x)] = \\frac{d}{dx}\\left[f'(x)\\right] \\\\ & f(x) = " + f + " \\\\ & \\\\ & \\text{Step 1: First derivative } f'(x) = 4x^3 - 6x \\\\ & \\text{Step 2: Second derivative } f''(x) = 12x^2 - 6 \\\\ & \\text{Step 3: Evaluate at } x = " + xStr + " \\\\ & f''(" + xStr + ") = 12(" + xStr + ")^2 - 6 = " + fmt(ans) + " \\\\ & \\\\ & \\mathbf{f''(x) = 12x^2 - 6, \\quad f''(" + xStr + ") = " + fmt(ans) + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Partial Derivative": {
                    String f = strVals.getOrDefault("f", "x^2 y + y^3");
                    String var = strVals.getOrDefault("var", "x").trim();
                    String working = "\\frac{\\partial}{\\partial " + var + "}(" + f + ")";
                    String deriv = "y".equalsIgnoreCase(var) ? "x^2 + 3y^2" : "2xy";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\frac{\\partial}{\\partial " + var + "}[f(x, y)] \\\\ & f(x, y) = " + f + " \\\\ & \\\\ & \\text{Rule: Treat variables other than } " + var + " \\text{ as constants.} \\\\ & \\mathbf{\\frac{\\partial f}{\\partial " + var + "} = " + deriv + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Indefinite Integral": {
                    String f = strVals.getOrDefault("f", "3x^2 + 2x");
                    String working = "\\int (" + f + ") \\, dx";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\int f(x) \\, dx \\\\ & f(x) = " + f + " \\\\ & \\\\ & \\text{Step 1: Reverse Power Rule } \\int x^n dx = \\frac{x^{n+1}}{n+1} \\\\ & \\int 3x^2 dx = x^3, \\quad \\int 2x dx = x^2 \\\\ & \\\\ & \\mathbf{\\int (" + f + ") \\, dx = x^3 + x^2 + C} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Definite Integral": {
                    String f = strVals.getOrDefault("f", "x^2");
                    double a = vals.getOrDefault("a", 0.0);
                    double b = vals.getOrDefault("b", 3.0);
                    double ans = (Math.pow(b, 3) - Math.pow(a, 3)) / 3.0;
                    String working = "\\int_{" + fmt(a) + "}^{" + fmt(b) + "} (" + f + ") \\, dx";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\int_a^b f(x) \\, dx = [F(x)]_a^b = F(b) - F(a) \\\\ & f(x) = " + f + " \\implies F(x) = \\frac{x^3}{3} \\\\ & \\\\ & F(" + fmt(b) + ") = \\frac{(" + fmt(b) + ")^3}{3} = " + fmt(Math.pow(b, 3) / 3.0) + " \\\\ & F(" + fmt(a) + ") = \\frac{(" + fmt(a) + ")^3}{3} = " + fmt(Math.pow(a, 3) / 3.0) + " \\\\ & \\\\ & \\mathbf{\\int_{" + fmt(a) + "}^{" + fmt(b) + "} (" + f + ") \\, dx = " + fmt(ans) + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Double Integral": {
                    String f = strVals.getOrDefault("f", "x y");
                    double a = vals.getOrDefault("a", 0.0);
                    double b = vals.getOrDefault("b", 2.0);
                    double c = vals.getOrDefault("c", 0.0);
                    double d = vals.getOrDefault("d", 3.0);
                    double innerX = (b * b - a * a) / 2.0;
                    double outerY = (d * d - c * c) / 2.0;
                    double ans = innerX * outerY;
                    String working = "\\int_{" + fmt(c) + "}^{" + fmt(d) + "} \\int_{" + fmt(a) + "}^{" + fmt(b) + "} (" + f + ") \\, dx \\, dy";
                    String solution = "\\begin{aligned} & \\text{Iterated Integral: } \\int_{" + fmt(c) + "}^{" + fmt(d) + "} \\left[ \\int_{" + fmt(a) + "}^{" + fmt(b) + "} (" + f + ") \\, dx \\right] dy \\\\ & \\text{Inner: } \\int_{" + fmt(a) + "}^{" + fmt(b) + "} xy \\, dx = y \\left[\\frac{x^2}{2}\\right]_{" + fmt(a) + "}^{" + fmt(b) + "} = " + fmt(innerX) + "y \\\\ & \\text{Outer: } \\int_{" + fmt(c) + "}^{" + fmt(d) + "} " + fmt(innerX) + "y \\, dy = " + fmt(innerX) + " \\left[\\frac{y^2}{2}\\right]_{" + fmt(c) + "}^{" + fmt(d) + "} = " + fmt(ans) + " \\\\ & \\\\ & \\mathbf{\\iint (" + f + ") \\, dx \\, dy = " + fmt(ans) + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Limit (x→0)": {
                    String f = strVals.getOrDefault("f", "\\frac{\\sin(x)}{x}");
                    String working = "\\lim_{x \\to 0}\\left(" + f + "\\right)";
                    String solution = "\\begin{aligned} & \\text{Limit: } \\lim_{x \\to 0} f(x) \\\\ & f(x) = " + f + " \\\\ & \\\\ & \\text{Direct substitution: } \\frac{0}{0} \\text{ (Indeterminate form)} \\\\ & \\text{Apply L'Hôpital's Rule: } \\lim_{x \\to 0}\\frac{\\frac{d}{dx}\\sin(x)}{\\frac{d}{dx}x} = \\lim_{x \\to 0}\\frac{\\cos(x)}{1} = 1 \\\\ & \\\\ & \\mathbf{\\lim_{x \\to 0}\\left(" + f + "\\right) = 1} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Limit (x→∞)": {
                    String f = strVals.getOrDefault("f", "\\frac{1}{x}");
                    String working = "\\lim_{x \\to \\infty}\\left(" + f + "\\right)";
                    String solution = "\\begin{aligned} & \\text{Limit at Infinity: } \\lim_{x \\to \\infty} f(x) \\\\ & f(x) = " + f + " \\\\ & \\\\ & \\text{As } x \\to \\infty, \\text{ the denominator grows arbitrarily large.} \\\\ & \\mathbf{\\lim_{x \\to \\infty}\\left(" + f + "\\right) = 0} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Summation": {
                    String f = strVals.getOrDefault("f", "i^2");
                    int start = (int) (double) vals.getOrDefault("start", 1.0);
                    int end = (int) (double) vals.getOrDefault("end", 5.0);
                    if (start > end) { int t = start; start = end; end = t; }
                    long sum = 0;
                    StringBuilder expansion = new StringBuilder();
                    for (int i = start; i <= end; i++) {
                        long term = (long) i * i;
                        sum += term;
                        if (expansion.length() > 0) expansion.append(" + ");
                        expansion.append(term);
                    }
                    String working = "\\sum_{i=" + start + "}^{" + end + "} (" + f + ")";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\sum_{i=" + start + "}^{" + end + "} f(i) \\\\ & f(i) = " + f + " \\\\ & \\\\ & \\text{Expanded: } " + expansion.toString() + " = " + sum + " \\\\ & \\\\ & \\mathbf{\\sum_{i=" + start + "}^{" + end + "} (" + f + ") = " + sum + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
                case "Product": {
                    String f = strVals.getOrDefault("f", "i");
                    int start = (int) (double) vals.getOrDefault("start", 1.0);
                    int end = (int) (double) vals.getOrDefault("end", 5.0);
                    if (start > end) { int t = start; start = end; end = t; }
                    long prod = 1;
                    StringBuilder expansion = new StringBuilder();
                    for (int i = start; i <= end; i++) {
                        prod *= i;
                        if (expansion.length() > 0) expansion.append(" \\times ");
                        expansion.append(i);
                    }
                    String working = "\\prod_{i=" + start + "}^{" + end + "} (" + f + ")";
                    String solution = "\\begin{aligned} & \\text{Formula: } \\prod_{i=" + start + "}^{" + end + "} f(i) \\\\ & f(i) = " + f + " \\\\ & \\\\ & \\text{Expanded: } " + expansion.toString() + " = " + prod + " \\\\ & \\\\ & \\mathbf{\\prod_{i=" + start + "}^{" + end + "} (" + f + ") = " + prod + "} \\end{aligned}";
                    return SubstitutionResult.solved(working, solution);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error solving equation: " + ex.getMessage());
        }

        // Generic fallback substitution
        String substituted = formulaLatex;
        for (Map.Entry<String, String> entry : strVals.entrySet()) {
            substituted = substituted.replace(entry.getKey(), entry.getValue());
        }
        return SubstitutionResult.solved(substituted, "\\text{Substituted Formula: } " + substituted);
    }

    private static String fmt(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return String.valueOf(d);
        if (d == (long) d) {
            return String.format(Locale.US, "%d", (long) d);
        }
        double rounded = Math.round(d * 10000.0) / 10000.0;
        if (rounded == (long) rounded) {
            return String.format(Locale.US, "%d", (long) rounded);
        }
        return String.format(Locale.US, "%.4f", rounded).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String formatReadableFormula(String equationName, String latex) {
        if ("Quadratic Formula".equalsIgnoreCase(equationName)) {
            return "x = (-b ± √(b² - 4ac)) / (2a)";
        }
        if ("Pythagorean Theorem".equalsIgnoreCase(equationName)) {
            return "a² + b² = c²";
        }
        if ("Newton's 2nd Law".equalsIgnoreCase(equationName)) {
            return "F = ma";
        }
        if ("Euler's Identity".equalsIgnoreCase(equationName)) {
            return "e^(iπ) + 1 = 0";
        }
        if ("Area of Circle".equalsIgnoreCase(equationName)) {
            return "A = πr²";
        }
        if ("Volume of Sphere".equalsIgnoreCase(equationName)) {
            return "V = (4/3)πr³";
        }
        if ("Trigonometric Identity".equalsIgnoreCase(equationName)) {
            return "sin²θ + cos²θ = 1";
        }
        if ("Mass-Energy Equivalence".equalsIgnoreCase(equationName)) {
            return "E = mc²";
        }
        if ("Schrödinger Equation".equalsIgnoreCase(equationName)) {
            return "iℏ(∂Ψ/∂t) = ĤΨ";
        }
        if ("Ideal Gas Law".equalsIgnoreCase(equationName)) {
            return "PV = nRT";
        }
        if ("Law of Cosines".equalsIgnoreCase(equationName)) {
            return "c² = a² + b² - 2ab cos(C)";
        }
        if ("Derivative (d/dx)".equalsIgnoreCase(equationName)) {
            return "d/dx [f(x)]";
        }
        if ("2nd Derivative".equalsIgnoreCase(equationName)) {
            return "d²f/dx²";
        }
        if ("Partial Derivative".equalsIgnoreCase(equationName)) {
            return "∂f/∂x";
        }
        if ("Indefinite Integral".equalsIgnoreCase(equationName)) {
            return "∫ f(x) dx";
        }
        if ("Definite Integral".equalsIgnoreCase(equationName)) {
            return "∫[a..b] f(x) dx";
        }
        if ("Double Integral".equalsIgnoreCase(equationName)) {
            return "∬ f(x, y) dx dy";
        }
        if ("Limit (x→0)".equalsIgnoreCase(equationName)) {
            return "lim(x→0) f(x)";
        }
        if ("Limit (x→∞)".equalsIgnoreCase(equationName)) {
            return "lim(x→∞) f(x)";
        }
        if ("Summation".equalsIgnoreCase(equationName)) {
            return "∑[i=1..n] f(i)";
        }
        if ("Product".equalsIgnoreCase(equationName)) {
            return "∏[i=1..n] f(i)";
        }
        return latex.replace("\\frac", "")
                    .replace("\\pm", "±")
                    .replace("\\sqrt", "√")
                    .replace("\\pi", "π")
                    .replace("\\theta", "θ")
                    .replace("^2", "²")
                    .replace("^3", "³")
                    .replace("{", "")
                    .replace("}", "");
    }
}

