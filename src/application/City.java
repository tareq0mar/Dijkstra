package application;

public class City {
    public final String name;
    public final double lat;
    public final double lon;
    public final boolean isIntersection;

    public double x;
    public double y;

    public City(String name, double lat, double lon) {
        this.name = name;
        this.lat  = lat;
        this.lon  = lon;
        this.isIntersection = name.startsWith("Int_");
    }

    @Override
    public String toString() { return name; }
}
