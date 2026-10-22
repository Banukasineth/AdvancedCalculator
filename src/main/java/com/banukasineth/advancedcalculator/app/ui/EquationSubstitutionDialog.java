package com.banukasineth.advancedcalculator.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

        // Container
        VBox root = new VBox(15);
        root.setAlignment(Pos.TOP_LEFT);

        // Header Title
        Label titleLabel = new Label(equationName);
        titleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;");

        Label formulaLabel = new Label("Formula: " + formulaLatex);
        formulaLabel.setStyle("-fx-text-fill: #E82E88; -fx-font-size: 14px; -fx-font-family: 'Cambria Math', monospace;");

        Label instructionLabel = new Label("Enter values for known variables (leave one blank to solve for it):");
        instructionLabel.setStyle("-fx-text-fill: #A09EAA; -fx-font-size: 13px;");

        root.getChildren().addAll(titleLabel, formulaLabel, instructionLabel);

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

        // Custom Buttons
        ButtonType solveButtonType = new ButtonType("Substitute & Solve", ButtonBar.ButtonData.OK_DONE);
        ButtonType insertFormulaButtonType = new ButtonType("Insert Formula Only", ButtonBar.ButtonData.OTHER);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        pane.getButtonTypes().addAll(solveButtonType, insertFormulaButtonType, cancelButtonType);

        Button solveBtn = (Button) pane.lookupButton(solveButtonType);
        if (solveBtn != null) {
            solveBtn.setStyle("-fx-background-color: #E82E88; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        }

        Button insertBtn = (Button) pane.lookupButton(insertFormulaButtonType);
        if (insertBtn != null) {
            insertBtn.setStyle("-fx-background-color: #2F2C3F; -fx-text-fill: #C0BDCC; "
                    + "-fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        }

        Button cancelBtn = (Button) pane.lookupButton(cancelButtonType);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #807D8E; "
                    + "-fx-background-radius: 6; -fx-padding: 8 12; -fx-cursor: hand;");
        }

        pane.setContent(root);

        dialog.setResultConverter(btnType -> {
            if (btnType == solveButtonType) {
                Map<String, Double> values = new LinkedHashMap<>();
                String emptyVar = null;

                for (Map.Entry<String, TextField> entry : fieldMap.entrySet()) {
                    String text = entry.getValue().getText().trim();
                    if (!text.isEmpty()) {
                        try {
                            double val = Double.parseDouble(text);
                            values.put(entry.getKey(), val);
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (emptyVar == null) {
                        emptyVar = entry.getKey();
                    }
                }

                // Solve based on equation
                return solveEquation(equationName, formulaLatex, values, emptyVar);
            } else if (btnType == insertFormulaButtonType) {
                return SubstitutionResult.formulaOnly(formulaLatex);
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static List<VariableDef> getVariablesForEquation(String name) {
        List<VariableDef> list = new ArrayList<>();
        switch (name) {
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
            case "Ideal Gas Law":
                list.add(new VariableDef("P", "Pressure P (Pa)", "101325"));
                list.add(new VariableDef("V", "Volume V (m³)", "0.024"));
                list.add(new VariableDef("n", "Moles n (mol)", "1"));
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
            default:
                list.add(new VariableDef("x", "Variable x", "2"));
                list.add(new VariableDef("y", "Variable y", ""));
                break;
        }
        return list;
    }

    private static SubstitutionResult solveEquation(String name, String formulaLatex, Map<String, Double> vals, String emptyVar) {
        try {
            switch (name) {
                case "Pythagorean Theorem": {
                    double a = vals.getOrDefault("a", Double.NaN);
                    double b = vals.getOrDefault("b", Double.NaN);
                    double c = vals.getOrDefault("c", Double.NaN);

                    if (Double.isNaN(c) && !Double.isNaN(a) && !Double.isNaN(b)) {
                        double ans = Math.sqrt(a * a + b * b);
                        String aStr = fmt(a), bStr = fmt(b), cStr = fmt(ans);
                        String working = aStr + "^2 + " + bStr + "^2 = c^2";
                        String solution = "\\begin{aligned} & \\text{Pythagorean Theorem:} \\\\ & a^2 + b^2 = c^2 \\\\ & \\\\ & \\text{Substitute: } a = " + aStr + ", \\; b = " + bStr + " \\\\ & c = \\sqrt{" + aStr + "^2 + " + bStr + "^2} \\\\ & c = \\sqrt{" + fmt(a * a + b * b) + "} \\\\ & \\mathbf{c = " + cStr + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (Double.isNaN(a) && !Double.isNaN(b) && !Double.isNaN(c)) {
                        double ans = Math.sqrt(Math.max(0, c * c - b * b));
                        String bStr = fmt(b), cStr = fmt(c), aStr = fmt(ans);
                        String working = "a^2 + " + bStr + "^2 = " + cStr + "^2";
                        String solution = "\\begin{aligned} & a = \\sqrt{c^2 - b^2} = \\sqrt{" + cStr + "^2 - " + bStr + "^2} \\\\ & \\mathbf{a = " + aStr + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (Double.isNaN(b) && !Double.isNaN(a) && !Double.isNaN(c)) {
                        double ans = Math.sqrt(Math.max(0, c * c - a * a));
                        String aStr = fmt(a), cStr = fmt(c), bStr = fmt(ans);
                        String working = aStr + "^2 + b^2 = " + cStr + "^2";
                        String solution = "\\begin{aligned} & b = \\sqrt{c^2 - a^2} = \\sqrt{" + cStr + "^2 - " + aStr + "^2} \\\\ & \\mathbf{b = " + bStr + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Quadratic Formula": {
                    double a = vals.getOrDefault("a", 1.0);
                    double b = vals.getOrDefault("b", 0.0);
                    double c = vals.getOrDefault("c", 0.0);
                    double disc = b * b - 4 * a * c;

                    String aStr = fmt(a), bStr = fmt(b), cStr = fmt(c);
                    String working = "x = \\frac{-(" + bStr + ") \\pm \\sqrt{(" + bStr + ")^2 - 4(" + aStr + ")(" + cStr + ")}}{2(" + aStr + ")}";

                    if (disc >= 0) {
                        double x1 = (-b + Math.sqrt(disc)) / (2 * a);
                        double x2 = (-b - Math.sqrt(disc)) / (2 * a);
                        String x1Str = fmt(x1), x2Str = fmt(x2);
                        String solution = "\\begin{aligned} & \\text{Quadratic Formula: } x = \\frac{-b \\pm \\sqrt{b^2 - 4ac}}{2a} \\\\ & \\text{Discriminant } D = " + bStr + "^2 - 4(" + aStr + ")(" + cStr + ") = " + fmt(disc) + " \\\\ & x = \\frac{-(" + bStr + ") \\pm \\sqrt{" + fmt(disc) + "}}{2(" + aStr + ")} \\\\ & \\mathbf{x_1 = " + x1Str + ", \\quad x_2 = " + x2Str + "} \\end{aligned}";
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

                    if (Double.isNaN(F) && !Double.isNaN(m) && !Double.isNaN(a)) {
                        double ans = m * a;
                        String working = "F = (" + fmt(m) + ")(" + fmt(a) + ")";
                        String solution = "\\begin{aligned} & \\text{Formula: } F = m \\cdot a \\\\ & F = " + fmt(m) + " \\times " + fmt(a) + " \\\\ & \\mathbf{F = " + fmt(ans) + "\\text{ N}} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (Double.isNaN(a) && !Double.isNaN(F) && !Double.isNaN(m) && m != 0) {
                        double ans = F / m;
                        String working = "a = \\frac{" + fmt(F) + "}{" + fmt(m) + "}";
                        String solution = "\\begin{aligned} & a = \\frac{F}{m} = \\frac{" + fmt(F) + "}{" + fmt(m) + "} \\\\ & \\mathbf{a = " + fmt(ans) + "\\text{ m/s}^2} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    }
                    break;
                }
                case "Area of Circle": {
                    double r = vals.getOrDefault("r", Double.NaN);
                    double A = vals.getOrDefault("A", Double.NaN);

                    if (Double.isNaN(A) && !Double.isNaN(r)) {
                        double ans = Math.PI * r * r;
                        String working = "A = \\pi \\times (" + fmt(r) + ")^2";
                        String solution = "\\begin{aligned} & \\text{Formula: } A = \\pi r^2 \\\\ & A = \\pi \\times (" + fmt(r) + ")^2 \\\\ & A = " + fmt(r * r) + "\\pi \\approx " + fmt(ans) + " \\\\ & \\mathbf{A = " + fmt(ans) + "} \\end{aligned}";
                        return SubstitutionResult.solved(working, solution);
                    } else if (Double.isNaN(r) && !Double.isNaN(A) && A >= 0) {
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
                case "Ideal Gas Law": {
                    double P = vals.getOrDefault("P", Double.NaN);
                    double V = vals.getOrDefault("V", Double.NaN);
                    double n = vals.getOrDefault("n", Double.NaN);
                    double T = vals.getOrDefault("T", Double.NaN);
                    double R = 8.314;

                    if (Double.isNaN(T) && !Double.isNaN(P) && !Double.isNaN(V) && !Double.isNaN(n) && n > 0) {
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
            }
        } catch (Exception ex) {
            System.err.println("Error solving equation: " + ex.getMessage());
        }

        // Generic fallback substitution
        String substituted = formulaLatex;
        for (Map.Entry<String, Double> entry : vals.entrySet()) {
            substituted = substituted.replace(entry.getKey(), fmt(entry.getValue()));
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
}
