package com.banukasineth.advancedcalculator.app.ui;

import com.banukasineth.advancedcalculator.app.service.AIConfigManager;
import com.banukasineth.advancedcalculator.app.service.CameraMathService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Modern glassmorphic dialog for scanning equations via Camera, Screen Snip,
 * Image File Upload, and Clipboard Paste using Google Gemini Vision AI.
 */
public class CameraScannerDialog {

    @FunctionalInterface
    public interface EquationScanHandler {
        void onEquationAccepted(String latex, boolean solveImmediately);
    }

    private final Stage stage;
    private final Stage owner;
    private final EquationScanHandler onEquationAccepted;
    private final CameraMathService cameraMathService = new CameraMathService();

    private ImageView previewImageView;
    private StackPane dropZone;
    private VBox placeholderBox;
    private HBox statusBox;
    private Label statusLabel;
    private ProgressIndicator progressIndicator;
    private VBox resultBox;
    private TextField mathResultField;
    private WebView katexPreviewWebView;
    private Button solveButton;
    private Button insertButton;

    private byte[] currentImageBytes;
    private String currentMimeType = "image/png";
    private final AtomicBoolean isWatchingCamera = new AtomicBoolean(false);

    // SVG icon paths
    private static final String SVG_CAMERA = "M9.4 4l-.6 1H5C3.9 5 3 5.9 3 7v10c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2h-3.8l-.6-1h-5.2zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
    private static final String SVG_SCISSORS = "M9.64 7.64c.23-.5.36-1.05.36-1.64 0-2.21-1.79-4-4-4S2 3.79 2 6s1.79 4 4 4c.59 0 1.14-.13 1.64-.36L10 12l-2.36 2.36C7.14 14.13 6.59 14 6 14c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4c0-.59-.13-1.14-.36-1.64L12 14l7 7h3v-1L9.64 7.64zM6 8c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm0 12c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm6-7.5c-.28 0-.5-.22-.5-.5s.22-.5.5-.5.5.22.5.5-.22.5-.5.5zM19 3l-6 6 2 2 7-7V3h-3z";
    private static final String SVG_UPLOAD = "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z";
    private static final String SVG_PASTE = "M19 2h-4.18C14.4 0.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z";
    private static final String SVG_SPARKLE = "M12 2L9.5 8.5L3 11l6.5 2.5L12 20l2.5-6.5L21 11l-6.5-2.5L12 2zm7 13l-1.25 3.25L14.5 19.5l3.25 1.25L19 24l1.25-3.25L23.5 19.5l-3.25-1.25L19 15z";

    public static void show(Stage owner, Consumer<String> onEquationAccepted) {
        show(owner, (latex, solveImmediately) -> {
            if (onEquationAccepted != null) onEquationAccepted.accept(latex);
        });
    }

    public static void showWithImage(Stage owner, BufferedImage image, Consumer<String> onEquationAccepted) {
        showWithImage(owner, image, (latex, solveImmediately) -> {
            if (onEquationAccepted != null) onEquationAccepted.accept(latex);
        });
    }

    public static void showWithFile(Stage owner, File file, Consumer<String> onEquationAccepted) {
        showWithFile(owner, file, (latex, solveImmediately) -> {
            if (onEquationAccepted != null) onEquationAccepted.accept(latex);
        });
    }

    public static void show(Stage owner, EquationScanHandler onEquationAccepted) {
        CameraScannerDialog dialog = new CameraScannerDialog(owner, onEquationAccepted);
        dialog.stage.show();
    }

    public static void showWithImage(Stage owner, BufferedImage image, EquationScanHandler onEquationAccepted) {
        CameraScannerDialog dialog = new CameraScannerDialog(owner, onEquationAccepted);
        dialog.stage.show();
        dialog.loadBufferedImage(image);
    }

    public static void showWithFile(Stage owner, File file, EquationScanHandler onEquationAccepted) {
        CameraScannerDialog dialog = new CameraScannerDialog(owner, onEquationAccepted);
        dialog.stage.show();
        dialog.loadImageFile(file);
    }

    private CameraScannerDialog(Stage owner, EquationScanHandler onEquationAccepted) {
        this.owner = owner;
        this.onEquationAccepted = onEquationAccepted;
        this.stage = new Stage();
        this.stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            this.stage.initOwner(owner);
        }
        this.stage.setTitle("Math Equation Scanner (Camera & AI Vision)");

