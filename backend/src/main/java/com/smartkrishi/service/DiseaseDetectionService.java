package com.smartkrishi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartkrishi.dto.DiseaseAnalysisResponse;
import com.smartkrishi.dto.DiseaseAnalysisResponse.AlternativePrediction;
import com.smartkrishi.entity.DiseaseRecord;
import com.smartkrishi.entity.User;
import com.smartkrishi.exception.BadRequestException;
import com.smartkrishi.repository.DiseaseRecordRepository;
import com.smartkrishi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Collections;

@Service
public class DiseaseDetectionService implements CropDiseaseAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(DiseaseDetectionService.class);

    private final DiseaseRecordRepository diseaseRecordRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${smartkrishi.ai.api-url:https://router.huggingface.co/hf-inference/models/}")
    private String aiApiUrl;

    @Value("${smartkrishi.ai.api-key:}")
    private String aiApiKey;

    @Value("${smartkrishi.ai.model:linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification}")
    private String aiModel;

    @Value("${smartkrishi.ai.confidence-threshold:60.0}")
    private double confidenceThreshold;

    @Value("${smartkrishi.ai.ml-service-url:}")
    private String mlServiceUrl;

    public DiseaseDetectionService(DiseaseRecordRepository diseaseRecordRepository,
                                  UserRepository userRepository,
                                  RestTemplateBuilder restTemplateBuilder) {
        this.diseaseRecordRepository = diseaseRecordRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(25))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static class PathologyInfo {
        public final String cropName;
        public final String diseaseName;
        public final String severity;
        public final String symptoms;
        public final String organicTreatment;
        public final String chemicalTreatment;
        public final String prevention;

        public PathologyInfo(String cropName, String diseaseName, String severity,
                             String symptoms, String organicTreatment, String chemicalTreatment, String prevention) {
            this.cropName = cropName;
            this.diseaseName = diseaseName;
            this.severity = severity;
            this.symptoms = symptoms;
            this.organicTreatment = organicTreatment;
            this.chemicalTreatment = chemicalTreatment;
            this.prevention = prevention;
        }
    }

    private static final Map<String, PathologyInfo> PATHOLOGY_REGISTRY = new HashMap<>();

    static {
        // Tomatoes
        PATHOLOGY_REGISTRY.put("tomato_early_blight", new PathologyInfo(
                "Tomato", "Tomato Early Blight (Alternaria solani)", "MODERATE",
                "Concentric dark brown rings ('target board' pattern) on older lower foliage, progressive yellowing of surrounding tissue, eventual premature leaf shedding.",
                "Spray 5% Neem Seed Kernel Extract (NSKE) or Copper Oxychloride @ 3g/L. Remove and destroy infected lower leaves. Ensure generous spacing to improve airflow.",
                "Foliar spray of Chlorothalonil 75% WP @ 2g/L or Mancozeb 75% WP @ 2.5g/L at early onset. Alternate with Azoxystrobin 23% SC @ 1 ml/L.",
                "Practice 3-year crop rotation without solanaceous crops. Adopt drip irrigation rather than overhead sprinklers to keep leaf canopies dry."
        ));
        PATHOLOGY_REGISTRY.put("tomato_late_blight", new PathologyInfo(
                "Tomato", "Tomato Late Blight (Phytophthora infestans)", "HIGH",
                "Water-soaked dark lesions appearing near leaflet margins and petioles, rapidly enlarging into necrotic patches with white fuzzy mildew on leaf underside in damp weather.",
                "Apply Bordeaux mixture 1% or Trichoderma viride bio-fungicide @ 5g/L. Discard infected plant material immediately.",
                "Preventive spray of Mancozeb 75% WP @ 2.5 g/L followed by systemic Cymoxanil 8% + Mancozeb 64% WP @ 3 g/L or Metalaxyl-M 4% + Mancozeb 64% WP @ 2.5 g/L.",
                "Use certified disease-free transplants. Avoid overhead irrigation and provide high trellis staking."
        ));
        PATHOLOGY_REGISTRY.put("tomato_bacterial_spot", new PathologyInfo(
                "Tomato", "Tomato Bacterial Spot (Xanthomonas vesicatoria)", "MODERATE",
                "Small, dark, water-soaked circular spots with yellow halos on leaf blades; spots turn necrotic, greasy, and scabby.",
                "Spray Pseudomonas fluorescens @ 5g/L or copper hydroxide solution @ 2.5g/L.",
                "Foliar spray of Copper Oxychloride 50% WP @ 2.5 g/L combined with Streptocycline @ 0.5 g/10 L water.",
                "Rotate with non-host crops (corn, legumes). Disinfect stakes and tools between seasons."
        ));
        PATHOLOGY_REGISTRY.put("tomato_leaf_mold", new PathologyInfo(
                "Tomato", "Tomato Leaf Mold (Passalora fulva)", "LOW",
                "Pale greenish-yellow spots on upper leaf surfaces corresponding to dense olive-green to grayish velvety fungal patches underneath.",
                "Improve greenhouse and canopy ventilation; spray baking soda solution (5g/L) with horticultural mineral oil.",
                "Foliar spray of Difenoconazole 25% EC @ 0.5 ml/L or Chlorothalonil @ 2 g/L.",
                "Keep relative humidity below 85% by increasing ventilation and avoiding leaf wetness."
        ));
        PATHOLOGY_REGISTRY.put("tomato_septoria_leaf_spot", new PathologyInfo(
                "Tomato", "Septoria Leaf Spot (Septoria lycopersici)", "MODERATE",
                "Numerous small circular spots with dark brown margins and tan-to-gray centers containing tiny black pycnidia specks.",
                "Prune bottom 12 inches of foliage to prevent rain-splash transmission; spray copper soap fungicide.",
                "Spray Mancozeb 75% WP @ 2g/L or Pyraclostrobin 20% WG @ 1g/L at 10-day intervals.",
                "Mulch heavily around base with straw to create a splash barrier from soil spores."
        ));
        PATHOLOGY_REGISTRY.put("tomato_target_spot", new PathologyInfo(
                "Tomato", "Tomato Target Spot (Corynespora cassiicola)", "MODERATE",
                "Pinpoint brown spots that expand into concentric rings with chlorotic yellow margins on leaves and stems.",
                "Apply bio-agent Bacillus subtilis @ 5g/L or Trichoderma harzianum @ 5g/L.",
                "Foliar application of Azoxystrobin + Difenoconazole @ 1 ml/L or Fluopyram + Tebuconazole.",
                "Eliminate old crop residues immediately after harvest; practice wide plant spacing."
        ));
        PATHOLOGY_REGISTRY.put("tomato_yellow_leaf_curl_virus", new PathologyInfo(
                "Tomato", "Tomato Yellow Leaf Curl Virus (TYLCV)", "HIGH",
                "Severe stunting, erect bushy growth, pronounced upward curling and cupping of leaflets with interveinal yellowing (chlorosis).",
                "Install yellow sticky traps (15-20 per acre) to monitor and catch whitefly vectors. Spray neem oil (10,000 ppm) @ 2ml/L.",
                "Control vector Bemisia tabaci with systemic Imidacloprid 17.8% SL @ 0.5 ml/L or Thiamethoxam 25% WG @ 0.3 g/L.",
                "Use TYLCV-resistant hybrids. Grow barrier crops (maize/sorghum) around tomato fields."
        ));
        PATHOLOGY_REGISTRY.put("tomato_mosaic_virus", new PathologyInfo(
                "Tomato", "Tomato Mosaic Virus (ToMV)", "HIGH",
                "Mottling of light and dark green patterns on leaves, distortion into 'fern leaf' or blistering, stunted growth.",
                "Remove and burn infected plants immediately. Disinfect tools with 10% trisodium phosphate (TSP).",
                "No direct chemical curative viricide exists. Manage aphid vectors and prevent mechanical transmission.",
                "Wash hands thoroughly with soap before handling plants; purchase certified virus-free seeds."
        ));

        // Potatoes
        PATHOLOGY_REGISTRY.put("potato_early_blight", new PathologyInfo(
                "Potato", "Potato Early Blight (Alternaria solani)", "MODERATE",
                "Dark brown circular to angular spots with concentric rings on older leaves, causing yellowing and drying up.",
                "Spray Neem oil @ 3ml/L or Copper Oxychloride @ 3g/L. Avoid moisture stress and maintain balanced fertility.",
                "Apply Mancozeb 75% WP @ 2.5 g/L or Azoxystrobin 23% SC @ 1 ml/L.",
                "Use high quality certified seed tubers; destroy haulms 10-12 days prior to tuber digging."
        ));
        PATHOLOGY_REGISTRY.put("potato_late_blight", new PathologyInfo(
                "Potato", "Potato Late Blight (Phytophthora infestans)", "HIGH",
                "Water-soaked dark lesions appearing near leaflet margins and petioles, rapidly enlarging into necrotic patches with white fuzzy mildew.",
                "Apply Bordeaux mixture 1% or Trichoderma viride bio-fungicide @ 5g/L. Discard infected potato tubers immediately.",
                "Preventive spray of Mancozeb 75% WP @ 2.5 g/L followed by systemic Cymoxanil 8% + Mancozeb 64% WP @ 3 g/L.",
                "Use certified disease-free seed tubers. High-ridge earthing-up to prevent spores from washing down to developing tubers."
        ));

        // Apples
        PATHOLOGY_REGISTRY.put("apple_apple_scab", new PathologyInfo(
                "Apple", "Apple Scab (Venturia inaequalis)", "MODERATE",
                "Olive-green to velvety brown circular spots on leaves and fruit, causing leaf distortion, premature leaf drop, and cracked scabby fruit surface.",
                "Apply sulfur-based spray or potassium bicarbonate. Shred and compost fallen autumn leaves with urea (5%) to speed microbial breakdown.",
                "Spray Captan 50% WP @ 2.5 g/L at green tip stage, followed by Difenoconazole 25% EC @ 0.3 ml/L at petal fall stage.",
                "Prune trees to open inner canopy to maximum sun penetration and air movement. Select scab-tolerant rootstocks."
        ));
        PATHOLOGY_REGISTRY.put("apple_black_rot", new PathologyInfo(
                "Apple", "Apple Black Rot (Botryosphaeria obtusa)", "MODERATE",
                "Frog-eye leaf spots with purple margins and brown centers, fruit rot with dark concentric rings, branch cankers.",
                "Prune out dead wood, mummified fruits, and fire blight cankers during winter dormancy.",
                "Foliar spray of Thiophanate-methyl 70% WP @ 1g/L or Captan 50% WP @ 2g/L.",
                "Sanitize pruning shears between cuts; prevent bark insect wounding."
        ));
        PATHOLOGY_REGISTRY.put("apple_cedar_apple_rust", new PathologyInfo(
                "Apple", "Cedar Apple Rust (Gymnosporangium juniperi-virginianae)", "LOW",
                "Bright yellow-orange spots on upper leaf surfaces that enlarge and develop tiny tube-like projections on lower leaf surface.",
                "Eradicate nearby red cedar / juniper trees within 1-2 miles if feasible; spray sulfur solution.",
                "Foliar spray of Myclobutanil 10% WP @ 0.5g/L or Mancozeb 75% WP @ 2.5g/L starting at pink bud stage.",
                "Plant rust-resistant apple cultivars like Enterprise, Freedom, or Liberty."
        ));

        // Corn / Maize
        PATHOLOGY_REGISTRY.put("corn_northern_leaf_blight", new PathologyInfo(
                "Corn (Maize)", "Northern Corn Leaf Blight (Exserohilum turcicum)", "MODERATE",
                "Large, elongated, cigar-shaped greyish-green to tan lesions (2.5 to 15 cm long) developing on lower leaves and spreading upward.",
                "Incorporate composted mulch and foliar spray of Trichoderma harzianum @ 5g/L during early vegetative phase.",
                "Foliar spray of Mancozeb 75% WP @ 2.5 g/L or Azoxystrobin 18.2% + Difenoconazole 11.4% SC @ 1 ml/L.",
                "Plant hybrid cultivars with verified Ht gene resistance. Deep summer plowing to bury overwintering crop debris."
        ));
        PATHOLOGY_REGISTRY.put("corn_common_rust", new PathologyInfo(
                "Corn (Maize)", "Common Rust (Puccinia sorghi)", "MODERATE",
                "Small powdery cinnamon-brown to golden pustules scattered across both leaf surfaces, turning brownish-black late season.",
                "Apply potassium silicate spray or wood ash dusting during early morning calm hours.",
                "Foliar spray of Propiconazole 25% EC @ 1 ml/L or Azoxystrobin @ 1 ml/L upon reaching threshold pustule count.",
                "Early planting to avoid high humidity mid-summer spore flights; select resistant hybrids."
        ));
        PATHOLOGY_REGISTRY.put("corn_cercospora_leaf_spot", new PathologyInfo(
                "Corn (Maize)", "Gray Leaf Spot (Cercospora zeae-maydis)", "MODERATE",
                "Narrow, rectangular, vein-delimited tan to gray lesions that coalesce, turning leaves completely blighted.",
                "Apply bio-fungicide Bacillus subtilis or Trichoderma spray at knee-high stage.",
                "Foliar application of Pyraclostrobin + Fluxapyroxad or Azoxystrobin @ 1 ml/L.",
                "Avoid continuous monoculture of maize; use 2-year rotation with soybean or pulses."
        ));

        // Rice / Wheat / Cotton
        PATHOLOGY_REGISTRY.put("rice_bacterial_blight", new PathologyInfo(
                "Rice (Paddy)", "Bacterial Leaf Blight (Xanthomonas oryzae)", "MODERATE",
                "Water-soaked yellowish-green stripes initiating from leaf tips and margins, rapidly turning greyish-white with wavy borders and milky bacterial ooze beads.",
                "Drain excess standing water from field for 3-4 days. Spray fresh cow dung slurry supernatant (20%) or Neem oil @ 3ml/L.",
                "Spray Streptocycline (90% Streptomycin + 10% Tetracycline) @ 6g + Copper Oxychloride @ 250g in 200 liters of water per acre.",
                "Avoid excessive Nitrogen applications. Use tolerant paddy varieties such as IR-64 or Swarna-Sub1. Maintain balanced Potash levels."
        ));
        PATHOLOGY_REGISTRY.put("wheat_rust", new PathologyInfo(
                "Wheat", "Yellow / Stripe Rust (Puccinia striiformis)", "HIGH",
                "Linear rows of bright yellow to orange-yellow pustules forming continuous stripes parallel to leaf veins, creating powdery yellow deposits on handling.",
                "Spray fermented butter-milk (Lassi) solution (5%) or wood ash dusting during calm mornings. Prune heavily infested initial foci.",
                "Immediate foliar application of Propiconazole 25% EC (Tilt) @ 1 ml/liter of water (200 ml in 200 liters water/acre).",
                "Sow rust-resistant varieties like HD-2967, HD-3086, or PBW-550. Avoid late sowing to prevent exposure to high spring humidity."
        ));
        PATHOLOGY_REGISTRY.put("cotton_bacterial_blight", new PathologyInfo(
                "Cotton", "Cotton Bacterial Blight / Angular Leaf Spot (Xanthomonas malvacearum)", "MODERATE",
                "Small, dark green water-soaked spots bounded by small veinlets producing characteristic angular brown margins; lesions may spread along veins ('black arm').",
                "Soak seeds in 1% Streptocycline solution before planting. Spray Pseudomonas fluorescens @ 5g/L on foliage.",
                "Foliar spray of Copper Oxychloride 50% WP @ 3 g/L combined with Streptocycline @ 0.5 g/10 L water at 15-day intervals.",
                "Acid delinting of cottonseed with concentrated Sulphuric Acid (100 ml/kg seed). Burn or deep-plough crop residues post harvest."
        ));

        // Grape
        PATHOLOGY_REGISTRY.put("grape_black_rot", new PathologyInfo(
                "Grape", "Grape Black Rot (Guignardia bidwellii)", "MODERATE",
                "Reddish-brown circular leaf lesions with tiny black pycnidia dots; infected berries shrivel into hard black mummies.",
                "Prune mummified fruit clusters during winter; apply wettable sulfur or copper soap fungicide.",
                "Spray Myclobutanil 10% WP @ 0.5 g/L or Mancozeb 75% WP @ 2.5 g/L from bud-break to veraison.",
                "Maintain open training canopy to facilitate rapid leaf drying after rain."
        ));
        PATHOLOGY_REGISTRY.put("grape_esca", new PathologyInfo(
                "Grape", "Grape Esca (Black Measles)", "HIGH",
                "Interveinal chlorosis and necrosis creating a distinctive 'tiger-stripe' leaf pattern; dark brown spotting on berry skins.",
                "Paint large pruning wounds immediately with pruning seal containing Trichoderma; remove severely infected vines.",
                "Fosetyl-Al @ 2g/L or systemic phosphonates to support vine vascular defense.",
                "Avoid large pruning cuts in wet weather; disinfect pruning shears between vines."
        ));
        PATHOLOGY_REGISTRY.put("grape_leaf_blight", new PathologyInfo(
                "Grape", "Grape Leaf Blight (Pseudocercospora vitis)", "MODERATE",
                "Irregular reddish-brown spots with dark borders that coalesce into extensive leaf blight, causing premature defoliation.",
                "Apply Bordeaux mixture 1% or copper oxychloride @ 2.5g/L.",
                "Foliar spray of Azoxystrobin 23% SC @ 1 ml/L or Chlorothalonil 75% WP @ 2 g/L.",
                "Ensure proper row orientation for prevailing winds to reduce leaf canopy moisture."
        ));

        // Pepper / Bell
        PATHOLOGY_REGISTRY.put("pepper_bell_bacterial_spot", new PathologyInfo(
                "Pepper (Bell)", "Pepper Bacterial Spot (Xanthomonas campestris)", "MODERATE",
                "Small, circular to irregular water-soaked spots on lower leaves that turn dark brown with pale centers, causing heavy blossom and leaf drop.",
                "Spray copper hydroxide + Bacillus amyloliquefaciens @ 4g/L.",
                "Foliar spray of Copper Oxychloride 50% WP @ 2.5 g/L + Streptocycline @ 0.5 g/10 L.",
                "Use hot-water treated or certified disease-free seeds; avoid overhead irrigation."
        ));

        // Squash / Cucumber
        PATHOLOGY_REGISTRY.put("squash_powdery_mildew", new PathologyInfo(
                "Squash", "Powdery Mildew (Podosphaera xanthii)", "MODERATE",
                "White, talcum-powder like fungal patches covering leaf surfaces, causing leaves to yellow, wither, and prematurely brown.",
                "Spray potassium bicarbonate (3g/L) or diluted milk solution (1:9 with water) or neem oil (5ml/L) in early morning.",
                "Foliar spray of Azoxystrobin 23% SC @ 1 ml/L or Myclobutanil 10% WP @ 0.5 g/L upon earliest spot detection.",
                "Ensure generous row spacing for sun exposure; irrigate at root base without wetting foliage."
        ));

        // Strawberry
        PATHOLOGY_REGISTRY.put("strawberry_leaf_scorch", new PathologyInfo(
                "Strawberry", "Leaf Scorch (Diplocarpon earlianum)", "MODERATE",
                "Numerous irregular purplish blotches on leaflets that enlarge and dry up, making leaves look burned or scorched.",
                "Remove and compost severely infected old foliage post-harvest; apply bio-agent Trichoderma viride.",
                "Spray Captan 50% WP @ 2g/L or Pyraclostrobin + Boscalid @ 1g/L prior to flowering.",
                "Avoid excessive spring nitrogen; renovate strawberry beds immediately after harvest."
        ));

        // Citrus / Orange
        PATHOLOGY_REGISTRY.put("orange_haunglongbing", new PathologyInfo(
                "Orange (Citrus)", "Citrus Greening / Huanglongbing (Candidatus Liberibacter)", "HIGH",
                "Asymmetric yellow mottling on leaf blades, vein corking, twig dieback, and lopsided bitter fruits that fail to color.",
                "Introduce predatory wasp Tamarixia radiata to parasitize psyllid vectors. Remove and burn confirmed positive trees.",
                "Manage vector Diaphorina citri with systemic Imidacloprid 17.8% SL @ 0.5 ml/L or Thiamethoxam 25% WG.",
                "Use certified disease-free rootstocks and protect nurseries with vector-proof screen nets."
        ));

        // Peach
        PATHOLOGY_REGISTRY.put("peach_bacterial_spot", new PathologyInfo(
                "Peach", "Peach Bacterial Spot (Xanthomonas arboricola pv. pruni)", "MODERATE",
                "Small angular reddish-purple to dark brown spots on leaves that drop out, producing a 'shot-hole' appearance.",
                "Apply copper sulfate or copper hydroxide sprays during late dormancy before bud break.",
                "Spray Oxytetracycline @ 150 ppm or Copper Oxychloride @ 2.5 g/L during post-bloom periods.",
                "Plant windbreak trees to reduce windblown rain abrasion; select tolerant peach varieties."
        ));

        // Cherry
        PATHOLOGY_REGISTRY.put("cherry_powdery_mildew", new PathologyInfo(
                "Cherry", "Cherry Powdery Mildew (Podosphaera clandestina)", "LOW",
                "Circular white powdery fungal mycelium on underside of leaves, causing upward curling and distorted new shoot tips.",
                "Foliar spray of wettable sulfur (3g/L) or Horticultural mineral oil (1%).",
                "Apply Trifloxystrobin 50% WG @ 0.5 g/L or Tebuconazole 25% EC @ 1 ml/L at first sign of infection.",
                "Open canopy through annual winter pruning to increase air circulation and reduce humidity."
        ));
    }

    @Override
    @Transactional
    public DiseaseAnalysisResponse analyze(MultipartFile file, String cropHint, Long userId) {
        // 1. Image Validation
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select an image file to analyze.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BadRequestException("Unsupported file type. Please upload a valid image file (JPEG, PNG, or WebP).");
        }

        long sizeInBytes = file.getSize();
        if (sizeInBytes > 10 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 10MB limit. Please upload an image under 10MB.");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "leaf_sample.jpg";

        // Read raw image bytes for AI model invocation
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read uploaded image bytes: " + e.getMessage());
        }

        if (imageBytes.length == 0) {
            throw new BadRequestException("Uploaded image file is empty.");
        }

        logger.info("Disease image received: {}, size: {} bytes, mime: {}", originalName, sizeInBytes, contentType);

        // 2. Invoke real AI Image Classification Model
        List<Map<String, Object>> aiPredictions = callAiModel(imageBytes, originalName, contentType);

        if (aiPredictions == null || aiPredictions.isEmpty()) {
            throw new BadRequestException("AI model returned no prediction. Please upload a clearer leaf image.");
        }

        // 3. Parse Top Prediction
        Map<String, Object> topPred = aiPredictions.get(0);
        String topLabel = (String) topPred.get("label");
        double topScore = ((Number) topPred.get("score")).doubleValue();
        double confidencePercent = Math.round(topScore * 1000.0) / 10.0;

        logger.info("AI prediction received: topLabel='{}', confidence={}%", topLabel, confidencePercent);

        // Parse Crop and Disease names from label (standard PlantVillage format: Crop___Disease)
        String[] parsed = parseLabel(topLabel);
        String detectedCrop = parsed[0];
        String detectedCondition = parsed[1];
        boolean isHealthy = detectedCondition.equalsIgnoreCase("healthy") || topLabel.toLowerCase().contains("healthy");

        // 4. Parse Alternative Predictions
        List<AlternativePrediction> alternatives = new ArrayList<>();
        for (int i = 1; i < Math.min(aiPredictions.size(), 4); i++) {
            Map<String, Object> alt = aiPredictions.get(i);
            String altLabel = (String) alt.get("label");
            double altScore = ((Number) alt.get("score")).doubleValue();
            double altPercent = Math.round(altScore * 1000.0) / 10.0;
            String[] altParsed = parseLabel(altLabel);
            alternatives.add(new AlternativePrediction(altParsed[0] + " " + altParsed[1], altPercent));
        }

        // 5. Construct Response
        DiseaseAnalysisResponse resp = new DiseaseAnalysisResponse();
        resp.setImageName(originalName);
        resp.setCropName(detectedCrop);
        resp.setConfidenceScore(confidencePercent);
        resp.setAlternatives(alternatives);

        // Generate base64 thumbnail for instant UI display
        String previewDataUrl;
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            previewDataUrl = "data:" + contentType + ";base64," + base64;
        } catch (Exception e) {
            previewDataUrl = "assets/default-leaf.png";
        }
        resp.setImageUrl(previewDataUrl);

        // 6. Handle Confidence Threshold
        if (confidencePercent < confidenceThreshold) {
            resp.setLowConfidence(true);
            resp.setStatus("Low Confidence Diagnosis");
            resp.setDetectedDisease("Low Confidence Diagnosis");
            resp.setSeverity("LOW");
            resp.setLowConfidenceMessage("Low confidence (" + confidencePercent + "%) — image quality, lighting, or symptoms are insufficient for a reliable diagnosis.");
            resp.setSymptoms("The AI model detected ambiguous visual markers (" + detectedCrop + " " + detectedCondition + " at " + confidencePercent + "% confidence). Diagnosis cannot be confirmed safely.");
            resp.setPreventionTips("Please upload a clear, well-focused, high-resolution photo of the affected plant leaf taken in natural daylight.");
            resp.setOrganicTreatment("Re-scan with a clearer leaf sample before applying any biological treatment.");
            resp.setChemicalTreatment("Do NOT apply chemical pesticides until positive pathology diagnosis is confirmed.");
        } else if (isHealthy) {
            resp.setLowConfidence(false);
            resp.setStatus("Healthy Leaf – No Disease Detected");
            resp.setDetectedDisease("Healthy " + detectedCrop);
            resp.setSeverity("LOW");
            resp.setSymptoms("Plant foliage exhibits healthy green tissue with no significant fungal necrosis, bacterial blighting, or viral chlorosis.");
            resp.setOrganicTreatment("Maintain regular balanced nutrition, compost mulching, and routine inspection.");
            resp.setChemicalTreatment("No chemical interventions required. Maintain current cultural practices.");
            resp.setPreventionTips("Continue balanced watering, proper plant spacing, and clean sanitation practices.");
        } else {
            // Diseased leaf with sufficient confidence
            resp.setLowConfidence(false);
            resp.setStatus("Disease Detected");
            resp.setDetectedDisease(detectedCrop + " " + detectedCondition);

            // Fetch detailed agronomic treatment and symptoms from pathology registry
            PathologyInfo info = lookupPathology(topLabel, detectedCrop, detectedCondition);
            resp.setSeverity(info.severity);
            resp.setSymptoms(info.symptoms);
            resp.setOrganicTreatment(info.organicTreatment);
            resp.setChemicalTreatment(info.chemicalTreatment);
            resp.setPreventionTips(info.prevention);
        }

        // 7. Persist disease record for authenticated farmer
        if (userId != null) {
            DiseaseRecord record = new DiseaseRecord();
            User user = userRepository.findById(userId).orElse(null);
            record.setUser(user);
            record.setCropName(resp.getCropName());
            record.setImageName(originalName);
            // Limit preview length in DB to prevent column overflow
            record.setImageUrl(previewDataUrl.length() > 500 ? originalName : previewDataUrl);
            record.setDetectedDisease(resp.getDetectedDisease());
            record.setConfidenceScore(resp.getConfidenceScore());
            record.setSymptoms(resp.getSymptoms());
            record.setTreatment("ORGANIC:\n" + resp.getOrganicTreatment() + "\n\nCHEMICAL:\n" + resp.getChemicalTreatment());
            record.setPrevention(resp.getPreventionTips());

            DiseaseRecord saved = diseaseRecordRepository.save(record);
            resp.setRecordId(saved.getId());
        }

        return resp;
    }

    /**
     * Dispatches image bytes to the AI Model:
     * - If ML_SERVICE_URL is configured, routes to the local Python ML microservice.
     * - Else, routes to Hugging Face Router / Inference API with AI_API_KEY.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> callAiModel(byte[] imageBytes, String filename, String contentType) {
        // Option B: Local Python ML microservice
        if (mlServiceUrl != null && !mlServiceUrl.isBlank()) {
            try {
                logger.info("Routing image to local ML Service at {}", mlServiceUrl);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                ByteArrayResource resource = new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };
                body.add("file", resource);

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(mlServiceUrl, requestEntity, String.class);
                logger.info("Local ML Service response status: {}", response.getStatusCode());

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return parseAiJsonResponse(response.getBody());
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Local ML Service failed: {}. Falling back to cloud endpoint.", e.getMessage());
                if (aiApiKey == null || aiApiKey.isBlank()) {
                    throw new BadRequestException("AI disease analysis is temporarily unavailable. Local ML service unreachable and AI_API_KEY is not configured.");
                }
            }
        }

        // Option A: Hugging Face Router / Inference API
        if ((aiApiKey == null || aiApiKey.trim().isBlank()) && (mlServiceUrl == null || mlServiceUrl.trim().isBlank())) {
            logger.error("AI_API_KEY is not configured and ML_SERVICE_URL is not set");
            throw new BadRequestException("AI service authentication failed. Please verify AI_API_KEY.");
        }

        String endpoint = aiApiUrl != null ? aiApiUrl.trim() : "https://router.huggingface.co/hf-inference/models/";
        if (!endpoint.endsWith("/") && aiModel != null && !aiModel.isBlank()) {
            endpoint += "/";
        }
        if (aiModel != null && !aiModel.isBlank()) {
            endpoint += aiModel.trim();
        }

        logger.info("Calling AI Disease Detection Endpoint: {}", endpoint);

        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = MediaType.IMAGE_JPEG;
        if (contentType != null && !contentType.isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {}
        }
        headers.setContentType(mediaType);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (aiApiKey != null && !aiApiKey.isBlank()) {
            headers.setBearerAuth(aiApiKey.trim());
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
            logger.info("AI response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseAiJsonResponse(response.getBody());
            } else {
                logger.error("AI service returned unexpected status code: {}", response.getStatusCode());
                throw new BadRequestException("AI service returned an unexpected response format.");
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("AI service timed out or connection failed: {}", e.getMessage());
            throw new BadRequestException("AI disease detection timed out. Please try again.");
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            logger.error("AI Inference HTTP error: status={}, body={}", statusCode, e.getResponseBodyAsString());
            if (statusCode == 401 || statusCode == 403) {
                throw new BadRequestException("AI service authentication failed. Please verify AI_API_KEY.");
            } else if (statusCode == 429) {
                throw new BadRequestException("AI service rate limit reached. Please try again later.");
            } else if (statusCode == 503) {
                throw new BadRequestException("AI disease detection model is currently loading. Please wait a few seconds and try again.");
            } else {
                throw new BadRequestException("AI disease analysis is temporarily unavailable. Status: " + statusCode);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("AI service communication failed: {}", e.getMessage());
            throw new BadRequestException("AI disease analysis is temporarily unavailable. Please try again.");
        }
    }

    private List<Map<String, Object>> parseAiJsonResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new BadRequestException("AI model returned no prediction. Please upload a clearer leaf image.");
        }
        try {
            // Standard HF format: [{"label": "...", "score": 0.95}, ...]
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (list != null && !list.isEmpty()) {
                if (list.get(0).containsKey("label") && list.get(0).containsKey("score")) {
                    return list;
                }
            } else if (list != null && list.isEmpty()) {
                throw new BadRequestException("AI model returned no prediction. Please upload a clearer leaf image.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ignored) {
            // Not a direct list, attempt object parse
        }

        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (map.containsKey("error")) {
                String errorMsg = String.valueOf(map.get("error"));
                logger.warn("AI service returned error payload: {}", errorMsg);
                if (errorMsg.toLowerCase().contains("loading") || errorMsg.toLowerCase().contains("initializing")) {
                    throw new BadRequestException("AI disease detection model is currently loading. Please wait a few seconds and try again.");
                }
                throw new BadRequestException("AI service returned an unexpected response format.");
            }
            if (map.containsKey("predictions")) {
                List<Map<String, Object>> preds = (List<Map<String, Object>>) map.get("predictions");
                if (preds != null && !preds.isEmpty()) {
                    return preds;
                } else {
                    throw new BadRequestException("AI model returned no prediction. Please upload a clearer leaf image.");
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ignored) {}

        logger.error("Failed to parse AI response JSON: {}", json);
        throw new BadRequestException("AI service returned an unexpected response format.");
    }

    private String[] parseLabel(String label) {
        if (label == null || label.isBlank()) {
            return new String[]{"Crop", "Unknown Condition"};
        }

        // Standard convention: Tomato___Early_blight
        String[] parts = label.split("___");
        if (parts.length >= 2) {
            String crop = formatName(parts[0]);
            String disease = formatName(parts[1]);
            return new String[]{crop, disease};
        }

        // Fallback for underscore format: Tomato_Early_blight
        String[] altParts = label.split("_", 2);
        if (altParts.length >= 2) {
            return new String[]{formatName(altParts[0]), formatName(altParts[1])};
        }

        return new String[]{"Crop", formatName(label)};
    }

    private String formatName(String raw) {
        String clean = raw.replace("(", " ")
                .replace(")", " ")
                .replace(",", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String[] words = clean.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private PathologyInfo lookupPathology(String label, String crop, String disease) {
        String key = (crop + "_" + disease).toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");

        // Try exact match
        for (Map.Entry<String, PathologyInfo> entry : PATHOLOGY_REGISTRY.entrySet()) {
            if (key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Try matching disease keywords
        String lowerDisease = disease.toLowerCase();
        for (Map.Entry<String, PathologyInfo> entry : PATHOLOGY_REGISTRY.entrySet()) {
            if (entry.getKey().contains(lowerDisease) || lowerDisease.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Generic fallback for any unindexed pathology
        return new PathologyInfo(
                crop,
                crop + " " + disease,
                "MODERATE",
                "Foliar lesions, chlorotic spotting, and pathogen signs observed on leaf tissue characteristic of " + disease + ".",
                "Spray 5% Neem Seed Kernel Extract (NSKE) or Copper Oxychloride @ 3g/L. Remove and safely dispose of severely infected leaves.",
                "Apply recommended broad-spectrum protective fungicide (Mancozeb 75% WP @ 2.5 g/L or Azoxystrobin @ 1 ml/L) as per label directions.",
                "Ensure adequate plant spacing for aeration, avoid overhead sprinkler irrigation, and rotate crops annually."
        );
    }

    public List<DiseaseAnalysisResponse> getUserRecords(Long userId) {
        return diseaseRecordRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<DiseaseAnalysisResponse> getAllRecords() {
        return diseaseRecordRepository.findAll()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private DiseaseAnalysisResponse mapToResponse(DiseaseRecord record) {
        DiseaseAnalysisResponse resp = new DiseaseAnalysisResponse();
        resp.setRecordId(record.getId());
        resp.setCropName(record.getCropName());
        resp.setImageName(record.getImageName());
        resp.setImageUrl(record.getImageUrl());
        resp.setDetectedDisease(record.getDetectedDisease());
        resp.setConfidenceScore(record.getConfidenceScore());
        resp.setSymptoms(record.getSymptoms());
        resp.setPreventionTips(record.getPrevention());

        if (record.getTreatment() != null && record.getTreatment().contains("CHEMICAL:\n")) {
            String[] parts = record.getTreatment().split("CHEMICAL:\n");
            resp.setOrganicTreatment(parts[0].replace("ORGANIC:\n", "").trim());
            resp.setChemicalTreatment(parts.length > 1 ? parts[1].trim() : "");
        } else {
            resp.setOrganicTreatment(record.getTreatment());
        }

        resp.setAnalyzedAt(record.getCreatedAt());
        return resp;
    }

    // Helper ByteArrayResource for multipart dispatch
    private static class ByteArrayResource extends org.springframework.core.io.ByteArrayResource {
        public ByteArrayResource(byte[] byteArray) {
            super(byteArray);
        }
    }
}
