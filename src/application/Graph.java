package application;

public class Graph {

    private final City[]           cities;
    private final DynArray<Edge>[] adj;
    private final int              V;

    private final double[] dist;
    private final int[]    prev;
    private final boolean[]settled;

    private final DynArray<Integer> touched;

    @SuppressWarnings("unchecked")
    public Graph(City[] cities) {
        this.cities  = cities;
        this.V       = cities.length;
        this.adj     = new DynArray[V];
        for (int i = 0; i < V; i++) adj[i] = new DynArray<>(4);

        dist    = new double[V];
        prev    = new int[V];
        settled = new boolean[V];
        touched = new DynArray<>(V);

        for (int i = 0; i < V; i++) dist[i] = Double.MAX_VALUE;
    }

    public void addEdge(int u, int v) {
        double w = haversine(cities[u], cities[v]);
        Edge eu = new Edge(u, v); eu.weight = w;
        Edge ev = new Edge(v, u); ev.weight = w;
        adj[u].add(eu);
        adj[v].add(ev);
    }

    public int cityCount()  { return V; }
    public City getCity(int i) { return cities[i]; }
    public DynArray<Edge> neighbors(int u) { return adj[u]; }

    public int[] shortestPath(int src, int dst) {
        for (int i = 0; i < touched.size(); i++) {
            int idx = touched.get(i);
            dist[idx]    = Double.MAX_VALUE;
            prev[idx]    = -1;
            settled[idx] = false;
        }
        touched.clear();

        dist[src] = 0.0;
        prev[src] = -1;
        markTouched(src);

        MinHeap pq = new MinHeap(V);
        pq.insert(src, 0.0);

        while (!pq.isEmpty()) {
            MinHeap.Node node = pq.deleteMin();
            int u = node.cityIdx;

            if (settled[u]) continue;
            settled[u] = true;

            if (u == dst) break;

            DynArray<Edge> nbrs = adj[u];
            for (int i = 0; i < nbrs.size(); i++) {
                Edge e = nbrs.get(i);
                int w = e.v;
                if (settled[w]) continue;

                double newDist = dist[u] + e.weight;
                if (newDist < dist[w]) {
                    dist[w] = newDist;
                    prev[w] = u;
                    markTouched(w);

                    if (pq.contains(w)) {
                        pq.decreaseKey(w, newDist);
                    } else {
                        pq.insert(w, newDist);
                    }
                }
            }
        }

        if (dist[dst] == Double.MAX_VALUE) return new int[0];

        int len = 0;
        int cur = dst;
        while (cur != -1) { len++; cur = prev[cur]; }

        int[] path = new int[len];
        cur = dst;
        for (int i = len - 1; i >= 0; i--) {
            path[i] = cur;
            cur = prev[cur];
        }
        return path;
    }

    public double distTo(int v) { return dist[v]; }

    public static double haversine(City a, City b) {
        final double R = 6371.0;
        double dLat = Math.toRadians(b.lat - a.lat);
        double dLon = Math.toRadians(b.lon - a.lon);
        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double h = sinLat * sinLat
                 + Math.cos(Math.toRadians(a.lat))
                 * Math.cos(Math.toRadians(b.lat))
                 * sinLon * sinLon;
        return 2.0 * R * Math.asin(Math.sqrt(h));
    }

    private void markTouched(int idx) {
        if (dist[idx] != Double.MAX_VALUE || idx == (touched.size() > 0 ? touched.get(0) : -1))
            ;
        touched.add(idx);
    }
}
