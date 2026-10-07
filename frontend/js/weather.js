/**
 * SMART KRISHI - Live Weather & Precision Irrigation Controller
 * Connects directly to backend Open-Meteo integration for genuine live telemetry.
 */
document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('weather');
  UI.renderFooter();

  // Check if user has district or default to Jaipur / Pune
  let initialCity = 'Jaipur';
  if (AUTH.isAuthenticated()) {
    const user = AUTH.getUser();
    if (user && user.district) {
      initialCity = user.district;
    }
  }

  document.getElementById('weather-search-input').value = initialCity;
  fetchWeather(initialCity);

  const searchForm = document.getElementById('weather-search-form');
  if (searchForm) {
    searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const city = document.getElementById('weather-search-input').value.trim();
      if (city) {
        fetchWeather(city);
      }
    });
  }
});

/**
 * Fetch live weather by city name using backend Open-Meteo geocoding & forecast API
 */
async function fetchWeather(city) {
  const btn = document.getElementById('btn-weather-search');
  setSearchLoading(true);
  hideWeatherError();

  try {
    const res = await API.get('/api/weather', { city });
    if (res && res.data) {
      renderWeatherDetails(res.data);
      document.getElementById('weather-search-input').value = res.data.location || city;
    } else {
      showWeatherError(`Unable to retrieve live weather for '${city}'.`);
    }
  } catch (err) {
    console.error('Weather load error:', err);
    showWeatherError(err.message || `Live weather temporarily unavailable for '${city}'.`);
  } finally {
    setSearchLoading(false);
  }
}

/**
 * Fetch live weather using browser GPS coordinates
 */
