// Standalone test runner — no package declaration needed at this path

import com.banukasineth.advancedcalculator.app.service.AIConfigManager;
import com.banukasineth.advancedcalculator.app.service.AISolverService;

public class AISolverCheck {
    public static void main(String[] args) {
        System.out.println("Configured Key: " + AIConfigManager.getApiKey());
        System.out.println("Configured Model: " + AIConfigManager.getModelName());

        System.out.println("\n--- Testing API Key Connection ---");
        AIConfigManager.TestResult testRes = AIConfigManager.testApiKey(AIConfigManager.getApiKey()).join();
        System.out.println("Connection Success: " + testRes.success);
        System.out.println("Connection Message: " + testRes.message);

        System.out.println("\n--- Testing Math Expression Solver ---");
        AISolverService solver = new AISolverService();
        String mathProblem = "2x + 6 = 18";
        String solution = solver.solveMathExpression(mathProblem).join();
        System.out.println("Solution for: " + mathProblem);
        System.out.println("========================================");
        System.out.println(solution);
        System.out.println("========================================");

        String calculusProblem = "\\int x \\cos(x) dx";
        String calcSolution = solver.solveMathExpression(calculusProblem).join();
        System.out.println("Solution for: " + calculusProblem);
        System.out.println("========================================");
        System.out.println(calcSolution);
        System.out.println("========================================");
    }
}
