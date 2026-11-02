package com.banukasineth.advancedcalculator.app.controller;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyValue;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button sidebarToggleBtn;
    @FXML private ComboBox<String> unitModeComboBox;
    @FXML private VBox measureCategoryBox;
    @FXML private VBox calculateCategoryBox;
    @FXML private ScrollPane mainScrollPane;

    @FXML private Label breadcrumbLabel;
    @FXML private Label categoryIconLabel;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;

    @FXML private TextField inputTextField;
    @FXML private TextField outputTextField;
    @FXML private ComboBox<String> fromUnitBox;
    @FXML private ComboBox<String> toUnitBox;
    @FXML private Button swapBtn;

    // Currency Country Flag Showcase Badges
    @FXML private HBox fromFlagBadge;
    @FXML private ImageView fromFlagView;
    @FXML private Label fromFlagCodeLabel;

    @FXML private HBox toFlagBadge;
    @FXML private ImageView toFlagView;
    @FXML private Label toFlagCodeLabel;

    @FXML private Label visualizerUnitPairLabel;
    @FXML private Pane visualizerPane;
    @FXML private Label formulaLabel;

    @FXML private Button refreshRatesBtn;
    @FXML private Button copyResultBtn;
    @FXML private VBox equivalentsContainer;

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
        public final String section; // "MEASURE" or "CALCULATE"
        public final String subtitle;
        public final List<UnitItem> units;
        public final String defaultFrom;
        public final String defaultTo;

        public CategoryData(String name, String icon, String section, String subtitle,
                            List<UnitItem> units, String defaultFrom, String defaultTo) {
            this.name = name;
            this.icon = icon;
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
            unitModeComboBox.getItems().addAll("Basic", "Advanced", "Scientific", "Programmer", "Graphing", "Unit Converter");
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
                if (newVal.doubleValue() > 50) {
                    drawVisualizer();
                }
            });
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
        if (isFetchingRates) return;
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
                        while ((line = reader.readLine()) != null) sb.append(line);
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
        if (symbol == null) return null;
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
                if (fromFlagCodeLabel != null) fromFlagCodeLabel.setText(from.symbol);
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
                if (toFlagCodeLabel != null) toFlagCodeLabel.setText(to.symbol);
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
                new UnitItem("Nautical Mile", "NM", 1852.0)
        );
        allCategories.add(new CategoryData("Length", "📏", "MEASURE",
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
                new UnitItem("Square Inch", "in²", 0.00064516)
        );
        allCategories.add(new CategoryData("Area", "⛶", "MEASURE",
                "Convert between square meters, acres, hectares, and square feet.", areaUnits, "Square Meter (m²)", "Square Foot (ft²)"));

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
                new UnitItem("Teaspoon", "tsp", 0.00492892159375)
        );
        allCategories.add(new CategoryData("Volume", "🧊", "MEASURE",
                "Fluid and solid capacity across metric, US customary, and imperial.", volumeUnits, "Liter (L)", "US Gallon (gal)"));

        // 4. Mass / Weight
        List<UnitItem> massUnits = Arrays.asList(
                new UnitItem("Kilogram", "kg", 1.0),
                new UnitItem("Gram", "g", 0.001),
                new UnitItem("Milligram", "mg", 1e-6),
                new UnitItem("Metric Ton", "t", 1000.0),
                new UnitItem("Pound", "lb", 0.45359237),
                new UnitItem("Ounce", "oz", 0.028349523125),
                new UnitItem("Stone", "st", 6.35029318),
                new UnitItem("Carat", "ct", 0.0002)
        );
        allCategories.add(new CategoryData("Mass / Weight", "⚖️", "MEASURE",
                "Weight and mass conversion with precise multi-unit comparison.", massUnits, "Kilogram (kg)", "Pound (lb)"));

        // 5. Temperature
        List<UnitItem> tempUnits = Arrays.asList(
                new UnitItem("Celsius", "°C", 1.0),
                new UnitItem("Fahrenheit", "°F", 1.0),
                new UnitItem("Kelvin", "K", 1.0),
                new UnitItem("Rankine", "°R", 1.0)
        );
        allCategories.add(new CategoryData("Temperature", "🌡️", "MEASURE",
                "Thermal conversion with key landmark reference points.", tempUnits, "Celsius (°C)", "Fahrenheit (°F)"));

        // 6. Speed
        List<UnitItem> speedUnits = Arrays.asList(
                new UnitItem("Meter per second", "m/s", 1.0),
                new UnitItem("Kilometer per hour", "km/h", 1.0 / 3.6),
                new UnitItem("Mile per hour", "mph", 0.44704),
                new UnitItem("Knot", "kn", 0.5144444444444445),
                new UnitItem("Foot per second", "ft/s", 0.3048)
        );
        allCategories.add(new CategoryData("Speed", "🏃", "MEASURE",
                "Velocity conversions with real-world motion benchmarks.", speedUnits, "Kilometer per hour (km/h)", "Mile per hour (mph)"));

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
                new UnitItem("Year (365d)", "yr", 31536000.0)
        );
        allCategories.add(new CategoryData("Time / Duration", "⏱️", "MEASURE",
                "Time intervals from microseconds to calendar years.", timeUnits, "Hour (h)", "Minute (min)"));

        // 8. Pressure
        List<UnitItem> pressureUnits = Arrays.asList(
                new UnitItem("Pascal", "Pa", 1.0),
                new UnitItem("Kilopascal", "kPa", 1000.0),
                new UnitItem("Bar", "bar", 100000.0),
                new UnitItem("Millibar", "mbar", 100.0),
                new UnitItem("Atmosphere", "atm", 101325.0),
                new UnitItem("PSI", "psi", 6894.757293),
                new UnitItem("Torr / mmHg", "Torr", 133.322368)
        );
        allCategories.add(new CategoryData("Pressure", "🎈", "MEASURE",
                "Atmospheric, hydraulic, and mechanical pressure comparisons.", pressureUnits, "Bar (bar)", "PSI (psi)"));

        // 9. Energy
        List<UnitItem> energyUnits = Arrays.asList(
                new UnitItem("Joule", "J", 1.0),
                new UnitItem("Kilojoule", "kJ", 1000.0),
                new UnitItem("Calorie", "cal", 4.184),
                new UnitItem("Kilocalorie", "kcal", 4184.0),
                new UnitItem("Watt-hour", "Wh", 3600.0),
                new UnitItem("Kilowatt-hour", "kWh", 3.6e6),
                new UnitItem("British Thermal Unit", "BTU", 1055.056),
                new UnitItem("Foot-pound", "ft-lb", 1.355818)
        );
        allCategories.add(new CategoryData("Energy", "⚡", "CALCULATE",
                "Work, thermal, and electrical energy conversion.", energyUnits, "Kilowatt-hour (kWh)", "Kilojoule (kJ)"));

        // 10. Power
        List<UnitItem> powerUnits = Arrays.asList(
                new UnitItem("Watt", "W", 1.0),
                new UnitItem("Kilowatt", "kW", 1000.0),
                new UnitItem("Megawatt", "MW", 1e6),
                new UnitItem("Horsepower", "hp", 745.699872),
                new UnitItem("Metric Horsepower", "PS", 735.49875),
                new UnitItem("Foot-pound per min", "ft-lb/min", 0.0225969658)
        );
        allCategories.add(new CategoryData("Power", "🔋", "CALCULATE",
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
                new UnitItem("Gigabit", "Gb", 134217728.0)
        );
        allCategories.add(new CategoryData("Data", "💾", "CALCULATE",
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
                new UnitItem("Mexican Peso", "MXN", 0.050)
        );
        allCategories.add(new CategoryData("Currency", "💱", "CALCULATE",
                "Global currency reference conversion rates with live country flags.", currencyUnits, "US Dollar (USD)", "Euro (EUR)"));
    }

    private void buildSidebar() {
        if (measureCategoryBox != null) measureCategoryBox.getChildren().clear();
        if (calculateCategoryBox != null) calculateCategoryBox.getChildren().clear();
        categoryButtons.clear();

        for (CategoryData cat : allCategories) {
            Button btn = new Button(cat.icon + "  " + cat.name);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.getStyleClass().add("category-btn");
            btn.setOnAction(e -> selectCategory(cat));
            categoryButtons.put(cat.name, btn);

            if ("MEASURE".equalsIgnoreCase(cat.section)) {
                if (measureCategoryBox != null) measureCategoryBox.getChildren().add(btn);
            } else {
                if (calculateCategoryBox != null) calculateCategoryBox.getChildren().add(btn);
            }
        }
    }

    private void selectCategory(CategoryData cat) {
        this.currentCategory = cat;

        // Highlight active category button
        for (Map.Entry<String, Button> entry : categoryButtons.entrySet()) {
            Button b = entry.getValue();
            if (entry.getKey().equals(cat.name)) {
                if (!b.getStyleClass().contains("category-btn-active")) {
                    b.getStyleClass().add("category-btn-active");
                }
            } else {
                b.getStyleClass().remove("category-btn-active");
            }
        }

        // Update headers
        if (breadcrumbLabel != null) breadcrumbLabel.setText("Convert  /  " + cat.name);
        if (categoryIconLabel != null) categoryIconLabel.setText(cat.icon);
        if (titleLabel != null) titleLabel.setText(cat.name + " converter");

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

        updateCardFlags();
        convertUnits();
    }

    private UnitItem findUnitByDisplay(CategoryData cat, String display) {
        if (cat == null || display == null) return null;
        for (UnitItem u : cat.units) {
            if (u.getDisplay().equals(display)) return u;
        }
        return null;
    }

    private double convert(double input, String category, UnitItem from, UnitItem to) {
        if (from == null || to == null) return 0;
        if (from.equals(to)) return input;

        if ("Temperature".equalsIgnoreCase(category)) {
            // First convert from -> Celsius
            double c = input;
            switch (from.symbol) {
                case "°F": c = (input - 32.0) * (5.0 / 9.0); break;
                case "K":  c = input - 273.15; break;
                case "°R": c = (input - 491.67) * (5.0 / 9.0); break;
                case "°C": default: c = input; break;
            }
            // Then convert Celsius -> to
            switch (to.symbol) {
                case "°F": return (c * (9.0 / 5.0)) + 32.0;
                case "K":  return c + 273.15;
                case "°R": return (c + 273.15) * (9.0 / 5.0);
                case "°C": default: return c;
            }
        } else if ("Currency".equalsIgnoreCase(category)) {
            // Real-time currency exchange rates (USD based rates)
            double rateFrom = liveRates.getOrDefault(from.symbol, 1.0);
            double rateTo = liveRates.getOrDefault(to.symbol, 1.0);
            if (rateFrom <= 0) rateFrom = 1.0;
            return input * (rateTo / rateFrom);
        } else {
            // Linear conversions via base factor
            double baseValue = input * from.factor;
            return baseValue / to.factor;
        }
    }

    public static String formatNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "Invalid";
        if (Math.abs(value) < 1e-12) return "0";
        if (Math.abs(value) >= 1e11 || (Math.abs(value) < 1e-5 && Math.abs(value) > 0)) {
            return String.format(Locale.US, "%.5e", value).replace("e+0", "e+").replace("e-0", "e-");
        }
        DecimalFormat df = new DecimalFormat("#,##0.######", new DecimalFormatSymbols(Locale.US));
        return df.format(value);
    }

    private void convertUnits() {
        if (inputTextField == null || outputTextField == null || currentCategory == null) return;

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

            if (from == null || to == null) return;

            double result = convert(inputVal, currentCategory.name, from, to);
            outputTextField.setText(formatNumber(result));

            // Record history
            String record = inputStr + " " + from.symbol + " = " + formatNumber(result) + " " + to.symbol;
            if (conversionHistory.isEmpty() || !conversionHistory.get(conversionHistory.size() - 1).equals(record)) {
                conversionHistory.add(record);
                if (conversionHistory.size() > 50) conversionHistory.remove(0);
            }

            updateCardFlags();
            drawVisualizer();
            updateEquivalentsTable(inputVal);

        } catch (NumberFormatException e) {
            outputTextField.setText("Invalid");
        }
    }

    private void drawVisualizer() {
        if (visualizerPane == null || currentCategory == null) return;
        visualizerPane.getChildren().clear();

        UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
        UnitItem to = findUnitByDisplay(currentCategory, toUnitBox != null ? toUnitBox.getValue() : null);

        if (from == null || to == null) return;

        if (visualizerUnitPairLabel != null) {
            visualizerUnitPairLabel.setText(from.symbol + "  ⇄  " + to.symbol);
        }

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
            if (norm <= 2.0) maxVal = 2.0 * factor;
            else if (norm <= 5.0) maxVal = 5.0 * factor;
            else maxVal = 10.0 * factor;
        }

        // 1. Horizontal Baseline Line
        Line baseline = new Line(x0, axisY, x1, axisY);
        baseline.setStroke(Color.web("rgba(255, 255, 255, 0.22)"));
        baseline.setStrokeWidth(1.5);
        visualizerPane.getChildren().add(baseline);

        // 2. 5 Major Ticks and Minor Ticks
        for (int i = 0; i <= 4; i++) {
            double xi = x0 + i * (w / 4.0);
            double ti = i * (maxVal / 4.0);

            // Major tick line
            Line majorTick = new Line(xi, axisY - 10, xi, axisY + 10);
            majorTick.setStroke(Color.web("rgba(255, 255, 255, 0.45)"));
            majorTick.setStrokeWidth(1.5);
            visualizerPane.getChildren().add(majorTick);

            // Top Label (From unit value)
            Label topLbl = new Label(formatNumber(ti));
            topLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 11px; -fx-font-weight: 500;");
            topLbl.setLayoutX(xi - 25);
            topLbl.setLayoutY(axisY - 32);
            topLbl.setPrefWidth(50);
            topLbl.setAlignment(Pos.CENTER);
            visualizerPane.getChildren().add(topLbl);

            // Bottom Label (To unit converted value)
            double convTi = convert(ti, currentCategory.name, from, to);
            Label botLbl = new Label(formatNumber(convTi));
            botLbl.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.75); -fx-font-size: 11px; -fx-font-weight: 500;");
            botLbl.setLayoutX(xi - 35);
            botLbl.setLayoutY(axisY + 16);
            botLbl.setPrefWidth(70);
            botLbl.setAlignment(Pos.CENTER);
            visualizerPane.getChildren().add(botLbl);

            // 3 minor ticks between each major tick
            if (i < 4) {
                for (int k = 1; k <= 3; k++) {
                    double xMinor = xi + k * (w / 16.0);
                    Line minorTick = new Line(xMinor, axisY - 5, xMinor, axisY + 5);
                    minorTick.setStroke(Color.web("rgba(255, 255, 255, 0.2)"));
                    minorTick.setStrokeWidth(1.0);
                    visualizerPane.getChildren().add(minorTick);
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
                pinX - 5.0, axisY
        );
        diamond.setFill(Color.web("#00D2FF"));
        diamond.setEffect(new DropShadow(8, Color.web("#00D2FF")));

        visualizerPane.getChildren().addAll(pinLine, diamond);

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
                    new Stop(1.0, Color.web("#FF3366"))
            );
            gradBar.setFill(grad);
            visualizerPane.getChildren().add(gradBar);

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

            visualizerPane.getChildren().addAll(freezeLbl, roomLbl, boilLbl);

        } else if ("Speed".equalsIgnoreCase(currentCategory.name)) {
            visualizerPane.setPrefHeight(125);
            double barY = axisY + 42.0;
            Label hint = new Label("Context:  🚶 Walking ~5 km/h   •   🚗 Highway ~100 km/h   •   ✈️ Jet ~900 km/h   •   ⚡ Mach 1 ~1235 km/h");
            hint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 11px;");
            hint.setLayoutX(x0);
            hint.setLayoutY(barY);
            visualizerPane.getChildren().add(hint);

        } else if ("Data".equalsIgnoreCase(currentCategory.name)) {
            visualizerPane.setPrefHeight(125);
            double barY = axisY + 42.0;
            Label hint = new Label("Hierarchy:  1 TB = 1,024 GB = 1,048,576 MB = 1,073,741,824 KB  (Base 2)");
            hint.setStyle("-fx-text-fill: rgba(255, 255, 255, 0.4); -fx-font-size: 11px;");
            hint.setLayoutX(x0);
            hint.setLayoutY(barY);
            visualizerPane.getChildren().add(hint);
        } else {
            visualizerPane.setPrefHeight(110);
        }
    }

    private void updateEquivalentsTable(double inputVal) {
        if (equivalentsContainer == null || currentCategory == null) return;
        equivalentsContainer.getChildren().clear();

        UnitItem from = findUnitByDisplay(currentCategory, fromUnitBox != null ? fromUnitBox.getValue() : null);
        if (from == null) return;

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
        if (fromUnitBox == null || toUnitBox == null) return;
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
        if (outputTextField == null || copyResultBtn == null) return;
        String res = outputTextField.getText();
        if (res == null || res.trim().isEmpty() || res.equals("Invalid")) return;

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
        dialog.setHeaderText("Recent Conversions");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: #1E1C27; -fx-text-fill: white;");

        VBox contentBox = new VBox(8);
        contentBox.setStyle("-fx-padding: 10;");

        if (conversionHistory.isEmpty()) {
            Label empty = new Label("No recent conversions.");
            empty.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 13px;");
            contentBox.getChildren().add(empty);
        } else {
            for (int i = conversionHistory.size() - 1; i >= 0; i--) {
                Label item = new Label("•  " + conversionHistory.get(i));
                item.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                contentBox.getChildren().add(item);
            }
        }

        ScrollPane sp = new ScrollPane(contentBox);
        sp.setPrefHeight(250);
        sp.setPrefWidth(320);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        pane.setContent(sp);

        dialog.showAndWait();
    }

    @FXML
    public void toggleSidebar() {
        if (sidebar == null) return;
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
