package com.banukasineth.advancedcalculator.app.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Beautiful, hardware-accelerated animated AI solution generating orb.
 * Renders a fluid, multi-colored chromatic sphere with glowing outer aura,
 * swirling harmonic energy vortices (amber-gold, hot magenta, electric cyan, violet),
 * dynamic breathing scale, and 3D spherical glass shading.
 */
public class AIOrbView extends StackPane {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer timer;
    private long startNano = 0;
    private boolean isRunning = false;

    private static final double WIDTH = 110.0;
    private static final double HEIGHT = 110.0;
    private static final double CX = WIDTH / 2.0;
    private static final double CY = HEIGHT / 2.0;
    private static final double BASE_RADIUS = 30.0;

    public AIOrbView() {
        this.canvas = new Canvas(WIDTH, HEIGHT);
        this.gc = canvas.getGraphicsContext2D();

        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        getChildren().add(canvas);

        DropShadow bloom = new DropShadow(BlurType.GAUSSIAN, Color.rgb(240, 45, 140, 0.65), 24, 0.35, 0, 0);
        canvas.setEffect(bloom);

        initTimer();

        // Automatically pause/resume based on scene visibility to conserve CPU
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stop();
            }
        });
        visibleProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                stop();
            }
        });

        // Initial render so it's not blank before timer starts
        renderFrame(0.0);
    }

    private void initTimer() {
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (startNano == 0) {
                    startNano = now;
                }
                double t = (now - startNano) * 1e-9;
                renderFrame(t);
            }
        };
    }

    public synchronized void start() {
        if (!isRunning) {
            isRunning = true;
            if (startNano == 0) {
                startNano = System.nanoTime();
            }
            if (timer != null) {
                timer.start();
            }
        }
    }

    public synchronized void stop() {
        if (isRunning) {
            isRunning = false;
            if (timer != null) {
                timer.stop();
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void renderFrame(double t) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // Breathing oscillation for the radius
        double currentR = BASE_RADIUS + 1.2 * Math.sin(t * 2.2);

        // 1. Outer ambient bloom/aura
        double auraR = currentR + 18.0 + 2.0 * Math.sin(t * 1.8);
        RadialGradient auraGrad = new RadialGradient(
                0, 0, CX, CY, auraR, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.20, 0.60, 0.40 + 0.08 * Math.sin(t * 2.5))),
                new Stop(0.45, Color.color(0.55, 0.18, 0.90, 0.22 + 0.05 * Math.cos(t * 1.9))),
                new Stop(0.78, Color.color(0.0, 0.85, 0.98, 0.10 + 0.04 * Math.sin(t * 1.4))),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(auraGrad);
        gc.fillOval(CX - auraR, CY - auraR, auraR * 2, auraR * 2);

        // 2. Spherical Boundary Clip
        gc.save();
        gc.beginPath();
        gc.arc(CX, CY, currentR, currentR, 0, 360);
        gc.closePath();
        gc.clip();

        // 3. Deep space / violet base sphere
        RadialGradient baseSphere = new RadialGradient(
                0, 0, CX, CY - 3, currentR, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(75, 18, 100)),
                new Stop(0.65, Color.rgb(38, 10, 58)),
                new Stop(1.0, Color.rgb(16, 6, 28))
        );
        gc.setFill(baseSphere);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 4. Energy Vortex 1: Amber Gold / Warm Sun (rotating clockwise)
        double a1 = t * 1.35;
        double r1 = 9.5 + 2.5 * Math.sin(t * 0.85);
        double x1 = CX + r1 * Math.cos(a1) - 3;
        double y1 = CY + r1 * Math.sin(a1) - 4;
        RadialGradient grad1 = new RadialGradient(
                0, 0, x1, y1, currentR * 0.95, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.78, 0.12, 0.95)),
                new Stop(0.35, Color.color(1.0, 0.46, 0.06, 0.65)),
                new Stop(0.75, Color.color(0.95, 0.16, 0.46, 0.25)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(grad1);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 5. Energy Vortex 2: Electric Fuchsia / Hot Pink (counter-rotating)
        double a2 = -t * 1.55 + 2.2;
        double r2 = 8.5 + 2.0 * Math.cos(t * 1.1);
        double x2 = CX + r2 * Math.cos(a2);
        double y2 = CY + r2 * Math.sin(a2);
        RadialGradient grad2 = new RadialGradient(
                0, 0, x2, y2, currentR * 0.98, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 0.12, 0.62, 0.95)),
                new Stop(0.45, Color.color(0.80, 0.05, 0.55, 0.60)),
                new Stop(0.80, Color.color(0.50, 0.05, 0.75, 0.22)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(grad2);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 6. Energy Vortex 3: Electric Cyan / Aqua Highlights
        double a3 = t * 1.15 + 3.8;
        double r3 = 10.5 + 2.0 * Math.sin(t * 1.3);
        double x3 = CX + r3 * Math.cos(a3) + 3;
        double y3 = CY + r3 * Math.sin(a3) + 2;
        RadialGradient grad3 = new RadialGradient(
                0, 0, x3, y3, currentR * 0.88, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.0, 0.92, 1.0, 0.90)),
                new Stop(0.42, Color.color(0.0, 0.58, 1.0, 0.52)),
                new Stop(0.78, Color.color(0.35, 0.10, 0.85, 0.20)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(grad3);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 7. Energy Vortex 4: Deep Royal Violet
        double a4 = -t * 0.90 + 1.0;
        double r4 = 9.0 + 2.5 * Math.cos(t * 0.7);
        double x4 = CX + r4 * Math.cos(a4) - 2;
        double y4 = CY + r4 * Math.sin(a4) + 5;
        RadialGradient grad4 = new RadialGradient(
                0, 0, x4, y4, currentR * 0.92, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(0.62, 0.22, 1.0, 0.85)),
                new Stop(0.52, Color.color(0.42, 0.10, 0.82, 0.45)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(grad4);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 8. Luminous Core Specular Swirl
        double x5 = CX + 3.0 * Math.cos(t * 1.8);
        double y5 = CY - 3.0 + 3.0 * Math.sin(t * 1.8);
        RadialGradient grad5 = new RadialGradient(
                0, 0, x5, y5, currentR * 0.55, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.color(1.0, 1.0, 1.0, 0.65)),
                new Stop(0.40, Color.color(1.0, 0.86, 0.96, 0.35)),
                new Stop(1.0, Color.TRANSPARENT)
        );
        gc.setFill(grad5);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 9. 3D Spherical Edge Vignette (creates tactile sphere depth)
        RadialGradient edgeShading = new RadialGradient(
                0, 0, CX - 4, CY - 4, currentR, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.TRANSPARENT),
                new Stop(0.70, Color.TRANSPARENT),
                new Stop(0.88, Color.color(0.08, 0.04, 0.15, 0.35)),
                new Stop(0.96, Color.color(0.05, 0.02, 0.10, 0.65)),
                new Stop(1.0, Color.color(0.02, 0.01, 0.06, 0.85))
        );
        gc.setFill(edgeShading);
        gc.fillOval(CX - currentR, CY - currentR, currentR * 2, currentR * 2);

        // 10. Specular Glass Arc Highlight (top-left)
        gc.setFill(Color.color(1.0, 1.0, 1.0, 0.22 + 0.06 * Math.sin(t * 2.0)));
        gc.fillOval(CX - currentR * 0.55, CY - currentR * 0.65, currentR * 0.65, currentR * 0.35);

        gc.restore();
    }
}
