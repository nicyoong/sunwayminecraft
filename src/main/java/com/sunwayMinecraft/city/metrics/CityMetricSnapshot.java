package com.sunwayMinecraft.city.metrics;

import java.util.Map;

public record CityMetricSnapshot(Map<String, Double> metrics) {
    public double getMetric(String key) {
        return metrics.getOrDefault(key, 0.0);
    }
}
