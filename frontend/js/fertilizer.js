/**
 * SMART KRISHI - Smart Fertilizer Advisor
 * Rule-based ICAR Nutrient Management Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('fertilizer');
  UI.renderFooter();

  const form = document.getElementById('fertilizer-form');
  if (form) {
    form.addEventListener('submit', handleFertilizerSubmit);
  }
});

function updatePhDisplay(val) {
  const display = document.getElementById('ph-display');
  const badge = document.getElementById('ph-badge');
  if (!display || !badge) return;

  const num = parseFloat(val);
  display.textContent = num.toFixed(1);

  if (num < 6.0) {
    badge.className = 'badge bg-danger-subtle text-danger small';
    badge.textContent = 'Acidic (अम्लीय)';
  } else if (num > 8.0) {
    badge.className = 'badge bg-warning-subtle text-warning-emphasis small';
    badge.textContent = 'Alkaline (क्षारीय)';
  } else {
    badge.className = 'badge bg-success-subtle text-success small';
    badge.textContent = 'Neutral / Optimal (संतुलित)';
  }
}

function applyFertilizerPreset(type) {
  const cropEl = document.getElementById('fert-crop');
  const soilEl = document.getElementById('fert-soil');
  const stageEl = document.getElementById('fert-stage');
  const areaEl = document.getElementById('fert-area');
  const nEl = document.getElementById('fert-n');
  const pEl = document.getElementById('fert-p');
  const kEl = document.getElementById('fert-k');
  const phEl = document.getElementById('fert-ph');

  switch (type) {
    case 'wheat_basal':
      cropEl.value = 'Wheat';
      soilEl.value = 'Alluvial Loam';
      stageEl.value = 'Sowing / Basal';
      areaEl.value = 2.0;
      nEl.value = 180;
      pEl.value = 18;
      kEl.value = 140;
      phEl.value = 7.0;
      break;

    case 'rice_vegetative':
      cropEl.value = 'Rice';
      soilEl.value = 'Clay Loam';
      stageEl.value = 'Vegetative';
      areaEl.value = 1.5;
      nEl.value = 130;
      pEl.value = 15;
      kEl.value = 110;
      phEl.value = 6.5;
      break;

    case 'cotton_flowering':
      cropEl.value = 'Cotton';
      soilEl.value = 'Black Clay (Regur)';
      stageEl.value = 'Flowering / Booting';
      areaEl.value = 3.0;
      nEl.value = 150;
      pEl.value = 22;
      kEl.value = 180;
      phEl.value = 7.8;
      break;

    case 'tomato_acidic':
      cropEl.value = 'Tomato';
      soilEl.value = 'Red Sandy Loam';
      stageEl.value = 'Sowing / Basal';
      areaEl.value = 1.0;
      nEl.value = 160;
      pEl.value = 25;
      kEl.value = 120;
      phEl.value = 5.2;
      break;

    case 'mustard_low_k':
      cropEl.value = 'Mustard';
      soilEl.value = 'Sandy Loam';
      stageEl.value = 'Tillering / Branching';
      areaEl.value = 2.5;
      nEl.value = 140;
      pEl.value = 12;
      kEl.value = 95;
      phEl.value = 7.2;
      break;
  }

  updatePhDisplay(phEl.value);
  // Auto-submit preset calculation
  handleFertilizerSubmit(new Event('submit'));
}

function resetForm() {
  const form = document.getElementById('fertilizer-form');
  if (form) form.reset();
  updatePhDisplay(7.0);

  document.getElementById('fert-placeholder')?.classList.remove('d-none');
  document.getElementById('fert-loading')?.classList.add('d-none');
  document.getElementById('fert-results')?.classList.add('d-none');
}

async function handleFertilizerSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();

  const crop = document.getElementById('fert-crop').value;
  const soilType = document.getElementById('fert-soil').value;
  const growthStage = document.getElementById('fert-stage').value;
  const landArea = parseFloat(document.getElementById('fert-area').value);
  const nitrogen = parseFloat(document.getElementById('fert-n').value);
  const phosphorus = parseFloat(document.getElementById('fert-p').value);
  const potassium = parseFloat(document.getElementById('fert-k').value);
  const ph = parseFloat(document.getElementById('fert-ph').value);

  if (isNaN(landArea) || landArea <= 0) {
    UI.showToast('Please enter a valid land area in acres', 'warning');
    return;
  }

  const payload = {
    crop,
    soilType,
    growthStage,
    landArea,
    nitrogen,
    phosphorus,
    potassium,
    ph
  };

  const placeholderEl = document.getElementById('fert-placeholder');
  const loadingEl = document.getElementById('fert-loading');
  const resultsEl = document.getElementById('fert-results');
  const btn = document.getElementById('btn-calculate-fert');

  try {
    if (placeholderEl) placeholderEl.classList.add('d-none');
    if (resultsEl) resultsEl.classList.add('d-none');
    if (loadingEl) loadingEl.classList.remove('d-none');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Calculating...';
    }

    const response = await API.post('/api/fertilizer/recommend', payload);

    if (loadingEl) loadingEl.classList.add('d-none');
    if (resultsEl) resultsEl.classList.remove('d-none');

    const data = (response && response.data) ? response.data : response;
    renderFertilizerResults(data, payload);
  } catch (error) {
    console.error('Fertilizer calculation error:', error);
    if (loadingEl) loadingEl.classList.add('d-none');
    if (placeholderEl) placeholderEl.classList.remove('d-none');
    UI.showToast(error.message || 'Failed to calculate fertilizer recommendations. Please verify backend connection.', 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-calculator me-1"></i>Calculate Nutrient Dosage';
    }
  }
}

function renderFertilizerResults(data, input) {
  // Target crop badge
  const cropBadge = document.getElementById('res-target-crop-badge');
  if (cropBadge) {
    cropBadge.textContent = `${data.crop} • Stage: ${data.growthStage}`;
  }

  // Land area pill
  const landPill = document.getElementById('res-land-area-pill');
  if (landPill) {
    landPill.textContent = `Prescribed for ${input.landArea} Acre(s) (${(input.landArea / 2.47).toFixed(2)} Ha)`;
  }

  // Summary message
  const summaryEl = document.getElementById('res-summary-text');
  if (summaryEl) {
    summaryEl.textContent = data.summary || 'Nutrient deficit evaluated against ICAR standard recommendations.';
  }

  // NPK Status indicators
  const nStatusEl = document.getElementById('res-n-status');
  const nDeficitEl = document.getElementById('res-n-deficit');
  if (nStatusEl && nDeficitEl) {
    nStatusEl.textContent = data.nitrogenStatus || 'Evaluated';
    nStatusEl.className = getStatusClass(data.nitrogenStatus);
    nDeficitEl.textContent = `Current: ${input.nitrogen} kg/ha`;
  }

  const pStatusEl = document.getElementById('res-p-status');
  const pDeficitEl = document.getElementById('res-p-deficit');
  if (pStatusEl && pDeficitEl) {
    pStatusEl.textContent = data.phosphorusStatus || 'Evaluated';
    pStatusEl.className = getStatusClass(data.phosphorusStatus);
    pDeficitEl.textContent = `Current: ${input.phosphorus} kg/ha`;
  }

  const kStatusEl = document.getElementById('res-k-status');
  const kDeficitEl = document.getElementById('res-k-deficit');
  if (kStatusEl && kDeficitEl) {
    kStatusEl.textContent = data.potassiumStatus || 'Evaluated';
    kStatusEl.className = getStatusClass(data.potassiumStatus);
    kDeficitEl.textContent = `Current: ${input.potassium} kg/ha`;
  }

  const phStatusEl = document.getElementById('res-ph-status');
  const phCommentEl = document.getElementById('res-ph-comment');
  if (phStatusEl && phCommentEl) {
    phStatusEl.textContent = `pH ${input.ph.toFixed(1)}`;
    phCommentEl.textContent = data.phStatus || 'Evaluated';
  }

  // Fertilizer Commercial Items
  const itemsContainer = document.getElementById('fertilizer-items-container');
  if (itemsContainer) {
    if (!data.fertilizers || data.fertilizers.length === 0) {
      itemsContainer.innerHTML = `
        <div class="col-12">
          <div class="alert alert-info small mb-0">
            No chemical fertilizers required at this stage. Soil nutrients are currently at or above ICAR target thresholds.
          </div>
        </div>
      `;
    } else {
      const fertList = data.fertilizers || data.recommendedFertilizers || [];
      itemsContainer.innerHTML = fertList.map(fertilizer => {
        const bagInfo = (fertilizer.bagCount != null && fertilizer.bagSizeKg != null)
          ? `${fertilizer.bagCount} Bag${fertilizer.bagCount === 1 ? '' : 's'} (${fertilizer.bagSizeKg} kg)`
          : (fertilizer.bagCount ? `${fertilizer.bagCount} Bags` : '');
        const qty = fertilizer.totalQuantityKg != null ? fertilizer.totalQuantityKg : (fertilizer.dosePerAcreKg != null ? fertilizer.dosePerAcreKg : 0);
        const cost = fertilizer.estimatedCost != null ? fertilizer.estimatedCost : (fertilizer.approximateCost != null ? fertilizer.approximateCost : 0);
        const method = fertilizer.applicationMethod || fertilizer.usageInstruction || fertilizer.nutrientProvided || 'Apply in moist soil conditions.';

        return `
        <div class="col-md-6 col-lg-4">
          <div class="dosage-card">
            <div class="d-flex justify-content-between align-items-center mb-1">
              <span class="badge bg-success-subtle text-success small fw-bold">${fertilizer.name}</span>
              <span class="small text-muted">${bagInfo}</span>
            </div>
            <div class="dosage-val my-1">${qty} <span class="fs-6 text-muted fw-normal">kg</span></div>
            <div class="small fw-semibold text-success mb-2">₹ ${Math.round(cost).toLocaleString('en-IN')}</div>
            <p class="text-secondary small mb-0" style="font-size: 0.8rem; line-height: 1.35;">
              ${method}
            </p>
          </div>
        </div>
      `;
      }).join('');
    }
  }

  // Total cost
  const totalCostEl = document.getElementById('res-total-cost');
  if (totalCostEl) {
    const totalCost = data.estimatedTotalCost != null 
      ? data.estimatedTotalCost 
      : (data.approximateTotalCost != null ? data.approximateTotalCost : 0);
    totalCostEl.textContent = `₹ ${Math.round(totalCost).toLocaleString('en-IN')}`;
  }

  // Split Schedule
  const splitContainer = document.getElementById('split-schedule-container');
  if (splitContainer) {
    if (data.splitSchedule && data.splitSchedule.length > 0) {
      splitContainer.innerHTML = data.splitSchedule.map((s, idx) => `
        <div class="p-2 border rounded bg-white d-flex align-items-center gap-2">
          <span class="split-pill"><i class="bi bi-clock-history"></i> Split ${idx + 1}</span>
          <span class="small text-dark fw-medium">${s}</span>
        </div>
      `).join('');
    } else {
      splitContainer.innerHTML = `<div class="small text-muted">Apply standard split doses per local agricultural university schedule.</div>`;
    }
  }

  // Precautions / Warnings
  const warningsList = document.getElementById('res-warnings-list');
  if (warningsList) {
    if (data.precautions && data.precautions.length > 0) {
      warningsList.innerHTML = data.precautions.map(w => `
        <li class="d-flex align-items-start gap-2">
          <i class="bi bi-check-circle-fill text-success mt-1 flex-shrink-0"></i>
          <span class="text-secondary">${w}</span>
        </li>
      `).join('');
    } else {
      warningsList.innerHTML = `<li class="text-muted">Ensure adequate soil moisture prior to fertilizer application.</li>`;
    }
  }
}

function getStatusClass(status) {
  if (!status) return 'fw-bold fs-6';
  const s = status.toUpperCase();
  if (s.includes('DEFICIENT') || s.includes('LOW') || s.includes('CRITICAL')) {
    return 'fw-bold fs-6 text-danger';
  } else if (s.includes('HIGH') || s.includes('EXCESS')) {
    return 'fw-bold fs-6 text-warning-emphasis';
  }
  return 'fw-bold fs-6 text-success';
}
