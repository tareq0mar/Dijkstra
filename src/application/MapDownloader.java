package application;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;

public class MapDownloader {

    public static final String OUT_FILE = "gaza_map_bg.png";
    public static final int    ZOOM     = 12;
    private static final int   TILE_PX  = 256;

    private static final String OSM_URL   = "https://tile.openstreetmap.org/%d/%d/%d.png";
    private static final String ESRI_URL  = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/%d/%d/%d";
    private static final String USER_AGENT = "GazaDijkstraProject/1.0 (student-edu-BZU)";

    private static final double WANT_MIN_LAT = 31.17;
    private static final double WANT_MAX_LAT = 31.63;
    private static final double WANT_MIN_LON = 34.14;
    private static final double WANT_MAX_LON = 34.61;

    public static double minLat, maxLat, minLon, maxLon;

    public static boolean downloadGazaMap(java.util.function.Consumer<String> onProgress) {
        try {
            int xMin = lonToX(WANT_MIN_LON);
            int xMax = lonToX(WANT_MAX_LON);
            int yMin = latToY(WANT_MAX_LAT);
            int yMax = latToY(WANT_MIN_LAT);

            int cols  = xMax - xMin + 1;
            int rows  = yMax - yMin + 1;
            int total = cols * rows;

            log(onProgress, "Downloading " + total + " tiles (" + cols + " cols × " + rows + " rows) at zoom " + ZOOM + "...");

            BufferedImage canvas = new BufferedImage(
                cols * TILE_PX, rows * TILE_PX, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int done = 0;
            for (int ty = yMin; ty <= yMax; ty++) {
                for (int tx = xMin; tx <= xMax; tx++) {
                    BufferedImage tile = fetchTile(tx, ty);
                    if (tile != null) {
                        g.drawImage(tile,
                            (tx - xMin) * TILE_PX,
                            (ty - yMin) * TILE_PX, null);
                    }
                    done++;
                    Thread.sleep(80);
                }
                log(onProgress, "  Row " + (ty - yMin + 1) + "/" + rows + " done  (" + done + "/" + total + " tiles)");
            }
            g.dispose();

            ImageIO.write(canvas, "PNG", new File(OUT_FILE));

            minLon = xToLon(xMin);
            maxLon = xToLon(xMax + 1);
            maxLat = yToLat(yMin);
            minLat = yToLat(yMax + 1);

            log(onProgress, String.format(
                "Saved '%s'  (%dx%d px)%n  lat [%.5f – %.5f]  lon [%.5f – %.5f]",
                OUT_FILE, cols * TILE_PX, rows * TILE_PX,
                minLat, maxLat, minLon, maxLon));
            return true;

        } catch (Exception e) {
            log(onProgress, "Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static BufferedImage fetchTile(int tx, int ty) {
        BufferedImage img = fetchUrl(String.format(OSM_URL, ZOOM, tx, ty));
        if (img != null) return img;
        img = fetchUrl(String.format(ESRI_URL, ZOOM, ty, tx));
        return img;
    }

    private static BufferedImage fetchUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(15_000);
            if (conn.getResponseCode() != 200) return null;
            return ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    public static int lonToX(double lon) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << ZOOM));
    }

    public static int latToY(double lat) {
        double r = Math.toRadians(lat);
        double y = (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI) / 2.0;
        return (int) Math.floor(y * (1 << ZOOM));
    }

    public static double xToLon(int x) {
        return x * 360.0 / (1 << ZOOM) - 180.0;
    }

    public static double yToLat(int y) {
        double n = Math.PI - 2.0 * Math.PI * y / (double) (1 << ZOOM);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public static void computeExactBounds() {
        int xMin = lonToX(WANT_MIN_LON);
        int xMax = lonToX(WANT_MAX_LON);
        int yMin = latToY(WANT_MAX_LAT);
        int yMax = latToY(WANT_MIN_LAT);

        minLon = xToLon(xMin);
        maxLon = xToLon(xMax + 1);
        maxLat = yToLat(yMin);
        minLat = yToLat(yMax + 1);
    }

    private static void log(java.util.function.Consumer<String> sink, String msg) {
        System.out.println(msg);
        if (sink != null) sink.accept(msg);
    }

    public static void main(String[] args) {
        System.out.println("=== Gaza Strip Map Downloader ===");
        System.out.println("Using tile images — no OSM node limit applies.\n");

        boolean ok = downloadGazaMap(null);

        if (ok) {
            System.out.println("\nDone! Copy these values into MainApp.computeMapBounds():");
            System.out.printf("  minLat = %.6f;%n", minLat);
            System.out.printf("  maxLat = %.6f;%n", maxLat);
            System.out.printf("  minLon = %.6f;%n", minLon);
            System.out.printf("  maxLon = %.6f;%n", maxLon);
        } else {
            System.out.println("Download failed. Check your internet connection.");
        }
    }
}
