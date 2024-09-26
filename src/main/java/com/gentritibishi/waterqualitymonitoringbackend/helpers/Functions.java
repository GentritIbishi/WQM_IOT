package com.gentritibishi.waterqualitymonitoringbackend.helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.gentritibishi.waterqualitymonitoringbackend.entities.SensorData;
import org.json.JSONObject;

public class Functions {
    // Method to get address from latitude and longitude using Nominatim API
    public static String getAddress(double latitude, double longitude) {
        if (latitude == 42.619447 && longitude == 21.234465) {
            return "Liqeni I Badovcit";
        } else if (latitude == 42.821412 && longitude == 21.308285) {
            return "Liqeni I Batllaves";
        } else if (latitude == 42.961875 && longitude == 20.570339) {
            return "Liqeni I Ujmanit";
        } else if (latitude == 42.486777 && longitude == 20.422350) {
            return "Liqeni I Radoniqit";
        } else {
            return Functions.getAddress(latitude, longitude); // default case for other locations
        }
    }

    public static boolean checkForAnomaly(SensorData data) {
        double tempValue = Double.parseDouble(data.getTemperature().replace(" Celsius", ""));

        // Condition 1: flow should be less than 0 or greater than 2
        if (data.getFlow() < 0 || data.getFlow() > 2) {
            return true;
        }

        // Condition 2: turbidity should be less than 10 or greater than 1000
        if (data.getTurbidity() < 10 || data.getTurbidity() > 1000) {
            return true;
        }

        // Condition 3: temperature should be under -10 or greater than 100
        if (tempValue < -10 || tempValue > 100) {
            return true;
        }

        // Condition 4: pH should be out of the 0 to 14 range
        if (data.getPh() < 0 || data.getPh() > 14) {
            return true;
        }

        // If none of the conditions are met, it's not an anomaly
        return false;
    }

    public static boolean checkIfNotDrinkable(SensorData data) {
        double tempValue = Double.parseDouble(data.getTemperature().replace(" Celsius", ""));

        // Condition 1: flow should be between 0 and 2 (so outside this range is not drinkable)
        if (data.getFlow() < 0 || data.getFlow() > 2) {
            return true;
        }

        // Condition 2: turbidity should be between 0 and 1000 (so outside this range is not drinkable)
        if (data.getTurbidity() < 0 || data.getTurbidity() > 1000) {
            return true;
        }

        // Condition 3: temperature should be between -10 and 100 (so outside this range is not drinkable)
        if (tempValue < -10 || tempValue > 100) {
            return true;
        }

        // Condition 4: pH should be between 0 and 14 (so outside this range is not drinkable)
        if (data.getPh() < 0 || data.getPh() > 14) {
            return true;
        }

        // If none of the conditions are violated, the water is considered drinkable
        return false;
    }

}
