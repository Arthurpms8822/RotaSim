package com.example.rotasim;

import java.util.List;

public class SpeedStatistics {
    public static double calculateAverageSpeed(List<Double> rawSpeedData) {
        double sum = 0.0;
        for (double speed : rawSpeedData) {
            sum += speed;
        }
        return sum / rawSpeedData.size();
    }

    public static double calculateStandardDeviation(List<Double> rawSpeedData) {
        double averageSpeed = calculateAverageSpeed(rawSpeedData);
        double sum = 0.0;
        for (double speed : rawSpeedData) {
            sum += Math.pow(speed - averageSpeed, 2);
        }
        return Math.sqrt(sum / rawSpeedData.size());
    }
}
