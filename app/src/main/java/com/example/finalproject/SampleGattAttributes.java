package com.example.finalproject;

import java.util.HashMap;


public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "0000ffe1-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("0000ffe1-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