        VBox root = buildUI();
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 640, 680);
        scene.setFill(Color.web("#14121E"));
        this.stage.setScene(scene);
        this.stage.setResizable(false);

        this.stage.setOnCloseRequest(ev -> isWatchingCamera.set(false));
    }

    private VBox buildUI() {
        VBox root = new VBox(14);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setStyle("-fx-background-color: #161424; -fx-border-color: #2D2940; -fx-border-width: 1;");

        // 1. Header with Camera Icon
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath camIcon = new SVGPath();
        camIcon.setContent(SVG_CAMERA);
        camIcon.setFill(Color.web("#00D2FF"));
        camIcon.setScaleX(1.3);
        camIcon.setScaleY(1.3);

        VBox titleBox = new VBox(2);
        Label title = new Label("Math Equation Scanner");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 18px; -fx-font-weight: bold; -fx-font-family: -apple-system, 'Segoe UI', Roboto, sans-serif;");

        Label subtitle = new Label("Extract and solve equations from webcam photos, screen snips, or images with AI.");
        subtitle.setStyle("-fx-text-fill: #A09DB0; -fx-font-size: 12px;");

        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(camIcon, titleBox);

        // 2. Toolbar with 4 clean SVG icon action buttons
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnCamera = createToolbarButton("Take Photo", SVG_CAMERA, "#00D2FF", ev -> handleTakePhoto());
        Button btnSnip = createToolbarButton("Snip Screen", SVG_SCISSORS, "#38EF7D", ev -> handleSnipScreen());
        Button btnFile = createToolbarButton("Upload Image", SVG_UPLOAD, "#C084FC", ev -> handleChooseFile());
        Button btnPaste = createToolbarButton("Paste Image", SVG_PASTE, "#FF8E53", ev -> handlePasteClipboard());

        toolbar.getChildren().addAll(btnCamera, btnSnip, btnFile, btnPaste);

        // 3. Drop Zone & Image Preview Area
        dropZone = new StackPane();
        dropZone.setPrefHeight(210);
        dropZone.setMinHeight(210);
        dropZone.setMaxHeight(210);
        dropZone.setCursor(Cursor.HAND);
        dropZone.setStyle("-fx-background-color: #1C192E; -fx-border-color: #383452; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-border-style: dashed; -fx-border-width: 1.5;");

        dropZone.setOnMouseClicked(ev -> {
            if (previewImageView == null || !previewImageView.isVisible()) {
                handleChooseFile();
            }
        });

        placeholderBox = new VBox(10);
        placeholderBox.setAlignment(Pos.CENTER);

        SVGPath uploadSvg = new SVGPath();
        uploadSvg.setContent(SVG_UPLOAD);
        uploadSvg.setFill(Color.web("#5C577A"));
        uploadSvg.setScaleX(1.4);
        uploadSvg.setScaleY(1.4);

        Label dropLabel = new Label("Drag & drop equation image here, or choose an option above");
        dropLabel.setStyle("-fx-text-fill: #8E8A9F; -fx-font-size: 13px; -fx-font-weight: 500;");

        Button btnBrowseMain = new Button("Choose Image to Upload");
        SVGPath uploadBtnIcon = new SVGPath();
        uploadBtnIcon.setContent(SVG_UPLOAD);
        uploadBtnIcon.setFill(Color.web("#0B0E14"));
        uploadBtnIcon.setScaleX(0.85);
        uploadBtnIcon.setScaleY(0.85);
        btnBrowseMain.setGraphic(uploadBtnIcon);
        btnBrowseMain.setGraphicTextGap(8);
        btnBrowseMain.setStyle("-fx-background-color: #00D2FF; -fx-text-fill: #0B0E14; -fx-padding: 8 22; "
                + "-fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,210,255,0.4), 8, 0, 0, 0);");
        btnBrowseMain.setOnAction(e -> handleChooseFile());

        Label dropSublabel = new Label("Supports handwritten homework, printed textbooks, screenshots (PNG, JPG, WEBP)");
        dropSublabel.setStyle("-fx-text-fill: #615C77; -fx-font-size: 11px;");

        placeholderBox.getChildren().addAll(uploadSvg, dropLabel, btnBrowseMain, dropSublabel);

        previewImageView = new ImageView();
        previewImageView.setFitWidth(580);
        previewImageView.setFitHeight(195);
        previewImageView.setPreserveRatio(true);
        previewImageView.setVisible(false);

        dropZone.getChildren().addAll(placeholderBox, previewImageView);

        // Drag and drop event handlers
        setupDragAndDrop();

        // 4. Status indicator
        statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setStyle("-fx-background-color: #211E33; -fx-padding: 8 12; -fx-background-radius: 6;");

        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(18, 18);
        progressIndicator.setMaxSize(18, 18);
        progressIndicator.setVisible(false);

        statusLabel = new Label("Ready to scan math image. Select an option above.");
        statusLabel.setStyle("-fx-text-fill: #B3AFC6; -fx-font-size: 12px;");

        statusBox.getChildren().addAll(progressIndicator, statusLabel);

        // 5. Result Card & KaTeX preview
        resultBox = new VBox(8);
        resultBox.setStyle("-fx-background-color: #1A172B; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: #35304F; -fx-border-radius: 8; -fx-border-width: 1;");
        resultBox.setVisible(false);
        resultBox.setManaged(false);

        Label resultTitle = new Label("Recognized Math Expression (Editable):");
        resultTitle.setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 12px; -fx-font-weight: bold;");

        mathResultField = new TextField();
        mathResultField.setStyle("-fx-background-color: #12101E; -fx-text-fill: #FFFFFF; -fx-border-color: #433D63; "
                + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-font-size: 15px; -fx-font-family: 'Segoe UI', 'Consolas', monospace; -fx-padding: 8 10;");
        mathResultField.textProperty().addListener((obs, old, text) -> updateKaTeXPreview(text));

        Label previewTitle = new Label("Rendered KaTeX Preview:");
        previewTitle.setStyle("-fx-text-fill: #B3AFC6; -fx-font-size: 11px;");

        katexPreviewWebView = new WebView();
        katexPreviewWebView.setPrefHeight(68);
        katexPreviewWebView.setMaxHeight(68);
        katexPreviewWebView.setStyle("-fx-page-fill: transparent;");

        resultBox.getChildren().addAll(resultTitle, mathResultField, previewTitle, katexPreviewWebView);

        // 6. Action Buttons Footer
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button btnSettings = new Button("AI Settings");
        SVGPath sparkIcon = new SVGPath();
        sparkIcon.setContent(SVG_SPARKLE);
        sparkIcon.setFill(Color.web("#A855F7"));
        sparkIcon.setScaleX(0.85);
        sparkIcon.setScaleY(0.85);
        btnSettings.setGraphic(sparkIcon);
        btnSettings.setGraphicTextGap(6);
        btnSettings.setStyle("-fx-background-color: transparent; -fx-text-fill: #A855F7; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: 500;");
        btnSettings.setOnAction(ev -> AISettingsDialog.show(stage));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #26233B; -fx-text-fill: #E0DEEC; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(ev -> {
            isWatchingCamera.set(false);
            stage.close();
        });

        insertButton = new Button("Insert into Input");
        insertButton.setDisable(true);
        insertButton.setStyle("-fx-background-color: #2E2A4A; -fx-text-fill: #00D2FF; -fx-padding: 8 16; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        insertButton.setOnAction(ev -> acceptAndClose(false));

        solveButton = new Button("Solve Step-by-Step");
        SVGPath solveSpark = new SVGPath();
        solveSpark.setContent(SVG_SPARKLE);
        solveSpark.setFill(Color.web("#0B0E14"));
        solveSpark.setScaleX(0.85);
        solveSpark.setScaleY(0.85);
        solveButton.setGraphic(solveSpark);
        solveButton.setGraphicTextGap(7);
        solveButton.setDisable(true);
        solveButton.setStyle("-fx-background-color: #00D2FF; -fx-text-fill: #0B0E14; -fx-padding: 8 18; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,210,255,0.4), 8, 0, 0, 0);");
        solveButton.setOnAction(ev -> acceptAndClose(true));

        footer.getChildren().addAll(btnSettings, spacer, btnCancel, insertButton, solveButton);

        root.getChildren().addAll(header, toolbar, dropZone, statusBox, resultBox, footer);
        return root;
    }

    private Button createToolbarButton(String text, String svgPathContent, String accentColor, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPathContent);
        icon.setFill(Color.web(accentColor));
        icon.setScaleX(0.9);
        icon.setScaleY(0.9);

        btn.setGraphic(icon);
        btn.setGraphicTextGap(8);
        btn.setStyle(
                "-fx-background-color: #211E36; -fx-text-fill: #EAE8F2; -fx-border-color: #383454; "
                        + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 7 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> {
            btn.setStyle(String.format(
                    "-fx-background-color: rgba(33, 30, 54, 0.9); -fx-text-fill: %s; -fx-border-color: %s; "
                            + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 7 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;",
                    accentColor, accentColor
            ));
            icon.setFill(Color.WHITE);
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle(
                    "-fx-background-color: #211E36; -fx-text-fill: #EAE8F2; -fx-border-color: #383454; "
                            + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 7 14; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;"
            );
            icon.setFill(Color.web(accentColor));
        });
        btn.setOnAction(handler);
        return btn;
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            dropZone.setStyle("-fx-background-color: #25203D; -fx-border-color: #00D2FF; -fx-border-radius: 10; "
                    + "-fx-background-radius: 10; -fx-border-style: solid; -fx-border-width: 1.5;");
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropZone.setStyle("-fx-background-color: #1C192E; -fx-border-color: #383452; -fx-border-radius: 10; "
                    + "-fx-background-radius: 10; -fx-border-style: dashed; -fx-border-width: 1.5;");
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                            || name.endsWith(".bmp") || name.endsWith(".webp") || name.endsWith(".gif")) {
                        loadImageFile(file);
                        success = true;
                        break;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleTakePhoto() {
        isWatchingCamera.set(true);
        setStatus(true, "Opening Windows Camera... Take a photo of your equation.");

        try {
            // Launch Windows Camera app
            new ProcessBuilder("cmd.exe", "/c", "start", "microsoft.windows.camera:").start();
        } catch (Exception e) {
            System.err.println("Could not launch Windows Camera: " + e.getMessage());
        }

        // Watch Camera Roll directory for new photos taken
        String picturesDir = System.getProperty("user.home") + File.separator + "Pictures";
        File cameraRollDir = new File(picturesDir, "Camera Roll");
        if (!cameraRollDir.exists()) {
            cameraRollDir = new File(picturesDir);
        }

        final File watchDir = cameraRollDir;
        final long startTime = System.currentTimeMillis() - 3000;

        Thread watcher = new Thread(() -> {
            int checks = 0;
            while (isWatchingCamera.get() && checks < 60) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    break;
                }
                checks++;

                File[] files = watchDir.listFiles((dir, name) -> {
                    String lower = name.toLowerCase();
                    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
                });

                if (files != null && files.length > 0) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
                    File newest = files[0];
                    if (newest.lastModified() > startTime) {
                        isWatchingCamera.set(false);
                        Platform.runLater(() -> {
                            stage.toFront();
                            loadImageFile(newest);
                        });
                        return;
                    }
                }
            }
            if (isWatchingCamera.get()) {
                Platform.runLater(() -> setStatus(false, "Camera wait timed out. Click 'Upload Image' to select your photo manually."));
            }
        }, "CameraRoll-Watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private void handleSnipScreen() {
        isWatchingCamera.set(false);
        stage.hide();
        ScreenSnipper.startSnip(owner, (BufferedImage snipped) -> {
            Platform.runLater(() -> {
                stage.show();
                stage.toFront();
                if (snipped != null) {
                    loadBufferedImage(snipped);
                }
            });
        });
    }

    private void handleChooseFile() {
        isWatchingCamera.set(false);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Math Equation Image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.webp, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        String picturesDir = System.getProperty("user.home") + File.separator + "Pictures";
        File defaultDir = new File(picturesDir);
        if (defaultDir.exists()) {
            chooser.setInitialDirectory(defaultDir);
        }

        File selected = chooser.showOpenDialog(stage);
        if (selected != null) {
            loadImageFile(selected);
        }
    }

    private void handlePasteClipboard() {
        isWatchingCamera.set(false);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasImage()) {
            Image img = clipboard.getImage();
            loadFXImage(img);
        } else if (clipboard.hasFiles()) {
            List<File> files = clipboard.getFiles();
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".bmp") || name.endsWith(".webp")) {
                    loadImageFile(file);
                    return;
                }
            }
            setStatus(false, "Clipboard files do not contain supported image files.");
        } else {
            setStatus(false, "No image found on clipboard. Press 'Win + Shift + S' to copy an equation, then click Paste.");
        }
    }

    public void loadImageFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int read = fis.read(bytes);
                if (read <= 0) return;
            }
            String name = file.getName().toLowerCase();
            String mime = "image/png";
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                mime = "image/jpeg";
            } else if (name.endsWith(".webp")) {
                mime = "image/webp";
            } else if (name.endsWith(".bmp")) {
                mime = "image/bmp";
            }
            currentImageBytes = bytes;
            currentMimeType = mime;

            Image fxImg = new Image(new ByteArrayInputStream(bytes));
            displayImagePreview(fxImg);
            triggerAiExtraction();
        } catch (Exception e) {
            setStatus(false, "Could not load image: " + e.getMessage());
        }
    }

    public void loadBufferedImage(BufferedImage bimg) {
        if (bimg == null) return;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            currentImageBytes = baos.toByteArray();
            currentMimeType = "image/png";

            Image fxImg = new Image(new ByteArrayInputStream(currentImageBytes));
            displayImagePreview(fxImg);
            triggerAiExtraction();
        } catch (Exception e) {
            setStatus(false, "Could not process image: " + e.getMessage());
        }
    }

    public void loadFXImage(Image fxImage) {
        if (fxImage == null) return;
        try {
            int w = (int) fxImage.getWidth();
            int h = (int) fxImage.getHeight();
            BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            javafx.scene.image.PixelReader pr = fxImage.getPixelReader();
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    bimg.setRGB(x, y, pr.getArgb(x, y));
                }
            }
            loadBufferedImage(bimg);
        } catch (Exception e) {
            setStatus(false, "Could not load clipboard image: " + e.getMessage());
        }
    }

    private void displayImagePreview(Image fxImage) {
        placeholderBox.setVisible(false);
        previewImageView.setImage(fxImage);
        previewImageView.setVisible(true);
        dropZone.setStyle("-fx-background-color: #1A1728; -fx-border-color: #00D2FF; -fx-border-radius: 10; "
                + "-fx-background-radius: 10; -fx-border-style: solid; -fx-border-width: 1.5;");
    }

    private void triggerAiExtraction() {
        if (currentImageBytes == null || currentImageBytes.length == 0) return;

        setStatus(true, "AI Vision is analyzing and extracting mathematical equation...");
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        solveButton.setDisable(true);
        insertButton.setDisable(true);

        cameraMathService.extractMath(currentImageBytes, currentMimeType)
                .thenAcceptAsync(extractedLatex -> {
                    Platform.runLater(() -> {
                        if (extractedLatex != null && !extractedLatex.trim().isEmpty()) {
                            setStatus(false, "Equation extracted successfully!");
                            showResult(extractedLatex.trim());
                        } else {
                            setStatus(false, "No clear math equation detected in the image. Please try a clearer crop or image.");
                        }
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        String msg = ex.getMessage();
                        if (msg != null && msg.contains("API key not configured")) {
                            setStatus(false, "Gemini API key required. Click 'AI Settings' below to configure.");
                        } else {
                            setStatus(false, "OCR Error: " + (msg != null ? msg : "Failed to extract math"));
                        }
                    });
                    return null;
                });
    }

    private void showResult(String latex) {
        mathResultField.setText(latex);
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        solveButton.setDisable(false);
        insertButton.setDisable(false);
        updateKaTeXPreview(latex);
    }

    private void updateKaTeXPreview(String latex) {
        if (katexPreviewWebView == null) return;
        String cleanMath = latex != null ? latex.trim() : "";
        // Convert ÷ to \div and × to \times for KaTeX rendering
        String renderLatex = cleanMath
                .replace("÷", "\\div ")
                .replace("×", "\\times ");
        String jsSafeLatex = renderLatex.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        String html = "<!DOCTYPE html><html><head>"
                + "<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css'>"
                + "<script src='https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js'></script>"
                + "<style>"
                + "html, body { margin: 0; padding: 0; background: transparent; overflow: hidden; display: flex; align-items: center; justify-content: center; height: 100%; width: 100%; }"
                + "#math { color: #00D2FF; font-size: 20px; text-align: center; font-weight: 500; }"
                + ".katex-display { margin: 0 !important; }"
                + "</style>"
                + "</head><body><div id='math'></div>"
                + "<script>try{katex.render('" + jsSafeLatex + "', document.getElementById('math'), {throwOnError:false, displayMode:true});}catch(e){document.getElementById('math').innerText='" + jsSafeLatex + "';}</script>"
                + "</body></html>";
        katexPreviewWebView.getEngine().loadContent(html);
    }

    private void setStatus(boolean inProgress, String text) {
        progressIndicator.setVisible(inProgress);
        statusLabel.setText(text);
        if (inProgress) {
            statusLabel.setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #B3AFC6; -fx-font-size: 12px;");
        }
    }

    private void acceptAndClose(boolean solveImmediately) {
        String math = mathResultField.getText();
        if (math != null && !math.trim().isEmpty()) {
            if (onEquationAccepted != null) {
                onEquationAccepted.onEquationAccepted(math.trim(), solveImmediately);
            }
        }
        stage.close();
    }
}
