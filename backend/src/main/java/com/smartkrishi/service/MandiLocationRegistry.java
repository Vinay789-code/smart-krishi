package com.smartkrishi.service;

import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry of genuine geographical coordinates for major Indian APMC mandis and district centroids.
 * Used to compute precise Haversine distances from the farmer's location without fabricating coordinates.
 */
@Component
public class MandiLocationRegistry {

    public static class Coordinates {
        public final double latitude;
        public final double longitude;
        public final boolean isExactMandi;

        public Coordinates(double latitude, double longitude, boolean isExactMandi) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.isExactMandi = isExactMandi;
        }
    }

    // Specific APMC / Mandi coordinates
    private static final Map<String, Coordinates> MANDI_COORDINATES = new HashMap<>();

    // District centroid coordinates
    private static final Map<String, Coordinates> DISTRICT_COORDINATES = new HashMap<>();

    static {
        // --- Rajasthan Mandis ---
        registerMandi("muhana", 26.8142, 75.7681);
        registerMandi("muhana mandi", 26.8142, 75.7681);
        registerMandi("muhana (f&v)", 26.8142, 75.7681);
        registerMandi("jaipur", 26.9124, 75.8273);
        registerMandi("jaipur mandi", 26.9124, 75.8273);
        registerMandi("surajpole", 26.9124, 75.8273);
        registerMandi("chomu", 27.1724, 75.7231);
        registerMandi("chomu mandi", 27.1724, 75.7231);
        registerMandi("bassi", 26.8322, 76.0422);
        registerMandi("bassi mandi", 26.8322, 76.0422);
        registerMandi("kotputli", 27.7025, 76.2008);
        registerMandi("kotputli mandi", 27.7025, 76.2008);
        registerMandi("bharatpur", 27.2152, 77.4930);
        registerMandi("bharatpur mandi", 27.2152, 77.4930);
        registerMandi("bikaner", 28.0229, 73.3119);
        registerMandi("bikaner apmc", 28.0229, 73.3119);
        registerMandi("kota", 25.1800, 75.8300);
        registerMandi("kota mandi", 25.1800, 75.8300);
        registerMandi("bhamashah mandi", 25.1800, 75.8300);
        registerMandi("alwar", 27.5530, 76.6346);
        registerMandi("alwar apmc", 27.5530, 76.6346);
        registerMandi("jodhpur", 26.2389, 73.0243);
        registerMandi("bhagat ki kothi", 26.2389, 73.0243);
        registerMandi("sri ganganagar", 29.9038, 73.8772);
        registerMandi("ajmer", 26.4499, 74.6399);
        registerMandi("dausa", 26.8931, 76.3375);
        registerMandi("tonk", 26.1664, 75.7885);
        registerMandi("sikar", 27.6094, 75.1398);

        // --- Maharashtra Mandis ---
        registerMandi("lasalgaon", 20.1472, 74.2255);
        registerMandi("lasalgaon mandi", 20.1472, 74.2255);
        registerMandi("nashik", 19.9975, 73.7898);
        registerMandi("nashik market yard", 19.9975, 73.7898);
        registerMandi("pune", 18.4967, 73.8672);
        registerMandi("pune apmc", 18.4967, 73.8672);
        registerMandi("gultekdi", 18.4967, 73.8672);
        registerMandi("vashi", 19.0760, 73.0033);
        registerMandi("vashi apmc", 19.0760, 73.0033);
        registerMandi("latur", 18.4088, 76.5604);
        registerMandi("latur apmc", 18.4088, 76.5604);
        registerMandi("nagpur", 21.1458, 79.0882);
        registerMandi("nagpur cotton market", 21.1458, 79.0882);
        registerMandi("solapur", 17.6599, 75.9064);
        registerMandi("ahmednagar", 19.0952, 74.7496);

        // --- Madhya Pradesh Mandis ---
        registerMandi("indore", 22.6868, 75.8338);
        registerMandi("indore apmc", 22.6868, 75.8338);
        registerMandi("choithram mandi", 22.6868, 75.8338);
        registerMandi("ujjain", 23.1765, 75.7885);
        registerMandi("ujjain mandi", 23.1765, 75.7885);
        registerMandi("bhopal", 23.3082, 77.3996);
        registerMandi("karond mandi", 23.3082, 77.3996);
        registerMandi("neemuch", 24.4754, 74.8697);
        registerMandi("mandsaur", 24.0722, 75.0681);

        // --- Haryana & Punjab Mandis ---
        registerMandi("karnal", 29.6857, 76.9905);
        registerMandi("karnal grain mandi", 29.6857, 76.9905);
        registerMandi("sirsa", 29.5349, 75.0298);
        registerMandi("khanna", 30.7068, 76.2198);
        registerMandi("khanna grain mandi", 30.7068, 76.2198);
        registerMandi("bathinda", 30.2110, 74.9455);
        registerMandi("ludhiana", 30.9010, 75.8573);

        // --- Gujarat Mandis ---
        registerMandi("rajkot", 22.3039, 70.8022);
        registerMandi("rajkot market yard", 22.3039, 70.8022);
        registerMandi("surat", 21.1702, 72.8311);
        registerMandi("surat apmc", 21.1702, 72.8311);
        registerMandi("unjha", 23.8037, 72.3924);
        registerMandi("unjha apmc", 23.8037, 72.3924);
        registerMandi("ahmedabad", 23.0225, 72.5714);

        // --- South India Mandis ---
        registerMandi("guntur", 16.3067, 80.4365);
        registerMandi("guntur apmc", 16.3067, 80.4365);
        registerMandi("warangal", 17.9689, 79.5941);
        registerMandi("warangal apmc", 17.9689, 79.5941);
        registerMandi("kolar", 13.1367, 78.1348);
        registerMandi("kolar market yard", 13.1367, 78.1348);
        registerMandi("davangere", 14.4644, 75.9218);
        registerMandi("davangere apmc", 14.4644, 75.9218);
        registerMandi("nizamabad", 18.6725, 78.0941);
        registerMandi("nizamabad mandi", 18.6725, 78.0941);

        // --- Uttar Pradesh & Delhi Mandis ---
        registerMandi("agra", 27.1767, 78.0081);
        registerMandi("agra mandi", 27.1767, 78.0081);
        registerMandi("kanpur", 26.4499, 80.3319);
        registerMandi("lucknow", 26.8722, 80.8654);
        registerMandi("dubagga", 26.8722, 80.8654);
        registerMandi("varanasi", 25.3176, 82.9739);
        registerMandi("azadpur", 28.7163, 77.1758);
        registerMandi("azadpur mandi", 28.7163, 77.1758);
        registerMandi("ghazipur", 28.6253, 77.3292);

        // --- District Centroids ---
        registerDistrict("jaipur", 26.9124, 75.7873);
        registerDistrict("jodhpur", 26.2389, 73.0243);
        registerDistrict("kota", 25.1800, 75.8300);
        registerDistrict("bikaner", 28.0229, 73.3119);
        registerDistrict("alwar", 27.5530, 76.6346);
        registerDistrict("bharatpur", 27.2152, 77.4930);
        registerDistrict("ajmer", 26.4499, 74.6399);
        registerDistrict("dausa", 26.8931, 76.3375);
        registerDistrict("tonk", 26.1664, 75.7885);
        registerDistrict("sikar", 27.6094, 75.1398);
        registerDistrict("sri ganganagar", 29.9038, 73.8772);
        registerDistrict("udaipur", 24.5854, 73.7125);
        registerDistrict("nashik", 19.9975, 73.7898);
        registerDistrict("pune", 18.5204, 73.8567);
        registerDistrict("mumbai", 19.0760, 72.8777);
        registerDistrict("latur", 18.4088, 76.5604);
        registerDistrict("solapur", 17.6599, 75.9064);
        registerDistrict("nagpur", 21.1458, 79.0882);
        registerDistrict("indore", 22.7196, 75.8577);
        registerDistrict("ujjain", 23.1765, 75.7885);
        registerDistrict("bhopal", 23.2599, 77.4126);
        registerDistrict("karnal", 29.6857, 76.9905);
        registerDistrict("sirsa", 29.5349, 75.0298);
        registerDistrict("ludhiana", 30.9010, 75.8573);
        registerDistrict("bathinda", 30.2110, 74.9455);
        registerDistrict("rajkot", 22.3039, 70.8022);
        registerDistrict("surat", 21.1702, 72.8311);
        registerDistrict("ahmedabad", 23.0225, 72.5714);
        registerDistrict("guntur", 16.3067, 80.4365);
        registerDistrict("warangal", 17.9689, 79.5941);
        registerDistrict("kolar", 13.1367, 78.1348);
        registerDistrict("davangere", 14.4644, 75.9218);
        registerDistrict("agra", 27.1767, 78.0081);
        registerDistrict("kanpur", 26.4499, 80.3319);
        registerDistrict("lucknow", 26.8467, 80.9462);
        registerDistrict("delhi", 28.6139, 77.2090);
    }

    private static void registerMandi(String name, double lat, double lon) {
        MANDI_COORDINATES.put(normalizeKey(name), new Coordinates(lat, lon, true));
    }

    private static void registerDistrict(String name, double lat, double lon) {
        DISTRICT_COORDINATES.put(normalizeKey(name), new Coordinates(lat, lon, false));
    }

    private static String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Resolves coordinates for a given mandi, district, or state.
     * Returns null if no verified location coordinates are known (never invent coordinates).
     */
    public Coordinates resolveLocation(String mandiName, String district, String state) {
        String normMandi = normalizeKey(mandiName);
        if (!normMandi.isEmpty()) {
            for (Map.Entry<String, Coordinates> entry : MANDI_COORDINATES.entrySet()) {
                if (normMandi.contains(entry.getKey()) || entry.getKey().contains(normMandi)) {
                    return entry.getValue();
                }
            }
        }

        String normDistrict = normalizeKey(district);
        if (!normDistrict.isEmpty()) {
            for (Map.Entry<String, Coordinates> entry : DISTRICT_COORDINATES.entrySet()) {
                if (normDistrict.contains(entry.getKey()) || entry.getKey().contains(normDistrict)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Computes Haversine great-circle distance between two geographic coordinates in kilometers.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's mean radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return Math.round(distance * 10.0) / 10.0;
    }

    /**
     * Constructs a Google Maps directions URL if coordinates are available,
     * or a search URL based on location names.
     */
    public String buildNavigateUrl(Double lat, Double lon, String mandiName, String district, String state) {
        if (lat != null && lon != null) {
            return "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon;
        }
        String query = (mandiName != null ? mandiName + " " : "")
                + (district != null ? district + " " : "")
                + (state != null ? state : "");
        return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
    }
}
