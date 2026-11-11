package com.banukasineth.advancedcalculator.app.controller;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyValue;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UnitConverterController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox sidebar;
    @FXML
    private Button sidebarToggleBtn;
    @FXML
    private ComboBox<String> unitModeComboBox;
    @FXML
    private VBox measureCategoryBox;
    @FXML
    private VBox calculateCategoryBox;
    @FXML
    private ScrollPane mainScrollPane;

    @FXML
    private Label breadcrumbLabel;
    @FXML
    private Label categoryIconLabel;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;

    @FXML
    private TextField inputTextField;
    @FXML
    private TextField outputTextField;
    @FXML
    private ComboBox<String> fromUnitBox;
    @FXML
    private ComboBox<String> toUnitBox;
    @FXML
    private Button swapBtn;

    // Currency Country Flag Showcase Badges
    @FXML
    private HBox fromFlagBadge;
    @FXML
    private ImageView fromFlagView;
    @FXML
    private Label fromFlagCodeLabel;

    @FXML
    private HBox toFlagBadge;
    @FXML
    private ImageView toFlagView;
    @FXML
    private Label toFlagCodeLabel;

    @FXML
    private Label visualizerUnitPairLabel;
    @FXML
    private StackPane visualizerPane;
    @FXML
    private Label formulaLabel;
    @FXML
    private Button hubModeBtn;
    @FXML
    private Button rulerModeBtn;
    @FXML
    private Button historyBtn;

    // High-Resolution Vector SVG Paths for Categories
    private static final String SVG_LENGTH = "M21.71 6.29l-4-4a1 1 0 0 0-1.42 0l-14 14a1 1 0 0 0 0 1.42l4 4a1 1 0 0 0 1.42 0l14-14a1 1 0 0 0 0-1.42zM7 19.59l-2.59-2.59L16 5.41 18.59 8 7 19.59zM10 13l2-2 1.41 1.41-2 2L10 13zm3-3l2-2 1.41 1.41-2 2L13 10zm3-3l2-2 1.41 1.41-2 2L16 7z";
    private static final String SVG_AREA = "M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8zm2 2v4h4v-4h-4zm-10 0v4h4v-4H5zm0-10v4h4V5H5zm10 0v4h4V5h-4z";
    private static final String SVG_VOLUME = "M12 2L3 7v10l9 5 9-5V7l-9-5zm0 2.2l6.5 3.6L12 11.4 5.5 7.8 12 4.2zM4.5 9.1l7 3.9v7.7l-7-3.9V9.1zm8.5 11.6v-7.7l7-3.9v7.7l-7 3.9z";
    private static final String SVG_MASS = "M13 2h-2v3H3v2l3.5 7A2.5 2.5 0 0 0 9 15.5a2.5 2.5 0 0 0 2.5-1.5L11 9.5V19H7v2h10v-2h-4V9.5l-.5 4.5A2.5 2.5 0 0 0 15 15.5a2.5 2.5 0 0 0 2.5-1.5L21 7V5h-8V2zm-5 11.5L5.5 7h5L8 13.5zm8 0L13.5 7h5L16 13.5z";
    private static final String SVG_TEMP = "M15 13.5V5a3 3 0 0 0-6 0v8.5a5 5 0 1 0 6 0zM12 4a1 1 0 0 1 1 1v6h-2V5a1 1 0 0 1 1-1zm0 17a3 3 0 0 1-1.5-5.6V13h3v2.4A3 3 0 0 1 12 21z";
    private static final String SVG_SPEED = "M12 4a9 9 0 0 0-9 9 8.9 8.9 0 0 0 2.7 6.4l1.4-1.4A7 7 0 1 1 18.9 18l1.4 1.4A9 9 0 0 0 21 13a9 9 0 0 0-9-9zm-1 4v5.2l3.2 3.2 1.4-1.4-2.6-2.6V8h-2z";
    private static final String SVG_TIME = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8zm.5-13H11v6l5.2 3.1.8-1.3-4.5-2.7V7z";
    private static final String SVG_PRESSURE = "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8zm-1-14h2v3h-2V6zm-4.2 1.8l1.4-1.4 2.1 2.1-1.4 1.4-2.1-2.1zm8.4 0l2.1 2.1-1.4 1.4-2.1-2.1 1.4-1.4zM12 10a2 2 0 0 0-2 2c0 .7.4 1.4 1 1.7V17h2v-3.3c.6-.3 1-1 1-1.7a2 2 0 0 0-2-2z";
    private static final String SVG_ENERGY = "M7 2v11h3v9l7-12h-4l4-8H7z";
    private static final String SVG_POWER = "M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42A7.92 7.92 0 0 1 20 12a8 8 0 1 1-13.41-5.41L5.17 5.17A9.97 9.97 0 0 0 2 12a10 10 0 1 0 20 0c0-2.76-1.12-5.26-2.93-7.07l-.24.24z";
    private static final String SVG_DATA = "M12 2C7.58 2 4 3.79 4 6v12c0 2.21 3.58 4 8 4s8-1.79 8-4V6c0-2.21-3.58-4-8-4zm0 2c3.87 0 6 1.46 6 2s-2.13 2-6 2-6-1.46-6-2 2.13-2 6-2zm6 14c0 .54-2.13 2-6 2s-6-1.46-6-2v-2.22c1.61.78 3.72 1.22 6 1.22s4.39-.44 6-1.22V18zm0-4c0 .54-2.13 2-6 2s-6-1.46-6-2v-2.22c1.61.78 3.72 1.22 6 1.22s4.39-.44 6-1.22V14zm0-4c0 .54-2.13 2-6 2s-6-1.46-6-2V7.78c1.61.78 3.72 1.22 6 1.22s4.39-.44 6-1.22V10z";
    private static final String SVG_CURRENCY = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 16h-2v-1.12c-1.7-.29-3-1.63-3-3.38h2c0 1.1.9 2 2 2s2-.9 2-2-.9-2-2-2c-2.21 0-4-1.79-4-4 0-1.75 1.3-3.09 3-3.38V5h2v1.12c1.7.29 3 1.63 3 3.38h-2c0-1.1-.9-2-2-2s-2 .9-2 2 .9 2 2 2c2.21 0 4 1.79 4 4 0 1.75-1.3 3.09-3 3.38V18z";

    private boolean isDrawingVisualizer = false;
    private double lastDrawnWidth = 0.0;
    private PauseTransition resizeDebounce;

    public enum VisualizerMode {
        CALCULATION_HUB,
        DUAL_RULER
    }

    private VisualizerMode visualizerMode = VisualizerMode.CALCULATION_HUB;

    public static class BenchmarkItem {
        public final String icon;
        public final String multiplier;
        public final String title;
        public final String reference;

        public BenchmarkItem(String icon, String multiplier, String title, String reference) {
            this.icon = icon;
            this.multiplier = multiplier;
            this.title = title;
            this.reference = reference;
        }
    }

    @FXML
    private Button refreshRatesBtn;
    @FXML
    private Button copyResultBtn;
    @FXML
    private VBox equivalentsContainer;

    private boolean sidebarVisible = true;
    private Consumer<String> modeChangeListener;

    public static class UnitItem {
        public final String name;
        public final String symbol;
        public final double factor; // factor to multiply to get base unit (for linear units)

        public UnitItem(String name, String symbol, double factor) {
            this.name = name;
            this.symbol = symbol;
            this.factor = factor;
        }

        public String getDisplay() {
            return name + " (" + symbol + ")";
        }
    }

    public static class CategoryData {
        public final String name;
        public final String icon;
        public final String svgPath;
        public final String section; // "MEASURE" or "CALCULATE"
        public final String subtitle;
        public final List<UnitItem> units;
        public final String defaultFrom;
        public final String defaultTo;

        public CategoryData(String name, String icon, String svgPath, String section, String subtitle,
                List<UnitItem> units, String defaultFrom, String defaultTo) {
            this.name = name;
            this.icon = icon;
            this.svgPath = svgPath;
            this.section = section;
            this.subtitle = subtitle;
            this.units = units;
            this.defaultFrom = defaultFrom;
            this.defaultTo = defaultTo;
        }
    }

    private final List<CategoryData> allCategories = new ArrayList<>();
    private final Map<String, Button> categoryButtons = new HashMap<>();
    private CategoryData currentCategory;
    private final List<String> conversionHistory = new ArrayList<>();
    private final Map<String, Image> flagCache = new HashMap<>();
    private boolean isUpdatingUnits = false;

    // Real-Time Currency Exchange Rates Cache (Base: USD)
    private final Map<String, Double> liveRates = new ConcurrentHashMap<>();
    private String liveRatesTimestamp = null;
    private boolean isFetchingRates = false;

    public void setOnModeChange(Consumer<String> listener) {
        this.modeChangeListener = listener;
    }

    public void setMode(String mode) {
        if (unitModeComboBox != null && mode != null) {
            unitModeComboBox.setValue(mode);
        }
    }

    @FXML
    public void initialize() {
        initDefaultRates();
        initCategories();

        if (unitModeComboBox != null) {
            unitModeComboBox.getItems().addAll("Basic", "Advanced", "Scientific", "Programmer", "Graphing",
                    "Unit Converter");
            unitModeComboBox.setValue("Unit Converter");
            unitModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals("Unit Converter") && modeChangeListener != null) {
                    modeChangeListener.accept(newVal);
                }
            });
        }

        buildSidebar();

        if (fromUnitBox != null) {
            setupComboBoxCellFactory(fromUnitBox);
        }
        if (toUnitBox != null) {
            setupComboBoxCellFactory(toUnitBox);
        }

        if (inputTextField != null) {
            inputTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingUnits) {
                    convertUnits();
                }
            });
        }

        if (fromUnitBox != null) {
            fromUnitBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingUnits) {
                    updateCardFlags();
                    convertUnits();
                }
            });
        }

        if (toUnitBox != null) {
            toUnitBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!isUpdatingUnits) {
                    updateCardFlags();
                    convertUnits();
                }
            });
        }

        if (visualizerPane != null) {
            visualizerPane.widthProperty().addListener((obs, oldVal, newVal) -> {
                double nw = newVal.doubleValue();
                if (nw > 100 && Math.abs(nw - lastDrawnWidth) > 8.0 && !isDrawingVisualizer) {
                    if (resizeDebounce == null) {
                        resizeDebounce = new PauseTransition(Duration.millis(100));
                        resizeDebounce.setOnFinished(e -> drawVisualizer());
                    }
                    resizeDebounce.playFromStart();
                }
            });
        }

        // Setup vector SVG icons on switcher and history buttons
        if (historyBtn != null) {
            SVGPath hSvg = new SVGPath();
            hSvg.setContent(SVG_TIME);
            hSvg.setFill(Color.web("#B8B5C8"));
            hSvg.setScaleX(0.7);
            hSvg.setScaleY(0.7);
            StackPane hBox = new StackPane(hSvg);
            hBox.setPrefSize(16, 16);
            hBox.setMinSize(16, 16);
            hBox.setMaxSize(16, 16);
            hBox.setAlignment(Pos.CENTER);
            historyBtn.setGraphic(hBox);
            historyBtn.setGraphicTextGap(7);
        }

        if (hubModeBtn != null) {
            SVGPath hubSvg = new SVGPath();
            hubSvg.setContent("M19 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2zm-8 4h2v2h-2V7zm-4 0h2v2H7V7zm0 4h2v2H7v-2zm4 0h2v2h-2v-2zm-4 4h2v2H7v-2zm4 0h2v2h-2v-2zm6 2h-2v-6h2v6zm0-8h-2V7h2v2z");
            hubSvg.setFill(Color.web("#00D2FF"));
            hubSvg.setScaleX(0.7);
            hubSvg.setScaleY(0.7);
            StackPane hubBox = new StackPane(hubSvg);
            hubBox.setPrefSize(16, 16);
            hubBox.setMinSize(16, 16);
            hubBox.setMaxSize(16, 16);
            hubBox.setAlignment(Pos.CENTER);
            hubModeBtn.setGraphic(hubBox);
            hubModeBtn.setGraphicTextGap(6);
        }

        if (rulerModeBtn != null) {
            SVGPath rSvg = new SVGPath();
            rSvg.setContent(SVG_LENGTH);
            rSvg.setFill(Color.web("#9E9AA8"));
            rSvg.setScaleX(0.65);
            rSvg.setScaleY(0.65);
            StackPane rBox = new StackPane(rSvg);
            rBox.setPrefSize(16, 16);
            rBox.setMinSize(16, 16);
            rBox.setMaxSize(16, 16);
            rBox.setAlignment(Pos.CENTER);
            rulerModeBtn.setGraphic(rBox);
            rulerModeBtn.setGraphicTextGap(6);
        }

        // Fetch live market rates asynchronously on startup
        fetchLiveExchangeRatesAsync(false);

        // Select Length by default
        selectCategory(allCategories.get(0));
    }

    private void initDefaultRates() {
        liveRates.put("USD", 1.0);
        liveRates.put("EUR", 0.896);
        liveRates.put("GBP", 0.747);
        liveRates.put("JPY", 157.53);
        liveRates.put("CAD", 1.365);
        liveRates.put("AUD", 1.528);
        liveRates.put("CHF", 0.884);
        liveRates.put("CNY", 7.15);
        liveRates.put("INR", 84.15);
        liveRates.put("LKR", 332.39);
        liveRates.put("SGD", 1.32);
        liveRates.put("NZD", 1.67);
        liveRates.put("AED", 3.6725);
        liveRates.put("SAR", 3.75);
        liveRates.put("KRW", 1380.0);
        liveRates.put("BRL", 5.75);
        liveRates.put("ZAR", 18.2);
        liveRates.put("RUB", 97.0);
        liveRates.put("TRY", 34.5);
        liveRates.put("SEK", 10.7);
        liveRates.put("NOK", 11.0);
        liveRates.put("MXN", 20.1);
    }

    public void fetchLiveExchangeRatesAsync(boolean userInitiated) {
        if (isFetchingRates)
            return;
        isFetchingRates = true;

        if (userInitiated && refreshRatesBtn != null) {
            refreshRatesBtn.setText("🔄 Fetching...");
        }

        CompletableFuture.runAsync(() -> {
            boolean success = false;
            String[] apis = {
                    "https://open.er-api.com/v6/latest/USD",
                    "https://api.exchangerate-api.com/v4/latest/USD"
            };

            for (String api : apis) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) URI.create(api).toURL().openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);
                    if (conn.getResponseCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null)
                            sb.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(sb.toString());
                        JSONObject ratesObj = json.optJSONObject("rates");
                        if (ratesObj != null) {
                            for (String key : ratesObj.keySet()) {
                                double val = ratesObj.optDouble(key, -1);
                                if (val > 0) {
                                    liveRates.put(key, val);
                                }
                            }
                            success = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                    // Try next fallback endpoint
                }
            }

            final boolean finalSuccess = success;
            final String updateTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

            Platform.runLater(() -> {
                isFetchingRates = false;
                if (finalSuccess) {
                    liveRatesTimestamp = updateTime;
                    if (currentCategory != null && "Currency".equalsIgnoreCase(currentCategory.name)) {
                        if (subtitleLabel != null) {
                            subtitleLabel.setText("Global exchange rates  •  🟢 Live updated at " + updateTime);
                        }
                        convertUnits();
                    }
                    if (refreshRatesBtn != null) {
                        refreshRatesBtn.setText("✓ Rates Live!");
                        PauseTransition pt = new PauseTransition(Duration.millis(2000));
                        pt.setOnFinished(e -> refreshRatesBtn.setText("🔄 Live Rates"));
                        pt.play();
                    }
                } else {
                    if (refreshRatesBtn != null) {
                        refreshRatesBtn.setText("⚪ Offline Rates");
                        PauseTransition pt = new PauseTransition(Duration.millis(2000));
                        pt.setOnFinished(e -> refreshRatesBtn.setText("🔄 Live Rates"));
                        pt.play();
                    }
                }
            });
        });
    }

    @FXML
    public void refreshExchangeRates() {
        fetchLiveExchangeRatesAsync(true);
    }

    private Image getFlagImage(String symbol) {
        if (symbol == null)
            return null;
        String code = symbol.toLowerCase().trim();
        if (flagCache.containsKey(code)) {
            return flagCache.get(code);
        }
        try {
            InputStream stream = getClass().getResourceAsStream("/images/flags/" + code + ".png");
            if (stream != null) {
                Image img = new Image(stream);
                flagCache.put(code, img);
                return img;
            }
        } catch (Exception e) {
            // Ignore if flag not found
        }
        return null;
    }

    private void setupComboBoxCellFactory(ComboBox<String> comboBox) {
        comboBox.setCellFactory(lv -> new ListCell<String>() {
            private final ImageView flagImg = new ImageView();
            private final Label textLabel = new Label();
            private final HBox hbox = new HBox(8, flagImg, textLabel);
            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                flagImg.setFitWidth(24);
                flagImg.setFitHeight(16);
                Rectangle clip = new Rectangle(24, 16);
                clip.setArcWidth(4);
                clip.setArcHeight(4);
                flagImg.setClip(clip);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    textLabel.setText(item);
                    textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                    if (currentCategory != null && "Currency".equalsIgnoreCase(currentCategory.name)) {
                        UnitItem u = findUnitByDisplay(currentCategory, item);
                        if (u != null) {
                            Image flag = getFlagImage(u.symbol);
                            if (flag != null) {
                                flagImg.setImage(flag);
                                flagImg.setVisible(true);
                                flagImg.setManaged(true);
                            } else {
                                flagImg.setVisible(false);
                                flagImg.setManaged(false);
                            }
                        }
                    } else {
                        flagImg.setVisible(false);
                        flagImg.setManaged(false);
                    }
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });

        comboBox.setButtonCell(new ListCell<String>() {
            private final ImageView flagImg = new ImageView();
            private final Label textLabel = new Label();
            private final HBox hbox = new HBox(8, flagImg, textLabel);
            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                flagImg.setFitWidth(24);
                flagImg.setFitHeight(16);
                Rectangle clip = new Rectangle(24, 16);
                clip.setArcWidth(4);
                clip.setArcHeight(4);
                flagImg.setClip(clip);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    textLabel.setText(item);
                    textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 500;");
                    if (currentCategory != null && "Currency".equalsIgnoreCase(currentCategory.name)) {
                        UnitItem u = findUnitByDisplay(currentCategory, item);
                        if (u != null) {
                            Image flag = getFlagImage(u.symbol);
                            if (flag != null) {
                                flagImg.setImage(flag);
                                flagImg.setVisible(true);
                                flagImg.setManaged(true);
                            } else {
                                flagImg.setVisible(false);
                                flagImg.setManaged(false);
                            }
                        }
                    } else {
                        flagImg.setVisible(false);
                        flagImg.setManaged(false);
                    }
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
    }

    private void updateCardFlags() {
        if (currentCategory != null && "Currency".equalsIgnoreCase(currentCategory.name)) {
            if (fromFlagBadge != null) {
                fromFlagBadge.setVisible(true);
                fromFlagBadge.setManaged(true);
            }
            if (toFlagBadge != null) {
                toFlagBadge.setVisible(true);
                toFlagBadge.setManaged(true);
            }

            UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
            UnitItem to = findUnitByDisplay(currentCategory, toUnitBox != null ? toUnitBox.getValue() : null);

            if (from != null && fromFlagView != null) {
                Image img = getFlagImage(from.symbol);
                if (img != null) {
                    fromFlagView.setImage(img);
                    Rectangle clip = new Rectangle(34, 22);
                    clip.setArcWidth(6);
                    clip.setArcHeight(6);
                    fromFlagView.setClip(clip);
                }
                if (fromFlagCodeLabel != null)
                    fromFlagCodeLabel.setText(from.symbol);
            }

            if (to != null && toFlagView != null) {
                Image img = getFlagImage(to.symbol);
                if (img != null) {
                    toFlagView.setImage(img);
                    Rectangle clip = new Rectangle(34, 22);
                    clip.setArcWidth(6);
                    clip.setArcHeight(6);
                    toFlagView.setClip(clip);
                }
                if (toFlagCodeLabel != null)
                    toFlagCodeLabel.setText(to.symbol);
            }
        } else {
            if (fromFlagBadge != null) {
                fromFlagBadge.setVisible(false);
                fromFlagBadge.setManaged(false);
            }
            if (toFlagBadge != null) {
                toFlagBadge.setVisible(false);
                toFlagBadge.setManaged(false);
            }
        }
    }

    private void initCategories() {
        allCategories.clear();

        // 1. Length
        List<UnitItem> lengthUnits = Arrays.asList(
                new UnitItem("Meter", "m", 1.0),
                new UnitItem("Kilometer", "km", 1000.0),
                new UnitItem("Centimeter", "cm", 0.01),
                new UnitItem("Millimeter", "mm", 0.001),
                new UnitItem("Micrometer", "µm", 1e-6),
                new UnitItem("Nanometer", "nm", 1e-9),
                new UnitItem("Mile", "mi", 1609.344),
                new UnitItem("Yard", "yd", 0.9144),
                new UnitItem("Foot", "ft", 0.3048),
                new UnitItem("Inch", "in", 0.0254),
                new UnitItem("Nautical Mile", "NM", 1852.0));
        allCategories.add(new CategoryData("Length", "📏", SVG_LENGTH, "MEASURE",
                "Every unit in its place. Every answer within reach.", lengthUnits, "Meter (m)", "Foot (ft)"));

        // 2. Area
        List<UnitItem> areaUnits = Arrays.asList(
                new UnitItem("Square Meter", "m²", 1.0),
                new UnitItem("Square Kilometer", "km²", 1e6),
                new UnitItem("Square Centimeter", "cm²", 0.0001),
                new UnitItem("Square Millimeter", "mm²", 1e-6),
                new UnitItem("Hectare", "ha", 10000.0),
                new UnitItem("Acre", "ac", 4046.8564224),
                new UnitItem("Square Mile", "mi²", 2589988.110336),
                new UnitItem("Square Yard", "yd²", 0.83612736),
                new UnitItem("Square Foot", "ft²", 0.09290304),
                new UnitItem("Square Inch", "in²", 0.00064516));
        allCategories.add(new CategoryData("Area", "⛶", SVG_AREA, "MEASURE",
                "Convert between square meters, acres, hectares, and square feet.", areaUnits, "Square Meter (m²)",
                "Square Foot (ft²)"));

        // 3. Volume
        List<UnitItem> volumeUnits = Arrays.asList(
                new UnitItem("Liter", "L", 1.0),
                new UnitItem("Milliliter", "mL", 0.001),
                new UnitItem("Cubic Meter", "m³", 1000.0),
                new UnitItem("Cubic Centimeter", "cm³", 0.001),
                new UnitItem("US Gallon", "gal", 3.785411784),
                new UnitItem("US Quart", "qt", 0.946352946),
                new UnitItem("US Pint", "pt", 0.473176473),
                new UnitItem("US Cup", "cup", 0.2365882365),
                new UnitItem("US Fluid Ounce", "fl oz", 0.0295735295625),
                new UnitItem("Imperial Gallon", "imp gal", 4.54609),
                new UnitItem("Tablespoon", "tbsp", 0.01478676478125),
                new UnitItem("Teaspoon", "tsp", 0.00492892159375));
        allCategories.add(new CategoryData("Volume", "🧊", SVG_VOLUME, "MEASURE",
                "Fluid and solid capacity across metric, US customary, and imperial.", volumeUnits, "Liter (L)",
                "US Gallon (gal)"));

        // 4. Mass / Weight
        List<UnitItem> massUnits = Arrays.asList(
                new UnitItem("Kilogram", "kg", 1.0),
                new UnitItem("Gram", "g", 0.001),
                new UnitItem("Milligram", "mg", 1e-6),
                new UnitItem("Metric Ton", "t", 1000.0),
                new UnitItem("Pound", "lb", 0.45359237),
                new UnitItem("Ounce", "oz", 0.028349523125),
                new UnitItem("Stone", "st", 6.35029318),
                new UnitItem("Carat", "ct", 0.0002));
        allCategories.add(new CategoryData("Mass / Weight", "⚖", SVG_MASS, "MEASURE",
                "Weight and mass conversion with precise multi-unit comparison.", massUnits, "Kilogram (kg)",
                "Pound (lb)"));

        // 5. Temperature
        List<UnitItem> tempUnits = Arrays.asList(
                new UnitItem("Celsius", "°C", 1.0),
                new UnitItem("Fahrenheit", "°F", 1.0),
                new UnitItem("Kelvin", "K", 1.0),
                new UnitItem("Rankine", "°R", 1.0));
        allCategories.add(new CategoryData("Temperature", "🌡", SVG_TEMP, "MEASURE",
                "Thermal conversion with key landmark reference points.", tempUnits, "Celsius (°C)",
                "Fahrenheit (°F)"));

        // 6. Speed
        List<UnitItem> speedUnits = Arrays.asList(
                new UnitItem("Meter per second", "m/s", 1.0),
                new UnitItem("Kilometer per hour", "km/h", 1.0 / 3.6),
                new UnitItem("Mile per hour", "mph", 0.44704),
                new UnitItem("Knot", "kn", 0.5144444444444445),
                new UnitItem("Foot per second", "ft/s", 0.3048));
        allCategories.add(new CategoryData("Speed", "🏃", SVG_SPEED, "MEASURE",
                "Velocity conversions with real-world motion benchmarks.", speedUnits, "Kilometer per hour (km/h)",
                "Mile per hour (mph)"));

        // 7. Time / Duration
        List<UnitItem> timeUnits = Arrays.asList(
                new UnitItem("Second", "s", 1.0),
                new UnitItem("Millisecond", "ms", 0.001),
                new UnitItem("Microsecond", "µs", 1e-6),
                new UnitItem("Minute", "min", 60.0),
                new UnitItem("Hour", "h", 3600.0),
                new UnitItem("Day", "d", 86400.0),
                new UnitItem("Week", "wk", 604800.0),
                new UnitItem("Month (30d)", "mo", 2592000.0),
                new UnitItem("Year (365d)", "yr", 31536000.0));
        allCategories.add(new CategoryData("Time / Duration", "⏱", SVG_TIME, "MEASURE",
                "Time intervals from microseconds to calendar years.", timeUnits, "Hour (h)", "Minute (min)"));

        // 8. Pressure
        List<UnitItem> pressureUnits = Arrays.asList(
                new UnitItem("Pascal", "Pa", 1.0),
                new UnitItem("Kilopascal", "kPa", 1000.0),
                new UnitItem("Bar", "bar", 100000.0),
                new UnitItem("Millibar", "mbar", 100.0),
                new UnitItem("Atmosphere", "atm", 101325.0),
                new UnitItem("PSI", "psi", 6894.757293),
                new UnitItem("Torr / mmHg", "Torr", 133.322368));
        allCategories.add(new CategoryData("Pressure", "🎈", SVG_PRESSURE, "MEASURE",
                "Atmospheric, hydraulic, and mechanical pressure comparisons.", pressureUnits, "Bar (bar)",
                "PSI (psi)"));

        // 9. Energy
        List<UnitItem> energyUnits = Arrays.asList(
                new UnitItem("Joule", "J", 1.0),
                new UnitItem("Kilojoule", "kJ", 1000.0),
                new UnitItem("Calorie", "cal", 4.184),
                new UnitItem("Kilocalorie", "kcal", 4184.0),
                new UnitItem("Watt-hour", "Wh", 3600.0),
                new UnitItem("Kilowatt-hour", "kWh", 3.6e6),
                new UnitItem("British Thermal Unit", "BTU", 1055.056),
                new UnitItem("Foot-pound", "ft-lb", 1.355818));
        allCategories.add(new CategoryData("Energy", "⚡", SVG_ENERGY, "CALCULATE",
                "Work, thermal, and electrical energy conversion.", energyUnits, "Kilowatt-hour (kWh)",
                "Kilojoule (kJ)"));

        // 10. Power
        List<UnitItem> powerUnits = Arrays.asList(
                new UnitItem("Watt", "W", 1.0),
                new UnitItem("Kilowatt", "kW", 1000.0),
                new UnitItem("Megawatt", "MW", 1e6),
                new UnitItem("Horsepower", "hp", 745.699872),
                new UnitItem("Metric Horsepower", "PS", 735.49875),
                new UnitItem("Foot-pound per min", "ft-lb/min", 0.0225969658));
        allCategories.add(new CategoryData("Power", "🔋", SVG_POWER, "CALCULATE",
                "Mechanical and electrical rate of energy transfer.", powerUnits, "Kilowatt (kW)", "Horsepower (hp)"));

        // 11. Data
        List<UnitItem> dataUnits = Arrays.asList(
                new UnitItem("Byte", "B", 1.0),
                new UnitItem("Kilobyte", "KB", 1024.0),
                new UnitItem("Megabyte", "MB", 1048576.0),
                new UnitItem("Gigabyte", "GB", 1073741824.0),
                new UnitItem("Terabyte", "TB", 1099511627776.0),
                new UnitItem("Petabyte", "PB", 1125899906842624.0),
                new UnitItem("Bit", "bit", 0.125),
                new UnitItem("Kilobit", "Kb", 128.0),
                new UnitItem("Megabit", "Mb", 131072.0),
                new UnitItem("Gigabit", "Gb", 134217728.0));
        allCategories.add(new CategoryData("Data", "💾", SVG_DATA, "CALCULATE",
                "Binary digital storage and network bandwidth units.", dataUnits, "Gigabyte (GB)", "Megabyte (MB)"));

        // 12. Currency (22 Major Global Currencies with Country Flags & Real-time Rates)
        List<UnitItem> currencyUnits = Arrays.asList(
                new UnitItem("US Dollar", "USD", 1.0),
                new UnitItem("Euro", "EUR", 1.08),
                new UnitItem("British Pound", "GBP", 1.27),
                new UnitItem("Japanese Yen", "JPY", 0.0065),
                new UnitItem("Canadian Dollar", "CAD", 0.73),
                new UnitItem("Australian Dollar", "AUD", 0.65),
                new UnitItem("Swiss Franc", "CHF", 1.11),
                new UnitItem("Chinese Yuan", "CNY", 0.14),
                new UnitItem("Indian Rupee", "INR", 0.012),
                new UnitItem("Sri Lankan Rupee", "LKR", 0.0033),
                new UnitItem("Singapore Dollar", "SGD", 0.74),
                new UnitItem("New Zealand Dollar", "NZD", 0.60),
                new UnitItem("UAE Dirham", "AED", 0.272),
                new UnitItem("Saudi Riyal", "SAR", 0.266),
                new UnitItem("South Korean Won", "KRW", 0.00073),
                new UnitItem("Brazilian Real", "BRL", 0.174),
                new UnitItem("South African Rand", "ZAR", 0.055),
                new UnitItem("Russian Ruble", "RUB", 0.0105),
                new UnitItem("Turkish Lira", "TRY", 0.029),
                new UnitItem("Swedish Krona", "SEK", 0.093),
                new UnitItem("Norwegian Krone", "NOK", 0.091),
                new UnitItem("Mexican Peso", "MXN", 0.050));
        allCategories.add(new CategoryData("Currency", "💱", SVG_CURRENCY, "CALCULATE",
                "Global currency reference conversion rates with live country flags.", currencyUnits, "US Dollar (USD)",
                "Euro (EUR)"));
    }

    private void buildSidebar() {
        if (measureCategoryBox != null)
            measureCategoryBox.getChildren().clear();
        if (calculateCategoryBox != null)
            calculateCategoryBox.getChildren().clear();
        categoryButtons.clear();

        for (CategoryData cat : allCategories) {
            Button btn = new Button(cat.name);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("category-btn");
            btn.setGraphicTextGap(10);

            SVGPath icon = new SVGPath();
            icon.setContent(cat.svgPath);
            icon.setFill(Color.web("#9E9AA8"));
            icon.setScaleX(0.75);
            icon.setScaleY(0.75);

            StackPane iconBox = new StackPane(icon);
            iconBox.setPrefSize(20, 20);
            iconBox.setMinSize(20, 20);
            iconBox.setMaxSize(20, 20);
            iconBox.setAlignment(Pos.CENTER);

            btn.setGraphic(iconBox);
            btn.setOnAction(e -> selectCategory(cat));
            categoryButtons.put(cat.name, btn);

            if ("MEASURE".equalsIgnoreCase(cat.section)) {
                if (measureCategoryBox != null)
                    measureCategoryBox.getChildren().add(btn);
            } else {
                if (calculateCategoryBox != null)
                    calculateCategoryBox.getChildren().add(btn);
            }
        }
    }

    private void selectCategory(CategoryData cat) {
        this.currentCategory = cat;

        // Highlight active category button & update icon tinting
        for (Map.Entry<String, Button> entry : categoryButtons.entrySet()) {
            Button b = entry.getValue();
            boolean isActive = entry.getKey().equals(cat.name);
            if (isActive) {
                if (!b.getStyleClass().contains("category-btn-active")) {
                    b.getStyleClass().add("category-btn-active");
                }
            } else {
                b.getStyleClass().remove("category-btn-active");
            }
            if (b.getGraphic() instanceof StackPane sp && !sp.getChildren().isEmpty()) {
                if (sp.getChildren().get(0) instanceof SVGPath svg) {
                    svg.setFill(isActive ? Color.web("#00D2FF") : Color.web("#9E9AA8"));
                }
            }
        }

        // Update headers
        if (breadcrumbLabel != null)
            breadcrumbLabel.setText("Convert  /  " + cat.name);
        if (categoryIconLabel != null) {
            categoryIconLabel.setText("");
            SVGPath headerSvg = new SVGPath();
            headerSvg.setContent(cat.svgPath);
            headerSvg.setFill(Color.web("#00D2FF"));
            headerSvg.setScaleX(1.15);
            headerSvg.setScaleY(1.15);

            StackPane headerIconBadge = new StackPane(headerSvg);
            headerIconBadge.setPrefSize(38, 38);
            headerIconBadge.setMinSize(38, 38);
            headerIconBadge.setMaxSize(38, 38);
            headerIconBadge.setAlignment(Pos.CENTER);
            headerIconBadge.setStyle("-fx-background-color: rgba(0, 210, 255, 0.12); " +
                    "-fx-background-radius: 9; -fx-border-color: rgba(0, 210, 255, 0.35); " +
                    "-fx-border-radius: 9; -fx-border-width: 1;");
            categoryIconLabel.setGraphic(headerIconBadge);
        }
        if (titleLabel != null)
            titleLabel.setText(cat.name + " converter");

        if ("Currency".equalsIgnoreCase(cat.name)) {
            if (refreshRatesBtn != null) {
                refreshRatesBtn.setVisible(true);
                refreshRatesBtn.setManaged(true);
            }
            if (subtitleLabel != null) {
                if (liveRatesTimestamp != null) {
                    subtitleLabel.setText("Global exchange rates  •  🟢 Live updated at " + liveRatesTimestamp);
                } else {
                    subtitleLabel.setText("Global exchange rates  •  🟡 Fetching live market rates...");
                }
            }
            fetchLiveExchangeRatesAsync(false);
        } else {
            if (refreshRatesBtn != null) {
                refreshRatesBtn.setVisible(false);
                refreshRatesBtn.setManaged(false);
            }
            if (subtitleLabel != null) {
                subtitleLabel.setText(cat.subtitle);
            }
        }

        isUpdatingUnits = true;
        if (fromUnitBox != null) {
            fromUnitBox.getItems().clear();
            for (UnitItem u : cat.units) {
                fromUnitBox.getItems().add(u.getDisplay());
            }
            fromUnitBox.setValue(cat.defaultFrom);
        }
        if (toUnitBox != null) {
            toUnitBox.getItems().clear();
            for (UnitItem u : cat.units) {
                toUnitBox.getItems().add(u.getDisplay());
            }
            toUnitBox.setValue(cat.defaultTo);
        }
        isUpdatingUnits = false;

        if (mainScrollPane != null) {
            mainScrollPane.setVvalue(0.0);
            Platform.runLater(() -> mainScrollPane.setVvalue(0.0));
        }

        updateCardFlags();
        convertUnits();
    }

    private UnitItem findUnitByDisplay(CategoryData cat, String display) {
        if (cat == null || display == null)
            return null;
        for (UnitItem u : cat.units) {
            if (u.getDisplay().equals(display))
                return u;
        }
        return null;
    }

    private double convert(double input, String category, UnitItem from, UnitItem to) {
        if (from == null || to == null)
            return 0;
        if (from.equals(to))
            return input;

        if ("Temperature".equalsIgnoreCase(category)) {
            // First convert from -> Celsius
            double c = input;
            switch (from.symbol) {
                case "°F":
                    c = (input - 32.0) * (5.0 / 9.0);
                    break;
                case "K":
                    c = input - 273.15;
                    break;
                case "°R":
                    c = (input - 491.67) * (5.0 / 9.0);
                    break;
                case "°C":
                default:
                    c = input;
                    break;
            }
            // Then convert Celsius -> to
            switch (to.symbol) {
                case "°F":
                    return (c * (9.0 / 5.0)) + 32.0;
                case "K":
                    return c + 273.15;
                case "°R":
                    return (c + 273.15) * (9.0 / 5.0);
                case "°C":
                default:
                    return c;
            }
        } else if ("Currency".equalsIgnoreCase(category)) {
            // Real-time currency exchange rates (USD based rates)
            double rateFrom = liveRates.getOrDefault(from.symbol, 1.0);
            double rateTo = liveRates.getOrDefault(to.symbol, 1.0);
            if (rateFrom <= 0)
                rateFrom = 1.0;
            return input * (rateTo / rateFrom);
        } else {
            // Linear conversions via base factor
            double baseValue = input * from.factor;
            return baseValue / to.factor;
        }
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            return "Invalid";
        if (Math.abs(value) < 1e-12)
            return "0";
        if (Math.abs(value) >= 1e11 || (Math.abs(value) < 1e-5 && Math.abs(value) > 0)) {
            return String.format(Locale.US, "%.5e", value).replace("e+0", "e+").replace("e-0", "e-");
        }
        DecimalFormat df = new DecimalFormat("#,##0.######", new DecimalFormatSymbols(Locale.US));
        return df.format(value);
    }

    private void convertUnits() {
        if (inputTextField == null || outputTextField == null || currentCategory == null)
            return;

        String inputStr = inputTextField.getText();
        if (inputStr == null || inputStr.trim().isEmpty() || inputStr.equals("-")) {
            outputTextField.setText("");
            drawVisualizer();
            updateEquivalentsTable(0);
            return;
        }

        try {
            double inputVal = Double.parseDouble(inputStr.trim().replace(",", ""));
            UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
            UnitItem to = findUnitByDisplay(currentCategory, toUnitBox != null ? toUnitBox.getValue() : null);

            if (from == null || to == null)
                return;

            double result = convert(inputVal, currentCategory.name, from, to);
            outputTextField.setText(formatNumber(result));

            // Record history
            String record = inputStr + " " + from.symbol + " = " + formatNumber(result) + " " + to.symbol;
            if (conversionHistory.isEmpty() || !conversionHistory.get(conversionHistory.size() - 1).equals(record)) {
                conversionHistory.add(record);
                if (conversionHistory.size() > 50)
                    conversionHistory.remove(0);
            }

            updateCardFlags();
            drawVisualizer();
            updateEquivalentsTable(inputVal);

        } catch (NumberFormatException e) {
            outputTextField.setText("Invalid");
        }
    }

    @FXML
    public void switchToHubMode() {
        visualizerMode = VisualizerMode.CALCULATION_HUB;
        updateVisualizerModeButtons();
        drawVisualizer();
    }

    @FXML
    public void switchToRulerMode() {
        visualizerMode = VisualizerMode.DUAL_RULER;
        updateVisualizerModeButtons();
        drawVisualizer();
    }

    private void updateVisualizerModeButtons() {
        if (hubModeBtn != null) {
            hubModeBtn.getStyleClass().removeAll("visualizer-mode-active");
            boolean isHub = visualizerMode == VisualizerMode.CALCULATION_HUB;
            if (isHub) {
                hubModeBtn.getStyleClass().add("visualizer-mode-active");
            }
            if (hubModeBtn.getGraphic() instanceof StackPane sp && !sp.getChildren().isEmpty() && sp.getChildren().get(0) instanceof SVGPath svg) {
                svg.setFill(isHub ? Color.web("#00D2FF") : Color.web("#9E9AA8"));
            }
        }
        if (rulerModeBtn != null) {
            rulerModeBtn.getStyleClass().removeAll("visualizer-mode-active");
            boolean isRuler = visualizerMode == VisualizerMode.DUAL_RULER;
            if (isRuler) {
                rulerModeBtn.getStyleClass().add("visualizer-mode-active");
            }
            if (rulerModeBtn.getGraphic() instanceof StackPane sp && !sp.getChildren().isEmpty() && sp.getChildren().get(0) instanceof SVGPath svg) {
                svg.setFill(isRuler ? Color.web("#00D2FF") : Color.web("#9E9AA8"));
            }
        }
    }

    private void drawVisualizer() {
        if (visualizerPane == null || currentCategory == null || isDrawingVisualizer) return;
        isDrawingVisualizer = true;
        try {
            lastDrawnWidth = visualizerPane.getWidth();
            visualizerPane.getChildren().clear();

            UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
            UnitItem to = findUnitByDisplay(currentCategory, toUnitBox != null ? toUnitBox.getValue() : null);

            if (from == null || to == null) return;

            double oneUnit = convert(1.0, currentCategory.name, from, to);
            if (formulaLabel != null) {
                formulaLabel.setText("1 " + from.symbol + " = " + formatNumber(oneUnit) + " " + to.symbol);
            }

            double inputVal = 0;
            try {
                String s = inputTextField.getText();
                if (s != null && !s.trim().isEmpty() && !s.equals("-")) {
                    inputVal = Double.parseDouble(s.trim().replace(",", ""));
                }
            } catch (Exception ignored) {}

            double paneWidth = visualizerPane.getWidth() > 200 ? visualizerPane.getWidth() : 600;

            if (visualizerMode == VisualizerMode.CALCULATION_HUB) {
                if (visualizerUnitPairLabel != null) {
                    visualizerUnitPairLabel.setText(from.symbol + "  ⇄  " + to.symbol + "   •   Visual Calculation Hub");
                }
                drawCalculationHub(from, to, inputVal, oneUnit, paneWidth);
            } else {
                if (visualizerUnitPairLabel != null) {
                    visualizerUnitPairLabel.setText(from.symbol + "  ⇄  " + to.symbol + "   •   Dual Scale Ruler");
                }
                drawDualRuler(from, to, inputVal, paneWidth);
            }
        } finally {
            isDrawingVisualizer = false;
        }
    }

    private void drawCalculationHub(UnitItem from, UnitItem to, double inputVal, double oneUnit, double paneWidth) {
        visualizerPane.setPrefHeight(230);

        VBox hubRoot = new VBox(10);
        hubRoot.setFillWidth(true);
        hubRoot.setMaxWidth(Double.MAX_VALUE);
        hubRoot.setAlignment(Pos.TOP_CENTER);

        double outputVal = convert(inputVal, currentCategory.name, from, to);

        HBox pipelineBox = buildPipelineBox(from, to, inputVal, outputVal, oneUnit);
        Node magnitudeScale = buildMagnitudeScaleNode(from, to, inputVal, outputVal, oneUnit, paneWidth);
        HBox benchmarksBox = buildBenchmarksBox(from, to, inputVal, outputVal);

        hubRoot.getChildren().addAll(pipelineBox, magnitudeScale, benchmarksBox);
        visualizerPane.getChildren().add(hubRoot);

        // Clip visualizerPane to prevent any possible visual overflow
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(visualizerPane.widthProperty());
        clip.heightProperty().bind(visualizerPane.heightProperty());
        visualizerPane.setClip(clip);
    }

    private HBox buildPipelineBox(UnitItem from, UnitItem to, double inputVal, double outputVal, double oneUnit) {
        HBox pipeline = new HBox(8);
        pipeline.setAlignment(Pos.CENTER);
        pipeline.setFillHeight(true);
        pipeline.setMaxWidth(Double.MAX_VALUE);

        // Center Operator Pill (Compact, Fixed Width)
        VBox opPill = new VBox(2);
        opPill.setAlignment(Pos.CENTER);
        opPill.setMinWidth(125);
        opPill.setPrefWidth(145);
        opPill.setMaxWidth(160);
        HBox.setHgrow(opPill, Priority.NEVER);
        opPill.setStyle("-fx-background-color: #242033; -fx-background-radius: 12; " +
                "-fx-border-color: rgba(255, 255, 255, 0.14); -fx-border-radius: 12; -fx-padding: 6 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 8, 0, 0, 2);");

        if ("Temperature".equalsIgnoreCase(currentCategory.name)) {
            Label opLbl = new Label(getTemperatureFormula(from, to));
            opLbl.setStyle("-fx-text-fill: #00E5FF; -fx-font-size: 11px; -fx-font-weight: bold;");
            Label formulaDesc = new Label("Thermal Formula Shift");
            formulaDesc.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.55); -fx-font-size: 9px;");
            opPill.getChildren().addAll(opLbl, formulaDesc);
        } else {
            Label opLbl = new Label("➔   × " + formatNumber(oneUnit));
            opLbl.setStyle("-fx-text-fill: #00E5FF; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label formulaDesc = new Label("1 " + from.symbol + " = " + formatNumber(oneUnit) + " " + to.symbol);
            formulaDesc.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.65); -fx-font-size: 10px;");
            opPill.getChildren().addAll(opLbl, formulaDesc);
        }

        // Left Node: Input Card
        VBox inputCard = new VBox(2);
        inputCard.setAlignment(Pos.CENTER_LEFT);
        inputCard.setMinWidth(100);
        inputCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(inputCard, Priority.ALWAYS);
        inputCard.setStyle("-fx-background-color: rgba(0, 210, 255, 0.08); -fx-background-radius: 10; " +
                "-fx-border-color: rgba(0, 210, 255, 0.35); -fx-border-radius: 10; -fx-padding: 8 12;");

        Label inTag = new Label("INPUT VALUE");
        inTag.setStyle("-fx-text-fill: rgba(0, 210, 255, 0.75); -fx-font-size: 10px; -fx-font-weight: bold;");

        Label inVal = new Label(formatNumber(inputVal) + " " + from.symbol);
        inVal.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label inName = new Label(from.name);
        inName.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.55); -fx-font-size: 11px;");

        inputCard.getChildren().addAll(inTag, inVal, inName);

        // Right Node: Result Card
        VBox resultCard = new VBox(2);
        resultCard.setAlignment(Pos.CENTER_LEFT);
        resultCard.setMinWidth(100);
        resultCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resultCard, Priority.ALWAYS);
        resultCard.setStyle("-fx-background-color: rgba(232, 46, 136, 0.08); -fx-background-radius: 10; " +
                "-fx-border-color: rgba(232, 46, 136, 0.4); -fx-border-radius: 10; -fx-padding: 8 12;");

        Label outTag = new Label("CALCULATED RESULT");
        outTag.setStyle("-fx-text-fill: rgba(255, 102, 170, 0.85); -fx-font-size: 10px; -fx-font-weight: bold;");

        Label outVal = new Label(formatNumber(outputVal) + " " + to.symbol);
        outVal.setStyle("-fx-text-fill: #FF66AA; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label outName = new Label(to.name);
        outName.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.55); -fx-font-size: 11px;");

        resultCard.getChildren().addAll(outTag, outVal, outName);

        // Perfectly balance inputCard and resultCard to have identical 50/50 widths
        inputCard.prefWidthProperty().bind(pipeline.widthProperty().subtract(165).multiply(0.5));
        resultCard.prefWidthProperty().bind(inputCard.prefWidthProperty());

        pipeline.getChildren().addAll(inputCard, opPill, resultCard);
        return pipeline;
    }

    private String getTemperatureFormula(UnitItem from, UnitItem to) {
        if (from.symbol.equals("°C") && to.symbol.equals("°F"))
            return "(°C × 9/5) + 32";
        if (from.symbol.equals("°F") && to.symbol.equals("°C"))
            return "(°F - 32) × 5/9";
        if (from.symbol.equals("°C") && to.symbol.equals("K"))
            return "°C + 273.15";
        if (from.symbol.equals("K") && to.symbol.equals("°C"))
            return "K - 273.15";
        if (from.symbol.equals("°F") && to.symbol.equals("K"))
            return "(°F - 32) × 5/9 + 273.15";
        if (from.symbol.equals("K") && to.symbol.equals("°F"))
            return "(K - 273.15) × 9/5 + 32";
        return from.symbol + " ➔ " + to.symbol;
    }

    private Node buildMagnitudeScaleNode(UnitItem from, UnitItem to, double inputVal, double outputVal, double oneUnit, double paneWidth) {
        VBox containerBox = new VBox(4);
        containerBox.setStyle("-fx-background-color: rgba(25, 23, 35, 0.65); -fx-background-radius: 10; " +
                "-fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 10; -fx-padding: 7 12;");
        containerBox.setMaxWidth(Double.MAX_VALUE);

        if ("Temperature".equalsIgnoreCase(currentCategory.name)) {
            // Temperature spectrum
            HBox header = new HBox();
            Label title = new Label("Thermal Spectrum");
            title.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 11px; -fx-font-weight: bold;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label currentLbl = new Label(
                    formatNumber(inputVal) + " " + from.symbol + " = " + formatNumber(outputVal) + " " + to.symbol);
            currentLbl.setStyle("-fx-text-fill: #00E5FF; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().addAll(title, spacer, currentLbl);

            Pane tempBarPane = new Pane();
            tempBarPane.setPrefHeight(18);
            tempBarPane.setMaxWidth(Double.MAX_VALUE);

            Rectangle gradBar = new Rectangle();
            gradBar.setHeight(8);
            gradBar.setY(5);
            gradBar.setArcWidth(8);
            gradBar.setArcHeight(8);
            gradBar.widthProperty().bind(tempBarPane.widthProperty());
            LinearGradient grad = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#00A8FF")),
                    new Stop(0.25, Color.web("#00E5FF")),
                    new Stop(0.5, Color.web("#00E676")),
                    new Stop(0.75, Color.web("#FFB300")),
                    new Stop(1.0, Color.web("#FF3366")));
            gradBar.setFill(grad);
            tempBarPane.getChildren().add(gradBar);

            double inCelsius = convert(inputVal, "Temperature", from, new UnitItem("Celsius", "°C", 1.0));
            double clampedNorm = Math.max(0.0, Math.min(1.0, (inCelsius - (-20.0)) / (120.0 - (-20.0))));

            Polygon marker = new Polygon(0.0, -5.0, 5.0, 0.0, 0.0, 5.0, -5.0, 0.0);
            marker.setFill(Color.web("#FFFFFF"));
            marker.setStroke(Color.web("#00D2FF"));
            marker.setStrokeWidth(1.5);
            marker.setEffect(new DropShadow(6, Color.web("#00D2FF")));
            marker.layoutYProperty().set(9);
            marker.layoutXProperty().bind(tempBarPane.widthProperty().multiply(clampedNorm));
            tempBarPane.getChildren().add(marker);

            HBox markersLblRow = new HBox();
            Label l1 = new Label("❄️ Freezing: 0°C (32°F)");
            l1.setStyle("-fx-text-fill: #00E5FF; -fx-font-size: 10px;");
            Region s1 = new Region();
            HBox.setHgrow(s1, Priority.ALWAYS);
            Label l2 = new Label("🛋️ Room: 20°C (68°F)");
            l2.setStyle("-fx-text-fill: #00E676; -fx-font-size: 10px;");
            Region s2 = new Region();
            HBox.setHgrow(s2, Priority.ALWAYS);
            Label l3 = new Label("🔥 Boiling: 100°C (212°F)");
            l3.setStyle("-fx-text-fill: #FF3366; -fx-font-size: 10px;");
            markersLblRow.getChildren().addAll(l1, s1, l2, s2, l3);

            containerBox.getChildren().addAll(header, tempBarPane, markersLblRow);
            return containerBox;
        }

        // Linear Units: Proportional container fill
        double oneToInFrom = convert(1.0, currentCategory.name, to, from);
        double absIn = Math.abs(inputVal);

        HBox header = new HBox();
        Label title = new Label("Container Magnitude & Fill Scale");
        title.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 11px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label ratioLbl = new Label();
        header.getChildren().addAll(title, spacer, ratioLbl);

        if (absIn <= 0.000001) {
            ratioLbl.setText("Fill: 0%");
            ratioLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.5); -fx-font-size: 11px;");

            StackPane emptyBar = new StackPane();
            emptyBar.setPrefHeight(22);
            emptyBar.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 8; -fx-border-color: rgba(255, 255, 255, 0.15); -fx-border-radius: 8;");

            Label caption = new Label("Enter an input value above to visualize container fill and magnitude scale.");
            caption.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.45); -fx-font-size: 10px;");
            containerBox.getChildren().addAll(header, emptyBar, caption);
            return containerBox;
        }

        if (outputVal <= 1.05 && outputVal > 0) {
            // Fits within 1 container!
            double fillRatio = Math.max(0.0, Math.min(1.0, outputVal));
            int pct = (int) Math.round(fillRatio * 100);
            ratioLbl.setText("1 " + to.name + " (" + formatNumber(oneToInFrom) + " " + from.symbol + ")  •  " + pct + "% Filled");
            ratioLbl.setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 11px; -fx-font-weight: bold;");

            StackPane barStack = new StackPane();
            barStack.setPrefHeight(22);
            barStack.setAlignment(Pos.CENTER_LEFT);
            barStack.setStyle("-fx-background-color: rgba(20, 18, 28, 0.8); -fx-background-radius: 8; -fx-border-color: rgba(0, 210, 255, 0.3); -fx-border-radius: 8;");

            Region fill = new Region();
            fill.setPrefHeight(22);
            fill.setStyle("-fx-background-color: linear-gradient(to right, #00D2FF, #0088FF); -fx-background-radius: 7;");
            fill.maxWidthProperty().bind(barStack.widthProperty().multiply(fillRatio));
            fill.setMinWidth(0);

            HBox textOverlay = new HBox(10);
            textOverlay.setAlignment(Pos.CENTER_LEFT);
            textOverlay.setStyle("-fx-padding: 0 10;");
            Label inFillText = new Label(formatNumber(inputVal) + " " + from.symbol + " (" + pct + "%)");
            inFillText.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 3, 0, 0, 1);");

            Region barSpacer = new Region();
            HBox.setHgrow(barSpacer, Priority.ALWAYS);

            double remaining = Math.max(0.0, oneToInFrom - inputVal);
            Label remainText = new Label(
                    remaining > 0.0001 ? ("+" + formatNumber(remaining) + " " + from.symbol + " to full") : "100% Full");
            remainText.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.55); -fx-font-size: 10px;");

            textOverlay.getChildren().addAll(inFillText, barSpacer, remainText);
            barStack.getChildren().addAll(fill, textOverlay);

            Label caption = new Label(
                    "💡 " + formatNumber(inputVal) + " " + from.symbol + " fills " + pct + "% of a 1 " + to.name +
                            " container. Needs +" + formatNumber(remaining) + " " + from.symbol + " to make 1 full " + to.symbol + ".");
            caption.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 10px;");

            containerBox.getChildren().addAll(header, barStack, caption);

        } else if (outputVal > 1.05 && outputVal <= 10.0) {
            // Multi-container fill (e.g. 10 L to Gallons = 2.64 Gallons, or 3 m to ft = 9.84 ft)
            int fullContainers = (int) Math.floor(outputVal);
            double partial = outputVal - fullContainers;
            int partialPct = (int) Math.round(partial * 100);

            ratioLbl.setText(fullContainers + " Full " + to.symbol + " + " + partialPct + "% of next");
            ratioLbl.setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 11px; -fx-font-weight: bold;");

            if (fullContainers <= 3) {
                // Individual container blocks
                HBox multiContainers = new HBox(6);
                multiContainers.setAlignment(Pos.CENTER_LEFT);
                multiContainers.setPrefHeight(22);

                int totalBoxes = fullContainers + (partial > 0.01 ? 1 : 0);
                for (int i = 0; i < totalBoxes; i++) {
                    StackPane box = new StackPane();
                    HBox.setHgrow(box, Priority.ALWAYS);
                    box.setPrefHeight(22);
                    box.setMinWidth(40);
                    box.setStyle("-fx-background-color: rgba(20, 18, 28, 0.8); -fx-background-radius: 6; -fx-border-color: rgba(0, 210, 255, 0.3); -fx-border-radius: 6;");

                    if (i < fullContainers) {
                        box.setStyle("-fx-background-color: #00B0FF; -fx-background-radius: 6;");
                        Label lbl = new Label("1 " + to.symbol + " (100%)");
                        lbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: bold;");
                        box.getChildren().add(lbl);
                    } else {
                        Region partFill = new Region();
                        partFill.setPrefHeight(22);
                        partFill.setStyle("-fx-background-color: #00D2FF; -fx-background-radius: 5;");
                        partFill.maxWidthProperty().bind(box.widthProperty().multiply(partial));
                        box.setAlignment(Pos.CENTER_LEFT);

                        Label lbl = new Label("  " + partialPct + "%");
                        lbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: bold;");
                        box.getChildren().addAll(partFill, lbl);
                    }
                    multiContainers.getChildren().add(box);
                }
                containerBox.getChildren().addAll(header, multiContainers);
            } else {
                // If more than 3 full containers (e.g. 9 full ft for 3 m), show unified proportional segmented bar
                StackPane barStack = new StackPane();
                barStack.setPrefHeight(22);
                barStack.setAlignment(Pos.CENTER_LEFT);
                barStack.setStyle("-fx-background-color: rgba(20, 18, 28, 0.8); -fx-background-radius: 6; -fx-border-color: rgba(0, 210, 255, 0.3); -fx-border-radius: 6;");

                double fullRatio = Math.min(0.92, (double) fullContainers / outputVal);
                Region fullFill = new Region();
                fullFill.setPrefHeight(22);
                fullFill.setStyle("-fx-background-color: #0099FF; -fx-background-radius: 5;");
                fullFill.maxWidthProperty().bind(barStack.widthProperty().multiply(fullRatio));

                HBox barText = new HBox(8);
                barText.setAlignment(Pos.CENTER_LEFT);
                barText.setStyle("-fx-padding: 0 10;");

                Label fullLbl = new Label(fullContainers + " Full " + to.symbol + " (" + (int) Math.round(fullRatio * 100) + "%)");
                fullLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: bold;");

                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);

                Label partLbl = new Label("+" + formatNumber(partial) + " " + to.symbol + " (" + partialPct + "%)");
                partLbl.setStyle("-fx-text-fill: rgba(0, 229, 255, 0.95); -fx-font-size: 10px; -fx-font-weight: bold;");

                barText.getChildren().addAll(fullLbl, spacer2, partLbl);
                barStack.getChildren().addAll(fullFill, barText);
                containerBox.getChildren().addAll(header, barStack);
            }

            Label caption = new Label("💡 " + formatNumber(inputVal) + " " + from.symbol + " completely fills "
                    + fullContainers + " full " + to.name + " (" + to.symbol + ") containers, plus " + partialPct + "% of another.");
            caption.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 10px;");
            containerBox.getChildren().add(caption);

        } else {
            // High magnitude multiplier scale
            ratioLbl.setText("1 " + from.symbol + " = " + formatNumber(oneUnit) + " " + to.symbol);
            ratioLbl.setStyle("-fx-text-fill: #FF66AA; -fx-font-size: 11px; -fx-font-weight: bold;");

            StackPane barStack = new StackPane();
            barStack.setPrefHeight(22);
            barStack.setAlignment(Pos.CENTER_LEFT);
            barStack.setStyle("-fx-background-color: rgba(20, 18, 28, 0.8); -fx-background-radius: 8; -fx-border-color: rgba(232, 46, 136, 0.3); -fx-border-radius: 8;");

            Region fill = new Region();
            fill.setPrefHeight(22);
            fill.setStyle("-fx-background-color: linear-gradient(to right, #E82E88, #9C27B0); -fx-background-radius: 7;");
            fill.maxWidthProperty().bind(barStack.widthProperty().multiply(0.85));

            Label barLbl = new Label("Magnitude Expansion: ×" + formatNumber(oneUnit));
            barLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 10;");

            barStack.getChildren().addAll(fill, barLbl);

            Label caption = new Label(
                    "💡 Each 1 " + from.name + " expands to " + formatNumber(oneUnit) + " " + to.symbol +
                            ". Total output yield is " + formatNumber(outputVal) + " " + to.symbol + ".");
            caption.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 10px;");

            containerBox.getChildren().addAll(header, barStack, caption);
        }

        return containerBox;
    }

    private HBox buildBenchmarksBox(UnitItem from, UnitItem to, double inputVal, double outputVal) {
        HBox benchmarksBox = new HBox(6);
        benchmarksBox.setAlignment(Pos.CENTER);
        benchmarksBox.setMaxWidth(Double.MAX_VALUE);

        List<BenchmarkItem> items = getCategoryBenchmarks(currentCategory.name, inputVal, from, to);
        for (BenchmarkItem item : items) {
            VBox tile = new VBox(1);
            tile.setAlignment(Pos.CENTER_LEFT);
            tile.setMinWidth(60);
            tile.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(tile, Priority.ALWAYS);
            tile.setStyle("-fx-background-color: rgba(30, 27, 43, 0.75); -fx-background-radius: 8; " +
                    "-fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 8; -fx-padding: 5 8;");

            Label countLbl = new Label(item.icon + "  " + item.multiplier);
            countLbl.setStyle("-fx-text-fill: #00E5FF; -fx-font-size: 11px; -fx-font-weight: bold;");

            Label titleLbl = new Label(item.title);
            titleLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 10px; -fx-font-weight: 600;");

            Label refLbl = new Label(item.reference);
            refLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.45); -fx-font-size: 9px;");

            tile.getChildren().addAll(countLbl, titleLbl, refLbl);
            benchmarksBox.getChildren().add(tile);
        }

        return benchmarksBox;
    }

    private String formatBenchmark(double val) {
        if (Math.abs(val) < 1e-9) return "0";
        if (val >= 1000) return String.format(Locale.US, "%,.0f", val);
        if (val >= 100) return String.format(Locale.US, "%.0f", val);
        if (val >= 10) return String.format(Locale.US, "%.1f", val).replaceAll("\\.0$", "");
        if (val >= 1) return String.format(Locale.US, "%.1f", val).replaceAll("\\.0$", "");
        if (val >= 0.01) {
            String s = String.format(Locale.US, "%.2f", val);
            return s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.format(Locale.US, "%.1e", val);
    }

    private List<BenchmarkItem> getCategoryBenchmarks(String category, double inputVal, UnitItem from, UnitItem to) {
        List<BenchmarkItem> list = new ArrayList<>();

        if ("Volume".equalsIgnoreCase(category)) {
            double liters = inputVal * from.factor;
            list.add(new BenchmarkItem("🥤", formatBenchmark(liters / 0.5) + "×", "Water Bottles", "500 mL standard"));
            list.add(new BenchmarkItem("🧃", formatBenchmark(liters / 0.355) + "×", "Drink Cans", "355 mL soda can"));
            list.add(new BenchmarkItem("☕", formatBenchmark(liters / 0.2366) + "×", "Standard Cups", "236 mL metric cup"));
            list.add(new BenchmarkItem("🪣", formatBenchmark(liters / 10.0) + "×", "10L Buckets", "10 L utility bucket"));
        } else if ("Length".equalsIgnoreCase(category)) {
            double meters = inputVal * from.factor;
            list.add(new BenchmarkItem("🚶", formatBenchmark(meters / 0.75) + "×", "Walking Steps", "0.75 m adult stride"));
            list.add(new BenchmarkItem("📏", formatBenchmark(meters / 1.75) + "×", "Adult Height", "1.75 m person"));
            list.add(new BenchmarkItem("🚗", formatBenchmark(meters / 4.5) + "×", "Family Cars", "4.5 m sedan car"));
            list.add(new BenchmarkItem("⚽", formatBenchmark(meters / 105.0) + "×", "Football Pitches", "105 m pitch"));
        } else if ("Mass / Weight".equalsIgnoreCase(category) || "Weight".equalsIgnoreCase(category)) {
            double kg = inputVal * from.factor;
            list.add(new BenchmarkItem("📱", formatBenchmark(kg / 0.2) + "×", "Smartphones", "200 g phone"));
            list.add(new BenchmarkItem("🏀", formatBenchmark(kg / 0.62) + "×", "Basketballs", "620 g match ball"));
            list.add(new BenchmarkItem("💻", formatBenchmark(kg / 1.5) + "×", "Laptops", "1.5 kg notebook"));
            list.add(new BenchmarkItem("🚲", formatBenchmark(kg / 12.0) + "×", "Bicycles", "12 kg commuter bike"));
        } else if ("Area".equalsIgnoreCase(category)) {
            double m2 = inputVal * from.factor;
            list.add(new BenchmarkItem("🛏", formatBenchmark(m2 / 14.0) + "×", "Bedrooms", "14 m² room"));
            list.add(new BenchmarkItem("🎾", formatBenchmark(m2 / 260.7) + "×", "Tennis Courts", "260 m² court"));
            list.add(new BenchmarkItem("🏀", formatBenchmark(m2 / 436.6) + "×", "Basketball Courts", "436 m² court"));
            list.add(new BenchmarkItem("⚽", formatBenchmark(m2 / 7140.0) + "×", "Football Fields", "7,140 m² pitch"));
        } else if ("Speed".equalsIgnoreCase(category)) {
            double kmh = (inputVal * from.factor) * 3.6;
            list.add(new BenchmarkItem("🚶", formatBenchmark(kmh / 5.0) + "×", "Walking Pace", "5 km/h human walk"));
            list.add(new BenchmarkItem("🚴", formatBenchmark(kmh / 20.0) + "×", "City Cycling", "20 km/h bicycle"));
            list.add(new BenchmarkItem("🚗", formatBenchmark(kmh / 100.0) + "×", "Highway Speed", "100 km/h speed limit"));
            list.add(new BenchmarkItem("✈", formatBenchmark(kmh / 900.0) + "×", "Passenger Jet", "900 km/h cruising"));
        } else if ("Data".equalsIgnoreCase(category)) {
            double bytes = inputVal * from.factor;
            list.add(new BenchmarkItem("🎵", formatBenchmark(bytes / 4194304.0) + "×", "MP3 Songs", "4 MB audio track"));
            list.add(new BenchmarkItem("📸", formatBenchmark(bytes / 5242880.0) + "×", "HD Photos", "5 MB jpeg image"));
            list.add(new BenchmarkItem("🎬", formatBenchmark(bytes / 2147483648.0) + "×", "1080p Movies", "2 GB full HD movie"));
            list.add(new BenchmarkItem("💾", formatBenchmark(bytes / 1099511627776.0) + "×", "1 TB Drives", "1,024 GB storage"));
        } else if ("Time".equalsIgnoreCase(category) || "Time / Duration".equalsIgnoreCase(category)) {
            double sec = inputVal * from.factor;
            list.add(new BenchmarkItem("🎵", formatBenchmark(sec / 240.0) + "×", "Pop Songs", "4 minute track"));
            list.add(new BenchmarkItem("🎬", formatBenchmark(sec / 7200.0) + "×", "Feature Films", "2 hour movie"));
            list.add(new BenchmarkItem("🌙", formatBenchmark(sec / 28800.0) + "×", "Full Sleep", "8 hour rest"));
            list.add(new BenchmarkItem("☀️", formatBenchmark(sec / 86400.0) + "×", "Whole Days", "24 hour solar day"));
        } else if ("Currency".equalsIgnoreCase(category)) {
            double rate = convert(1.0, "Currency", from, to);
            double invRate = rate > 0 ? (1.0 / rate) : 0;
            list.add(new BenchmarkItem("💱", "1 " + from.symbol + " =", formatNumber(rate) + " " + to.symbol, "Direct conversion rate"));
            list.add(new BenchmarkItem("🔄", "1 " + to.symbol + " =", formatNumber(invRate) + " " + from.symbol, "Inverse exchange rate"));
            list.add(new BenchmarkItem("🌐", "Live Rates", "Global Forex", "Updated exchange data"));
            list.add(new BenchmarkItem("💵", formatNumber(inputVal * rate), to.symbol + " Total", "Market mid-price equivalent"));
        } else if ("Temperature".equalsIgnoreCase(category)) {
            list.add(new BenchmarkItem("❄", "0°C", "Freezing Point", "32°F / 273.15 K"));
            list.add(new BenchmarkItem("🛋", "20°C", "Room Temp", "68°F / 293.15 K"));
            list.add(new BenchmarkItem("🌡", "37°C", "Human Body", "98.6°F / 310.15 K"));
            list.add(new BenchmarkItem("🔥", "100°C", "Boiling Point", "212°F / 373.15 K"));
        } else if ("Power".equalsIgnoreCase(category)) {
            double watts = inputVal * from.factor;
            list.add(new BenchmarkItem("💡", formatBenchmark(watts / 10.0) + "×", "LED Bulbs", "10 W light"));
            list.add(new BenchmarkItem("💻", formatBenchmark(watts / 65.0) + "×", "Laptops", "65 W charger"));
            list.add(new BenchmarkItem("☕", formatBenchmark(watts / 1500.0) + "×", "Kettles", "1,500 W boiling"));
            list.add(new BenchmarkItem("🏎", formatBenchmark(watts / 110000.0) + "×", "Car Engines", "~150 hp output"));
        } else if ("Energy".equalsIgnoreCase(category)) {
            double joules = inputVal * from.factor;
            list.add(new BenchmarkItem("🍎", formatBenchmark(joules / 400000.0) + "×", "Apples", "95 kcal nutrition"));
            list.add(new BenchmarkItem("🔋", formatBenchmark(joules / 40000.0) + "×", "Phone Charges", "40 kJ full battery"));
            list.add(new BenchmarkItem("⚡", formatBenchmark(joules / 3600000.0) + "×", "Kilowatt-Hours", "1 kWh home power"));
            list.add(new BenchmarkItem("🚗", formatBenchmark(joules / 130000000.0) + "×", "Gasoline (Gal)", "130 MJ energy"));
        } else { // Pressure or other
            double pa = inputVal * from.factor;
            list.add(new BenchmarkItem("💨", formatBenchmark(pa / 100000.0) + "×", "1 Bar Pressure", "100 kPa standard"));
            list.add(new BenchmarkItem("🌊", formatBenchmark(pa / 101325.0) + "×", "Atmosphere", "1 atm (101.3 kPa)"));
            list.add(new BenchmarkItem("🚗", formatBenchmark(pa / 220000.0) + "×", "Car Tires", "2.2 bar (~32 psi)"));
            list.add(new BenchmarkItem("🤿", formatBenchmark(pa / 20000000.0) + "×", "Scuba Tanks", "200 bar tank"));
        }

        return list;
    }

    private void drawDualRuler(UnitItem from, UnitItem to, double inputVal, double paneWidth) {
        Pane rulerPane = new Pane();
        rulerPane.setMaxWidth(Double.MAX_VALUE);
        rulerPane.setPrefWidth(paneWidth);

        double x0 = 40.0;
        double x1 = paneWidth - 40.0;
        double w = x1 - x0;
        double axisY = 54.0;

        // Dynamic tick upper bound
        double absIn = Math.abs(inputVal);
        double maxVal;
        if (currentCategory.name.equals("Temperature")) {
            maxVal = 100.0;
        } else if (absIn <= 0.0001) {
            maxVal = 5.0;
        } else if (absIn <= 1.0) {
            maxVal = 1.0;
        } else if (absIn <= 5.0) {
            maxVal = 5.0;
        } else if (absIn <= 10.0) {
            maxVal = 10.0;
        } else {
            double target = absIn * 1.25;
            double exp = Math.floor(Math.log10(target));
            double factor = Math.pow(10, exp);
            double norm = target / factor;
            if (norm <= 2.0)
                maxVal = 2.0 * factor;
            else if (norm <= 5.0)
                maxVal = 5.0 * factor;
            else
                maxVal = 10.0 * factor;
        }

        // 1. Horizontal Baseline Line
        Line baseline = new Line(x0, axisY, x1, axisY);
        baseline.setStroke(Color.web("rgba(255, 255, 255, 0.22)"));
        baseline.setStrokeWidth(1.5);
        rulerPane.getChildren().add(baseline);

        // 2. 5 Major Ticks and Minor Ticks
        for (int i = 0; i <= 4; i++) {
            double xi = x0 + i * (w / 4.0);
            double ti = i * (maxVal / 4.0);

            // Major tick line
            Line majorTick = new Line(xi, axisY - 10, xi, axisY + 10);
            majorTick.setStroke(Color.web("rgba(255, 255, 255, 0.45)"));
            majorTick.setStrokeWidth(1.5);
            rulerPane.getChildren().add(majorTick);

            // Top Label (From unit value)
            Label topLbl = new Label(formatNumber(ti));
            topLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 11px; -fx-font-weight: 500;");
            topLbl.setLayoutX(xi - 25);
            topLbl.setLayoutY(axisY - 32);
            topLbl.setPrefWidth(50);
            topLbl.setAlignment(Pos.CENTER);
            rulerPane.getChildren().add(topLbl);

            // Bottom Label (To unit converted value)
            double convTi = convert(ti, currentCategory.name, from, to);
            Label botLbl = new Label(formatNumber(convTi));
            botLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 11px; -fx-font-weight: 500;");
            botLbl.setLayoutX(xi - 35);
            botLbl.setLayoutY(axisY + 16);
            botLbl.setPrefWidth(70);
            botLbl.setAlignment(Pos.CENTER);
            rulerPane.getChildren().add(botLbl);

            // 3 minor ticks between each major tick
            if (i < 4) {
                for (int k = 1; k <= 3; k++) {
                    double xMinor = xi + k * (w / 16.0);
                    Line minorTick = new Line(xMinor, axisY - 5, xMinor, axisY + 5);
                    minorTick.setStroke(Color.web("rgba(255, 255, 255, 0.2)"));
                    minorTick.setStrokeWidth(1.0);
                    rulerPane.getChildren().add(minorTick);
                }
            }
        }

        // 3. Pointer Pin (Indicator)
        double ratio = maxVal > 0 ? (inputVal / maxVal) : 0;
        double clampedRatio = Math.max(0.0, Math.min(1.0, ratio));
        double pinX = x0 + clampedRatio * w;

        Line pinLine = new Line(pinX, axisY - 18, pinX, axisY + 18);
        pinLine.setStroke(Color.web("#00D2FF"));
        pinLine.setStrokeWidth(2.0);

        Polygon diamond = new Polygon();
        diamond.getPoints().addAll(
                pinX, axisY - 7.0,
                pinX + 5.0, axisY,
                pinX, axisY + 7.0,
                pinX - 5.0, axisY);
        diamond.setFill(Color.web("#00D2FF"));
        diamond.setEffect(new DropShadow(8, Color.web("#00D2FF")));

        rulerPane.getChildren().addAll(pinLine, diamond);

        // 4. Special Category Visualizer Enhancements
        if ("Temperature".equalsIgnoreCase(currentCategory.name)) {
            double barY = axisY + 44.0;
            visualizerPane.setPrefHeight(140);
            Rectangle gradBar = new Rectangle(x0, barY, w, 8);
            gradBar.setArcWidth(8);
            gradBar.setArcHeight(8);
            LinearGradient grad = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.web("#00A8FF")),
                    new Stop(0.2, Color.web("#00E5FF")),
                    new Stop(0.4, Color.web("#00E676")),
                    new Stop(0.7, Color.web("#FFB300")),
                    new Stop(1.0, Color.web("#FF3366")));
            gradBar.setFill(grad);
            rulerPane.getChildren().add(gradBar);

            Label freezeLbl = new Label("Freezing: 0°C (32°F)");
            freezeLbl.setStyle("-fx-text-fill: rgba(0, 229, 255, 0.85); -fx-font-size: 10px;");
            freezeLbl.setLayoutX(x0);
            freezeLbl.setLayoutY(barY + 12);

            Label roomLbl = new Label("Room: 20°C (68°F)");
            roomLbl.setStyle("-fx-text-fill: rgba(0, 230, 118, 0.85); -fx-font-size: 10px;");
            roomLbl.setLayoutX(x0 + w * 0.28);
            roomLbl.setLayoutY(barY + 12);

            Label boilLbl = new Label("Boiling: 100°C (212°F)");
            boilLbl.setStyle("-fx-text-fill: rgba(255, 51, 102, 0.85); -fx-font-size: 10px;");
            boilLbl.setLayoutX(x1 - 105);
            boilLbl.setLayoutY(barY + 12);

            rulerPane.getChildren().addAll(freezeLbl, roomLbl, boilLbl);

        } else if ("Speed".equalsIgnoreCase(currentCategory.name)) {
            visualizerPane.setPrefHeight(125);
            double barY = axisY + 42.0;
            Label hint = new Label(
                    "Context:  🚶 Walking ~5 km/h   •   🚗 Highway ~100 km/h   •   ✈️ Jet ~900 km/h   •   ⚡ Mach 1 ~1235 km/h");
            hint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 11px;");
            hint.setLayoutX(x0);
            hint.setLayoutY(barY);
            rulerPane.getChildren().add(hint);

        } else if ("Data".equalsIgnoreCase(currentCategory.name)) {
            visualizerPane.setPrefHeight(125);
            double barY = axisY + 42.0;
            Label hint = new Label("Hierarchy:  1 TB = 1,024 GB = 1,048,576 MB = 1,073,741,824 KB  (Base 2)");
            hint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 11px;");
            hint.setLayoutX(x0);
            hint.setLayoutY(barY);
            rulerPane.getChildren().add(hint);
        } else {
            visualizerPane.setPrefHeight(110);
        }

        visualizerPane.getChildren().add(rulerPane);
    }

    private void updateEquivalentsTable(double inputVal) {
        if (equivalentsContainer == null || currentCategory == null)
            return;
        equivalentsContainer.getChildren().clear();

        UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
        if (from == null)
            return;

        boolean isCurrency = "Currency".equalsIgnoreCase(currentCategory.name);

        for (UnitItem u : currentCategory.units) {
            double converted = convert(inputVal, currentCategory.name, from, u);
            String formatted = formatNumber(converted);

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("equivalent-row");

            // Show country flag in equivalent rows if currency
            if (isCurrency) {
                Image flag = getFlagImage(u.symbol);
                if (flag != null) {
                    ImageView flagView = new ImageView(flag);
                    flagView.setFitWidth(24);
                    flagView.setFitHeight(16);
                    Rectangle clip = new Rectangle(24, 16);
                    clip.setArcWidth(4);
                    clip.setArcHeight(4);
                    flagView.setClip(clip);
                    row.getChildren().add(flagView);
                }
            }

            Label nameLabel = new Label(u.name);
            nameLabel.setStyle("-fx-text-fill: #D8D6E5; -fx-font-size: 14px; -fx-font-weight: 500;");
            row.getChildren().add(nameLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);

            Label valLabel = new Label(formatted + "  " + u.symbol);
            valLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 15px; -fx-font-weight: bold;");
            row.getChildren().add(valLabel);

            Button copyBtn = new Button("Copy");
            copyBtn.getStyleClass().add("equivalent-copy-btn");
            copyBtn.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(formatted);
                Clipboard.getSystemClipboard().setContent(content);

                copyBtn.setText("Copied!");
                copyBtn.setStyle("-fx-background-color: #0E7490; -fx-text-fill: white; -fx-background-radius: 6;");
                PauseTransition p = new PauseTransition(Duration.millis(1200));
                p.setOnFinished(ev -> {
                    copyBtn.setText("Copy");
                    copyBtn.setStyle("");
                });
                p.play();
            });
            row.getChildren().add(copyBtn);

            equivalentsContainer.getChildren().add(row);
        }
    }

    @FXML
    public void swapUnits() {
        if (fromUnitBox == null || toUnitBox == null)
            return;
        String fromVal = fromUnitBox.getValue();
        String toVal = toUnitBox.getValue();

        isUpdatingUnits = true;
        fromUnitBox.setValue(toVal);
        toUnitBox.setValue(fromVal);
        isUpdatingUnits = false;

        if (swapBtn != null) {
            javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(Duration.millis(250), swapBtn);
            rt.setByAngle(180);
            rt.play();
        }

        updateCardFlags();
        convertUnits();
    }

    @FXML
    public void clearEntry() {
        if (inputTextField != null) {
            inputTextField.setText("0");
            convertUnits();
        }
    }

    @FXML
    public void copyResult() {
        if (outputTextField == null || copyResultBtn == null)
            return;
        String res = outputTextField.getText();
        if (res == null || res.trim().isEmpty() || res.equals("Invalid"))
            return;

        ClipboardContent content = new ClipboardContent();
        content.putString(res.trim());
        Clipboard.getSystemClipboard().setContent(content);

        copyResultBtn.setText("✓ Copied!");
        copyResultBtn.setStyle("-fx-background-color: #059669;");
        PauseTransition p = new PauseTransition(Duration.millis(1500));
        p.setOnFinished(e -> {
            copyResultBtn.setText("📋 Copy result");
            copyResultBtn.setStyle("");
        });
        p.play();
    }

    @FXML
    public void showHistory() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Conversion History");
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #171522; -fx-padding: 20; -fx-background-radius: 14; " +
                "-fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-radius: 14; -fx-border-width: 1;");

        VBox root = new VBox(14);
        root.setPrefWidth(440);

        // Header: History icon + Title + "Clear All" button
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        SVGPath clockSvg = new SVGPath();
        clockSvg.setContent(SVG_TIME);
        clockSvg.setFill(Color.web("#00D2FF"));
        clockSvg.setScaleX(0.95);
        clockSvg.setScaleY(0.95);

        StackPane clockBadge = new StackPane(clockSvg);
        clockBadge.setPrefSize(32, 32);
        clockBadge.setMinSize(32, 32);
        clockBadge.setMaxSize(32, 32);
        clockBadge.setAlignment(Pos.CENTER);
        clockBadge.setStyle("-fx-background-color: rgba(0, 210, 255, 0.12); -fx-background-radius: 8;");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label("Conversion History");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label countLabel = new Label();
        countLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.45); -fx-font-size: 11px;");

        titleBox.getChildren().addAll(titleLabel, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Clear History Button
        Button clearBtn = new Button("Clear All");
        SVGPath trashSvg = new SVGPath();
        trashSvg.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        trashSvg.setFill(Color.web("#FF5370"));
        trashSvg.setScaleX(0.75);
        trashSvg.setScaleY(0.75);
        clearBtn.setGraphic(trashSvg);
        clearBtn.setGraphicTextGap(6);
        clearBtn.setStyle("-fx-background-color: rgba(255, 83, 112, 0.12); -fx-text-fill: #FF5370; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; " +
                "-fx-border-color: rgba(255, 83, 112, 0.35); -fx-border-radius: 6; -fx-border-width: 1; " +
                "-fx-padding: 6 12; -fx-cursor: hand;");
        clearBtn.setOnMouseEntered(e -> {
            if (!clearBtn.isDisabled()) {
                clearBtn.setStyle("-fx-background-color: rgba(255, 83, 112, 0.25); -fx-text-fill: #FF7590; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; " +
                        "-fx-border-color: #FF5370; -fx-border-radius: 6; -fx-border-width: 1; " +
                        "-fx-padding: 6 12; -fx-cursor: hand;");
            }
        });
        clearBtn.setOnMouseExited(e -> {
            if (!clearBtn.isDisabled()) {
                clearBtn.setStyle("-fx-background-color: rgba(255, 83, 112, 0.12); -fx-text-fill: #FF5370; " +
                        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6; " +
                        "-fx-border-color: rgba(255, 83, 112, 0.35); -fx-border-radius: 6; -fx-border-width: 1; " +
                        "-fx-padding: 6 12; -fx-cursor: hand;");
            }
        });

        header.getChildren().addAll(clockBadge, titleBox, spacer, clearBtn);

        // List Container inside ScrollPane
        VBox listContainer = new VBox(6);
        listContainer.setStyle("-fx-padding: 4;");

        // Helper to refresh view
        Runnable refreshView = () -> {
            listContainer.getChildren().clear();
            int count = conversionHistory.size();
            countLabel.setText(count + (count == 1 ? " saved conversion" : " saved conversions"));
            clearBtn.setDisable(count == 0);
            if (count == 0) {
                clearBtn.setOpacity(0.4);
            } else {
                clearBtn.setOpacity(1.0);
            }

            if (count == 0) {
                VBox emptyBox = new VBox(6);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(30, 10, 30, 10));

                Label emptyLabel = new Label("No recent conversions");
                emptyLabel.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.7); -fx-font-size: 13px; -fx-font-weight: bold;");

                Label emptySub = new Label("Conversions you calculate will appear here.");
                emptySub.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 11px;");

                emptyBox.getChildren().addAll(emptyLabel, emptySub);
                listContainer.getChildren().add(emptyBox);
            } else {
                for (int i = conversionHistory.size() - 1; i >= 0; i--) {
                    String record = conversionHistory.get(i);
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color: #201D2D; -fx-background-radius: 8; -fx-padding: 8 12; " +
                            "-fx-border-color: rgba(255, 255, 255, 0.06); -fx-border-radius: 8; -fx-border-width: 1;");

                    Label dot = new Label("•");
                    dot.setStyle("-fx-text-fill: #00D2FF; -fx-font-size: 15px; -fx-font-weight: bold;");

                    Label itemText = new Label(record);
                    itemText.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: 500;");
                    HBox.setHgrow(itemText, Priority.ALWAYS);

                    Button copyBtn = new Button("Copy");
                    copyBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #D8D6E5; " +
                            "-fx-font-size: 11px; -fx-background-radius: 5; -fx-padding: 3 8; -fx-cursor: hand;");
                    copyBtn.setOnAction(ev -> {
                        ClipboardContent content = new ClipboardContent();
                        content.putString(record);
                        Clipboard.getSystemClipboard().setContent(content);
                        copyBtn.setText("Copied!");
                        copyBtn.setStyle("-fx-background-color: #0E7490; -fx-text-fill: white; -fx-font-size: 11px; -fx-background-radius: 5; -fx-padding: 3 8;");
                        PauseTransition p = new PauseTransition(Duration.millis(1000));
                        p.setOnFinished(e -> {
                            copyBtn.setText("Copy");
                            copyBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #D8D6E5; -fx-font-size: 11px; -fx-background-radius: 5; -fx-padding: 3 8; -fx-cursor: hand;");
                        });
                        p.play();
                    });

                    row.getChildren().addAll(dot, itemText, copyBtn);
                    listContainer.getChildren().add(row);
                }
            }
        };

        // Wire Clear Action
        clearBtn.setOnAction(e -> {
            conversionHistory.clear();
            refreshView.run();
        });

        refreshView.run();

        ScrollPane sp = new ScrollPane(listContainer);
        sp.setFitToWidth(true);
        sp.setPrefHeight(260);
        sp.setMaxHeight(340);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

        // Footer with Close Button
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(4, 0, 0, 0));

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2C293A; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #5A5474; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 7 18; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #3E3952; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #7D769E; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 7 18; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: #2C293A; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #5A5474; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-padding: 7 18; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> dialog.close());

        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, sp, footer);
        pane.setContent(root);

        pane.getButtonTypes().add(ButtonType.CLOSE);
        dialog.setOnShowing(ev -> {
            Node bar = pane.lookup(".button-bar");
            if (bar != null) {
                bar.setVisible(false);
                bar.setManaged(false);
            }
        });

        dialog.showAndWait();
    }

    @FXML
    public void toggleSidebar() {
        if (sidebar == null)
            return;
        sidebarVisible = !sidebarVisible;
        Timeline timeline = new Timeline();
        KeyValue kvW = new KeyValue(sidebar.prefWidthProperty(), sidebarVisible ? 215.0 : 0.0);
        KeyValue kvOp = new KeyValue(sidebar.opacityProperty(), sidebarVisible ? 1.0 : 0.0);
        KeyFrame kf = new KeyFrame(Duration.millis(220), kvW, kvOp);
        timeline.getKeyFrames().add(kf);
        timeline.setOnFinished(e -> {
            if (!sidebarVisible) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            }
        });
        if (sidebarVisible) {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
        }
        timeline.play();
    }
}
