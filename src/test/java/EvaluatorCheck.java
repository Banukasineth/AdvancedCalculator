// Standalone test runner — no package declaration needed at this path

import com.banukasineth.advancedcalculator.app.util.MathEvaluator;

public class EvaluatorCheck {
    public static void main(String[] args) {
        String[] exprs = {
            "12 + 34",
            "5 + 3 \\times 2",
            "100 - 25 \\times 2",
            "\\sqrt{16} + 5",
            "(4 + 2) \\times 3 - 5",
            "10 + 20 + 30",
            "5 \\times 6"
        };

        for (String e : exprs) {
            MathEvaluator.EvaluationResult res = MathEvaluator.evaluate(e);
            System.out.println("EXPRESSION: " + e);
            System.out.println("STEPS: " + res.getSteps());
            System.out.println("ALIGNED LATEX:\n" + res.getFormattedAlignedLatex(e));
            System.out.println("----------------------------------------");
        }

        System.out.println("=== PROGRAMMER MODE TESTS ===");
        MathEvaluator.EvaluationResult hexRes = MathEvaluator.evaluateProgrammer("A + B", 16);
        System.out.println("HEX A + B: " + hexRes.getFormattedResult() + ", steps: " + hexRes.getSteps());

        MathEvaluator.EvaluationResult binRes = MathEvaluator.evaluateProgrammer("1010 + 0101", 2);
        System.out.println("BIN 1010 + 0101: " + binRes.getFormattedResult() + ", steps: " + binRes.getSteps());

        MathEvaluator.EvaluationResult octRes = MathEvaluator.evaluateProgrammer("77 + 1", 8);
        System.out.println("OCT 77 + 1: " + octRes.getFormattedResult() + ", steps: " + octRes.getSteps());

        MathEvaluator.EvaluationResult andRes = MathEvaluator.evaluateProgrammer("F \\operatorname{AND} 7", 16);
        System.out.println("HEX F AND 7: " + andRes.getFormattedResult() + ", steps: " + andRes.getSteps());
    }
}
