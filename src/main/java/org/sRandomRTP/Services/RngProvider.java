package org.sRandomRTP.Services;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;

public class RngProvider {

    public int nextInt(int boundExclusive) {
        return ThreadLocalRandom.current().nextInt(boundExclusive);
    }

    public int nextInt(int originInclusive, int boundExclusive) {
        if (boundExclusive <= originInclusive) {
            return originInclusive;
        }
        return ThreadLocalRandom.current().nextInt(originInclusive, boundExclusive);
    }

    public double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    public double nextDouble(double boundExclusive) {
        return ThreadLocalRandom.current().nextDouble(boundExclusive);
    }

    public double nextDouble(double originInclusive, double boundExclusive) {
        if (boundExclusive <= originInclusive) {
            return originInclusive;
        }
        return ThreadLocalRandom.current().nextDouble(originInclusive, boundExclusive);
    }

    public SplittableRandom deterministic(long seed) {
        return new SplittableRandom(seed);
    }

    public <T> T pickRandom(List<T> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(nextInt(values.size()));
    }
}
