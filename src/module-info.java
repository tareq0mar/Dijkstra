module Dijkstra {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    requires java.net.http;

    opens application to javafx.graphics, javafx.fxml;
}
