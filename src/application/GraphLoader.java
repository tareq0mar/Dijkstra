package application;

import java.io.*;

public class GraphLoader {

    public static Graph load(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line = nextLine(br);
        String[] header = line.trim().split("\\s+");
        int numCities = Integer.parseInt(header[0]);
        int numEdges  = Integer.parseInt(header[1]);

        City[] cities = new City[numCities];
        SimpleHashMap nameMap = new SimpleHashMap(numCities * 2);

        for (int i = 0; i < numCities; i++) {
            String[] parts = nextLine(br).trim().split("\\s+");
            String name = parts[0];
            double lat  = Double.parseDouble(parts[1]);
            double lon  = Double.parseDouble(parts[2]);
            cities[i] = new City(name, lat, lon);
            nameMap.put(name, i);
        }

        Graph g = new Graph(cities);

        for (int i = 0; i < numEdges; i++) {
            String[] parts = nextLine(br).trim().split("\\s+");
            int u = nameMap.get(parts[0]);
            int v = nameMap.get(parts[1]);
            g.addEdge(u, v);
        }

        br.close();
        return g;
    }

    private static String nextLine(BufferedReader br) throws IOException {
        String s;
        while ((s = br.readLine()) != null) {
            s = s.trim();
            if (!s.isEmpty() && !s.startsWith("#")) return s;
        }
        throw new IOException("Unexpected end of file");
    }

    static class SimpleHashMap {
        private String[] keys;
        private int[]    vals;
        private int      cap;
        private int      size;

        SimpleHashMap(int cap) {
            this.cap  = cap;
            keys = new String[cap];
            vals = new int[cap];
        }

        void put(String key, int val) {
            int i = slot(key);
            keys[i] = key;
            vals[i] = val;
            size++;
        }

        int get(String key) {
            int i = slot(key);
            if (keys[i] == null) throw new RuntimeException("City not found: " + key);
            return vals[i];
        }

        private int slot(String key) {
            int h = (key.hashCode() & 0x7fffffff) % cap;
            while (keys[h] != null && !keys[h].equals(key))
                h = (h + 1) % cap;
            return h;
        }
    }
}