async function useMyLocation() {
  if (!navigator.geolocation) {
    UI.showToast('Geolocation is not supported by your browser', 'warning');
    return;
  }

  const locBtn = document.getElementById('btn-use-location');
  if (locBtn) {
    locBtn.disabled = true;
    locBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Locating...`;
  }
  hideWeatherError();

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const lat = position.coords.latitude;
      const lon = position.coords.longitude;

      try {
        const res = await API.get('/api/weather', { latitude: lat, longitude: lon });
        if (res && res.data) {
          renderWeatherDetails(res.data);
          document.getElementById('weather-search-input').value = res.data.location;
          UI.showToast(`Loaded live weather for ${res.data.location}`, 'success');
        } else {
          showWeatherError('Unable to retrieve weather for your GPS coordinates.');
        }
      } catch (err) {
        console.error('Coordinate weather error:', err);
        showWeatherError(err.message || 'Live weather temporarily unavailable for your coordinates.');
      } finally {
        if (locBtn) {
          locBtn.disabled = false;
          locBtn.innerHTML = `<i class="bi bi-crosshair me-1"></i>Use My Location`;
        }
      }
    },
    (err) => {
      console.warn('Geolocation error:', err);
      let msg = 'Location access denied. Please type your city or district manually.';
      if (err.code === err.POSITION_UNAVAILABLE) {
        msg = 'Location information is currently unavailable.';
      } else if (err.code === err.TIMEOUT) {
        msg = 'Request to obtain GPS coordinates timed out.';
      }
      UI.showToast(msg, 'warning');
      if (locBtn) {
        locBtn.disabled = false;
        locBtn.innerHTML = `<i class="bi bi-crosshair me-1"></i>Use My Location`;
      }
    },
    { timeout: 10000, enableHighAccuracy: true }
  );
}

/**
 * Render complete live weather metrics, irrigation advice, and 5-day forecast
 */
function renderWeatherDetails(w) {
  hideWeatherError();

  // Location & Header
  document.getElementById('weather-city-name').textContent = w.location || 'Unknown Location';
  if (w.latitude != null && w.longitude != null) {
    document.getElementById('weather-coords-tag').textContent = `Lat: ${w.latitude.toFixed(2)}°, Lon: ${w.longitude.toFixed(2)}°`;
  } else {
    document.getElementById('weather-coords-tag').textContent = 'Live Telemetry';
  }

  // Temperatures & Conditions
  const tempVal = w.temperature != null ? Math.round(w.temperature * 10) / 10 : '--';
  const feelsVal = (w.feelsLike != null ? w.feelsLike : w.apparentTemperature);
  const feelsFormatted = feelsVal != null ? Math.round(feelsVal * 10) / 10 : tempVal;

  document.getElementById('weather-temp').textContent = `${tempVal} °C`;
  document.getElementById('weather-feels-like').textContent = `Feels like ${feelsFormatted} °C`;
  document.getElementById('weather-condition').textContent = w.condition || 'Clear Sky';
  document.getElementById('weather-updated-time').innerHTML = `<i class="bi bi-clock-history me-1"></i>Updated: ${w.lastUpdated || 'Just now'}`;

  // Atmospheric metrics
  document.getElementById('weather-humidity').textContent = `${w.humidity != null ? w.humidity : '--'}%`;
  document.getElementById('weather-wind').textContent = `${w.windSpeed != null ? w.windSpeed : '--'} km/h`;
  const rainVal = (w.precipitation != null ? w.precipitation : w.rainfall);
  document.getElementById('weather-rain').textContent = `${rainVal != null ? rainVal.toFixed(1) : '0.0'} mm`;
  document.getElementById('weather-cloud').textContent = `${w.cloudCover != null ? w.cloudCover : '0'}%`;

  // Main Icon
  const iconEl = document.getElementById('weather-main-icon');
  if (iconEl) {
    iconEl.className = `bi ${w.conditionIcon || 'bi-sun-fill'} display-1 text-warning`;
  }

  // Irrigation Guidance Card
  const advice = w.irrigationAdvice || {};
  const adviceCard = document.getElementById('irrigation-advice-card');
  if (adviceCard) {
    let alertClass = 'alert-success';
    if (advice.badgeColor === 'danger') alertClass = 'alert-danger';
    if (advice.badgeColor === 'warning') alertClass = 'alert-warning';
    if (advice.badgeColor === 'info') alertClass = 'alert-info';

    adviceCard.className = `alert ${alertClass} p-3 rounded-3 shadow-sm border mb-4`;
    adviceCard.innerHTML = `
      <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
        <span class="badge bg-${advice.badgeColor || 'success'} fs-6">${advice.headline || 'Irrigation Guide'}</span>
        <span class="text-muted small ms-auto"><i class="bi bi-moisture me-1"></i>Soil Moisture Status: <strong>${advice.soilMoistureEstimation || 'Normal'}</strong></span>
      </div>
      <p class="mb-2 text-dark">${advice.detailedAdvice || 'Optimum soil moisture present.'}</p>
      <div class="small fw-semibold text-secondary">
        <i class="bi bi-clock-history me-1 text-primary"></i>Recommended Watering Window: 
        <span class="text-dark">${advice.bestTime || 'Early morning'}</span>
      </div>
    `;
  }

  // 5-Day Forecast Grid
  const forecastContainer = document.getElementById('weather-forecast-container');
  if (forecastContainer && w.forecast && w.forecast.length > 0) {
    forecastContainer.innerHTML = w.forecast.map((f, i) => `
      <div class="col">
        <div class="weather-forecast-card p-3 rounded-3 text-center border h-100 bg-light">
          <div class="fw-bold text-dark mb-1">${i === 0 ? 'Today' : f.dayOfWeek}</div>
          <div class="small text-muted mb-2">${f.date}</div>
          <i class="bi ${f.conditionIcon || 'bi-cloud-sun'} fs-2 text-warning mb-2 d-block"></i>
          <div class="fw-bold text-dark fs-6">${Math.round(f.tempMax)}° / <span class="text-muted">${Math.round(f.tempMin)}°</span></div>
          <div class="small text-secondary text-truncate mt-1" title="${f.condition}">${f.condition}</div>
          <div class="small text-info mt-2">
            <i class="bi bi-droplet me-1"></i>${Math.round(f.precipitationProbability || 0)}% Rain (${(f.rainAmountMm || 0).toFixed(1)} mm)
          </div>
        </div>
      </div>
    `).join('');
  }
}

function showWeatherError(message) {
  const errBox = document.getElementById('weather-error-box');
  const errMsg = document.getElementById('weather-error-message');
  if (errBox && errMsg) {
    errMsg.textContent = message;
    errBox.classList.remove('d-none');
    errBox.scrollIntoView({ behavior: 'smooth' });
  }
  UI.showToast(message, 'error');
}

function hideWeatherError() {
  const errBox = document.getElementById('weather-error-box');
  if (errBox) {
    errBox.classList.add('d-none');
  }
}

function setSearchLoading(isLoading) {
  const btn = document.getElementById('btn-weather-search');
  if (!btn) return;
  if (isLoading) {
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Loading...`;
  } else {
    btn.disabled = false;
    btn.innerHTML = `<i class="bi bi-search me-1"></i>Search Location`;
  }
}

function searchChip(cityName) {
  document.getElementById('weather-search-input').value = cityName;
  fetchWeather(cityName);
}
