package application;

public class MinHeap {

    public static class Node {
        public double dist;
        public int    cityIdx;
        Node(double d, int c) { dist = d; cityIdx = c; }
    }

    private Node[] heap;
    private int[]  pos;
    private int    size;
    private final int capacity;

    public MinHeap(int capacity) {
        this.capacity = capacity;
        heap = new Node[capacity + 1];
        pos  = new int[capacity];
        for (int i = 0; i < capacity; i++) pos[i] = -1;
        size = 0;
    }

    public boolean isEmpty() { return size == 0; }

    public void insert(int cityIdx, double dist) {
        size++;
        heap[size] = new Node(dist, cityIdx);
        pos[cityIdx] = size;
        bubbleUp(size);
    }

    public Node deleteMin() {
        if (size == 0) return null;
        Node min = heap[1];
        pos[min.cityIdx] = -1;
        heap[1] = heap[size];
        if (size > 1) pos[heap[1].cityIdx] = 1;
        heap[size] = null;
        size--;
        if (size > 0) siftDown(1);
        return min;
    }

    public void decreaseKey(int cityIdx, double newDist) {
        int i = pos[cityIdx];
        if (i == -1) return;
        heap[i].dist = newDist;
        bubbleUp(i);
    }

    public boolean contains(int cityIdx) {
        return pos[cityIdx] != -1;
    }

    private void bubbleUp(int i) {
        while (i > 1) {
            int parent = i / 2;
            if (heap[parent].dist > heap[i].dist) {
                swap(i, parent);
                i = parent;
            } else break;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int smallest = i;
            int l = 2 * i, r = 2 * i + 1;
            if (l <= size && heap[l].dist < heap[smallest].dist) smallest = l;
            if (r <= size && heap[r].dist < heap[smallest].dist) smallest = r;
            if (smallest == i) break;
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        pos[heap[a].cityIdx] = b;
        pos[heap[b].cityIdx] = a;
        Node tmp = heap[a];
        heap[a] = heap[b];
        heap[b] = tmp;
    }
}
