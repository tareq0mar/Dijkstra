package application;

@SuppressWarnings("unchecked")
public class DynArray<T> {
    private Object[] data;
    private int size;

    public DynArray() {
        data = new Object[4];
        size = 0;
    }

    public DynArray(int initialCapacity) {
        data = new Object[Math.max(initialCapacity, 1)];
        size = 0;
    }

    public void add(T item) {
        ensureCapacity();
        data[size++] = item;
    }

    public T get(int i) { return (T) data[i]; }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public void clear() { size = 0; }

    private void ensureCapacity() {
        if (size == data.length) {
            Object[] bigger = new Object[data.length * 2];
            for (int i = 0; i < size; i++) bigger[i] = data[i];
            data = bigger;
        }
    }
}
