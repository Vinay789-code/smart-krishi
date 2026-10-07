package com.smartkrishi.service;

import com.smartkrishi.dto.CropRecommendationRequest;
import com.smartkrishi.dto.CropRecommendationResponse;
import com.smartkrishi.entity.CropRecommendationRecord;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.ResourceNotFoundException;
import com.smartkrishi.repository.CropRecommendationRepository;
import com.smartkrishi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CropRecommendationService {

    private final CropRecommendationRepository recommendationRepository;
    private final UserRepository userRepository;

    public CropRecommendationService(CropRecommendationRepository recommendationRepository,
                                     UserRepository userRepository) {
        this.recommendationRepository = recommendationRepository;
        this.userRepository = userRepository;
    }

    public static class CropProfile {
        String name;
        String season;
        double optN, optP, optK, optPh, optTemp, optHum, optRain;
        List<String> preferredSoils;
        String description;
        String growingConditions;

        public CropProfile(String name, String season,
                           double optN, double optP, double optK, double optPh,
                           double optTemp, double optHum, double optRain,
                           List<String> preferredSoils, String description, String growingConditions) {
            this.name = name;
            this.season = season;
            this.optN = optN;
            this.optP = optP;
            this.optK = optK;
            this.optPh = optPh;
            this.optTemp = optTemp;
            this.optHum = optHum;
            this.optRain = optRain;
            this.preferredSoils = preferredSoils;
            this.description = description;
            this.growingConditions = growingConditions;
        }
    }

    private static final List<CropProfile> CROPS = Arrays.asList(
        new CropProfile("Rice (Paddy)", "Kharif", 90, 45, 40, 6.2, 26, 82, 220,
                Arrays.asList("Clay", "Clay Loam", "Alluvial", "Black Soil"),
                "High water demand staple cereal crop, essential for food security across India.",
                "Requires standing water during vegetative stages, warm humid climate (22°C–32°C), and well-puddled clayey soil."),

        new CropProfile("Wheat", "Rabi", 110, 55, 45, 6.8, 18, 55, 65,
                Arrays.asList("Alluvial", "Loam", "Sandy Loam", "Black Soil"),
                "Premier winter foodgrain, widely cultivated in Northern and Central India.",
                "Prefers cool winter growing temperatures (12°C–22°C), bright sunshine at grain filling, and well-drained fertile loam."),

        new CropProfile("Cotton", "Kharif", 115, 50, 45, 7.0, 27, 60, 85,
                Arrays.asList("Black Soil", "Alluvial", "Red Sandy Loam"),
                "Major commercial fiber cash crop with high economic returns.",
                "Thrives in deep black cotton soils (regur), warm temperatures with at least 180 frost-free days and moderate rainfall."),

        new CropProfile("Soybean", "Kharif", 30, 65, 40, 6.8, 25, 70, 95,
                Arrays.asList("Black Soil", "Loam", "Alluvial"),
                "High-protein oilseed crop that naturally enriches soil with nitrogen fixation.",
                "Requires warm, moist soil for germination, well-distributed rain during pod formation, and well-drained fertile soil."),

        new CropProfile("Maize (Corn)", "Kharif / Rabi", 85, 48, 35, 6.5, 23, 65, 80,
                Arrays.asList("Alluvial", "Red Loam", "Loam", "Black Soil"),
                "Versatile cereal crop used for food, livestock feed, and industrial starch.",
                "Sensitive to waterlogging, requires well-aerated loam rich in organic matter and moderate warm weather."),

        new CropProfile("Chickpea (Gram / Chana)", "Rabi", 25, 60, 30, 7.0, 19, 45, 45,
                Arrays.asList("Sandy Loam", "Loam", "Black Soil"),
                "Leading rabi pulse crop, resilient to moderate drought and low rainfall.",
                "Prefers cool, dry climate, moderate moisture, and avoids heavy waterlogged clay soils."),

        new CropProfile("Mustard / Rapeseed", "Rabi", 75, 40, 30, 6.8, 17, 50, 45,
                Arrays.asList("Alluvial", "Sandy Loam", "Loam"),
                "Vital rabi oilseed crop yielding high quality edible oil and cattle feed cake.",
                "Requires cold, dry weather, frost-free maturity stage, and light to medium loam."),

        new CropProfile("Tomato", "Rabi / Zaid", 100, 70, 80, 6.5, 22, 60, 60,
                Arrays.asList("Sandy Loam", "Loam", "Red Soil", "Black Soil"),
                "High-value horticultural vegetable crop with continuous harvesting cycles.",
                "Needs warm sunny days, cool nights, well-drained organically rich soil, and drip irrigation for best quality."),

        new CropProfile("Potato", "Rabi", 110, 85, 95, 5.8, 18, 70, 60,
                Arrays.asList("Sandy Loam", "Alluvial", "Loam"),
                "Leading tuber crop with high calorific yield per unit area.",
                "Requires loose, friable sandy loam allowing unhindered tuber expansion, cool night temperatures (15°C–20°C)."),

        new CropProfile("Sugarcane", "Annual", 150, 70, 90, 7.0, 30, 75, 160,
                Arrays.asList("Deep Alluvial", "Black Soil", "Clay Loam"),
                "Long duration heavy cash crop requiring abundant water and sunlight.",
                "Tropical climate with temperature 20°C–35°C, high solar radiation, and reliable irrigation."),

        new CropProfile("Groundnut (Peanut)", "Kharif / Zaid", 25, 50, 35, 6.4, 26, 60, 65,
                Arrays.asList("Sandy Loam", "Red Sandy Loam", "Loam"),
                "Leguminous oilseed thriving in light, well-aerated soils for easy peg penetration.",
                "Needs warm weather, loose sandy loam free from hard pan, and moderate well-spaced rainfall."),

        new CropProfile("Onion", "Kharif / Rabi", 80, 50, 65, 6.8, 20, 55, 50,
                Arrays.asList("Loam", "Alluvial", "Sandy Loam"),
                "High commercial demand kitchen staple across all Indian culinary markets.",
                "Requires friable, well-manured loamy soil, moderate temperature, and good drainage to prevent bulb rot.")
    );

    @Transactional
    public CropRecommendationResponse recommendCrop(CropRecommendationRequest req, Long userId) {
        CropProfile bestCrop = evaluateBestCrop(req);

        String fertilizerAdvice = generateFertilizerSuggestion(req, bestCrop);

        CropRecommendationRecord record = new CropRecommendationRecord();
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            record.setUser(user);
        }
        record.setNitrogen(req.getNitrogen());
        record.setPhosphorus(req.getPhosphorus());
        record.setPotassium(req.getPotassium());
        record.setPh(req.getPh());
        record.setTemperature(req.getTemperature());
        record.setHumidity(req.getHumidity());
        record.setRainfall(req.getRainfall());
        record.setSoilType(req.getSoilType());
        record.setRecommendedCrop(bestCrop.name);
        record.setSuitableSeason(bestCrop.season);
        record.setFertilizerSuggestion(fertilizerAdvice);
        record.setGrowingConditions(bestCrop.growingConditions);
        record.setCropDetails(bestCrop.description);

        CropRecommendationRecord saved = recommendationRepository.save(record);

        return mapToResponse(saved);
    }

    public List<CropRecommendationResponse> getUserRecommendations(Long userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<CropRecommendationResponse> getAllRecommendations() {
        return recommendationRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public CropRecommendationResponse getRecommendationById(Long id) {
        CropRecommendationRecord record = recommendationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation record not found with id: " + id));
        return mapToResponse(record);
    }

    private CropProfile evaluateBestCrop(CropRecommendationRequest req) {
        CropProfile bestMatch = CROPS.get(0);
        double minDistance = Double.MAX_VALUE;

        for (CropProfile crop : CROPS) {
            // Normalized Euclidean distance across agronomic parameters
            double dN = Math.pow((req.getNitrogen() - crop.optN) / 80.0, 2);
            double dP = Math.pow((req.getPhosphorus() - crop.optP) / 50.0, 2);
            double dK = Math.pow((req.getPotassium() - crop.optK) / 50.0, 2);
            double dPh = Math.pow((req.getPh() - crop.optPh) / 1.5, 2);
            double dTemp = Math.pow((req.getTemperature() - crop.optTemp) / 10.0, 2);
            double dHum = Math.pow((req.getHumidity() - crop.optHum) / 25.0, 2);
            double dRain = Math.pow((req.getRainfall() - crop.optRain) / 75.0, 2);

            // Soil suitability bonus
            double soilPenalty = 0.0;
            boolean soilMatches = crop.preferredSoils.stream()
                    .anyMatch(s -> s.equalsIgnoreCase(req.getSoilType()) || req.getSoilType().toLowerCase().contains(s.toLowerCase()));
            if (!soilMatches) {
                soilPenalty = 1.8;
            }

            double totalDistance = (dN * 1.2) + (dP * 1.0) + (dK * 1.0) + (dPh * 1.5) + (dTemp * 1.8) + (dHum * 1.2) + (dRain * 2.0) + soilPenalty;

            if (totalDistance < minDistance) {
                minDistance = totalDistance;
                bestMatch = crop;
            }
        }

        return bestMatch;
    }

    private String generateFertilizerSuggestion(CropRecommendationRequest req, CropProfile crop) {
        StringBuilder sb = new StringBuilder();

        // N assessment
        double diffN = crop.optN - req.getNitrogen();
        if (diffN > 25) {
            sb.append("• Nitrogen is Low: Apply Urea @ 45-60 kg/acre in 2-3 split doses.\n");
        } else if (diffN < -30) {
            sb.append("• Nitrogen is High: Reduce urea to prevent excessive vegetative growth and lodging.\n");
        } else {
            sb.append("• Nitrogen status is optimal for ").append(crop.name).append(".\n");
        }

        // P assessment
        double diffP = crop.optP - req.getPhosphorus();
        if (diffP > 20) {
            sb.append("• Phosphorus is Low: Apply DAP (Di-Ammonium Phosphate) @ 40 kg/acre or Single Super Phosphate (SSP) as basal application.\n");
        } else if (diffP < -25) {
            sb.append("• Phosphorus is Sufficient: Avoid excess DAP to maintain trace mineral uptake.\n");
        } else {
            sb.append("• Phosphorus status is balanced.\n");
        }

        // K assessment
        double diffK = crop.optK - req.getPotassium();
        if (diffK > 20) {
            sb.append("• Potassium is Low: Apply MOP (Muriate of Potash) @ 25 kg/acre to boost pest resistance and grain filling.\n");
        } else {
            sb.append("• Potassium status is satisfactory.\n");
        }

        // pH assessment
        if (req.getPh() < 5.8) {
            sb.append("• Acidic Soil (pH ").append(req.getPh()).append("): Apply Agricultural Lime (Calcium Carbonate) @ 200-300 kg/acre.\n");
        } else if (req.getPh() > 7.8) {
            sb.append("• Alkaline Soil (pH ").append(req.getPh()).append("): Apply Agricultural Gypsum @ 150-250 kg/acre and incorporate organic compost/FYM.\n");
        } else {
            sb.append("• Soil pH (").append(req.getPh()).append(") is in the ideal range.\n");
        }

        sb.append("• General Recommendation: Incorporate well-decomposed Farmyard Manure (FYM) @ 4-5 tonnes/acre before sowing.");

        return sb.toString();
    }

    private CropRecommendationResponse mapToResponse(CropRecommendationRecord record) {
        CropRecommendationResponse resp = new CropRecommendationResponse();
        resp.setId(record.getId());
        resp.setRecommendedCrop(record.getRecommendedCrop());
        resp.setSuitableSeason(record.getSuitableSeason());
        resp.setFertilizerSuggestion(record.getFertilizerSuggestion());
        resp.setGrowingConditions(record.getGrowingConditions());
        resp.setCropDetails(record.getCropDetails());
        resp.setNitrogen(record.getNitrogen());
        resp.setPhosphorus(record.getPhosphorus());
        resp.setPotassium(record.getPotassium());
        resp.setPh(record.getPh());
        resp.setTemperature(record.getTemperature());
        resp.setHumidity(record.getHumidity());
        resp.setRainfall(record.getRainfall());
        resp.setSoilType(record.getSoilType());
        resp.setCreatedAt(record.getCreatedAt());
        return resp;
    }
}
