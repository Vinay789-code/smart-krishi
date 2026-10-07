# 🌾 Smart Krishi – Precision Agriculture Platform

A modern, responsive precision agriculture platform designed for Indian farmers. Features precision crop recommendation, real-time live weather & precision irrigation guidance, deep learning leaf disease diagnostics, APMC mandi market prices, and government scheme advisories.

---

## 🏗️ Architecture & Deployment Overview

| Component | Platform / Provider | Technology / Model |
| :--- | :--- | :--- |
| **Frontend** | [Netlify](https://www.netlify.com) | HTML5, CSS3, JavaScript (ES6+), Bootstrap 5 |
| **Backend** | [Render](https://render.com) | Java 17, Spring Boot 3.3.4, Spring Data JPA, Spring Security, REST |
| **Database** | Cloud MySQL (Aiven / TiDB / Railway) | MySQL 8.0+ (Auto-migrated via JPA & `schema.sql`) |
| **Live Weather** | [Open-Meteo](https://open-meteo.com) | Live Open-Meteo Geocoding & Forecast APIs (100% Free, zero fake data) |
| **AI Disease Detection** | [Hugging Face](https://huggingface.co) / Local ML | `linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification` or local Python microservice |
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

## 🌿 AI-Based Crop Disease Detection

Crop disease diagnosis runs real-time computer vision inference on uploaded plant leaf images. Filename-based heuristics, random selections, and static demo guesses have been completely removed.

### AI Model & Integration Architecture:
```
Farmer Leaf Upload (JPG/PNG/WebP <= 10MB)
  ↓
Spring Boot Backend (/api/disease/analyze)
  ↓
Hugging Face Serverless Inference Router (or Local Python ML Microservice)
  ↓
Plant Pathology Registry & Treatment Engine
  ↓
Frontend UI (Confidence Bar, Severity, Observed Symptoms, Organic & Chemical Remedies)
```

- **Cloud Inference (Option A - Default)**:
  - **Endpoint Router**: `https://router.huggingface.co/hf-inference/models/linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification`
  - **Model**: `linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification` (trained on PlantVillage benchmark dataset covering 38 disease & healthy classes across tomato, potato, corn, apple, grape, pepper, etc.)
  - **Authorization**: `Bearer ${AI_API_KEY}` (Token stays strictly on backend; never exposed to browser)
- **Local Microservice (Option B - Optional)**:
  - Microservice included in `ml-service/app.py` running on `http://localhost:5000/predict`.
  - Configured via `ML_SERVICE_URL=http://localhost:5000/predict`.

### Configurable Confidence Threshold & Robust Error Handling:
- **Confidence Threshold**: Configured via `AI_CONFIDENCE_THRESHOLD` (default **60.0%**).
- **Low Confidence Diagnosis**: If model confidence < 60%, the system flags a low-confidence diagnosis advising the farmer to take a clearer, focused photo under natural daylight.
- **Dedicated Error Card**: If an API error or network failure occurs, the UI displays a clear `#disease-error-card` with an exact error message and never gets stuck in a permanent scanning loop.
- **Specific Error Messages Handled**:
  - `401 / 403`: *"AI service authentication failed. Please verify AI_API_KEY."*
  - `429`: *"AI service rate limit reached. Please try again later."*
  - `503 / Loading`: *"AI disease detection model is currently loading. Please wait a few seconds and try again."*
  - `Timeout`: *"AI disease detection timed out. Please try again."*
  - `Malformed response`: *"AI service returned an unexpected response format."*
  - `Empty prediction`: *"AI model returned no prediction. Please upload a clearer leaf image."*

### Getting a Free Hugging Face Token:
1. Create a free account at [huggingface.co](https://huggingface.co).
2. Go to **Settings** → **Access Tokens** (`https://huggingface.co/settings/tokens`).
3. Create a new token with **Read** permission.
4. Set the environment variable:
   ```bash
   # Windows PowerShell
   $env:AI_API_KEY="hf_your_token_here"
   # Linux / Mac / Render Dashboard
   export AI_API_KEY="hf_your_token_here"
   ```

### Farmer Disease History:
- Authenticated farmers can view past scans via `GET /api/disease/records` (or `/api/disease/my-history`).
- Admins can query all farm scans globally.

---

## 🌾 Nearby Live Mandi Prices & APMC Commodity Telemetry

Smart Krishi provides Indian farmers with nearby agricultural market (APMC Mandi) rates, minimum/maximum/modal prices (₹/quintal), commodities, and turn-by-turn navigation.

### Core Features:
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
| `AI_API_URL` | AI Inference endpoint base URL | `https://router.huggingface.co/hf-inference/models/` |
| `AI_API_KEY` | Hugging Face free user access token | `hf_...` *(from huggingface.co/settings/tokens)* |
| `AI_MODEL` | AI classification model repository | `linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification` |
| `AI_CONFIDENCE_THRESHOLD` | Minimum confidence score percentage | `60.0` |
| `ML_SERVICE_URL` | *(Optional)* Local Python ML service URL | `http://localhost:5000/predict` |
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
- Python 3.9+ *(optional, for running local ML microservice)*

### Run Backend
```bash
cd backend
mvn clean spring-boot:run
```
- Server starts on `http://localhost:8080`.
- In-memory H2 database runs with automatic schema initialization and seed data.
- H2 Console available at `http://localhost:8080/h2-console`.

### (Optional) Run Option B Local Python ML Service
```bash
cd ml-service
python app.py
```
Starts on `http://localhost:5000/predict`.

### Run Frontend
```bash
cd frontend
python -m http.server 3000
```
Open `http://localhost:3000` in your web browser.

---

## 🧪 Testing Live Weather, AI Disease Detection & Mandi Prices

### Testing Live Weather:
1. Open the Weather page (`weather.html`).
2. Search for Indian farming hubs: `Jaipur`, `Delhi`, `Mumbai`, `Kota`, `Pune`, `Bengaluru`.
   - Verify that coordinates update accordingly and real temperatures are shown.
3. Test invalid location: Search `xxxxxxxx`.
   - Verify that the error message *"Live weather temporarily unavailable: Location not found"* is displayed.
4. Click **"Use My Location"**:
   - Allow location access and verify that weather for your current GPS coordinates is loaded.

### Testing AI Disease Detection:
1. Open the Disease Detection page (`disease-detection.html`).
2. Drag and drop a plant leaf image (JPG/PNG/WebP).
3. Verify image preview and file size indicator appear.
4. Click **"Analyze with AI"**:
   - The loading indicator *"Analyzing image with AI..."* will run while sending bytes to the model.
   - Output displays confidence bar, alternative diseases, and organic/chemical treatments.
   - If confidence is below 60%, a low-confidence warning prompts for a clearer photo.

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
git commit -m "feat: complete Smart Krishi live weather and AI disease detection"
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
   - `AI_API_URL`: `https://router.huggingface.co/hf-inference/models/`
   - `AI_API_KEY`: `<your-free-huggingface-token>`
   - `AI_MODEL`: `linkanjarad/mobilenet_v2_1.0_224-plant-disease-identification`
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
| **Farmer** | `ramesh@smartkrishi.com` | `Farmer@123` | Dashboard, Profile, Farm Management, Scans, History |
| **System Admin** | `admin@smartkrishi.com` | `Admin@123` | Admin Portal, User Management, Add Mandi Prices & Advisories |

---

## 🔧 Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **CORS error in browser console** | Frontend origin missing in `CORS_ALLOWED_ORIGINS` | Add your Netlify URL to Render's `CORS_ALLOWED_ORIGINS` environment variable and redeploy. |
| **Backend 502 / Cold Start on Render** | Free tier Render instances sleep after inactivity | Free tier instances sleep after 15 mins of inactivity. The first wake-up request takes ~40 seconds. |
| **AI Disease Analysis Unavailable** | Missing `AI_API_KEY` or model cold start | Generate a free token at [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens) and configure `AI_API_KEY`. Cold models wake up in 15 seconds. |
| **Invalid Location on Weather Search** | Non-existent city name | Open-Meteo geocoding rejects non-existent names (e.g. `xxxxxxxx`). Check spelling or use GPS location. |
| **Database Connection Failure** | Incorrect JDBC URL or IP restrictions | Verify `useSSL=true&allowPublicKeyRetrieval=true` is appended to the JDBC URL. Check cloud database IP whitelisting (`0.0.0.0/0`). |
