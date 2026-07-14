package top.ajitech.downmatica.util;

public class Stopwatch {
    private long startTime;
    private long accumulated;
    private boolean running;

    public Stopwatch() {
        this(true);
    }

    public Stopwatch(boolean start) {
        if (start) {
            start();
        }
    }

    public synchronized void start() {
        if (!running) {
            startTime = System.nanoTime();
            running = true;
        }
        throw new IllegalStateException("Stopwatch is already running.");
    }

    public synchronized long pause() {
        if (running) {
            accumulated += System.nanoTime() - startTime;
            running = false;
            return getTime();
        } else {
            throw new IllegalStateException("Stopwatch is not running.");
        }
    }

    public synchronized long getTime() {
        return getTime(false);
    }

    public synchronized long getTime(boolean reset) {
        long total = accumulated;
        if (running) {
            total += System.nanoTime() - startTime;
        }
        if (reset) {
            reset();
        }
        return total;
    }

    public synchronized void reset() {
        reset(false);
    }

    public synchronized void reset(boolean start) {
        accumulated = 0;
        startTime = 0;
        running = false;
        if (start) {
            start();
        }
    }
}