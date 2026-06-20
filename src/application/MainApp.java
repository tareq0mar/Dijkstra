package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Graph  graph;
    private int    selectedSrc = -1;
    private int    selectedDst = -1;
    private int[]  currentPath = new int[0];

    private int[] comboToGraph;
    private int[] graphToCombo;

    private Canvas           canvas;
    private ComboBox<String> cbSrc, cbDst;
    private TextArea         taPath;
    private Label            lblDist;
    private Label            lblStatus;

    private double minLat, maxLat, minLon, maxLon;

    private double mapW = 720;
    private double mapH = 800;
    private static final double PANEL_W = 260;
    private static final double PADDING = 44;
    private static final double MIN_W   = 820;
    private static final double MIN_H   = 580;

    private Image mapImage = null;

    private static final Color COL_BG    = Color.rgb(10, 12, 18);
    private static final Color COL_GREEN = Color.rgb(0, 200, 80);
    private static final Color COL_CYAN  = Color.rgb(0, 180, 220);
    private static final Color COL_RED   = Color.rgb(220, 60, 80);

    @Override
    public void start(Stage stage) {
        try {
            graph = GraphLoader.load("gaza_map.txt");
        } catch (Exception e) {
            showError("Cannot load gaza_map.txt:\n" + e.getMessage());
            return;
        }

        buildIndexMaps();
        computeMapBounds();
        loadMapImage();
        projectCities();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a;");
        root.setTop(buildTitleBar());
        root.setCenter(buildMapPane());

        VBox panel = buildRightPanel();
        panel.setPrefWidth(PANEL_W);
        root.setRight(panel);

        Scene scene = new Scene(root, mapW + PANEL_W, mapH + 44);
        scene.setFill(COL_BG);

        stage.setTitle("Gaza Strip — Dijkstra Path Visualizer");
        stage.setScene(scene);
        stage.setMinWidth(MIN_W);
        stage.setMinHeight(MIN_H);
        stage.setResizable(true);
        stage.show();

        scene.widthProperty().addListener((obs, ov, nv) -> onResize(nv.doubleValue(), scene.getHeight()));
        scene.heightProperty().addListener((obs, ov, nv) -> onResize(scene.getWidth(), nv.doubleValue()));

        setStatus("Ready. Select source and destination.");
        drawMap();
    }

    private void buildIndexMaps() {
        int V = graph.cityCount();
        graphToCombo = new int[V];
        int cityCount = 0;
        for (int i = 0; i < V; i++) {
            if (!graph.getCity(i).isIntersection) cityCount++;
        }
        comboToGraph = new int[cityCount];
        int pos = 0;
        for (int i = 0; i < V; i++) {
            if (!graph.getCity(i).isIntersection) {
                comboToGraph[pos] = i;
                graphToCombo[i]   = pos;
                pos++;
            } else {
                graphToCombo[i] = -1;
            }
        }
    }

    private void onResize(double sceneW, double sceneH) {
        mapW = Math.max(200, sceneW - PANEL_W);
        mapH = Math.max(200, sceneH - 44);
        canvas.setWidth(mapW);
        canvas.setHeight(mapH);
        projectCities();
        drawMap();
    }

    private HBox buildTitleBar() {
        Label title = new Label("Dijkstra Path Visualizer — Gaza Strip");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setStyle("-fx-text-fill: #c8c8c8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, title, spacer);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 16));
        bar.setPrefHeight(44);
        bar.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #2e2e2e; -fx-border-width: 0 0 1 0;");
        return bar;
    }

    private StackPane buildMapPane() {
        canvas = new Canvas(mapW, mapH);
        canvas.setOnMouseClicked(this::onMapClick);

        StackPane pane = new StackPane(canvas);
        pane.setStyle("-fx-background-color: #0a0c12; -fx-border-color: #2e2e2e; -fx-border-width: 1;");
        return pane;
    }

    private VBox buildRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16, 12, 16, 12));
        panel.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #2e2e2e; -fx-border-width: 0 0 0 1;");
        panel.setAlignment(Pos.TOP_CENTER);

        Label hdr = sectionLabel("ROUTE CONFIG");

        Label lSrc = fieldLabel("Source");
        cbSrc = styledCombo();
        Label lDst = fieldLabel("Target");
        cbDst = styledCombo();

        for (int pos = 0; pos < comboToGraph.length; pos++) {
            String n = graph.getCity(comboToGraph[pos]).name.replace('_', ' ');
            cbSrc.getItems().add(n);
            cbDst.getItems().add(n);
        }

        cbSrc.setOnAction(e -> {
            int idx = cbSrc.getSelectionModel().getSelectedIndex();
            if (idx >= 0) { selectedSrc = comboToGraph[idx]; currentPath = new int[0]; drawMap(); }
        });
        cbDst.setOnAction(e -> {
            int idx = cbDst.getSelectionModel().getSelectedIndex();
            if (idx >= 0) { selectedDst = comboToGraph[idx]; currentPath = new int[0]; drawMap(); }
        });

        Button btnRun = new Button("Run");
        styleButton(btnRun);
        btnRun.setOnAction(e -> runDijkstra());

        Button btnClear = new Button("Clear");
        styleButton(btnClear);
        btnClear.setOnAction(e -> {
            selectedSrc = -1; selectedDst = -1;
            currentPath = new int[0];
            cbSrc.getSelectionModel().clearSelection();
            cbDst.getSelectionModel().clearSelection();
            taPath.clear();
            lblDist.setText("—");
            setStatus("Ready. Select source and destination.");
            drawMap();
        });

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #2e2e2e;");

        Label lLog = sectionLabel("PATH");

        taPath = new TextArea();
        taPath.setEditable(false);
        taPath.setPrefHeight(160);
        taPath.setWrapText(true);
        taPath.setFont(Font.font("Segoe UI", 10));
        taPath.setStyle(
            "-fx-control-inner-background: #161616; " +
            "-fx-text-fill: #c8c8c8; " +
            "-fx-font-size: 10; " +
            "-fx-border-color: #2e2e2e; " +
            "-fx-background-color: transparent;"
        );

        Label lDistLabel = fieldLabel("Distance");
        lblDist = new Label("—");
        lblDist.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblDist.setStyle("-fx-text-fill: #d4d4d4;");

        lblStatus = new Label("Ready.");
        lblStatus.setWrapText(true);
        lblStatus.setFont(Font.font("Segoe UI", 10));
        lblStatus.setStyle("-fx-text-fill: #7a7a7a;");

        Label lHint = new Label("Click map: 1st = source, 2nd = target");
        lHint.setFont(Font.font("Segoe UI", 9));
        lHint.setStyle("-fx-text-fill: #4a4a4a; -fx-wrap-text: true;");

        panel.getChildren().addAll(
            hdr,
            lSrc, cbSrc,
            lDst, cbDst,
            btnRun, btnClear,
            sep,
            lLog, taPath,
            lDistLabel, lblDist,
            lblStatus, lHint
        );
        return panel;
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text.charAt(0) + text.substring(1).toLowerCase());
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        l.setStyle("-fx-text-fill: #7a7a7a;");
        return l;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", 10));
        l.setStyle("-fx-text-fill: #5a5a5a;");
        return l;
    }

    private ComboBox<String> styledCombo() {
        ComboBox<String> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
            "-fx-background-color: #161616; " +
            "-fx-text-fill: #c8c8c8; " +
            "-fx-border-color: #2e2e2e; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-font-size: 11;"
        );
        return cb;
    }

    private void styleButton(Button b) {
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(
            "-fx-background-color: #252525; " +
            "-fx-text-fill: #c8c8c8; " +
            "-fx-font-family: 'Segoe UI'; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11; " +
            "-fx-padding: 7 0; " +
            "-fx-cursor: hand; " +
            "-fx-border-color: #3a3a3a; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 2; " +
            "-fx-background-radius: 2;"
        );
    }

    private void runDijkstra() {
        if (selectedSrc < 0 || selectedDst < 0) {
            setStatus("Select both source and target.");
            return;
        }
        if (selectedSrc == selectedDst) {
            setStatus("Source and target must differ.");
            return;
        }

        setStatus("Computing shortest path...");

        long t0 = System.nanoTime();
        currentPath = graph.shortestPath(selectedSrc, selectedDst);
        long us = (System.nanoTime() - t0) / 1_000;

        if (currentPath.length == 0) {
            setStatus("No path found.");
            taPath.setText("No path.");
            lblDist.setText("∞");
            drawMap();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentPath.length; i++) {
            City c = graph.getCity(currentPath[i]);
            if (!c.isIntersection) {
                if (sb.length() > 0) sb.append(" →\n");
                sb.append(c.name.replace('_', ' '));
            }
        }
        taPath.setText(sb.toString());

        double km = graph.distTo(selectedDst);
        lblDist.setText(String.format("%.2f km", km));
        setStatus(String.format("Done: %d hops | %d μs", currentPath.length - 1, us));

        drawMap();
    }

    private void drawMap() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.clearRect(0, 0, w, h);
        gc.setFill(COL_BG);
        gc.fillRect(0, 0, w, h);

        LinearGradient grad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(0, 20, 40, 0.12)),
            new Stop(1, Color.rgb(0, 0, 0, 0.0)));
        gc.setFill(grad);
        gc.fillRect(0, 0, w, h);

        drawMapImage(gc, w, h);
        drawEdges(gc);
        drawPath(gc);
        drawNodes(gc);
        drawCornerUI(gc, w, h);
    }

    private void drawMapImage(GraphicsContext gc, double w, double h) {
        if (mapImage == null || mapImage.isError()) return;

        gc.save();
        gc.setGlobalAlpha(0.78);
        gc.drawImage(mapImage, 0, 0, w, h);
        gc.restore();
    }

    private void drawEdges(GraphicsContext gc) {
        gc.setStroke(Color.rgb(0, 100, 160, 0.28));
        gc.setLineWidth(1.2);
        for (int u = 0; u < graph.cityCount(); u++) {
            City cu = graph.getCity(u);
            DynArray<Edge> nbrs = graph.neighbors(u);
            for (int i = 0; i < nbrs.size(); i++) {
                int v = nbrs.get(i).v;
                if (v > u) {
                    City cv = graph.getCity(v);
                    gc.strokeLine(cu.x, cu.y, cv.x, cv.y);
                }
            }
        }
    }

    private void drawPath(GraphicsContext gc) {
        if (currentPath.length < 2) return;

        gc.setStroke(Color.rgb(0, 200, 80, 0.9));
        gc.setLineWidth(3.0);
        for (int i = 0; i < currentPath.length - 1; i++) {
            City ca = graph.getCity(currentPath[i]);
            City cb = graph.getCity(currentPath[i + 1]);
            gc.strokeLine(ca.x, ca.y, cb.x, cb.y);
        }
    }

    private void drawNodes(GraphicsContext gc) {
        final double R_CITY = 6.5;
        final double R_INT  = 3.0;

        for (int i = 0; i < graph.cityCount(); i++) {
            City c = graph.getCity(i);
            boolean isSrc  = (i == selectedSrc);
            boolean isDst  = (i == selectedDst);
            boolean onPath = onPath(i);

            if (c.isIntersection) {
                double r = onPath ? R_INT + 1.5 : R_INT;
                gc.setFill(onPath ? Color.rgb(0, 200, 80, 0.9) : Color.rgb(60, 100, 140, 0.6));
                gc.fillOval(c.x - r, c.y - r, r * 2, r * 2);
            } else {
                double r = R_CITY;
                if (isSrc)       gc.setFill(Color.rgb(0, 200, 80));
                else if (isDst)  gc.setFill(Color.rgb(220, 60, 80));
                else if (onPath) gc.setFill(Color.rgb(0, 180, 220));
                else             gc.setFill(Color.rgb(30, 80, 130));
                gc.fillOval(c.x - r, c.y - r, r * 2, r * 2);

                gc.setStroke(isSrc ? Color.rgb(0, 200, 80)
                            : isDst ? Color.rgb(220, 60, 80)
                            : onPath ? Color.rgb(0, 180, 220)
                            : Color.rgb(0, 100, 180, 0.8));
                gc.setLineWidth(isSrc || isDst ? 2.0 : 1.0);
                gc.strokeOval(c.x - r, c.y - r, r * 2, r * 2);

                gc.setFont(Font.font("Courier New", FontWeight.BOLD, 9));
                String label = c.name.replace('_', ' ');
                double lx = c.x + r + 4;
                double ly = c.y + 4;

                gc.setFill(Color.rgb(0, 0, 0, 0.7));
                gc.fillText(label, lx + 1, ly + 1);

                gc.setFill(isSrc ? Color.rgb(0, 200, 80)
                          : isDst ? Color.rgb(220, 80, 100)
                          : onPath ? Color.rgb(0, 180, 220)
                          : Color.rgb(120, 180, 220));
                gc.fillText(label, lx, ly);
            }
        }
    }

    private void drawCornerUI(GraphicsContext gc, double w, double h) {
    }

    private void onMapClick(javafx.scene.input.MouseEvent ev) {
        double mx = ev.getX(), my = ev.getY();
        final double HIT_R = 15;
        int hit = -1;
        double best = Double.MAX_VALUE;

        for (int i = 0; i < graph.cityCount(); i++) {
            if (graph.getCity(i).isIntersection) continue;
            City c = graph.getCity(i);
            double d = Math.hypot(c.x - mx, c.y - my);
            if (d < HIT_R && d < best) { best = d; hit = i; }
        }
        if (hit < 0) return;

        int comboPos = graphToCombo[hit];

        if (selectedSrc < 0) {
            selectedSrc = hit;
            cbSrc.getSelectionModel().select(comboPos);
            setStatus("Source: " + graph.getCity(hit).name.replace('_', ' '));
        } else if (selectedDst < 0) {
            selectedDst = hit;
            cbDst.getSelectionModel().select(comboPos);
            setStatus("Target: " + graph.getCity(hit).name.replace('_', ' ') + " — press Run.");
        } else {
            selectedSrc = hit;
            selectedDst = -1;
            currentPath = new int[0];
            cbSrc.getSelectionModel().select(comboPos);
            cbDst.getSelectionModel().clearSelection();
            setStatus("Source reset: " + graph.getCity(hit).name.replace('_', ' '));
        }
        drawMap();
    }

    private boolean onPath(int idx) {
        for (int p : currentPath) if (p == idx) return true;
        return false;
    }

    private void setStatus(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }

    private void loadMapImage() {
        java.io.File osmFile = new java.io.File(MapDownloader.OUT_FILE);
        if (osmFile.exists()) {
            try {
                mapImage = new Image(osmFile.toURI().toString(), false);
                MapDownloader.computeExactBounds();
                minLat = MapDownloader.minLat;
                maxLat = MapDownloader.maxLat;
                minLon = MapDownloader.minLon;
                maxLon = MapDownloader.maxLon;
                return;
            } catch (Exception ignored) {}
        }

        java.io.File wikiFile = new java.io.File("Location_map_Palestine_Gaza_Strip.png");
        if (wikiFile.exists()) {
            try { mapImage = new Image(wikiFile.toURI().toString(), false); return; }
            catch (Exception ignored) {}
        }

        for (String name : new String[]{ MapDownloader.OUT_FILE, "Location_map_Palestine_Gaza_Strip.png" }) {
            try {
                java.io.InputStream is = getClass().getResourceAsStream("/" + name);
                if (is != null) { mapImage = new Image(is); is.close(); return; }
            } catch (Exception ignored) {}
        }

        setStatus("Map not found — downloading from OpenStreetMap...");
        Thread dl = new Thread(() -> {
            boolean ok = MapDownloader.downloadGazaMap(msg ->
                javafx.application.Platform.runLater(() -> setStatus(msg)));

            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    try {
                        java.io.File f = new java.io.File(MapDownloader.OUT_FILE);
                        mapImage = new Image(f.toURI().toString(), false);
                        minLat = MapDownloader.minLat;
                        maxLat = MapDownloader.maxLat;
                        minLon = MapDownloader.minLon;
                        maxLon = MapDownloader.maxLon;
                        projectCities();
                        drawMap();
                        setStatus("Map downloaded. Ready.");
                    } catch (Exception e) {
                        setStatus("Map download error: " + e.getMessage());
                    }
                } else {
                    setStatus("Download failed — running without map background.");
                }
            });
        });
        dl.setDaemon(true);
        dl.start();
    }

    private void computeMapBounds() {
        minLat = 31.17;
        maxLat = 31.63;
        minLon = 34.14;
        maxLon = 34.61;
    }

    private void projectCities() {
        double margin   = 8;
        double usableW  = mapW - 2 * margin;
        double usableH  = mapH - 2 * margin;
        double lonRange = maxLon - minLon;

        double mercYMin  = mercatorY(minLat);
        double mercYMax  = mercatorY(maxLat);
        double mercRange = mercYMax - mercYMin;

        for (int i = 0; i < graph.cityCount(); i++) {
            City c = graph.getCity(i);
            c.x = margin + (c.lon - minLon) / lonRange * usableW;
            double mY = mercatorY(c.lat);
            c.y = margin + (1.0 - (mY - mercYMin) / mercRange) * usableH;
        }
    }

    private double mercatorY(double lat) {
        double r = Math.toRadians(lat);
        return Math.log(Math.tan(r) + 1.0 / Math.cos(r));
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
