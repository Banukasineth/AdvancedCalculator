# Advanced Calculator

A modern, high-precision desktop calculator application built with **JavaFX** and **Google Gemini AI** integration. Features a sleek glassmorphic dark theme, KaTeX mathematical typesetting, comprehensive formula substitution libraries, differentiation/calculus solving, programmer base conversions, and offline fallbacks.

---

## 🌟 Key Features

- **Standard & Advanced Math**: High-precision arithmetic, trigonometric, exponential, and algebraic calculations.
- **Equation Substitution Library**:
  - Interactive variable substitution dialog with KaTeX formula preview.
  - Pre-configured formulas: Pythagorean Theorem, Quadratic Formula, Newton's 2nd Law, Ideal Gas Law, Area of Circle, Volume of Sphere, Law of Cosines, Euler's Identity, Schrödinger Equation, and more.
- **Differentiation & Calculus Engine**:
  - Supports Derivatives ($d/dx$), 2nd Derivatives ($d^2/dx^2$), Partial Derivatives, Indefinite Integrals, Definite Integrals, Double Integrals, Limits ($x \to 0$ and $x \to \infty$), Summations ($\sum$), and Products ($\prod$).
  - Interactive variable input with step-by-step mathematical working.
- **AI Math Solver (Google Gemini)**:
  - Real-time step-by-step mathematical explanations powered by Google Gemini (`gemini-3.5-flash-lite`, `gemini-3.5-flash`, `gemini-3.8-flash`).
  - Seamless offline fallback when no API key is provided.
- **Programmer Mode**: Binary, Octal, Decimal, and Hexadecimal representations with bitwise operations (AND, OR, XOR, NOT, shift).
- **Unit Converter**: Quick conversions across length, mass, temperature, time, and data units.
- **Beautiful Dark Glassmorphism UI**: High-contrast typography, interactive hover micro-animations, and animated AI thinking orb.

---

## 🔑 AI Configuration (Security & Privacy)

This repository is safe for public hosting: **no API keys or private credentials are included in the source code**.

To enable real-time AI solutions, you can provide your own free Gemini API key using **any** of the following methods:

### Method 1: In-App Settings (Recommended)
1. Launch the application.
2. In the AI Solution card, click the **Settings (gear icon)**.
3. Paste your Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
4. Click **Test Connection** and then **Save & Enable AI**.
   *(Your key is securely stored in your local user preferences and never tracked by Git.)*

### Method 2: Local `config.properties`
1. Copy the example configuration template:
   ```bash
   cp config.properties.example config.properties
   ```
2. Open `config.properties` and paste your key:
   ```properties
   gemini.api.key=YOUR_GEMINI_API_KEY
   gemini.model.name=gemini-3.5-flash-lite
   ```
   *(Note: `config.properties` is listed in `.gitignore` and will never be committed to Git.)*

### Method 3: Environment Variable
Set the `GEMINI_API_KEY` environment variable in your shell or system:
```bash
# Windows PowerShell
$env:GEMINI_API_KEY="AIzaSy..."

# Linux / macOS
export GEMINI_API_KEY="AIzaSy..."
```

---

## 🚀 Building & Running

### Prerequisites
- **JDK 21 or higher** (JDK 25 supported)
- **Maven 3.8+** (or IntelliJ IDEA / Eclipse)
- JavaFX 25

### Run via Maven
```bash
mvn clean compile javafx:run
```

### Run via IntelliJ IDEA
1. Open the project in IntelliJ IDEA.
2. Ensure Project SDK is set to JDK 21+.
3. Run `com.banukasineth.advancedcalculator.app.Main`.

---

## 🔒 Security Notice
- Private configuration files (`config.properties`, `.env`, keystores) and user data are strictly excluded via `.gitignore`.
- Always verify before pushing to public repositories:
  ```bash
  git status
  ```
