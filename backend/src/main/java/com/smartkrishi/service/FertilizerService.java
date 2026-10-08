package com.smartkrishi.service;

import com.smartkrishi.dto.FertilizerItemDto;
import com.smartkrishi.dto.FertilizerRecommendRequest;
import com.smartkrishi.dto.FertilizerRecommendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FertilizerService {

    private static final Logger logger = LoggerFactory.getLogger(FertilizerService.class);

    // Standard subsidized Indian fertilizer unit costs (approx 2026 national benchmarks)
    private static final double COST_UREA_PER_KG = 6.0;        // ~₹270 per 45kg bag
    private static final double COST_DAP_PER_KG = 27.0;        // ~₹1,350 per 50kg bag
    private static final double COST_MOP_PER_KG = 34.0;        // ~₹1,700 per 50kg bag
    private static final double COST_SSP_PER_KG = 10.5;        // ~₹525 per 50kg bag
    private static final double COST_ZINC_PER_KG = 80.0;       // ~₹80/kg
    private static final double COST_FOLIAR_NPK_PER_KG = 180.0; // Water soluble 19:19:19

    public FertilizerRecommendResponse recommendFertilizer(FertilizerRecommendRequest request) {
        return recommendFertilizers(request);
    }

    public FertilizerRecommendResponse recommendFertilizers(FertilizerRecommendRequest request) {
        logger.info("Generating rule-based fertilizer recommendation for crop: {}, area: {} acres",
                request.getCrop(), request.getLandArea());

        FertilizerRecommendResponse res = new FertilizerRecommendResponse();
        res.setCrop(request.getCrop().trim());
        res.setSoilType(request.getSoilType().trim());
        String stage = request.getGrowthStage() != null && !request.getGrowthStage().isBlank()
                ? request.getGrowthStage().trim() : "Basal / Pre-Sowing";
        res.setGrowthStage(stage);
        res.setLandArea(request.getLandArea());

        // 1. Evaluate N, P, K, pH status
        double n = request.getNitrogen();
        double p = request.getPhosphorus();
        double k = request.getPotassium();
        double ph = request.getPh();
        double area = request.getLandArea();

        String nStatus = n < 50 ? "DEFICIENT" : (n <= 100 ? "OPTIMAL" : "EXCESS");
        String pStatus = p < 25 ? "DEFICIENT" : (p <= 55 ? "OPTIMAL" : "EXCESS");
        String kStatus = k < 35 ? "DEFICIENT" : (k <= 75 ? "OPTIMAL" : "EXCESS");

        String phStatus;
        if (ph < 6.0) phStatus = "ACIDIC";
        else if (ph <= 7.5) phStatus = "OPTIMAL_NEUTRAL";
        else if (ph <= 8.5) phStatus = "ALKALINE";
        else phStatus = "SALINE_SODIC";

        res.setNitrogenStatus(nStatus);
        res.setPhosphorusStatus(pStatus);
        res.setPotassiumStatus(kStatus);
        res.setPhStatus(phStatus);

        // 2. Determine target N-P-K requirement (kg per acre) based on crop
        CropNutrientTarget target = getTargetNutrients(request.getCrop());

        // 3. Compute net deficit per acre
        double nDeficit = Math.max(0, target.targetN - (nStatus.equals("EXCESS") ? target.targetN * 0.7 : n * 0.5));
        double pDeficit = Math.max(0, target.targetP - (pStatus.equals("EXCESS") ? target.targetP * 0.7 : p * 0.6));
        double kDeficit = Math.max(0, target.targetK - (kStatus.equals("EXCESS") ? target.targetK * 0.7 : k * 0.5));

        // Adjust for growth stage
        List<FertilizerItemDto> items = new ArrayList<>();
        List<String> advice = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        double totalCost = 0.0;

        // Stage-based rule logic
        boolean isBasal = stage.equalsIgnoreCase("Basal") || stage.toLowerCase().contains("sowing") || stage.toLowerCase().contains("seedbed");
        boolean isVegetative = stage.toLowerCase().contains("vegetative") || stage.toLowerCase().contains("tillering");
        boolean isFlowering = stage.toLowerCase().contains("flowering") || stage.toLowerCase().contains("reproductive");
        boolean isMaturity = stage.toLowerCase().contains("maturity") || stage.toLowerCase().contains("grain");

        // Phosphorus source (DAP or SSP)
        if (pDeficit > 0 && (isBasal || !isFlowering)) {
            // DAP provides 18% N and 46% P2O5
            double dapKgPerAcre = Math.round((pDeficit / 0.46) * 10.0) / 10.0;
            if (dapKgPerAcre > 5.0) {
                double totalDapKg = Math.round(dapKgPerAcre * area);
                int bags = (int) Math.ceil(totalDapKg / 50.0);
                double itemCost = totalDapKg * COST_DAP_PER_KG;
                totalCost += itemCost;

                items.add(new FertilizerItemDto(
                        "Di-Ammonium Phosphate (DAP 18:46:0)",
                        "Phosphorus (P) + Starter Nitrogen (N)",
                        dapKgPerAcre, totalDapKg, bags, 50.0,
                        "Basal Application",
                        "Band placement 2-3 inches deep below the seed line at sowing",
                        Math.round(itemCost)
                ));

                // Account for N supplied by DAP (18% of DAP)
                nDeficit = Math.max(0, nDeficit - (dapKgPerAcre * 0.18));
            }
        }

        // Nitrogen source (Urea)
        if (nDeficit > 0) {
            double ureaKgPerAcre = Math.round((nDeficit / 0.46) * 10.0) / 10.0;
            // Split dose recommendation
            double currentDoseKg = isBasal ? ureaKgPerAcre * 0.35 : (isVegetative ? ureaKgPerAcre * 0.5 : ureaKgPerAcre * 0.2);
            currentDoseKg = Math.round(currentDoseKg * 10.0) / 10.0;

            if (currentDoseKg > 5.0) {
                double totalUreaKg = Math.round(currentDoseKg * area);
                int bags = (int) Math.ceil(totalUreaKg / 45.0);
                double itemCost = totalUreaKg * COST_UREA_PER_KG;
                totalCost += itemCost;

                String method = isBasal ? "Broadcast and incorporate during final tillage" : "Top dressing under moist soil condition";
                items.add(new FertilizerItemDto(
                        "Neem Coated Urea (46% N)",
                        "Primary Nitrogen (N)",
                        currentDoseKg, totalUreaKg, bags, 45.0,
                        stage,
                        method,
                        Math.round(itemCost)
                ));
            }
        }

        // Potassium source (MOP)
        if (kDeficit > 0 && !isMaturity) {
            double mopKgPerAcre = Math.round((kDeficit / 0.60) * 10.0) / 10.0;
            if (mopKgPerAcre > 5.0) {
                double totalMopKg = Math.round(mopKgPerAcre * area);
                int bags = (int) Math.ceil(totalMopKg / 50.0);
                double itemCost = totalMopKg * COST_MOP_PER_KG;
                totalCost += itemCost;

                items.add(new FertilizerItemDto(
                        "Muriate of Potash (MOP 60% K2O)",
                        "Potassium (K)",
                        mopKgPerAcre, totalMopKg, bags, 50.0,
                        isBasal ? "Basal Application" : "Early Vegetative",
                        "Soil application along crop rows",
                        Math.round(itemCost)
                ));
            }
        }

        // Flowering booster (19:19:19 foliar spray)
        if (isFlowering) {
            double foliarKgPerAcre = 1.5;
            double totalFoliarKg = Math.round(foliarKgPerAcre * area * 10.0) / 10.0;
            double itemCost = totalFoliarKg * COST_FOLIAR_NPK_PER_KG;
            totalCost += itemCost;

            items.add(new FertilizerItemDto(
                    "Water-Soluble NPK (19:19:19) Foliar Grade",
                    "Balanced Micronutrient & Macro Foliar Nutrition",
                    foliarKgPerAcre, totalFoliarKg, 1, totalFoliarKg,
                    "Flowering / Fruit Setting",
                    "Foliar spray dissolved in 150-200 liters of water per acre during morning hours",
                    Math.round(itemCost)
            ));
        }

        // Micronutrient: Zinc (if deficient or high yielding cereal)
        if (isBasal && (request.getCrop().equalsIgnoreCase("Rice") || request.getCrop().equalsIgnoreCase("Wheat") || request.getCrop().equalsIgnoreCase("Maize"))) {
            double znKg = 5.0;
            double totalZn = znKg * area;
            double itemCost = totalZn * COST_ZINC_PER_KG;
            totalCost += itemCost;

            items.add(new FertilizerItemDto(
                    "Zinc Sulphate Heptahydrate (21% Zn)",
                    "Micronutrient Zinc (Zn) + Sulphur (S)",
                    znKg, totalZn, (int) Math.ceil(totalZn / 10.0), 10.0,
                    "Basal Soil Dressing",
                    "Apply to soil once in 2-3 crop cycles (do not mix directly with phosphatic DAP)",
                    Math.round(itemCost)
            ));
        }

        res.setRecommendedFertilizers(items);
        res.setEstimatedTotalCost(Math.round(totalCost * 100.0) / 100.0);

        // 4. Generate Soil and Management Advice
        advice.add(String.format("Recommended nutrient schedule for %s in %s soil at %s stage.",
                request.getCrop(), request.getSoilType(), stage));

        if (nStatus.equals("DEFICIENT")) {
            advice.add("Soil nitrogen is low. Apply recommended urea in 2-3 split doses (1/3 basal, 1/3 at crown-root/tillering, 1/3 at panicle initiation) to reduce leaching losses.");
        } else if (nStatus.equals("EXCESS")) {
            advice.add("Soil nitrogen is already high. Reduce nitrogenous fertilizers to prevent vegetative overgrowth, crop lodging, and excessive pest vulnerability.");
        }

        if (pStatus.equals("DEFICIENT")) {
            advice.add("Phosphorus is deficient. Apply phosphatic fertilizers (DAP/SSP) exclusively as basal placement near the root zone, as phosphorus is immobile in soil.");
        }

        if (kStatus.equals("DEFICIENT")) {
            advice.add("Potassium is low. Adequate potash application improves drought tolerance, disease resistance, and grain weight.");
        }

        if (request.getSoilType().equalsIgnoreCase("Sandy")) {
            advice.add("Sandy soils have high percolation and lower cation exchange. Split fertilizer doses into smaller, more frequent applications to minimize leaching.");
        } else if (request.getSoilType().equalsIgnoreCase("Clay") || request.getSoilType().equalsIgnoreCase("Black")) {
            advice.add("Heavy clay/black soils have strong nutrient retention. Ensure adequate field drainage to prevent waterlogging and root asphyxiation.");
        }

        // 5. Generate Warnings
        if (phStatus.equals("ACIDIC")) {
            warnings.add(String.format("Soil pH is acidic (%.1f). Nutrient availability (especially P, Ca, Mg) is constrained. Apply Agricultural Lime (CaCO3) @ 200-400 kg/acre prior to sowing.", ph));
        } else if (phStatus.equals("SALINE_SODIC")) {
            warnings.add(String.format("Soil pH is highly alkaline/sodic (%.1f). Micro-nutrients like Iron and Zinc will be locked. Apply Gypsum @ 500 kg/acre and incorporate green manure (Dhaincha) to reclaim soil.", ph));
        }

        warnings.add("Do NOT mix Zinc Sulphate directly with DAP or SSP in the same slurry or furrow, as insoluble Zinc Phosphate precipitates, rendering both inactive.");
        warnings.add("Avoid broadcasting Urea over dry soil or in standing water under harsh mid-day sun. Top-dress after light irrigation or during evening hours.");

        // Summary string
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            FertilizerItemDto item = items.get(i);
            summary.append(item.getName()).append(": ").append(item.getTotalQuantityKg()).append(" kg");
            if (item.getBagCount() > 0 && item.getBagSizeKg() > 0) {
                summary.append(" (~").append(item.getBagCount()).append(" bags)");
            }
            if (i < items.size() - 1) summary.append(", ");
        }
        res.setApproximateQuantitySummary(summary.toString());
        res.setApplicationAdvice(advice);
        res.setWarnings(warnings);

        return res;
    }

    private CropNutrientTarget getTargetNutrients(String crop) {
        if (crop == null) return new CropNutrientTarget(80, 40, 40);
        String c = crop.toLowerCase().trim();

        if (c.contains("wheat")) return new CropNutrientTarget(120, 60, 40);
        if (c.contains("rice") || c.contains("paddy")) return new CropNutrientTarget(100, 50, 50);
        if (c.contains("maize") || c.contains("corn")) return new CropNutrientTarget(120, 60, 40);
        if (c.contains("cotton")) return new CropNutrientTarget(80, 40, 40);
        if (c.contains("tomato")) return new CropNutrientTarget(140, 80, 100);
        if (c.contains("potato")) return new CropNutrientTarget(120, 100, 120);
        if (c.contains("onion")) return new CropNutrientTarget(100, 50, 80);
        if (c.contains("mustard")) return new CropNutrientTarget(80, 40, 40);
        if (c.contains("soybean")) return new CropNutrientTarget(30, 60, 40); // legume
        if (c.contains("chickpea") || c.contains("gram")) return new CropNutrientTarget(25, 50, 25); // legume
        if (c.contains("bajra")) return new CropNutrientTarget(80, 40, 30);
        if (c.contains("sugarcane")) return new CropNutrientTarget(150, 60, 60);

        return new CropNutrientTarget(80, 40, 40);
    }

    private static class CropNutrientTarget {
        final double targetN;
        final double targetP;
        final double targetK;

        CropNutrientTarget(double n, double p, double k) {
            this.targetN = n;
            this.targetP = p;
            this.targetK = k;
        }
    }
}
