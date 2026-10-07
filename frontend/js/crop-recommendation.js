/**
 * SMART KRISHI - Crop Recommendation Controller
 */
document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('crops');
  UI.renderFooter();

  const form = document.getElementById('crop-rec-form');
  if (form) {
    form.addEventListener('submit', handleCropRecommendation);
  }

  // Pre-load user soil type if logged in
  if (AUTH.isAuthenticated()) {
    const user = AUTH.getUser();
    if (user && user.soilType) {
      const soilSelect = document.getElementById('rec-soil');
      if (soilSelect) {
        for (let opt of soilSelect.options) {
          if (opt.value.toLowerCase().includes(user.soilType.toLowerCase()) || user.soilType.toLowerCase().includes(opt.value.toLowerCase())) {
            opt.selected = true;
            break;
          }
        }
      }
    }
    loadRecommendationHistory(user.id);
  }
});

async function handleCropRecommendation(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-recommend-submit');
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>Analyzing Soil & Agro-climatic Factors...`;

  const payload = {
    nitrogen: parseFloat(document.getElementById('rec-n').value),
    phosphorus: parseFloat(document.getElementById('rec-p').value),
    potassium: parseFloat(document.getElementById('rec-k').value),
    ph: parseFloat(document.getElementById('rec-ph').value),
    temperature: parseFloat(document.getElementById('rec-temp').value),
    humidity: parseFloat(document.getElementById('rec-humidity').value),
    rainfall: parseFloat(document.getElementById('rec-rainfall').value),
    soilType: document.getElementById('rec-soil').value
  };

  try {
    const res = await API.post('/api/crops/recommend', payload);
    if (res && res.data) {
      renderRecommendationResult(res.data);
      UI.showToast(`Recommended Crop: ${res.data.recommendedCrop}!`, 'success');

      // Refresh history if logged in
      if (AUTH.isAuthenticated()) {
        const user = AUTH.getUser();
        loadRecommendationHistory(user.id);
      }
    }
  } catch (err) {
    console.error('Crop recommendation error:', err);
    UI.showToast(err.message || 'Failed to generate recommendation', 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<i class="bi bi-cpu me-2"></i>Analyze & Recommend Best Crop`;
  }
}

function renderRecommendationResult(r) {
  const resultCard = document.getElementById('recommendation-result-card');
  resultCard.classList.remove('d-none');
  resultCard.scrollIntoView({ behavior: 'smooth' });

  document.getElementById('res-crop-name').textContent = r.recommendedCrop;
  document.getElementById('res-season-badge').textContent = r.suitableSeason + ' Season';
  document.getElementById('res-crop-desc').textContent = r.cropDetails || 'High yielding crop suitable for current agronomic conditions.';
  document.getElementById('res-growing-cond').textContent = r.growingConditions || 'Maintain optimum moisture and fertility.';

  // Format fertilizer suggestions
  const fertBox = document.getElementById('res-fertilizer-box');
  fertBox.innerHTML = (r.fertilizerSuggestion || '')
    .split('\n')
    .filter(line => line.trim().length > 0)
    .map(line => `<div class="mb-1"><i class="bi bi-arrow-right-circle text-success me-2"></i>${line.replace(/^[•\s]+/, '')}</div>`)
    .join('');

  document.getElementById('res-inputs-summary').textContent =
    `N: ${r.nitrogen} kg/ha | P: ${r.phosphorus} kg/ha | K: ${r.potassium} kg/ha | pH: ${r.ph} | Temp: ${r.temperature}°C | Humidity: ${r.humidity}% | Rain: ${r.rainfall}mm | Soil: ${r.soilType}`;
}

async function loadRecommendationHistory(userId) {
  try {
    const res = await API.get(`/api/farmers/${userId}/recommendations`);
    const historyContainer = document.getElementById('rec-history-container');
    if (!historyContainer) return;

    if (!res.data || res.data.length === 0) {
      historyContainer.innerHTML = `<div class="text-muted small py-2">No prior recommendations on file.</div>`;
      return;
    }

    historyContainer.innerHTML = res.data.slice(0, 5).map(item => `
      <div class="p-3 rounded border mb-2 bg-white">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <strong class="text-success fs-6">${item.recommendedCrop}</strong>
          <span class="badge bg-light text-dark border">${item.suitableSeason}</span>
        </div>
        <div class="small text-muted mb-1">Soil: ${item.soilType} • Rain: ${item.rainfall}mm • pH: ${item.ph}</div>
        <div class="small text-secondary text-truncate">${item.cropDetails || ''}</div>
      </div>
    `).join('');
  } catch (err) {
    console.warn('Could not load recommendation history:', err);
  }
}

// Preset fills for instant testing
function applyPreset(cropType) {
  if (cropType === 'rice') {
    document.getElementById('rec-n').value = 90;
    document.getElementById('rec-p').value = 45;
    document.getElementById('rec-k').value = 40;
    document.getElementById('rec-ph').value = 6.2;
    document.getElementById('rec-temp').value = 27;
    document.getElementById('rec-humidity').value = 82;
    document.getElementById('rec-rainfall').value = 220;
    document.getElementById('rec-soil').value = 'Clay';
  } else if (cropType === 'wheat') {
    document.getElementById('rec-n').value = 110;
    document.getElementById('rec-p').value = 55;
    document.getElementById('rec-k').value = 45;
    document.getElementById('rec-ph').value = 6.8;
    document.getElementById('rec-temp').value = 18;
    document.getElementById('rec-humidity').value = 55;
    document.getElementById('rec-rainfall').value = 65;
    document.getElementById('rec-soil').value = 'Alluvial';
  } else if (cropType === 'cotton') {
    document.getElementById('rec-n').value = 115;
    document.getElementById('rec-p').value = 50;
    document.getElementById('rec-k').value = 45;
    document.getElementById('rec-ph').value = 7.0;
    document.getElementById('rec-temp').value = 28;
    document.getElementById('rec-humidity').value = 60;
    document.getElementById('rec-rainfall').value = 85;
    document.getElementById('rec-soil').value = 'Black Soil';
  } else if (cropType === 'potato') {
    document.getElementById('rec-n').value = 110;
    document.getElementById('rec-p').value = 85;
    document.getElementById('rec-k').value = 95;
    document.getElementById('rec-ph').value = 5.8;
    document.getElementById('rec-temp').value = 18;
    document.getElementById('rec-humidity').value = 70;
    document.getElementById('rec-rainfall').value = 60;
    document.getElementById('rec-soil').value = 'Sandy Loam';
  }
  UI.showToast(`Applied preset parameters for ${cropType.toUpperCase()}`, 'info');
}

async function autofillWeather() {
  const btn = document.getElementById('btn-autofill-weather');
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Fetching Weather...`;

  const user = AUTH.getUser();
  const city = (user && user.district) ? user.district : 'Pune';

  try {
    const res = await API.get('/api/weather', { city });
    if (res && res.data) {
      document.getElementById('rec-temp').value = Math.round(res.data.temperature);
      document.getElementById('rec-humidity').value = res.data.humidity;
      document.getElementById('rec-rainfall').value = res.data.rainfall > 0 ? res.data.rainfall : 75; // realistic rainfall estimate if 0 today
      UI.showToast(`Populated live temperature & humidity for ${res.data.location}!`, 'success');
    }
  } catch (err) {
    console.error('Failed to autofill weather:', err);
    UI.showToast('Could not fetch local weather', 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<i class="bi bi-cloud-sun me-1"></i>Use Live Weather Data`;
  }
}
