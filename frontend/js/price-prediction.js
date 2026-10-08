/**
 * SMART KRISHI - Crop Price Prediction & Trend
 * Statistical Baseline Trend Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('price-prediction');
  UI.renderFooter();

  const form = document.getElementById('prediction-filter-form');
  if (form) {
    form.addEventListener('submit', handlePredictionSubmit);
  }

  // Load default prediction
  selectPredictionPreset('Wheat', 'All', 10);
});

function selectPredictionPreset(crop, state, days) {
  const cropEl = document.getElementById('pred-crop');
  const stateEl = document.getElementById('pred-state');
  const daysEl = document.getElementById('pred-days');

  if (cropEl) cropEl.value = crop;
  if (stateEl) stateEl.value = state || 'All';
  if (daysEl) daysEl.value = days || 10;

  loadPrediction(crop, state, days);
}

function handlePredictionSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();

  const crop = document.getElementById('pred-crop').value;
  const state = document.getElementById('pred-state').value;
  const days = document.getElementById('pred-days').value;

  loadPrediction(crop, state, days);
}

async function loadPrediction(crop, state, days) {
  const loadingEl = document.getElementById('pred-loading');
  const insufficientEl = document.getElementById('pred-insufficient');
  const resultsEl = document.getElementById('pred-results');
  const btn = document.getElementById('btn-predict');

  try {
    if (loadingEl) loadingEl.classList.remove('d-none');
    if (insufficientEl) insufficientEl.classList.add('d-none');
    if (resultsEl) resultsEl.classList.add('d-none');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Analyzing...';
    }

    const params = {
      days: days || 10
    };
    if (state && state !== 'All') {
      params.state = state;
    }

    const response = await API.get(`/api/price-prediction/${encodeURIComponent(crop)}`, params);
    if (loadingEl) loadingEl.classList.add('d-none');

    const data = (response && response.data) ? response.data : response;

    if (data && data.status === 'INSUFFICIENT_DATA') {
      if (insufficientEl) {
        insufficientEl.classList.remove('d-none');
        const msgEl = document.getElementById('insufficient-msg-text');
        if (msgEl) {
          msgEl.textContent = data.message || 'Not enough historical market records to produce a reliable statistical prediction.';
        }
      }
    } else if (data) {
      if (resultsEl) {
        resultsEl.classList.remove('d-none');
        renderPredictionResults(data);
      }
    }
  } catch (error) {
    console.error('Error fetching price prediction:', error);
    if (loadingEl) loadingEl.classList.add('d-none');
    UI.showToast(error.message || 'Unable to analyze market price trend.', 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-graph-up me-1"></i>Analyze Trend';
    }
  }
}

function renderPredictionResults(data) {
  if (!data) return;

  // Title & Location
  document.getElementById('pred-crop-title').textContent = data.crop || 'Commodity';
  document.getElementById('pred-location-title').textContent = `APMC Mandis: ${data.location || 'All India Record'}`;

  // Trend Badge
  const trendBadge = document.getElementById('pred-trend-badge');
  if (trendBadge) {
    const trend = (data.trend || 'STABLE').toUpperCase();
    const chg = data.changePercentage || 0;
    const sign = chg >= 0 ? '+' : '';

    if (trend === 'INCREASING') {
      trendBadge.className = 'badge-trend-increasing';
      trendBadge.innerHTML = `<i class="bi bi-arrow-up-right me-1"></i>RISING (${sign}${chg.toFixed(1)}%)`;
    } else if (trend === 'DECREASING') {
      trendBadge.className = 'badge-trend-decreasing';
      trendBadge.innerHTML = `<i class="bi bi-arrow-down-right me-1"></i>FALLING (${sign}${chg.toFixed(1)}%)`;
    } else {
      trendBadge.className = 'badge-trend-stable';
      trendBadge.innerHTML = `<i class="bi bi-arrow-right me-1"></i>STABLE (${sign}${chg.toFixed(1)}%)`;
    }
  }

  // Statistical Confidence
  const confEl = document.getElementById('pred-confidence-score');
  if (confEl) {
    confEl.textContent = `${Math.round(data.confidenceScore || 75)}%`;
  }

  // Unit
  const unit = data.priceUnit || '₹/Quintal';
  document.getElementById('pred-price-unit').textContent = unit;

  // Latest modal price
  document.getElementById('pred-latest-price').textContent = `₹ ${Math.round(data.currentPrice || 0).toLocaleString()}`;

  // Forecast price
  document.getElementById('pred-horizon-label').textContent = `Forecast in ${data.horizonDays || 10} Days`;
  document.getElementById('pred-forecast-price').textContent = `₹ ${Math.round(data.predictedPrice || 0).toLocaleString()}`;

  // Change amount
  const diff = (data.predictedPrice || 0) - (data.currentPrice || 0);
  const diffSign = diff >= 0 ? '+' : '';
  const chgPct = data.changePercentage || 0;
  document.getElementById('pred-forecast-change').textContent = `${diffSign}₹ ${Math.round(Math.abs(diff))} / Qtl (${diffSign}${chgPct.toFixed(1)}%)`;

  // Range
  document.getElementById('pred-expected-range').textContent =
    `₹ ${Math.round(data.predictedMinPrice || 0).toLocaleString()} – ₹ ${Math.round(data.predictedMaxPrice || 0).toLocaleString()}`;

  // Moving average
  document.getElementById('pred-moving-avg').textContent = `₹ ${Math.round(data.movingAverage || 0).toLocaleString()}`;

  // Historical table
  const tbody = document.getElementById('pred-history-tbody');
  if (tbody) {
    if (data.historicalPoints && data.historicalPoints.length > 0) {
      tbody.innerHTML = data.historicalPoints.map(p => `
        <tr>
          <td><i class="bi bi-calendar3 text-muted me-1"></i>${p.date || 'Recent'}</td>
          <td><span class="fw-semibold text-dark">${p.marketName || 'APMC Mandi'}</span></td>
          <td class="text-end text-muted">₹ ${(p.minPrice || 0).toLocaleString()}</td>
          <td class="text-end text-muted">₹ ${(p.maxPrice || 0).toLocaleString()}</td>
          <td class="text-end fw-bold text-success">₹ ${(p.modalPrice || 0).toLocaleString()}</td>
        </tr>
      `).join('');
    } else {
      tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No detailed historical rows recorded.</td></tr>`;
    }
  }

  // Methodology & Disclaimer
  const methEl = document.getElementById('pred-methodology-text');
  if (methEl && data.methodology) {
    methEl.textContent = data.methodology;
  }

  const discEl = document.getElementById('pred-disclaimer-text');
  if (discEl && data.disclaimer) {
    discEl.textContent = data.disclaimer;
  }
}
