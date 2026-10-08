package com.smartkrishi.service;

import com.smartkrishi.dto.CropCalendarDto;
import com.smartkrishi.dto.CropStageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CropCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(CropCalendarService.class);

    private static final List<String> SUPPORTED_CROPS = Arrays.asList(
            "Wheat", "Rice", "Maize", "Mustard", "Cotton",
            "Tomato", "Potato", "Onion", "Chickpea", "Bajra", "Soybean"
    );

    public List<String> getSupportedCrops() {
        return Collections.unmodifiableList(SUPPORTED_CROPS);
    }

    public CropCalendarDto getCalendar(String cropName, String state) {
        return getCropCalendar(cropName, state);
    }

    public CropCalendarDto getCropCalendar(String cropName, String state) {
        String cleanCrop = cropName != null ? cropName.trim() : "Wheat";
        String cleanState = state != null && !state.isBlank() && !state.equalsIgnoreCase("All")
                ? state.trim() : null;

        logger.info("Building crop calendar for: {}, state: {}", cleanCrop, cleanState);

        CropCalendarDto calendar = new CropCalendarDto();
        calendar.setCrop(cleanCrop);
        calendar.setState(cleanState != null ? cleanState : "All India Baseline");

        String lower = cleanCrop.toLowerCase();

        if (lower.contains("wheat")) {
            buildWheatCalendar(calendar, cleanState);
        } else if (lower.contains("rice") || lower.contains("paddy")) {
            buildRiceCalendar(calendar, cleanState);
        } else if (lower.contains("maize") || lower.contains("corn")) {
            buildMaizeCalendar(calendar, cleanState);
        } else if (lower.contains("mustard")) {
            buildMustardCalendar(calendar, cleanState);
        } else if (lower.contains("cotton")) {
            buildCottonCalendar(calendar, cleanState);
        } else if (lower.contains("tomato")) {
            buildTomatoCalendar(calendar, cleanState);
        } else if (lower.contains("potato")) {
            buildPotatoCalendar(calendar, cleanState);
        } else if (lower.contains("onion")) {
            buildOnionCalendar(calendar, cleanState);
        } else if (lower.contains("chickpea") || lower.contains("gram") || lower.contains("chana")) {
            buildChickpeaCalendar(calendar, cleanState);
        } else if (lower.contains("bajra") || lower.contains("millet")) {
            buildBajraCalendar(calendar, cleanState);
        } else if (lower.contains("soybean") || lower.contains("soya")) {
            buildSoybeanCalendar(calendar, cleanState);
        } else {
            buildGenericCalendar(calendar, cleanCrop, cleanState);
        }

        return calendar;
    }

    private void buildWheatCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Triticum aestivum");
        c.setSeason("Rabi");
        c.setTotalDurationDays("120 – 135 Days");
        c.setIdealTemperature("12°C – 25°C");
        c.setRainfallRequirement("350 – 500 mm");
        c.setSoilSuitability("Well-drained Fertile Loam to Clay Loam (pH 6.5 – 7.8)");

        String sowingWindow = "October 25 – November 20";
        if (state != null && state.equalsIgnoreCase("Rajasthan")) {
            sowingWindow = "November 1 – November 25 (avoid high early temperatures)";
            c.setStateSpecific(true);
            c.setNotice("Tailored for Rajasthan agro-climatic zones (late sowing window).");
        } else if (state != null && state.equalsIgnoreCase("Punjab")) {
            sowingWindow = "November 1 – November 15 (optimum yield window)";
            c.setStateSpecific(true);
            c.setNotice("Tailored for Punjab-Haryana irrigated plains.");
        } else {
            c.setStateSpecific(false);
            c.setNotice("General National Agro-Advisory applied. Local sowing may vary by 7-10 days.");
        }

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(
                1, "Sowing & Basal Nutrition", sowingWindow,
                "Pre-sowing irrigation (Paleva) to ensure uniform soil moisture.",
                "Full dose of DAP (50 kg/acre) + MOP (20 kg/acre) + 1/3 dose of Neem Coated Urea.",
                Arrays.asList(
                        "Deep plowing followed by 2 cross-harrowings for fine tilth.",
                        "Seed treatment with Trichoderma viride @ 5g/kg or Carboxin @ 2g/kg seed to prevent loose smut and root rot.",
                        "Maintain 20-22 cm row-to-row spacing and 4-5 cm sowing depth."
                ),
                Arrays.asList("Avoid deep sowing beyond 5 cm as it delays emergence.", "Ensure certified high-germination seed (>85%).")
        ));

        stages.add(new CropStageDto(
                2, "Germination & Crown Root Initiation (CRI)", "Day 7 – Day 25",
                "CRITICAL WATERING: First irrigation strictly at 20-25 days after sowing (CRI stage). Delay causes severe tiller loss.",
                "First top dressing: 1/3 dose of Urea (30-35 kg/acre) right after first irrigation.",
                Arrays.asList(
                        "Monitor seedling emergence and germination density.",
                        "Inspect for early termite attack in sandy soils (Chlorpyrifos if needed).",
                        "Manual weeding or pre-emergence herbicide inspection."
                ),
                Arrays.asList("Do not over-flood; shallow, uniform irrigation is vital at CRI stage.")
        ));

        stages.add(new CropStageDto(
                3, "Tillering & Vegetative Growth", "Day 26 – Day 55",
                "Second irrigation at late tillering (40-45 DAS).",
                "Apply Zinc Sulphate spray (0.5%) if leaf yellowing appears.",
                Arrays.asList(
                        "Apply post-emergence broadleaf & grass weed control (e.g. Clodinafop + Metsulfuron) at 30-35 DAS.",
                        "Light hoeing between rows if weeds are prominent."
                ),
                Arrays.asList("Avoid spray in windy conditions or during peak afternoon sunlight.")
        ));

        stages.add(new CropStageDto(
                4, "Jointing & Flowering / Heading", "Day 56 – Day 85",
                "Third irrigation at flowering/boot leaf stage (70-75 DAS). Stress at flowering causes grain sterility.",
                "Final 1/3 top dressing of Urea before boot stage. Foliar spray of NPK 19:19:19 or Potassium Nitrate (13:0:45) at 1% for spikelet development.",
                Arrays.asList(
                        "Scout for Yellow/Brown Rust pustules on leaf blades.",
                        "Monitor for aphid colonies on emerging earheads."
                ),
                Arrays.asList("Never apply heavy nitrogen at or after flowering to prevent lodging and fungal blast.")
        ));

        stages.add(new CropStageDto(
                5, "Milking & Dough / Maturity", "Day 86 – Day 115",
                "Fourth irrigation at grain filling / milking (90-95 DAS). Avoid irrigation during high gusty winds.",
                "No soil fertilizer needed. Foliar spray of Boron (0.2%) can be applied for grain plumpness if deficient.",
                Arrays.asList(
                        "Inspect grain hardness as earheads turn golden yellow.",
                        "Protect field from stray cattle and birds during milking stage."
                ),
                Arrays.asList("Irrigating on windy days causes crop lodging (falling over), destroying up to 40% yield.")
        ));

        stages.add(new CropStageDto(
                6, "Harvesting & Storage", "Day 116 – Day 135",
                "Withhold irrigation completely 10-15 days before harvest.",
                "None.",
                Arrays.asList(
                        "Harvest when grain moisture drops below 14% and stems turn straw-colored.",
                        "Combine harvesting or manual sickle harvesting during dry sunny weather.",
                        "Threshing, winnowing, and sun-drying grain for 2-3 days."
                ),
                Arrays.asList("Store in clean, moisture-free gunny bags treated with neem oil; maintain <12% storage moisture to prevent weevil attack.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Timely sowing between Nov 1 and Nov 15 gives 20% higher yield than late December sowing.",
                "CRI stage (21 days) is the single most sensitive window for water deficit in wheat.",
                "Adopt Happy Seeder or Super Seeder technology for direct in-situ residue sowing without burning stubble."
        ));
    }

    private void buildRiceCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Oryza sativa");
        c.setSeason("Kharif");
        c.setTotalDurationDays("115 – 140 Days");
        c.setIdealTemperature("22°C – 35°C");
        c.setRainfallRequirement("900 – 1200 mm");
        c.setSoilSuitability("Heavy Clay or Clay Loam with high water retention capacity");

        c.setNotice("Baseline Kharif rice calendar. Nursery raising starts with pre-monsoon rains.");
        List<CropStageDto> stages = new ArrayList<>();

        stages.add(new CropStageDto(1, "Nursery Raising & Land Preparation", "May 25 – June 20",
                "Ensure saturated nursery bed with 2 cm standing water.",
                "Basal nursery dose: 2 kg Urea + 3 kg SSP per 100 sq meter nursery area.",
                Arrays.asList("Seed soaking and sprouting; treatment with Carbendazim @ 2g/kg.", "Raise 25-day-old vigorous seedlings."),
                Arrays.asList("Discard diseased and stunted seedlings.")
        ));

        stages.add(new CropStageDto(2, "Puddling & Transplanting", "June 25 – July 20",
                "Maintain 3-5 cm standing water during and after transplanting.",
                "Full P (DAP) + full K (MOP) + 1/3 N (Urea) + Zinc Sulphate (10 kg/acre).",
                Arrays.asList("Transplant 2-3 seedlings per hill at 20x15 cm spacing.", "Apply pre-emergence herbicide (Pretilachlor) within 3 days."),
                Arrays.asList("Do not let the puddle dry out during the first 10 days.")
        ));

        stages.add(new CropStageDto(3, "Active Tillering & Vegetative", "Day 25 – Day 55",
                "Intermittent drying and wetting (alternate wetting and drying AWD) saves 30% water.",
                "Top dress 1/3 Urea at active tillering (30 DAS).",
                Arrays.asList("Manual hand weeding or cono-weeder pass.", "Monitor for Stem Borer dead hearts and Leaf Folder damage."),
                Arrays.asList("Drain stagnant water if bacterial leaf blight occurs.")
        ));

        stages.add(new CropStageDto(4, "Panicle Initiation & Flowering", "Day 56 – Day 90",
                "Critical stage: Maintain continuous 5 cm water depth from panicle emergence to flowering.",
                "Last 1/3 top dressing of Urea before panicle emergence. Spray NPK 0:52:34 for grain filling.",
                Arrays.asList("Scout for Brown Planthopper (BPH) at the base of stems.", "Pheromone traps @ 8 per acre for stem borer."),
                Arrays.asList("Water stress during flowering causes empty, chaffy grains.")
        ));

        stages.add(new CropStageDto(5, "Milking & Grain Ripening", "Day 91 – Day 120",
                "Shallow water depth (2-3 cm) during dough stage.",
                "No fertilizer application.",
                Arrays.asList("Inspect panicle golden coloration.", "Drain water completely 10 days before harvest."),
                Arrays.asList("Avoid late water drainage to prevent lodging.")
        ));

        stages.add(new CropStageDto(6, "Harvesting & Post-Harvest", "Day 121 – Day 140",
                "Dry soil condition for harvesting machinery.",
                "None.",
                Arrays.asList("Harvest when 80-85% of grains in panicles turn straw golden.", "Thresh and dry paddy down to 12% moisture."),
                Arrays.asList("Never store moist paddy; fungal contamination causes yellowing and mycotoxins.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Alternate Wetting and Drying (AWD) reduces methane emissions and conserves ground water.",
                "Zinc deficiency ('Khaira' disease) causes rusty brown leaf spots; apply Zinc Sulphate heptahydrate.",
                "Avoid spraying pesticides during peak bee foraging hours (9 AM - 12 PM)."
        ));
    }

    private void buildMustardCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Brassica juncea");
        c.setSeason("Rabi");
        c.setTotalDurationDays("110 – 125 Days");
        c.setIdealTemperature("15°C – 25°C");
        c.setRainfallRequirement("250 – 400 mm");
        c.setSoilSuitability("Sandy loam to loam soils with good drainage (pH 6.0 – 7.5)");
        c.setNotice("Mustard requires cool weather and dry conditions at maturity. Sulphur is essential.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Basal Application", "October 5 – October 25",
                "Presowing irrigation to ensure optimal seedbed moisture.",
                "Basal: 40 kg Urea + 50 kg SSP (contains Sulphur) + 15 kg MOP + 10 kg Bentonite Sulphur per acre.",
                Arrays.asList("Fine seedbed preparation; depth 3-4 cm; row spacing 30 cm.", "Seed treatment with Metalaxyl @ 2g/kg against white rust."),
                Arrays.asList("Avoid late sowing beyond Oct 30 as aphid attack drastically increases.")
        ));

        stages.add(new CropStageDto(2, "Germination & Thinning", "Day 7 – Day 25",
                "First light irrigation at 25-30 DAS (pre-flowering).",
                "Top-dress remaining 1/2 Urea after first irrigation.",
                Arrays.asList("Thinning at 15-20 DAS to maintain 10-12 cm plant-to-plant distance.", "First weeding & hoeing."),
                Arrays.asList("Overcrowded plants produce weak stems and poor pod branching.")
        ));

        stages.add(new CropStageDto(3, "Branching & Flowering", "Day 30 – Day 65",
                "Second irrigation at pod formation (60-65 DAS).",
                "Foliar spray of 1% Urea or 0.2% Borax during flowering.",
                Arrays.asList("Monitor closely for Mustard Aphid (Lipaphis erysimi) colonies.", "Install yellow sticky traps @ 10-15 per acre."),
                Arrays.asList("Do not irrigate during peak bloom on windy days.")
        ));

        stages.add(new CropStageDto(4, "Pod Filling & Maturity", "Day 66 – Day 100",
                "Third light irrigation only if soil is dry and pod filling requires moisture.",
                "None.",
                Arrays.asList("Inspect siliquae (pods) turning yellowish brown.", "Scout for White Rust and Alternaria blight."),
                Arrays.asList("Excess late irrigation delays maturity and reduces oil content.")
        ));

        stages.add(new CropStageDto(5, "Harvesting & Threshing", "Day 101 – Day 120",
                "Withhold irrigation.",
                "None.",
                Arrays.asList("Harvest early in the morning when pods are moist with dew to avoid shattering.", "Sun-dry bundles for 4-5 days, then thresh."),
                Arrays.asList("Never harvest in hot afternoon; pod shattering can cause 20% yield loss.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Sulphur application (SSP or elemental sulphur) increases oil content by 2-3%.",
                "Aphid infestation is highest during cloudy/foggy weather in January; act at economic threshold (25 aphids/plant).",
                "Morning harvest prevents pod shattering."
        ));
    }

    private void buildMaizeCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Zea mays");
        c.setSeason("Kharif / Rabi");
        c.setTotalDurationDays("95 – 115 Days");
        c.setIdealTemperature("21°C – 32°C");
        c.setRainfallRequirement("500 – 750 mm");
        c.setSoilSuitability("Deep, fertile, well-drained loamy soils (pH 6.0 – 7.5)");
        c.setNotice("High nutrient feeder crop. Fall Armyworm scouting is essential.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Basal Placement", "June 15 – July 10",
                "Moist seedbed; irrigate if rains are delayed.",
                "Basal: 1/3 Urea + full DAP (50 kg) + full MOP (20 kg) + 10 kg Zinc Sulphate per acre.",
                Arrays.asList("Sow on ridges and furrows; 60 cm row-to-row, 20 cm plant-to-plant.", "Seed treatment with Cyantraniliprole against Fall Armyworm."),
                Arrays.asList("Maize cannot tolerate waterlogging even for 24 hours.")
        ));

        stages.add(new CropStageDto(2, "Knee-High Vegetative Stage", "Day 20 – Day 40",
                "Irrigate if dry spell exceeds 10 days.",
                "Second top-dressing: 1/3 Urea (35 kg/acre) placed 5 cm away from plant stem.",
                Arrays.asList("Earthing-up operation to support root anchoring.", "Pheromone traps for Fall Armyworm (FAW); apply neem cake in whorls."),
                Arrays.asList("Weed competition in first 35 days reduces yield by up to 40%.")
        ));

        stages.add(new CropStageDto(3, "Tasseling & Silking (Critical)", "Day 45 – Day 70",
                "CRITICAL WATERING: Moisture stress during silking causes poor pollination and barren cobs.",
                "Final 1/3 Urea top dressing before tasseling.",
                Arrays.asList("Inspect cob silk emergence and pollen shedding.", "Check for cob borer insects."),
                Arrays.asList("Never allow soil cracking during pollination.")
        ));

        stages.add(new CropStageDto(4, "Cob Development & Harvest", "Day 75 – Day 110",
                "Light irrigation during early dough stage.",
                "None.",
                Arrays.asList("Harvest when sheath husk turns dry, papery brown and grain black layer forms.", "Shelling and drying to 12% moisture."),
                Arrays.asList("Dry thoroughly before storage to prevent Aspergillus / aflatoxin infection.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Fall Armyworm (FAW) management: apply Metarhizium anisopliae or spinetoram in central leaf whorls.",
                "Ridge and furrow sowing prevents waterlogging during monsoon downpours."
        ));
    }

    private void buildTomatoCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Solanum lycopersicum");
        c.setSeason("Round-the-year (Kharif / Rabi)");
        c.setTotalDurationDays("120 – 150 Days");
        c.setIdealTemperature("18°C – 28°C");
        c.setRainfallRequirement("400 – 600 mm (drip irrigation ideal)");
        c.setSoilSuitability("Well-drained Sandy Loam rich in organic matter (pH 6.0 – 7.0)");
        c.setNotice("Horticultural cash crop requiring staking, balanced fertigation, and disease vigilance.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Nursery & Seedbed", "Month 1 (Day 1-25)",
                "Daily light sprinkler / rose-can watering in shaded nursery beds.",
                "Mix well-decomposed FYM (10 kg/sqm) + 50g Trichoderma viride.",
                Arrays.asList("Raised nursery beds of 15 cm height.", "Pro-tray seedling preparation with coco-peat."),
                Arrays.asList("Protect from whiteflies to prevent early Tomato Leaf Curl Virus (ToLCV).")
        ));

        stages.add(new CropStageDto(2, "Transplanting & Staking", "Day 25 – Day 45",
                "Immediate light irrigation after transplanting; drip fertigation setup.",
                "Basal: FYM 10 tonnes/acre + DAP 50 kg + MOP 30 kg + Zinc 5 kg.",
                Arrays.asList("Transplant in evening on raised beds; spacing 60x45 cm.", "Erect bamboo/trellis stakes at 30 days."),
                Arrays.asList("Unstaked tomatoes rot easily and suffer higher fungal blight.")
        ));

        stages.add(new CropStageDto(3, "Vegetative & Flowering", "Day 46 – Day 80",
                "Irrigate every 3-4 days via drip or weekly via furrow.",
                "Fertigation with 19:19:19 @ 3 kg/acre weekly + Calcium Nitrate 2 kg/acre.",
                Arrays.asList("Shoot pruning (suckering) to maintain single/double main stem.", "Spray Boron (0.15%) to prevent flower drop."),
                Arrays.asList("Sudden watering after drought causes fruit splitting and blossom end rot.")
        ));

        stages.add(new CropStageDto(4, "Fruiting & Harvesting", "Day 81 – Day 150",
                "Consistent moisture regime; avoid over-irrigation during picking.",
                "0:0:50 (Potassium sulphate) @ 3 kg/acre weekly for fruit firmness and color.",
                Arrays.asList("Multiple harvest pickings at breaker / turning stage for distant markets.", "Grade fruits by size and firmness."),
                Arrays.asList("Harvest with calyx attached; pack in plastic crates instead of rough sacks.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Blossom end rot is caused by Calcium deficiency and erratic watering.",
                "Install yellow sticky traps for whiteflies and pheromone traps for Helicoverpa fruit borer.",
                "Drip irrigation saves 40% water and reduces foliar fungal diseases."
        ));
    }

    private void buildPotatoCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Solanum tuberosum");
        c.setSeason("Rabi");
        c.setTotalDurationDays("90 – 110 Days");
        c.setIdealTemperature("15°C – 22°C (tuberization requires night temp < 20°C)");
        c.setRainfallRequirement("400 – 500 mm");
        c.setSoilSuitability("Loose, friable sandy loam rich in organic matter (pH 5.2 – 6.8)");
        c.setNotice("Tuberization depends heavily on earthing up and cool night temperatures.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Seed Tuber Prep & Planting", "October 15 – November 5",
                "Pre-planting irrigation to ensure optimal moisture.",
                "Basal: FYM 10 t/acre + DAP 60 kg + MOP 40 kg + Urea 30 kg/acre.",
                Arrays.asList("Use disease-free certified seed tubers (40-50g with 2-3 eyes).", "Plant on ridges 60 cm apart, 20 cm intra-row."),
                Arrays.asList("Cut tubers must be cured and treated with Mancozeb (2g/L) before planting.")
        ));

        stages.add(new CropStageDto(2, "Emergence & Earthing Up", "Day 15 – Day 40",
                "First irrigation 10-12 days after emergence; keep ridges moist.",
                "First top dressing of Urea (35 kg/acre) right before earthing-up.",
                Arrays.asList("Earthing up at 30-35 DAS to bury tubers deeply under soil ridges.", "Weed control before canopy closure."),
                Arrays.asList("Exposed tubers turn green and produce toxic solanine.")
        ));

        stages.add(new CropStageDto(3, "Tuber Bulking & Canopy Care", "Day 41 – Day 75",
                "Irrigate every 7-10 days; maintain 70% available soil moisture.",
                "Foliar spray of 0:52:34 (1%) or Potassium Nitrate for tuber size.",
                Arrays.asList("Prophylactic spray of Mancozeb against Late Blight.", "Monitor aphid vectors."),
                Arrays.asList("Avoid excess nitrogen late in season as it leads to hollow heart.")
        ));

        stages.add(new CropStageDto(4, "Dehaulming & Harvest", "Day 76 – Day 105",
                "Stop irrigation 10-12 days before dehaulming.",
                "None.",
                Arrays.asList("Cut/kill vines (dehaulming) 10-12 days before digging to harden tuber skin.", "Dig carefully to avoid cuts; cure in shade for 10 days."),
                Arrays.asList("Never expose dug potatoes to direct sunlight.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Late Blight is the most devastating disease; spray systemic fungicide upon first fog warning.",
                "Curing potatoes for 10-15 days in shade heals minor skin abrasions before cold storage."
        ));
    }

    private void buildOnionCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Allium cepa");
        c.setSeason("Rabi (Nov-April) & Kharif (June-Oct)");
        c.setTotalDurationDays("120 – 140 Days");
        c.setIdealTemperature("15°C – 30°C");
        c.setRainfallRequirement("350 – 550 mm");
        c.setSoilSuitability("Friable sandy loam with good organic humus (pH 6.5 – 7.5)");
        c.setNotice("Shallow-rooted crop requiring frequent light irrigations and sulphur.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Nursery & Seedling Raising", "Month 1 (Day 1-45)",
                "Daily light sprinkling.", "FYM + 1 kg 19:19:19 per nursery bed.",
                Arrays.asList("Raise 6-7 week old seedlings; trim top 1/3 leaf tips before transplanting."),
                Arrays.asList("Do not transplant overgrown bolting seedlings.")
        ));
        stages.add(new CropStageDto(2, "Transplanting & Basal Dose", "Day 45 – Day 65",
                "Immediate irrigation after planting; row spacing 15x10 cm.",
                "Basal: DAP 40 kg + MOP 30 kg + Urea 25 kg + Sulphur 15 kg/acre.",
                Arrays.asList("Transplant shallow (2-3 cm deep); deep planting creates oblong deformed bulbs."),
                Arrays.asList("Avoid deep planting.")
        ));
        stages.add(new CropStageDto(3, "Bulb Initiation & Development", "Day 66 – Day 110",
                "Light irrigation every 5-7 days; shallow root system.",
                "Top dress Urea 25 kg at 30 and 45 days after transplanting.",
                Arrays.asList("Control Onion Thrips using blue sticky traps or neem oil spray.", "Weeding at 30 and 60 DAT."),
                Arrays.asList("Thrips cause silver leaf streaks and purple blotch disease.")
        ));
        stages.add(new CropStageDto(4, "Neck Fall & Harvest", "Day 111 – Day 140",
                "Stop watering 15 days before harvest when 50% tops fall.",
                "None.",
                Arrays.asList("Harvest when 50% tops have collapsed (neck fall).", "Field cure bulbs with foliage covering bulbs for 3-5 days."),
                Arrays.asList("Irrigating right before harvest causes bulb rot in storage.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Sulphur application enhances bulb pungency, firmness, and storage shelf-life.",
                "Curing until neck is tight and paper-dry prevents bacterial soft rot."
        ));
    }

    private void buildCottonCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Gossypium hirsutum");
        c.setSeason("Kharif");
        c.setTotalDurationDays("150 – 170 Days");
        c.setIdealTemperature("21°C – 35°C");
        c.setRainfallRequirement("500 – 750 mm");
        c.setSoilSuitability("Deep Black Cotton Soil (Vertisol) or fertile loam (pH 7.0 – 8.5)");
        c.setNotice("Deep-rooted cash crop. Vigilance against Pink Bollworm is essential.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Early Establishment", "May 15 – June 20",
                "Presowing irrigation or after first monsoon shower.",
                "Basal: DAP 40 kg + MOP 20 kg + 1/4 Urea per acre.",
                Arrays.asList("Square planting (90x60 cm or 120x45 cm).", "Seed treatment with Imidacloprid against sucking pests."),
                Arrays.asList("Maintain non-Bt refuge rows where applicable.")
        ));
        stages.add(new CropStageDto(2, "Squaring & Vegetative", "Day 30 – Day 65",
                "Irrigate at 15-day intervals if rainfall pauses.",
                "Top dress 1/3 Urea + Magnesium Sulphate (10 kg/acre).",
                Arrays.asList("Monitor sucking pests (thrips, aphids, jassids).", "Earthing up to support tall plants."),
                Arrays.asList("Avoid waterlogging; cotton roots drown easily.")
        ));
        stages.add(new CropStageDto(3, "Flowering & Boll Formation", "Day 66 – Day 120",
                "Critical stage: water deficit causes square and boll shedding.",
                "Foliar spray of 2% DAP or 1% Potassium Nitrate.",
                Arrays.asList("Install Pink Bollworm pheromone traps (5 traps/acre).", "Check rosette flowers."),
                Arrays.asList("Moisture stress causes extensive boll dropping.")
        ));
        stages.add(new CropStageDto(4, "Boll Bursting & Picking", "Day 121 – Day 165",
                "Stop irrigation as bolls start opening.",
                "None.",
                Arrays.asList("Pick clean cotton early morning once dew dries.", "Separate stained/damaged bolls."),
                Arrays.asList("Do not mix plastic twines or trash in raw seed-cotton.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Pink Bollworm monitoring is critical from 60 days onwards.",
                "Magnesium deficiency ('reddening of leaves') is treated with 1% MgSO4 spray."
        ));
    }

    private void buildChickpeaCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Cicer arietinum");
        c.setSeason("Rabi");
        c.setTotalDurationDays("100 – 120 Days");
        c.setIdealTemperature("15°C – 25°C");
        c.setRainfallRequirement("250 – 400 mm");
        c.setSoilSuitability("Well-drained light to heavy soils (pH 6.5 – 8.0)");
        c.setNotice("Legume crop fixing atmospheric nitrogen. Needs minimal irrigation.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Seed Inoculation", "October 15 – November 10",
                "Conserve residual soil moisture with planking.",
                "Basal: DAP 35 kg + MOP 15 kg + Sulphur 10 kg/acre (No heavy Urea needed).",
                Arrays.asList("Inoculate seed with Rhizobium + PSB culture @ 20g/kg.", "Row spacing 30 cm, depth 6-8 cm."),
                Arrays.asList("Never apply heavy nitrogen as it causes excessive vegetative growth without pods.")
        ));
        stages.add(new CropStageDto(2, "Branching & Nipping", "Day 25 – Day 50",
                "Only 1 light irrigation at pre-flowering (40-45 DAS) if soil is dry.",
                "None.",
                Arrays.asList("Nipping (pinching off apical shoot tips at 30-35 DAS) to encourage heavy branching.", "Weeding."),
                Arrays.asList("Do not irrigate during peak flowering; it causes flower drop.")
        ));
        stages.add(new CropStageDto(3, "Pod Formation & Pod Borer Care", "Day 51 – Day 90",
                "Second light irrigation at pod development stage (70 DAS) if needed.",
                "Foliar spray of 2% Urea at pod filling.",
                Arrays.asList("Scout for Helicoverpa armigera (Gram Pod Borer).", "Set up bird perches @ 20/acre and pheromone traps."),
                Arrays.asList("Helicoverpa larvae bore into pods and hollow out developing seeds.")
        ));
        stages.add(new CropStageDto(4, "Harvesting & Threshing", "Day 91 – Day 115",
                "Dry soil condition.",
                "None.",
                Arrays.asList("Harvest when plants turn yellowish-brown and pods rattle when shaken.", "Threshing and sun drying."),
                Arrays.asList("Dry seeds to 9-10% moisture before storing.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Rhizobium seed inoculation fixes up to 40 kg N/ha naturally.",
                "Nipping at 30-35 days increases branch count and pod yield by 15-20%."
        ));
    }

    private void buildBajraCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Pennisetum glaucum");
        c.setSeason("Kharif / Summer");
        c.setTotalDurationDays("80 – 90 Days");
        c.setIdealTemperature("25°C – 35°C");
        c.setRainfallRequirement("300 – 450 mm");
        c.setSoilSuitability("Sandy to sandy loam soils (drought tolerant)");
        c.setNotice("Highly resilient dryland millet; thrives with minimal inputs.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Basal Placement", "June 20 – July 15",
                "Sow with onset of monsoon showers.",
                "Basal: DAP 30 kg + MOP 15 kg + 1/2 Urea per acre.",
                Arrays.asList("Seed treatment against downy mildew.", "Spacing 45x15 cm."),
                Arrays.asList("Avoid deep sowing; 2-3 cm is optimal for small millet seeds.")
        ));
        stages.add(new CropStageDto(2, "Tillering & Thinning", "Day 15 – Day 35",
                "Rainfed; supplemental irrigation only during prolonged dry spell.",
                "Top-dress remaining 1/2 Urea after interculture weeding.",
                Arrays.asList("Thinning to single plant per hill at 15-20 DAS.", "One hoeing & weeding."),
                Arrays.asList("Downy mildew (Green Ear disease) infected plants must be rogued out.")
        ));
        stages.add(new CropStageDto(3, "Heading & Harvest", "Day 36 – Day 85",
                "Moisture during grain filling boosts grain weight.",
                "None.",
                Arrays.asList("Harvest when earheads turn brown and grain hardens.", "Cut earheads separately, dry and thresh."),
                Arrays.asList("Protect maturing earheads from bird damage.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Bajra is an ideal low-water, climate-smart cereal for arid and semi-arid tracts.",
                "Green ear disease is seed-borne; use certified treated hybrid seed."
        ));
    }

    private void buildSoybeanCalendar(CropCalendarDto c, String state) {
        c.setScientificName("Glycine max");
        c.setSeason("Kharif");
        c.setTotalDurationDays("90 – 105 Days");
        c.setIdealTemperature("22°C – 30°C");
        c.setRainfallRequirement("600 – 750 mm");
        c.setSoilSuitability("Well-drained Black Cotton soils or Clay Loam (pH 6.5 – 7.5)");
        c.setNotice("Major kharif oilseed. Ridge-furrow system prevents waterlogging.");

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Sowing & Seed Inoculation", "June 20 – July 10",
                "Sow after receiving 75-100 mm monsoon rainfall.",
                "Basal: DAP 40 kg + MOP 20 kg + SSP 50 kg (for Sulphur).",
                Arrays.asList("Treat seed with Bradyrhizobium culture @ 5g/kg seed.", "Broad bed and furrow (BBF) or ridge-furrow planting."),
                Arrays.asList("Seed coat is fragile; calibrate seed drill to avoid splitting.")
        ));
        stages.add(new CropStageDto(2, "Vegetative & Flowering", "Day 20 – Day 55",
                "Ensure field drainage; provide protective irrigation if dry spell > 15 days.",
                "Foliar spray of 19:19:19 @ 1% at flowering.",
                Arrays.asList("Apply post-emergence herbicide (Imazethapyr) at 15-20 DAS.", "Scout for Girdle Beetle and Semilooper caterpillars."),
                Arrays.asList("Waterlogging at flowering causes massive flower dropping.")
        ));
        stages.add(new CropStageDto(3, "Pod Formation & Harvest", "Day 56 – Day 100",
                "Critical stage: moisture stress at pod filling reduces seed weight.",
                "None.",
                Arrays.asList("Harvest when 90% leaves turn yellow and drop.", "Thresh at low cylinder speed (400-500 RPM) to protect seed germination."),
                Arrays.asList("Over-drying in field causes pod shattering.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Broad Bed and Furrow (BBF) method drains excess rain and retains moisture during droughts.",
                "Sulphur application via SSP increases soybean oil and protein synthesis."
        ));
    }

    private void buildGenericCalendar(CropCalendarDto c, String crop, String state) {
        c.setScientificName(crop + " sp.");
        c.setSeason("Seasonal");
        c.setTotalDurationDays("90 – 120 Days");
        c.setIdealTemperature("18°C – 30°C");
        c.setRainfallRequirement("400 – 700 mm");
        c.setSoilSuitability("Well-drained fertile loam");
        c.setNotice(String.format("Standard agricultural calendar for %s. Follow local Krishi Vigyan Kendra advisories.", crop));

        List<CropStageDto> stages = new ArrayList<>();
        stages.add(new CropStageDto(1, "Land Preparation & Sowing", "Month 1 (Day 1-15)",
                "Pre-sowing irrigation to establish field capacity.",
                "Full P & K as basal dressing + 1/3 Nitrogen.",
                Arrays.asList("Deep summer plowing and seedbed pulverization.", "Certified seed treatment with bio-fungicide."),
                Arrays.asList("Check seed germination percentage before sowing.")
        ));
        stages.add(new CropStageDto(2, "Vegetative Growth", "Month 2 (Day 16-50)",
                "Regular irrigation according to soil texture.",
                "Top-dress remaining nitrogen in split doses.",
                Arrays.asList("Timely weeding and pest scouting.", "Foliar micronutrient spray if required."),
                Arrays.asList("Prevent prolonged weed competition.")
        ));
        stages.add(new CropStageDto(3, "Flowering & Fruit/Grain Filling", "Month 3 (Day 51-85)",
                "Critical stage: avoid drought stress during flowering.",
                "Foliar spray of 19:19:19 or 0:52:34.",
                Arrays.asList("Monitor for target insect pests and fungal blights.", "Erect traps or protective measures."),
                Arrays.asList("Avoid spraying insecticides during peak bee pollination.")
        ));
        stages.add(new CropStageDto(4, "Maturity & Harvesting", "Month 4 (Day 86-115)",
                "Withhold irrigation 10-14 days before harvest.",
                "None.",
                Arrays.asList("Harvest at optimum physiological maturity.", "Proper drying, grading, and storage."),
                Arrays.asList("Store under <12% seed moisture.")
        ));

        c.setStages(stages);
        c.setGeneralAdvisories(Arrays.asList(
                "Always use certified seeds treated with bio-fungicides.",
                "Follow integrated pest management (IPM) to minimize chemical pesticide load."
        ));
    }
}
