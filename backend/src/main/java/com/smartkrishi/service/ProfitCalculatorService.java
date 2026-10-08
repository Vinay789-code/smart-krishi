package com.smartkrishi.service;

import com.smartkrishi.dto.ProfitCalculationRequest;
import com.smartkrishi.dto.ProfitCalculationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProfitCalculatorService {

    private static final Logger logger = LoggerFactory.getLogger(ProfitCalculatorService.class);

    public ProfitCalculationResponse calculateProfit(ProfitCalculationRequest req) {
        logger.info("Computing ROI and Profit for crop: {}, land: {} acres", req.getCrop(), req.getLandArea());

        double landArea = req.getLandArea();
        double yieldPerAcre = req.getExpectedYieldPerAcre();
        double price = req.getExpectedSellingPrice();

        double seed = req.getSeedCost();
        double fert = req.getFertilizerCost();
        double pest = req.getPesticideCost();
        double labor = req.getLaborCost();
        double irrig = req.getIrrigationCost();
        double mach = req.getMachineryCost();
        double other = req.getOtherCost();

        // 1. Core formulas specified in requirements
        double totalProduction = round(landArea * yieldPerAcre);
        double totalCost = round(seed + fert + pest + labor + irrig + mach + other);
        double expectedRevenue = round(totalProduction * price);
        double estimatedProfit = round(expectedRevenue - totalCost);

        double profitPerAcre = landArea > 0 ? round(estimatedProfit / landArea) : 0.0;
        double roiPercentage = totalCost > 0 ? round((estimatedProfit / totalCost) * 100.0) : 0.0;
        double breakEvenPrice = totalProduction > 0 ? round(totalCost / totalProduction) : 0.0;

        double costPerAcre = landArea > 0 ? round(totalCost / landArea) : 0.0;
        double revenuePerAcre = landArea > 0 ? round(expectedRevenue / landArea) : 0.0;

        ProfitCalculationResponse res = new ProfitCalculationResponse();
        res.setCrop(req.getCrop().trim());
        res.setLandArea(landArea);
        res.setExpectedYieldPerAcre(yieldPerAcre);
        res.setExpectedSellingPrice(price);

        res.setTotalProduction(totalProduction);
        res.setTotalCost(totalCost);
        res.setExpectedRevenue(expectedRevenue);
        res.setEstimatedProfit(estimatedProfit);
        res.setProfitPerAcre(profitPerAcre);
        res.setRoiPercentage(roiPercentage);
        res.setBreakEvenPrice(breakEvenPrice);
        res.setCostPerAcre(costPerAcre);
        res.setRevenuePerAcre(revenuePerAcre);

        // 2. Cost breakdown
        Map<String, Double> costMap = new LinkedHashMap<>();
        costMap.put("Seeds & Nursery", seed);
        costMap.put("Fertilizers & Nutrients", fert);
        costMap.put("Crop Protection / Pesticides", pest);
        costMap.put("Labor & Harvesting", labor);
        costMap.put("Irrigation & Pumping", irrig);
        costMap.put("Machinery & Diesel", mach);
        costMap.put("Post-harvest & Misc", other);
        res.setCostBreakdown(costMap);

        // Percentage breakdown for visual UI
        Map<String, Double> pctMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : costMap.entrySet()) {
            double pct = totalCost > 0 ? round((entry.getValue() / totalCost) * 100.0) : 0.0;
            pctMap.put(entry.getKey(), pct);
        }
        res.setCostPercentageBreakdown(pctMap);

        // 3. Financial Health & Summary Notice
        if (estimatedProfit > 0) {
            if (roiPercentage >= 40.0) {
                res.setFinancialHealth("HIGHLY_PROFITABLE");
                res.setSummaryNotice(String.format("Excellent ROI of %.1f%%. Projected net profit of ₹%,.0f across %.1f acres.",
                        roiPercentage, estimatedProfit, landArea));
            } else {
                res.setFinancialHealth("MODERATE_PROFIT");
                res.setSummaryNotice(String.format("Healthy positive margin with %.1f%% ROI. To maximize returns, monitor input costs.",
                        roiPercentage));
            }
        } else if (estimatedProfit == 0) {
            res.setFinancialHealth("BREAK_EVEN");
            res.setSummaryNotice("Revenue strictly covers production expenses without net operating surplus.");
        } else {
            res.setFinancialHealth("LOSS_MAKING");
            res.setSummaryNotice(String.format("Projected loss of ₹%,.0f. Current price (₹%.0f) is below the break-even cost of ₹%.0f per quintal.",
                    Math.abs(estimatedProfit), price, breakEvenPrice));
        }

        return res;
    }

    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
