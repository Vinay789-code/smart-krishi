# 🌾 Smart Krishi – Precision Agriculture Platform

A modern, responsive precision agriculture platform designed for Indian farmers. Features precision crop recommendation, real-time live weather & precision irrigation guidance, APMC mandi market prices, smart fertilizer calculation, crop profit / ROI estimation, crop growth calendars, price trend prediction, and government scheme advisories.

---

## 🏗️ Architecture & Deployment Overview

| Component | Platform / Provider | Technology / Model |
| :--- | :--- | :--- |
| **Frontend** | [Netlify](https://www.netlify.com) | HTML5, CSS3, JavaScript (ES6+), Bootstrap 5 |
| **Backend** | [Render](https://render.com) | Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security, REST |
| **Database** | Cloud MySQL (Aiven / TiDB / Railway) | MySQL 8.0+ (Auto-migrated via JPA & `schema.sql`) |
| **Live Weather** | [Open-Meteo](https://open-meteo.com) | Live Open-Meteo Geocoding & Forecast APIs (100% Free, zero fake data) |
| **Mandi Market Prices** | [data.gov.in](https://data.gov.in) / AGMARKNET | Ministry of Agriculture & Farmers Welfare API (`9ef84268-d588-465a-a308-a864a43d0070`) + Haversine APMC Registry |

---

## ☀️ Real Live Weather & Precision Irrigation

Smart Krishi uses real-time, location-based meteorological telemetry. All simulated, fake, or hardcoded temperatures have been completely eliminated.

### How it Works:
1. **City / District Search**: When a farmer enters a location (e.g., *Jaipur*, *Pune*, *Delhi*, *Kota*), the Spring Boot backend queries the **Open-Meteo Geocoding API** (`https://geocoding-api.open-meteo.com/v1/search`) to resolve the exact geographical coordinates (latitude and longitude).
2. **GPS Geolocation ("Use My Location")**: Farmers can click the **"Use My Location"** button. The browser retrieves high-accuracy GPS coordinates via `navigator.geolocation.getCurrentPosition(...)` and queries `/api/weather?latitude={lat}&longitude={lon}`.
3. **Live Telemetry & Evapotranspiration**: The backend fetches real-time atmospheric data from **Open-Meteo Weather API** (`https://api.open-meteo.com/v1/forecast`), including:
   - Current Temperature (°C)
   - Apparent / Feels-like Temperature (°C)
   - Relative Humidity (%)
   - Wind Speed (km/h) and Direction
   - Precipitation & Rainfall (mm)
   - Cloud Cover (%) and WMO Weather Condition
   - Exact observation timestamp
   - 5-Day Agricultural Forecast
4. **Smart Irrigation Scheduling**: Computes soil moisture deficit and evapotranspiration from live conditions to advise farmers whether to irrigate heavily, moderately, lightly, or pause watering due to recent rainfall.
5. **Accurate Error Handling**: If an invalid location is searched (e.g., `xxxxxxxx`), the backend returns a `404 Not Found` error. The UI displays an explicit error message instead of displaying simulated or fake weather.

---
## 🌾 Nearby Live Mandi Prices & APMC Commodity Telemetry

Smart Krishi provides Indian farmers with nearby agricultural market (APMC Mandi) rates, minimum/maximum/modal prices (₹/quintal), commodities, and turn-by-turn navigation.

---

## 🧪 4 New Precision Agriculture Modules

### 1. Smart Fertilizer Recommendation (`POST /api/fertilizer/recommend`)
- **Engine**: Rule-based agronomic nutrient balancing aligned with Indian Council of Agricultural Research (ICAR) guidelines.
- **Inputs**: Target crop, soil classification (Alluvial, Black, Red, Clay, Sandy, Laterite), existing soil test nutrients (N, P, K in kg/ha, pH 3.0–11.0), land area (acres), and crop growth stage.
- **Calculations**:
  - Nutrient deficit against benchmark NPK targets.
  - Fertilizer equivalent conversions into standard commercial bags (Urea 46% N, DAP 18:46:0, MOP 60% K2O, SSP 16% P2O5, Zinc Sulphate 21% Zn).
  - Cost estimation based on subsidized Government MRP rates.
  - Split application schedule (Basal application %, first top-dressing %, second top-dressing %).
  - Soil pH correction advisories: recommends agricultural lime for acidic soils (pH < 6.0) and gypsum / organic matter for alkaline soils (pH > 8.0).
- **Frontend**: `fertilizer.html` (`js/fertilizer.js`) with one-click test presets for Wheat, Paddy, Cotton, Tomato, and Mustard.
- **Sample Request**:
  ```bash
  curl -X POST http://localhost:8080/api/fertilizer/recommend \
    -H "Content-Type: application/json" \
    -d '{
      "crop": "Wheat",
      "soilType": "Alluvial Loam",
      "growthStage": "Sowing / Basal",
      "landArea": 2.0,
      "nitrogen": 180.0,
      "phosphorus": 18.0,
      "potassium": 140.0,
      "ph": 7.0
    }'
  ```

### 2. Crop Profit & ROI Calculator (`POST /api/profit/calculate`)
- **Engine**: Precision farm economics computing net profitability, return on capital, and break-even selling price before sowing.
- **Inputs**: Crop name, cultivated area (acres), expected yield per acre (quintals), expected market price (₹/quintal), and cost breakdown (seed, fertilizers, pesticides, labor, irrigation/power, machinery/fuel, and mandi transport/misc).
- **Formulas**:
  $$\text{Total Production} = \text{Land Area} \times \text{Expected Yield per Acre}$$
  $$\text{Total Cost} = \sum \text{Input Costs}$$
  $$\text{Expected Revenue} = \text{Total Production} \times \text{Expected Selling Price}$$
  $$\text{Net Profit} = \text{Expected Revenue} - \text{Total Cost}$$
  $$\text{Profit per Acre} = \frac{\text{Net Profit}}{\text{Land Area}}$$
  $$\text{ROI \%} = \frac{\text{Net Profit}}{\text{Total Cost}} \times 100$$
  $$\text{Break-Even Price} = \frac{\text{Total Cost}}{\text{Total Production}}$$
- **Outputs**: Comprehensive farm economics dashboard with visual cost percentage allocation bars and financial health rating (`EXCELLENT`, `GOOD`, `MODERATE`, `HIGH_RISK`).
- **Frontend**: `profit-calculator.html` (`js/profit-calculator.js`) with preloaded farm budgets for Wheat, Tomato, Soybean, and Mustard.
- **Sample Request**:
  ```bash
  curl -X POST http://localhost:8080/api/profit/calculate \
    -H "Content-Type: application/json" \
    -d '{
      "cropName": "Wheat",
      "landAreaAcres": 2.0,
      "expectedYieldPerAcreQuintals": 20.0,
      "expectedSellingPricePerQuintal": 2550.0,
      "seedCost": 3500.0,
      "fertilizerCost": 6500.0,
      "pesticideCost": 2400.0,
      "laborCost": 8500.0,
      "irrigationCost": 3200.0,
      "machineryCost": 5500.0,
      "otherCost": 2500.0
    }'
  ```

### 3. Crop Growth Calendar & Advisory (`GET /api/crop-calendar/{crop}`)
- **Engine**: Phenological growth timeline detailing activities from seed sowing to post-harvest storage.
- **Coverage**: Specialized timelines for Wheat, Rice (Paddy), Maize, Mustard, Cotton, Tomato, Potato, Onion, Chickpea (Gram), Bajra, and Soybean, plus adaptive fallback for other crops.
- **Stages**:
  1. *Sowing & Land Preparation* (Pre-sowing irrigation / Paleva, seed treatment, basal nutrition)
  2. *Germination & Emergence / CRI* (Critical first watering window, early weed control)
  3. *Vegetative & Tillering* (Second irrigation, top-dressing, pest scouting)
  4. *Flowering & Booting* (Critical reproductive watering, micronutrient sprays)
  5. *Grain Filling & Maturity* (Milking stage precautions, lodging prevention)
  6. *Harvesting & Storage* (Withholding water before harvest, grain moisture testing <12%)
- **Dynamic Sowing Date Calculation**: User enters their actual sowing date; the frontend calculates real-world calendar date windows for each phase.
- **Frontend**: `crop-calendar.html` (`js/crop-calendar.js`).
- **Sample Request**:
  ```bash
  curl -X GET "http://localhost:8080/api/crop-calendar/Wheat?state=Rajasthan"
  ```

### 4. Crop Price Prediction & Market Trend (`GET /api/price-prediction/{crop}`)
- **Engine**: Statistical baseline rolling moving average and rate-of-change momentum analysis across historical APMC records.
- **Data Integrity & Non-Fabrication**:
  - Strictly requires a minimum of **3 historical time-series points** in the database.
  - If records < 3: Returns an honest `INSUFFICIENT_DATA` status explaining why more data is needed, without fabricating false forecasts.
  - If records >= 3: Projects price movements over the chosen horizon (7 to 30 days) clamped to realistic momentum bands, calculating expected range (± volatility), rolling moving average, and statistical confidence score.
  - Displays transparent methodology and statutory advisory disclaimer on all predictions.
- **Frontend**: `price-prediction.html` (`js/price-prediction.js`).
- **Sample Request**:
  ```bash
  curl -X GET "http://localhost:8080/api/price-prediction/Wheat?state=Rajasthan&days=10"
  ```

---
1. **Location-Aware Discovery**:
   - **📍 Use My Location**: Uses the browser Geolocation API *only* when the farmer explicitly clicks the button (never auto-collected). Accurately resolves nearby mandis.
   - **Manual State & District Filter**: For desktop users or if GPS permission is denied, full manual dropdown selection with live district loading.
2. **Dynamic Search Radius**:
   - Filter mandis by **10 km**, **25 km**, **50 km (default)**, or **100 km**. Mandis are automatically sorted by proximity.
3. **Crop / Commodity Filter**:
   - Filter by specific commodities: *Tomato, Potato, Onion, Wheat, Paddy, Mustard, Cotton, Soybean, Gram, Bajra*, or *All Crops*.
4. **Distance & Navigation**:
   - Accurate distance calculated using the **Haversine Formula** against the APMC Market Registry.
   - One-click **"🗺 Navigate"** button opens Google Maps navigation directly to the market yard.
5. **Multi-Tier Robust Fallback Architecture**:
   - 🟢 **Latest Government Mandi Data**: Live real-time records fetched from the Ministry of Agriculture & Farmers Welfare Open Data Platform (`data.gov.in` AGMARKNET dataset `9ef84268-d588-465a-a308-a864a43d0070`).
   - 🟡 **Cached Government Mandi Data**: Thread-safe in-memory cache with configurable TTL (default 15 minutes) prevents rate-limiting and provides instant responses.
   - 🔵 **Smart Krishi Admin Data**: When the external API is unreachable or has no records for a remote location, the system seamlessly falls back to verified database records.
6. **Strict Transparency**:
   - Fallback data is explicitly badged as `🔵 Smart Krishi Admin Data`.
   - Never fabricates fake live data, never claims database data is live, and uses honest wording: *"Latest Available Mandi Prices"*.

### REST API Endpoints:
- `GET /api/mandi/nearby?latitude={lat}&longitude={lon}&radiusKm={km}&crop={commodity}&sortBy={distance|price}`
- `GET /api/mandi/nearby?state={state}&district={district}&crop={commodity}`
- `GET /api/mandi/crops` — List of available commodities
- `GET /api/mandi/locations` — Hierarchical State → District mapping
- `GET /api/mandi/status` — Operational status & upstream government API connectivity

---

## 🛠️ Required Environment Variables

### Backend (Render / Cloud Server)

| Variable | Description | Example / Default |
| :--- | :--- | :--- |
| `PORT` | Web server port | `8080` *(Set automatically on Render)* |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `SPRING_DATASOURCE_URL` | Cloud MySQL JDBC connection URL | `jdbc:mysql://<host>:<port>/<dbname>?useSSL=true&allowPublicKeyRetrieval=true` |
| `SPRING_DATASOURCE_USERNAME` | Cloud MySQL username | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | Cloud MySQL password | `your_secret_password` |
| `JWT_SECRET` | 256-bit secret key for token authentication | `min_32_characters_random_secure_secret_key` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `https://*.netlify.app,https://smart-krishi.netlify.app` |
| `MANDI_API_URL` | Open Government Data (data.gov.in) AGMARKNET endpoint | `https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070` |
| `MANDI_API_KEY` | data.gov.in free API Key | `579b464db66ec23bdd000001...` *(from data.gov.in)* |
| `MANDI_CACHE_MINUTES` | Mandi cache duration in minutes | `15` |

### Frontend (Netlify)

| Variable / Setting | Description | Value |
| :--- | :--- | :--- |
| `API_BASE_URL` | Public Render backend URL | `https://smart-krishi-backend.onrender.com` |

---

## 💻 1. Local Setup

### Prerequisites
- Java 17+ (`java -version`)
- Apache Maven 3.8+ (`mvn -version`)

### Run Backend
```bash
cd backend
mvn clean spring-boot:run
```
- Server starts on `http://localhost:8080`.
- In-memory H2 database runs with automatic schema initialization and seed data.
- H2 Console available at `http://localhost:8080/h2-console`.

### Run Frontend
```bash
cd frontend
python -m http.server 3000
```
Open `http://localhost:3000` in your web browser.

---

## 🧪 Testing Live Weather & Mandi Prices

### Testing Live Weather:
1. Open the Weather page (`weather.html`).
2. Search for Indian farming hubs: `Jaipur`, `Delhi`, `Mumbai`, `Kota`, `Pune`, `Bengaluru`.
   - Verify that coordinates update accordingly and real temperatures are shown.
3. Test invalid location: Search `xxxxxxxx`.
   - Verify that the error message *"Live weather temporarily unavailable: Location not found"* is displayed.
4. Click **"Use My Location"**:
   - Allow location access and verify that weather for your current GPS coordinates is loaded.

### Testing Nearby Mandi Prices:
1. Open the Mandi Prices page (`market-prices.html`).
2. Notice the initial view presents **"Find Mandi Prices Near You"** with option to use GPS or choose state/district.
3. Click **"📍 Use My Location"**:
   - Browser requests geolocation permission (only on explicit click).
   - If granted, the system resolves coordinates, filters mandis within default 50 km, and displays prices sorted by nearest distance.
4. Test Radius selector:
   - Change radius between `10 km`, `25 km`, `50 km`, and `100 km` and watch the list update immediately.
5. Test Crop Filter:
   - Select `Tomato`, `Wheat`, `Onion`, or `Mustard` to filter commodities.
6. Test Manual Filter:
   - Select State: `Rajasthan`, District: `Jaipur` to view Jaipur APMC markets without GPS.
7. Click **"🗺 Navigate"**:
   - Opens turn-by-turn Google Maps directions directly to the APMC market yard.
8. Verify Data Transparency:
   - Status bar clearly shows whether data is `🟢 Live Government Mandi Data`, `🟡 Cached Government Mandi Data`, or `🔵 Smart Krishi Admin Data`.

---

## 🗄️ 2. Cloud MySQL Database Setup

1. Provision a free MySQL database on any cloud provider:
   - **Aiven.io** (Free MySQL service)
   - **TiDB Cloud** (Free MySQL-compatible Serverless cluster)
   - **Railway** / **Clever Cloud**
2. JDBC URL format:
   ```text
   jdbc:mysql://<HOST>:<PORT>/<DATABASE>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
   ```
3. Database tables are automatically managed via JPA and seed files located at `backend/src/main/resources/schema.sql` and `seed.sql`.

---

## 📤 3. GitHub Push

```bash
git init
git add .
git commit -m "feat: complete Smart Krishi precision agriculture platform"
git branch -M main
git remote add origin https://github.com/<your-username>/smart-krishi.git
git push -u origin main
```

---

## 🚀 4. Render Deployment (Backend)

1. Go to [Render Dashboard](https://dashboard.render.com) → **New** → **Web Service**.
2. Select your repository.
3. Configure settings:
   - **Name**: `smart-krishi-backend`
   - **Root Directory**: `backend`
   - **Environment**: `Docker` *(uses `backend/Dockerfile`)*
   - **Plan**: `Free`
4. Add **Environment Variables**:
   - `PORT`: `8080`
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `<your-cloud-mysql-jdbc-url>`
   - `SPRING_DATASOURCE_USERNAME`: `<your-db-username>`
   - `SPRING_DATASOURCE_PASSWORD`: `<your-db-password>`
   - `JWT_SECRET`: `<your-32-char-random-jwt-key>`
   - `CORS_ALLOWED_ORIGINS`: `https://<your-netlify-subdomain>.netlify.app,https://*.netlify.app`
5. Click **Create Web Service**. Copy your live backend URL (e.g., `https://smart-krishi-backend.onrender.com`).

---

## 🌐 5. Netlify Deployment (Frontend)

1. In [Netlify](https://app.netlify.com) → **Add new site** → **Import an existing project**.
2. Select your repository.
3. Set:
   - **Base directory**: `frontend`
   - **Build command**: *(leave blank)*
   - **Publish directory**: `frontend`
4. In `frontend/js/config.js`, set `PROD_API_URL` to your Render URL.
5. Click **Deploy Site**.

---

## 👤 Development Seed Users

*(Seeded into database for testing; login UI contains no hardcoded credentials or demo buttons)*

| Role | Email | Password | Access Rights |
| :--- | :--- | :--- | :--- |
| **Farmer** | `ramesh@smartkrishi.com` | `Farmer@123` | Dashboard, Profile, Farm Management, Crop History |
| **System Admin** | `admin@smartkrishi.com` | `Admin@123` | Admin Portal, User Management, Add Mandi Prices & Advisories |

---

## 🔧 Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **CORS error in browser console** | Frontend origin missing in `CORS_ALLOWED_ORIGINS` | Add your Netlify URL to Render's `CORS_ALLOWED_ORIGINS` environment variable and redeploy. |
| **Backend 502 / Cold Start on Render** | Free tier Render instances sleep after inactivity | Free tier instances sleep after 15 mins of inactivity. The first wake-up request takes ~40 seconds. |
| **Invalid Location on Weather Search** | Non-existent city name | Open-Meteo geocoding rejects non-existent names (e.g. `xxxxxxxx`). Check spelling or use GPS location. |
| **Database Connection Failure** | Incorrect JDBC URL or IP restrictions | Verify `useSSL=true&allowPublicKeyRetrieval=true` is appended to the JDBC URL. Check cloud database IP whitelisting (`0.0.0.0/0`). |
