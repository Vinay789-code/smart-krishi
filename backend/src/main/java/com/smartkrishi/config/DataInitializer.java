package com.smartkrishi.config;

import com.smartkrishi.entity.*;
import com.smartkrishi.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final FarmerProfileRepository profileRepository;
    private final FarmRepository farmRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final AdvisoryRepository advisoryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           FarmerProfileRepository profileRepository,
                           FarmRepository farmRepository,
                           MarketPriceRepository marketPriceRepository,
                           AdvisoryRepository advisoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.farmRepository = farmRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.advisoryRepository = advisoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsersAndProfiles();
        seedMarketPrices();
        seedAdvisories();
    }

    private void seedUsersAndProfiles() {
        if (!userRepository.existsByEmail("admin@smartkrishi.com")) {
            User admin = new User("System Administrator", "admin@smartkrishi.com", "9876543210",
                    passwordEncoder.encode("Admin@123"), Role.ROLE_ADMIN);
            userRepository.save(admin);
            logger.info("Seeded default admin: admin@smartkrishi.com / Admin@123");
        }

        if (!userRepository.existsByEmail("ramesh@smartkrishi.com")) {
            User farmer = new User("Ramesh Kumar Patel", "ramesh@smartkrishi.com", "9823456789",
                    passwordEncoder.encode("Farmer@123"), Role.ROLE_FARMER);
            User savedFarmer = userRepository.save(farmer);

            FarmerProfile profile = new FarmerProfile();
            profile.setUser(savedFarmer);
            profile.setVillage("Khed");
            profile.setDistrict("Pune");
            profile.setState("Maharashtra");
            profile.setLandArea(4.5);
            profile.setSoilType("Black Soil");
            profile.setIrrigationType("Drip Irrigation");
            profile.setPrimaryCrop("Soybean");
            FarmerProfile savedProfile = profileRepository.save(profile);

            Farm farm1 = new Farm(savedProfile, "Shree Ganesh Agro Farm", "Survey 104/A", 2.5, "Black Soil", "Borewell + Drip");
            Farm farm2 = new Farm(savedProfile, "River Valley Plot", "Survey 112/B", 2.0, "Alluvial Loam", "River Lift Canal");
            farmRepository.saveAll(Arrays.asList(farm1, farm2));

            logger.info("Seeded default farmer: ramesh@smartkrishi.com / Farmer@123");
        }
    }

    private void seedMarketPrices() {
        if (marketPriceRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            List<MarketPrice> prices = Arrays.asList(
                new MarketPrice("Wheat", "Sharbati (Grade A)", "Indore APMC", "Indore", "Madhya Pradesh", 2550.0, 2900.0, 2750.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Wheat", "Lokwan", "Nashik Market Yard", "Nashik", "Maharashtra", 2300.0, 2600.0, 2480.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Paddy (Rice)", "Basmati 1121", "Karnal Grain Mandi", "Karnal", "Haryana", 3800.0, 4400.0, 4150.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Paddy (Rice)", "Common IR-64", "Guntur APMC", "Guntur", "Andhra Pradesh", 2180.0, 2350.0, 2280.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Cotton", "Medium Staple", "Rajkot Market Yard", "Rajkot", "Gujarat", 6800.0, 7450.0, 7200.0, "₹/Quintal", today, "FALLING"),
                new MarketPrice("Cotton", "Long Staple", "Warangal APMC", "Warangal", "Telangana", 7100.0, 7700.0, 7500.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Soybean", "Yellow Bold", "Ujjain Mandi", "Ujjain", "Madhya Pradesh", 4400.0, 4850.0, 4650.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Soybean", "JS-335", "Latur APMC", "Latur", "Maharashtra", 4500.0, 4920.0, 4780.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Onion", "Nashik Red", "Lasalgaon Mandi", "Nashik", "Maharashtra", 1400.0, 2100.0, 1850.0, "₹/Quintal", today, "FALLING"),
                new MarketPrice("Tomato", "Hybrid Red", "Kolar Market Yard", "Kolar", "Karnataka", 1200.0, 1800.0, 1500.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Potato", "Jyoti / Pukhraj", "Agra Mandi", "Agra", "Uttar Pradesh", 1100.0, 1550.0, 1380.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Maize", "Yellow Hybrid", "Davangere APMC", "Davangere", "Karnataka", 2050.0, 2320.0, 2210.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Mustard", "Black Mustard", "Bharatpur Mandi", "Bharatpur", "Rajasthan", 5100.0, 5650.0, 5420.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Gram (Chana)", "Desi Chana", "Bikaner APMC", "Bikaner", "Rajasthan", 5800.0, 6400.0, 6150.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Turmeric", "Salem Variety", "Nizamabad Mandi", "Nizamabad", "Telangana", 13500.0, 15800.0, 14900.0, "₹/Quintal", today, "RISING"),
                // Jaipur & Nearby Rajasthan Mandis
                new MarketPrice("Tomato", "Hybrid Deshi", "Muhana Mandi", "Jaipur", "Rajasthan", 2000.0, 3200.0, 2650.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Potato", "Pukhraj / Badshah", "Muhana Mandi", "Jaipur", "Rajasthan", 1250.0, 1600.0, 1420.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Onion", "Nasik Red / Local", "Jaipur Mandi", "Jaipur", "Rajasthan", 1600.0, 2300.0, 1950.0, "₹/Quintal", today, "FALLING"),
                new MarketPrice("Wheat", "Lokwan / Sharbati", "Chomu Mandi", "Jaipur", "Rajasthan", 2400.0, 2750.0, 2580.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Mustard", "Yellow Mustard", "Bassi Mandi", "Jaipur", "Rajasthan", 5200.0, 5750.0, 5500.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Bajra", "Hybrid Pearl Millet", "Kotputli Mandi", "Jaipur", "Rajasthan", 1850.0, 2200.0, 2050.0, "₹/Quintal", today, "STABLE"),
                new MarketPrice("Soybean", "Yellow Bold", "Kota Mandi", "Kota", "Rajasthan", 4350.0, 4800.0, 4600.0, "₹/Quintal", today, "RISING"),
                new MarketPrice("Onion", "Red Medium", "Alwar APMC", "Alwar", "Rajasthan", 1500.0, 2150.0, 1800.0, "₹/Quintal", today, "STABLE")
            );
            marketPriceRepository.saveAll(prices);
            logger.info("Seeded initial market prices ({} records)", prices.size());
        }
    }

    private void seedAdvisories() {
        if (advisoryRepository.count() == 0) {
            List<Advisory> advisories = Arrays.asList(
                new Advisory(
                    "PM-KISAN Samman Nidhi Yojana",
                    "SCHEME",
                    "All Season",
                    "All Crops",
                    "Direct income support of ₹6,000 per year in 3 equal installments for landholding farmer families.",
                    "Under PM-KISAN, financial assistance of ₹6,000 per year is provided to all landholding farmers families across the country in three equal four-monthly installments of ₹2,000 each directly transferred into Aadhaar-linked bank accounts. Farmers must complete eKYC via OTP or biometrics at local CSCs to maintain uninterrupted installments.",
                    "All India",
                    "https://pmkisan.gov.in"
                ),
                new Advisory(
                    "Pradhan Mantri Fasal Bima Yojana (PMFBY)",
                    "SCHEME",
                    "Kharif & Rabi",
                    "Food crops & Oilseeds",
                    "Comprehensive crop insurance covering non-preventable natural risks from pre-sowing to post-harvest.",
                    "PMFBY provides comprehensive insurance coverage against crop failure due to drought, flood, pests, and cyclones. Farmer premium share is strictly capped: 2% for Kharif crops, 1.5% for Rabi food and oilseeds, and 5% for commercial/horticultural crops. Farmers can enroll through commercial banks, RRBs, PACS, or National Crop Insurance Portal.",
                    "All India",
                    "https://pmfby.gov.in"
                ),
                new Advisory(
                    "Kisan Credit Card (KCC) Scheme",
                    "SCHEME",
                    "All Season",
                    "All Crops",
                    "Low-interest institutional credit for crop cultivation and working capital needs.",
                    "KCC simplifies timely access to formal credit for farmers at an effective interest rate of 4% per annum (with prompt repayment incentive). Covers cultivation expenses, post-harvest costs, and maintenance of farm assets. Credit limit up to ₹1.60 lakh without collateral requirement.",
                    "All India",
                    "https://myscheme.gov.in/schemes/kcc"
                ),
                new Advisory(
                    "Soil Health Card Scheme",
                    "SCHEME",
                    "All Season",
                    "All Soils",
                    "Periodic soil testing providing customized nutrient status and fertilizer dosage recommendations.",
                    "Provides farmers with Soil Health Cards every 3 years containing nutrient status (N, P, K, S, micro-nutrients like Zn, Fe, Cu, Mn, B) and physical parameters (pH, EC, Organic Carbon), guiding balanced fertilizer application to lower cultivation costs by up to 20%.",
                    "All India",
                    "https://soilhealth.dac.gov.in"
                ),
                new Advisory(
                    "Balanced Fertilizer Management in Rabi Wheat",
                    "FERTILIZER",
                    "Rabi",
                    "Wheat",
                    "Recommended N:P:K dosage of 120:60:40 kg/ha with split application of Urea.",
                    "Apply full doses of Phosphorus (DAP or Single Super Phosphate) and Potassium (MOP) alongside one-third Nitrogen at sowing time as basal dose. Apply remaining Nitrogen in two equal splits: first top-dressing at first irrigation (CRI stage, 21-25 DAS) and second at tillering/jointing stage. For zinc-deficient soils, apply 25 kg Zinc Sulphate per hectare.",
                    "Punjab, Haryana, UP, MP, Bihar",
                    null
                ),
                new Advisory(
                    "Pink Bollworm Management in Cotton",
                    "PEST_CONTROL",
                    "Kharif",
                    "Cotton",
                    "Integrated Pest Management (IPM) guidelines to prevent and manage Pink Bollworm in cotton.",
                    "Install pheromone traps @ 5 traps/acre for monitoring moth activity. When trap catch exceeds 8 moths/trap/night for 3 consecutive days, spray Azadirachtin 1500 ppm (Neem oil) @ 5 ml/liter water. In severe boll infestation, spray Chlorantraniliprole 18.5% SC @ 0.3 ml/L or Emamectin Benzoate 5% SG @ 0.4 g/L during evening hours.",
                    "Maharashtra, Gujarat, Telangana, AP",
                    null
                ),
                new Advisory(
                    "Micro-Irrigation & Water Conservation Tips",
                    "FARMING_TIPS",
                    "Zaid / Summer",
                    "Horticulture & Field Crops",
                    "Tips to conserve up to 45% irrigation water using drip and micro-sprinklers in summer.",
                    "Switching from conventional flood irrigation to drip irrigation reduces water consumption by 40-50% while improving crop yields by 20-30%. Apply organic mulching (paddy straw, sugarcane bagasse, or black polythene mulch) around the root zone to suppress weeds and minimize evaporative soil moisture loss.",
                    "All India",
                    null
                ),
                new Advisory(
                    "Pre-Monsoon Kharif Sowing Guidelines",
                    "SEASONAL",
                    "Kharif",
                    "Soybean, Maize, Pulses",
                    "Ensure adequate soil moisture (at least 75-100 mm rainfall) before undertaking Kharif sowing.",
                    "Avoid dry sowing. Wait until the soil profile receives a cumulative rainfall of at least 75 to 100 mm. Treat seeds with Trichoderma viride @ 5g/kg seed or Rhizobium/Azotobacter bio-fertilizers. Maintain seed spacing of 45x5 cm for Soybean and 60x20 cm for Maize for optimal plant population.",
                    "Maharashtra, MP, Karnataka, Rajasthan",
                    null
                )
            );
            advisoryRepository.saveAll(advisories);
            logger.info("Seeded initial advisories (8 records)");
        }
    }
}
