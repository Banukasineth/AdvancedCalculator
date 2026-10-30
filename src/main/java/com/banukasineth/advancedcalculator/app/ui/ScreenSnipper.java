package com.banukasineth.advancedcalculator.app.ui;

import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Interactive Screen Snipping tool.
 * Dims the screen and allows the user to drag a marquee rectangle
 * around any mathematical equation, PDF problem, or slide on the desktop.
 */
public class ScreenSnipper {

    public static void startSnip(Stage parentStage, Consumer<BufferedImage> onCaptured) {
        // Temporarily minimize or hide parent stage so it's not in the way
        boolean wasIconified = parentStage != null && parentStage.isIconified();
        if (parentStage != null) {
            parentStage.setIconified(true);
        }

        // Brief delay to allow desktop window minimizing transition to finish
        new Thread(() -> {
            try {
                Thread.sleep(280);
            } catch (InterruptedException ignored) {}

            Platform.runLater(() -> captureAndOpenOverlay(parentStage, wasIconified, onCaptured));
        }, "ScreenSnipper-Worker").start();
    }

    private static void captureAndOpenOverlay(Stage parentStage, boolean wasIconified, Consumer<BufferedImage> onCaptured) {
        try {
            Rectangle2D primaryBounds = Screen.getPrimary().getBounds();
            Robot robot = new Robot();
            java.awt.Rectangle screenRect = new java.awt.Rectangle(
                    (int) primaryBounds.getMinX(),
                    (int) primaryBounds.getMinY(),
                    (int) primaryBounds.getWidth(),
                    (int) primaryBounds.getHeight()
            );

            BufferedImage fullDesktop = robot.createScreenCapture(screenRect);
            javafx.scene.image.Image fxImage = toFXImage(fullDesktop);

            double dpiScaleX = (double) fullDesktop.getWidth() / primaryBounds.getWidth();
            double dpiScaleY = (double) fullDesktop.getHeight() / primaryBounds.getHeight();

            Stage snipStage = new Stage(StageStyle.TRANSPARENT);
            snipStage.setAlwaysOnTop(true);
            snipStage.setX(primaryBounds.getMinX());
            snipStage.setY(primaryBounds.getMinY());
            snipStage.setWidth(primaryBounds.getWidth());
            snipStage.setHeight(primaryBounds.getHeight());

            Pane root = new Pane();
            root.setCursor(Cursor.CROSSHAIR);

            // 1. Background image of the desktop
            ImageView bgView = new ImageView(fxImage);
            bgView.setFitWidth(primaryBounds.getWidth());
            bgView.setFitHeight(primaryBounds.getHeight());

            // 2. Dimmed dark overlay
            Rectangle dimOverlay = new Rectangle(primaryBounds.getWidth(), primaryBounds.getHeight());
            dimOverlay.setFill(Color.rgb(10, 10, 20, 0.48));

            // 3. Selection marquee rectangle
            Rectangle selectionRect = new Rectangle();
            selectionRect.setFill(Color.TRANSPARENT);
            selectionRect.setStroke(Color.web("#00D2FF"));
            selectionRect.setStrokeWidth(2.0);
            selectionRect.getStrokeDashArray().addAll(6.0, 4.0);
            selectionRect.setStyle("-fx-effect: dropshadow(gaussian, #00D2FF, 8, 0.6, 0, 0);");
            selectionRect.setVisible(false);

            // 4. Highlighted viewport showing clear original screen inside selection
            ImageView highlightView = new ImageView(fxImage);
            highlightView.setFitWidth(primaryBounds.getWidth());
            highlightView.setFitHeight(primaryBounds.getHeight());
            Rectangle clipRect = new Rectangle(0, 0, 0, 0);
            highlightView.setClip(clipRect);
            highlightView.setVisible(false);

            // 5. Instruction & dimensions badge
            Label badge = new Label("Drag to snip math equation • Release to scan • ESC to cancel");
            badge.setStyle("-fx-background-color: rgba(20, 20, 35, 0.88); -fx-text-fill: #FFFFFF; "
                    + "-fx-padding: 6 12; -fx-background-radius: 6; -fx-border-color: #00D2FF; "
                    + "-fx-border-radius: 6; -fx-border-width: 1; -fx-font-size: 13px; -fx-font-weight: bold; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.7), 10, 0, 0, 2);");
            badge.setLayoutX((primaryBounds.getWidth() - 400) / 2);
            badge.setLayoutY(30);

            root.getChildren().addAll(bgView, dimOverlay, highlightView, selectionRect, badge);

            final double[] startPos = new double[2];

            root.setOnMousePressed((MouseEvent ev) -> {
                startPos[0] = ev.getX();
                startPos[1] = ev.getY();
                selectionRect.setX(startPos[0]);
                selectionRect.setY(startPos[1]);
                selectionRect.setWidth(0);
                selectionRect.setHeight(0);
                selectionRect.setVisible(true);

                clipRect.setX(startPos[0]);
                clipRect.setY(startPos[1]);
                clipRect.setWidth(0);
                clipRect.setHeight(0);
                highlightView.setVisible(true);
            });

            root.setOnMouseDragged((MouseEvent ev) -> {
                double curX = ev.getX();
                double curY = ev.getY();

                double minX = Math.max(0, Math.min(startPos[0], curX));
                double minY = Math.max(0, Math.min(startPos[1], curY));
                double width = Math.min(primaryBounds.getWidth() - minX, Math.abs(curX - startPos[0]));
                double height = Math.min(primaryBounds.getHeight() - minY, Math.abs(curY - startPos[1]));

                selectionRect.setX(minX);
                selectionRect.setY(minY);
                selectionRect.setWidth(width);
                selectionRect.setHeight(height);

                clipRect.setX(minX);
                clipRect.setY(minY);
                clipRect.setWidth(width);
                clipRect.setHeight(height);

                badge.setText(String.format("%.0f × %.0f px • Release to scan math", width, height));
                badge.setLayoutX(Math.max(10, Math.min(primaryBounds.getWidth() - 300, minX)));
                badge.setLayoutY(Math.max(10, minY - 36));
            });

            root.setOnMouseReleased((MouseEvent ev) -> {
                double w = selectionRect.getWidth();
                double h = selectionRect.getHeight();
                double x = selectionRect.getX();
                double y = selectionRect.getY();

                snipStage.close();
                if (parentStage != null) {
                    parentStage.setIconified(wasIconified);
                    parentStage.toFront();
                }

                if (w > 15 && h > 15) {
                    int cropX = (int) Math.round(x * dpiScaleX);
                    int cropY = (int) Math.round(y * dpiScaleY);
                    int cropW = (int) Math.round(w * dpiScaleX);
                    int cropH = (int) Math.round(h * dpiScaleY);

                    cropX = Math.max(0, Math.min(fullDesktop.getWidth() - 1, cropX));
                    cropY = Math.max(0, Math.min(fullDesktop.getHeight() - 1, cropY));
                    cropW = Math.min(fullDesktop.getWidth() - cropX, cropW);
                    cropH = Math.min(fullDesktop.getHeight() - cropY, cropH);

                    if (cropW > 5 && cropH > 5) {
                        BufferedImage cropped = fullDesktop.getSubimage(cropX, cropY, cropW, cropH);
                        if (onCaptured != null) {
                            onCaptured.accept(cropped);
                        }
                    }
                }
            });

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    snipStage.close();
                    if (parentStage != null) {
                        parentStage.setIconified(wasIconified);
                        parentStage.toFront();
                    }
                }
            });

            snipStage.setScene(scene);
            snipStage.show();

        } catch (Exception e) {
            System.err.println("ScreenSnipper error: " + e.getMessage());
            if (parentStage != null) {
                parentStage.setIconified(wasIconified);
            }
        }
    }

    public static Image toFXImage(BufferedImage bimg) {
        if (bimg == null) return null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) {
            System.err.println("Failed to convert BufferedImage to Image: " + e.getMessage());
            return null;
        }
    }
}
