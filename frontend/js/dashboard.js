/**
 * SMART KRISHI - Farmer Dashboard Controller
 */
document.addEventListener('DOMContentLoaded', async () => {
  UI.renderNavbar('dashboard');
  UI.renderFooter();

  // Guard: require authenticated user
  if (!AUTH.requireAuth()) return;

  const user = AUTH.getUser();
  document.getElementById('dashboard-farmer-name').textContent = user.name || 'Farmer';
  document.getElementById('farmer-location-tag').textContent = `${user.village || 'Village'}, ${user.district || 'District'}, ${user.state || 'State'}`;
  document.getElementById('farmer-land-tag').textContent = `${user.landArea || 0} Acres (${user.soilType || 'Soil'})`;

  loadDashboardData(user);
});

async function loadDashboardData(user) {
  // 1. Load Weather for user's district or default city
  const city = user.district || 'Pune';
  try {
    const weatherRes = await API.get('/api/weather', { city });
    if (weatherRes && weatherRes.data) {
      renderDashboardWeather(weatherRes.data);
    }
  } catch (err) {
    console.warn('Dashboard weather load error:', err);
    document.getElementById('dash-weather-box').innerHTML = `
      <div class="text-muted small p-2"><i class="bi bi-info-circle me-1"></i>Weather updates temporarily unavailable</div>
    `;
  }

  // 2. Load Market Highlights
  try {
    const marketRes = await API.get('/api/market/highlights');
    if (marketRes && marketRes.data) {
      renderDashboardMarket(marketRes.data);
    }
  } catch (err) {
    console.warn('Dashboard market highlights load error:', err);
  }

  // 3. Load Farmer's Recent Recommendations
  try {
    const recRes = await API.get(`/api/farmers/${user.id}/recommendations`);
    if (recRes && recRes.data) {
      renderRecentRecommendations(recRes.data);
    }
  } catch (err) {
    console.warn('Dashboard recommendations error:', err);
  }
}

function renderDashboardWeather(w) {
  const box = document.getElementById('dash-weather-box');
  const advice = w.irrigationAdvice || {};
  box.innerHTML = `
    <div class="d-flex align-items-center justify-content-between">
      <div>
        <div class="fs-1 fw-bold text-dark mb-0">${w.temperature != null ? w.temperature.toFixed(1) : '--'}°C</div>
        <div class="text-secondary fw-semibold"><span class="badge bg-success-subtle text-success border border-success-subtle me-1">Live Weather</span>${w.condition} • ${w.location}</div>
        <div class="small text-muted mt-1">
          <span><i class="bi bi-droplet-fill text-primary"></i> ${w.humidity}% Hum</span>
          <span class="mx-2">•</span>
          <span><i class="bi bi-wind text-secondary"></i> ${w.windSpeed} km/h</span>
          <span class="mx-2">•</span>
          <span><i class="bi bi-cloud-rain text-info"></i> ${w.rainfall} mm</span>
        </div>
      </div>
      <div class="text-end">
        <i class="bi ${w.conditionIcon || 'bi-sun-fill'} display-4 text-warning"></i>
      </div>
    </div>
    <div class="mt-3 p-2 rounded bg-light border">
      <div class="d-flex align-items-center gap-2 small">
        <span class="badge bg-${advice.badgeColor || 'success'}">${advice.headline || 'Irrigation Guide'}</span>
        <span class="text-secondary text-truncate">${advice.detailedAdvice || 'Optimum soil moisture present.'}</span>
      </div>
    </div>
  `;
}

function renderDashboardMarket(items) {
  const container = document.getElementById('dash-market-container');
  if (!items || items.length === 0) {
    container.innerHTML = `<div class="text-muted small py-3 text-center">No market prices available.</div>`;
    return;
  }

  container.innerHTML = items.slice(0, 4).map(p => {
    let trendBadge = `<span class="badge badge-trend-stable"><i class="bi bi-dash"></i> Stable</span>`;
    if (p.trend === 'RISING') trendBadge = `<span class="badge badge-trend-rising"><i class="bi bi-arrow-up-right"></i> Rising</span>`;
    if (p.trend === 'FALLING') trendBadge = `<span class="badge badge-trend-falling"><i class="bi bi-arrow-down-right"></i> Falling</span>`;

    return `
      <div class="d-flex align-items-center justify-content-between p-2 rounded border mb-2 bg-white">
        <div>
          <div class="fw-bold text-dark">${p.cropName} <small class="text-muted">(${p.variety || 'Standard'})</small></div>
          <div class="small text-muted">${p.marketName}, ${p.state}</div>
        </div>
        <div class="text-end">
          <div class="fw-bold text-success fs-6">${UI.formatCurrency(p.modalPrice)} <small class="text-muted font-monospace">${p.unit}</small></div>
          <div>${trendBadge}</div>
        </div>
      </div>
    `;
  }).join('');
}

function renderRecentRecommendations(items) {
  const container = document.getElementById('dash-recent-recs');
  if (!items || items.length === 0) {
    container.innerHTML = `
      <div class="text-center py-4 text-muted">
        <i class="bi bi-sprout fs-2 text-secondary mb-2 d-block"></i>
        <p class="mb-2">No crop recommendations yet.</p>
        <a href="crop-recommendation.html" class="btn btn-sm btn-agro-outline">Get Recommendation</a>
      </div>
    `;
    return;
  }

  const latest = items[0];
  container.innerHTML = `
    <div class="p-3 rounded border border-success-subtle bg-success-subtle bg-opacity-25 mb-3">
      <div class="d-flex justify-content-between align-items-start">
        <div>
          <span class="badge bg-success mb-1">Recommended Crop</span>
          <h4 class="fw-bold text-success mb-1">${latest.recommendedCrop}</h4>
          <div class="small text-muted">Season: <strong>${latest.suitableSeason}</strong> • Soil: <strong>${latest.soilType}</strong></div>
        </div>
        <i class="bi bi-patch-check-fill text-success fs-2"></i>
      </div>
      <p class="small text-secondary mt-2 mb-2">${latest.cropDetails || ''}</p>
      <div class="small p-2 rounded bg-white border">
        <strong>Nutrients Tested:</strong> N: ${latest.nitrogen} | P: ${latest.phosphorus} | K: ${latest.potassium} | pH: ${latest.ph}
      </div>
    </div>
  `;
}
