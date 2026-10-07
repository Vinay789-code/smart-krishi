-- Smart Krishi Seed Data for Indian Agriculture
-- Default Admin: admin@smartkrishi.com / Admin@123
-- Default Farmer: ramesh@smartkrishi.com / Farmer@123

INSERT INTO users (id, name, email, phone, password, role, created_at, updated_at) VALUES
(1, 'System Administrator', 'admin@smartkrishi.com', '9876543210', '$2a$10$uSg14m6f1fT01n5wWfQpQec39FpS7bWd1n8pE8C4w8R6y.jD0wLKG', 'ROLE_ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Ramesh Kumar Patel', 'ramesh@smartkrishi.com', '9823456789', '$2a$10$0k4/v6FvRzWbI4uE2k/2r.WkK7A3d6y9jY9c4k1V7w1A2b3C4d5E6', 'ROLE_FARMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Suresh Reddy', 'suresh@smartkrishi.com', '9845123456', '$2a$10$0k4/v6FvRzWbI4uE2k/2r.WkK7A3d6y9jY9c4k1V7w1A2b3C4d5E6', 'ROLE_FARMER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO farmer_profiles (id, user_id, village, district, state, land_area, soil_type, irrigation_type, primary_crop, created_at, updated_at) VALUES
(1, 2, 'Khed', 'Pune', 'Maharashtra', 4.5, 'Black Soil', 'Drip Irrigation', 'Soybean', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 3, 'Miryalaguda', 'Nalgonda', 'Telangana', 6.0, 'Red Sandy Loam', 'Canal & Borewell', 'Paddy (Rice)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO farms (id, farmer_profile_id, farm_name, plot_number, area_acres, soil_type, water_source, created_at) VALUES
(1, 1, 'Shree Ganesh Farm', 'Survey 104/A', 2.5, 'Black Soil', 'Borewell + Drip', CURRENT_TIMESTAMP),
(2, 1, 'River Valley Plot', 'Survey 112/B', 2.0, 'Alluvial Loam', 'River Lift Scheme', CURRENT_TIMESTAMP),
(3, 2, 'Red Soil Agro', 'Sy. 45/1', 6.0, 'Red Sandy Loam', 'Nagarjuna Sagar Canal', CURRENT_TIMESTAMP);

INSERT INTO market_prices (crop_name, variety, market_name, district, state, min_price, max_price, modal_price, unit, price_date, trend, created_at) VALUES
('Wheat', 'Sharbati (Grade A)', 'Indore APMC', 'Indore', 'Madhya Pradesh', 2550.0, 2900.0, 2750.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Wheat', 'Lokwan', 'Nashik Market Yard', 'Nashik', 'Maharashtra', 2300.0, 2600.0, 2480.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Paddy (Rice)', 'Basmati 1121', 'Karnal Grain Mandi', 'Karnal', 'Haryana', 3800.0, 4400.0, 4150.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Paddy (Rice)', 'Common IR-64', 'Guntur APMC', 'Guntur', 'Andhra Pradesh', 2180.0, 2350.0, 2280.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Cotton', 'Medium Staple', 'Rajkot Market Yard', 'Rajkot', 'Gujarat', 6800.0, 7450.0, 7200.0, '₹/Quintal', CURRENT_DATE, 'FALLING', CURRENT_TIMESTAMP),
('Cotton', 'Long Staple', 'Warangal APMC', 'Warangal', 'Telangana', 7100.0, 7700.0, 7500.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Soybean', 'Yellow Bold', 'Ujjain Mandi', 'Ujjain', 'Madhya Pradesh', 4400.0, 4850.0, 4650.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Soybean', 'JS-335', 'Latur APMC', 'Latur', 'Maharashtra', 4500.0, 4920.0, 4780.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Onion', 'Nashik Red', 'Lasalgaon Mandi', 'Nashik', 'Maharashtra', 1400.0, 2100.0, 1850.0, '₹/Quintal', CURRENT_DATE, 'FALLING', CURRENT_TIMESTAMP),
('Tomato', 'Hybrid Red', 'Kolar Market Yard', 'Kolar', 'Karnataka', 1200.0, 1800.0, 1500.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Potato', 'Jyoti / Pukhraj', 'Agra Mandi', 'Agra', 'Uttar Pradesh', 1100.0, 1550.0, 1380.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Maize', 'Yellow Hybrid', 'Davangere APMC', 'Davangere', 'Karnataka', 2050.0, 2320.0, 2210.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Mustard', 'Black Mustard', 'Bharatpur Mandi', 'Bharatpur', 'Rajasthan', 5100.0, 5650.0, 5420.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP),
('Gram (Chana)', 'Desi Chana', 'Bikaner APMC', 'Bikaner', 'Rajasthan', 5800.0, 6400.0, 6150.0, '₹/Quintal', CURRENT_DATE, 'STABLE', CURRENT_TIMESTAMP),
('Turmeric', 'Salem Variety', 'Nizamabad Mandi', 'Nizamabad', 'Telangana', 13500.0, 15800.0, 14900.0, '₹/Quintal', CURRENT_DATE, 'RISING', CURRENT_TIMESTAMP);

INSERT INTO advisories (title, category, season, target_crop, summary, details, applicable_state, official_link, created_at) VALUES
('PM-KISAN Samman Nidhi Yojana', 'SCHEME', 'All Season', 'All Crops', 'Direct income support of ₹6,000 per year in 3 equal installments for landholding farmer families.', 'Under PM-KISAN, financial assistance of ₹6,000 per year is provided to all landholding farmers families across the country in three equal four-monthly installments of ₹2,000 each directly transferred into Aadhaar-linked bank accounts. Farmers must complete eKYC via OTP or biometrics at local CSCs to maintain uninterrupted installments.', 'All India', 'https://pmkisan.gov.in', CURRENT_TIMESTAMP),
('Pradhan Mantri Fasal Bima Yojana (PMFBY)', 'SCHEME', 'Kharif & Rabi', 'Food crops & Oilseeds', 'Comprehensive crop insurance covering non-preventable natural risks from pre-sowing to post-harvest.', 'PMFBY provides comprehensive insurance coverage against crop failure due to drought, flood, pests, and cyclones. Farmer premium share is strictly capped: 2% for Kharif crops, 1.5% for Rabi food and oilseeds, and 5% for commercial/horticultural crops. Farmers can enroll through commercial banks, RRBs, PACS, or National Crop Insurance Portal.', 'All India', 'https://pmfby.gov.in', CURRENT_TIMESTAMP),
('Kisan Credit Card (KCC) Scheme', 'SCHEME', 'All Season', 'All Crops', 'Low-interest institutional credit for crop cultivation and working capital needs.', 'KCC simplifies timely access to formal credit for farmers at an effective interest rate of 4% per annum (with prompt repayment incentive). Covers cultivation expenses, post-harvest costs, and maintenance of farm assets. Credit limit up to ₹1.60 lakh without collateral requirement.', 'All India', 'https://myscheme.gov.in/schemes/kcc', CURRENT_TIMESTAMP),
('Soil Health Card Scheme', 'SCHEME', 'All Season', 'All Soils', 'Periodic soil testing providing customized nutrient status and fertilizer dosage recommendations.', 'Provides farmers with Soil Health Cards every 3 years containing nutrient status (N, P, K, S, micro-nutrients like Zn, Fe, Cu, Mn, B) and physical parameters (pH, EC, Organic Carbon), guiding balanced fertilizer application to lower cultivation costs by up to 20%.', 'All India', 'https://soilhealth.dac.gov.in', CURRENT_TIMESTAMP),
('Balanced Fertilizer Management in Rabi Wheat', 'FERTILIZER', 'Rabi', 'Wheat', 'Recommended N:P:K dosage of 120:60:40 kg/ha with split application of Urea.', 'Apply full doses of Phosphorus (DAP or Single Super Phosphate) and Potassium (MOP) alongside one-third Nitrogen at sowing time as basal dose. Apply remaining Nitrogen in two equal splits: first top-dressing at first irrigation (CRI stage, 21-25 DAS) and second at tillering/jointing stage. For zinc-deficient soils, apply 25 kg Zinc Sulphate per hectare.', 'Punjab, Haryana, UP, MP, Bihar', NULL, CURRENT_TIMESTAMP),
('Pink Bollworm Management in Cotton', 'PEST_CONTROL', 'Kharif', 'Cotton', 'Integrated Pest Management (IPM) guidelines to prevent and manage Pink Bollworm in cotton.', 'Install pheromone traps @ 5 traps/acre for monitoring moth activity. When trap catch exceeds 8 moths/trap/night for 3 consecutive days, spray Azadirachtin 1500 ppm (Neem oil) @ 5 ml/liter water. In severe boll infestation, spray Chlorantraniliprole 18.5% SC @ 0.3 ml/L or Emamectin Benzoate 5% SG @ 0.4 g/L during evening hours.', 'Maharashtra, Gujarat, Telangana, AP', NULL, CURRENT_TIMESTAMP),
('Micro-Irrigation & Water Conservation Tips', 'FARMING_TIPS', 'Zaid / Summer', 'Horticulture & Field Crops', 'Tips to conserve up to 45% irrigation water using drip and micro-sprinklers in summer.', 'Switching from conventional flood irrigation to drip irrigation reduces water consumption by 40-50% while improving crop yields by 20-30%. Apply organic mulching (paddy straw, sugarcane bagasse, or black polythene mulch) around the root zone to suppress weeds and minimize evaporative soil moisture loss.', 'All India', NULL, CURRENT_TIMESTAMP),
('Pre-Monsoon Kharif Sowing Guidelines', 'SEASONAL', 'Kharif', 'Soybean, Maize, Pulses', 'Ensure adequate soil moisture (at least 75-100 mm rainfall) before undertaking Kharif sowing.', 'Avoid dry sowing. Wait until the soil profile receives a cumulative rainfall of at least 75 to 100 mm. Treat seeds with Trichoderma viride @ 5g/kg seed or Rhizobium/Azotobacter bio-fertilizers. Maintain seed spacing of 45x5 cm for Soybean and 60x20 cm for Maize for optimal plant population.', 'Maharashtra, MP, Karnataka, Rajasthan', NULL, CURRENT_TIMESTAMP);
