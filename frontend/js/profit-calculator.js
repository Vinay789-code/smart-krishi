/**
 * SMART KRISHI - Crop Profit & ROI Calculator
 * Farm Economics Engine Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('profit');
  UI.renderFooter();

  const form = document.getElementById('profit-form');
  if (form) {
    form.addEventListener('submit', handleProfitSubmit);
  }
});

function applyProfitPreset(type) {
  const cropEl = document.getElementById('prof-crop');
  const areaEl = document.getElementById('prof-area');
  const yieldEl = document.getElementById('prof-yield');
  const priceEl = document.getElementById('prof-price');
  const seedEl = document.getElementById('prof-seed');
  const fertEl = document.getElementById('prof-fert');
  const pestEl = document.getElementById('prof-pest');
  const laborEl = document.getElementById('prof-labor');
  const irrigEl = document.getElementById('prof-irrig');
  const machEl = document.getElementById('prof-mach');
  const otherEl = document.getElementById('prof-other');

  switch (type) {
    case 'wheat_2acres':
      cropEl.value = 'Wheat';
      areaEl.value = 2.0;
      yieldEl.value = 20.0;
      priceEl.value = 2550;
      seedEl.value = 3500;
      fertEl.value = 6500;
      pestEl.value = 2400;
      laborEl.value = 8500;
      irrigEl.value = 3200;
      machEl.value = 5500;
      otherEl.value = 2500;
      break;

    case 'tomato_1acre':
      cropEl.value = 'Tomato';
      areaEl.value = 1.0;
      yieldEl.value = 140.0;
      priceEl.value = 1650;
      seedEl.value = 8500;
      fertEl.value = 14000;
      pestEl.value = 9500;
      laborEl.value = 28000;
      irrigEl.value = 6500;
      machEl.value = 7000;
      otherEl.value = 12000;
      break;

    case 'soybean_3acres':
      cropEl.value = 'Soybean';
      areaEl.value = 3.0;
      yieldEl.value = 10.5;
      priceEl.value = 4650;
      seedEl.value = 6500;
      fertEl.value = 9000;
      pestEl.value = 4500;
      laborEl.value = 11000;
      irrigEl.value = 3000;
      machEl.value = 7500;
      otherEl.value = 3500;
      break;

    case 'mustard_2acres':
      cropEl.value = 'Mustard';
      areaEl.value = 2.0;
      yieldEl.value = 9.0;
      priceEl.value = 5450;
      seedEl.value = 1800;
      fertEl.value = 5200;
      pestEl.value = 2000;
      laborEl.value = 7000;
      irrigEl.value = 2500;
      machEl.value = 5000;
      otherEl.value = 2000;
      break;
  }

  // Auto-submit preset calculation
  handleProfitSubmit(new Event('submit'));
}

function resetProfitForm() {
  const form = document.getElementById('profit-form');
  if (form) form.reset();

  document.getElementById('prof-placeholder')?.classList.remove('d-none');
  document.getElementById('prof-loading')?.classList.add('d-none');
  document.getElementById('prof-results')?.classList.add('d-none');
}

async function handleProfitSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();

  const cropName = document.getElementById('prof-crop').value;
  const landAreaAcres = parseFloat(document.getElementById('prof-area').value);
  const expectedYieldPerAcreQuintals = parseFloat(document.getElementById('prof-yield').value);
  const expectedSellingPricePerQuintal = parseFloat(document.getElementById('prof-price').value);

  const seedCost = parseFloat(document.getElementById('prof-seed').value) || 0;
  const fertilizerCost = parseFloat(document.getElementById('prof-fert').value) || 0;
  const pesticideCost = parseFloat(document.getElementById('prof-pest').value) || 0;
  const laborCost = parseFloat(document.getElementById('prof-labor').value) || 0;
  const irrigationCost = parseFloat(document.getElementById('prof-irrig').value) || 0;
  const machineryCost = parseFloat(document.getElementById('prof-mach').value) || 0;
  const otherCost = parseFloat(document.getElementById('prof-other').value) || 0;

  if (isNaN(landAreaAcres) || landAreaAcres <= 0) {
    UI.showToast('Please enter a valid land area in acres', 'warning');
    return;
  }
  if (isNaN(expectedYieldPerAcreQuintals) || expectedYieldPerAcreQuintals <= 0) {
    UI.showToast('Please enter a valid expected yield', 'warning');
    return;
  }
  if (isNaN(expectedSellingPricePerQuintal) || expectedSellingPricePerQuintal <= 0) {
    UI.showToast('Please enter a valid selling price', 'warning');
    return;
  }

  const payload = {
    crop: cropName,
    cropName,
    landArea: landAreaAcres,
    landAreaAcres,
    expectedYieldPerAcre: expectedYieldPerAcreQuintals,
    expectedYieldPerAcreQuintals,
    expectedSellingPrice: expectedSellingPricePerQuintal,
    expectedSellingPricePerQuintal,
    seedCost,
    fertilizerCost,
    pesticideCost,
    laborCost,
    irrigationCost,
    machineryCost,
    otherCost
  };

  const placeholderEl = document.getElementById('prof-placeholder');
  const loadingEl = document.getElementById('prof-loading');
  const resultsEl = document.getElementById('prof-results');
  const btn = document.getElementById('btn-calc-profit');

  try {
    if (placeholderEl) placeholderEl.classList.add('d-none');
    if (resultsEl) resultsEl.classList.add('d-none');
    if (loadingEl) loadingEl.classList.remove('d-none');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Computing Economics...';
    }

    const response = await API.post('/api/profit/calculate', payload);

    if (loadingEl) loadingEl.classList.add('d-none');
    if (resultsEl) resultsEl.classList.remove('d-none');

    const data = (response && response.data) ? response.data : response;
    renderProfitResults(data, payload);
  } catch (error) {
    console.error('Profit calculation error:', error);
    if (loadingEl) loadingEl.classList.add('d-none');
    if (placeholderEl) placeholderEl.classList.remove('d-none');
    UI.showToast(error.message || 'Failed to calculate profit economics.', 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-calculator me-1"></i>Calculate Profit & ROI';
    }
  }
}

function renderProfitResults(data, input) {
  // Title & Health Rating
  const titleEl = document.getElementById('prof-crop-title');
  if (titleEl) {
    titleEl.textContent = `${data.cropName} Cultivation on ${input.landAreaAcres} Acre(s)`;
  }

  const healthBadge = document.getElementById('prof-health-badge');
  if (healthBadge) {
    const health = (data.financialHealthRating || 'MODERATE').toUpperCase();
    if (health === 'EXCELLENT') {
      healthBadge.className = 'badge bg-success px-3 py-1 fw-bold fs-6';
      healthBadge.innerHTML = '<i class="bi bi-star-fill me-1"></i>EXCELLENT PROFITABILITY';
    } else if (health === 'GOOD') {
      healthBadge.className = 'badge bg-primary px-3 py-1 fw-bold fs-6';
      healthBadge.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>HEALTHY MARGIN';
    } else if (health === 'MODERATE') {
      healthBadge.className = 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-3 py-1 fw-bold fs-6';
      healthBadge.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i>MODERATE PROFIT';
    } else {
      healthBadge.className = 'badge bg-danger px-3 py-1 fw-bold fs-6';
      healthBadge.innerHTML = '<i class="bi bi-exclamation-octagon-fill me-1"></i>HIGH RISK / LOSS';
    }
  }

  // ROI Percentage
  const roiEl = document.getElementById('prof-roi-val');
  if (roiEl) {
    const isPositive = data.roiPercentage >= 0;
    roiEl.textContent = `${isPositive ? '+' : ''}${data.roiPercentage.toFixed(1)}%`;
    roiEl.className = `fs-4 fw-bold ${isPositive ? 'text-primary' : 'text-danger'}`;
  }

  // 4 Main metrics
  const totYieldEl = document.getElementById('prof-tot-yield');
  if (totYieldEl) totYieldEl.textContent = data.totalYieldQuintals.toFixed(1);

  const totRevEl = document.getElementById('prof-tot-rev');
  if (totRevEl) totRevEl.textContent = `₹ ${Math.round(data.grossRevenue).toLocaleString()}`;

  const totCostEl = document.getElementById('prof-tot-cost');
  if (totCostEl) totCostEl.textContent = `₹ ${Math.round(data.totalCost).toLocaleString()}`;

  const netProfitEl = document.getElementById('prof-net-profit');
  const perAcreEl = document.getElementById('prof-per-acre');
  if (netProfitEl && perAcreEl) {
    const isProfitable = data.netProfit >= 0;
    netProfitEl.textContent = `₹ ${Math.round(data.netProfit).toLocaleString()}`;
    netProfitEl.className = `metric-summary-val ${isProfitable ? 'text-success' : 'text-danger'}`;
    perAcreEl.textContent = `₹ ${Math.round(data.profitPerAcre).toLocaleString()} / Acre`;
    perAcreEl.className = `small ${isProfitable ? 'text-success-emphasis' : 'text-danger-emphasis'}`;
  }

  // Break-even
  const breakEvenEl = document.getElementById('prof-breakeven');
  if (breakEvenEl) {
    breakEvenEl.textContent = `₹ ${data.breakEvenPricePerQuintal.toFixed(2)} / Quintal`;
  }

  const sellingCompareEl = document.getElementById('prof-selling-compare');
  if (sellingCompareEl) {
    sellingCompareEl.textContent = `₹ ${input.expectedSellingPricePerQuintal.toLocaleString()}`;
  }

  const marginSafetyEl = document.getElementById('prof-margin-safety');
  if (marginSafetyEl) {
    const margin = input.expectedSellingPricePerQuintal - data.breakEvenPricePerQuintal;
    if (margin > 0) {
      marginSafetyEl.className = 'small text-success fw-bold';
      marginSafetyEl.innerHTML = `<i class="bi bi-shield-check me-1"></i>Healthy Safety Margin (₹ ${margin.toFixed(2)} buffer above break-even)`;
    } else {
      marginSafetyEl.className = 'small text-danger fw-bold';
      marginSafetyEl.innerHTML = `<i class="bi bi-exclamation-triangle me-1"></i>Loss Warning: Selling price is ₹ ${Math.abs(margin).toFixed(2)} below break-even!`;
    }
  }

  // Cost breakdown bars
  renderCostBreakdown(data.costBreakdownPercentages || data.costPercentageBreakdown, data.totalCost, data.costBreakdown);

  // Advisory text
  const advText = document.getElementById('prof-advisory-text');
  if (advText) {
    advText.textContent = data.advisory || 'Ensure balanced investment across soil nutrients and weed control to maintain profitability.';
  }
}

function renderCostBreakdown(pctMap, totalCost, costMap) {
  const container = document.getElementById('cost-breakdown-container');
  if (!container || !pctMap) return;

  const categories = [
    { match: ['seed', 'nursery'], name: 'Seeds & Nursery', color: '#16a34a' },
    { match: ['fertiliz'], name: 'Fertilizers & Nutrients', color: '#059669' },
    { match: ['pesticid', 'protection'], name: 'Crop Protection / Pesticides', color: '#d97706' },
    { match: ['labor', 'harvesting'], name: 'Labor & Harvesting', color: '#2563eb' },
    { match: ['irrigation', 'pumping'], name: 'Irrigation & Pumping', color: '#0891b2' },
    { match: ['machinery', 'diesel'], name: 'Machinery & Diesel', color: '#7c3aed' },
    { match: ['other', 'misc', 'post-harvest'], name: 'Post-harvest & Misc', color: '#6b7280' }
  ];

  const normalize = value => String(value).toLowerCase().replace(/[^a-z]/g, '');
  const pctEntries = Object.entries(pctMap);
  const costEntries = Object.entries(costMap || {});

  container.innerHTML = pctEntries.map(([key, rawPct]) => {
    const normalizedKey = normalize(key);
    const category = categories.find(item =>
      item.match.some(term => normalizedKey.includes(normalize(term)))
    );

    const name = category?.name || key;
    const color = category?.color || '#4b5563';
    const pct = Number(rawPct) || 0;

    const matchingCost = costEntries.find(([costKey]) =>
      normalize(costKey) === normalizedKey
    );
    const amount = matchingCost
      ? Number(matchingCost[1]) || 0
      : (pct / 100) * (Number(totalCost) || 0);

    return `
      <div>
        <div class="d-flex justify-content-between align-items-center small mb-1">
          <span class="fw-semibold text-secondary">
            <span class="d-inline-block rounded-circle me-1" style="width: 10px; height: 10px; background-color: ${color};"></span>
            ${name}
          </span>
          <span class="text-dark fw-bold">${pct.toFixed(1)}% <span class="text-muted fw-normal">(₹ ${Math.round(amount).toLocaleString('en-IN')})</span></span>
        </div>
        <div class="progress progress-cost">
          <div class="progress-bar" role="progressbar" style="width: ${Math.max(0, Math.min(100, pct))}%; background-color: ${color};" aria-valuenow="${pct}" aria-valuemin="0" aria-valuemax="100"></div>
        </div>
      </div>
    `;
  }).join('');
}
