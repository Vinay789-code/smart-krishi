/**
 * SMART KRISHI - Nearby Live Mandi Prices Controller
 * Integrates official Government data.gov.in / AGMARKNET API with Haversine distance,
 * browser geolocation, radius filtering, crop filtering, and verified database fallback.
 */

let userLat = null;
let userLon = null;
let currentResults = [];
let availableLocations = {};

function initMandiPage() {
  UI.renderNavbar('market');
  UI.renderFooter();

  // 1. Setup event listeners immediately (synchronously, never wait for network calls)
  const btnUseLocation = document.getElementById('btn-use-location');
  if (btnUseLocation) {
    btnUseLocation.addEventListener('click', handleUseMyLocation);
  }

  const btnFindPrices = document.getElementById('btn-find-prices');
  if (btnFindPrices) {
    btnFindPrices.addEventListener('click', () => fetchMandiPrices());
  }

  // Filter change handlers
  const stateSelect = document.getElementById('mandi-state-select');
  if (stateSelect) {
    stateSelect.addEventListener('change', handleStateChange);
  }

  const districtSelect = document.getElementById('mandi-district-select');
  if (districtSelect) {
    districtSelect.addEventListener('change', () => {
      // Clear GPS coordinates when manually choosing district
      userLat = null;
      userLon = null;
      fetchMandiPrices();
    });
  }

  const radiusSelect = document.getElementById('mandi-radius-select');
  if (radiusSelect) {
    radiusSelect.addEventListener('change', () => fetchMandiPrices());
  }

  const cropSelect = document.getElementById('mandi-crop-select');
  if (cropSelect) {
    cropSelect.addEventListener('change', () => fetchMandiPrices());
  }

  const sortSelect = document.getElementById('mandi-sort-select');
  if (sortSelect) {
    sortSelect.addEventListener('change', () => fetchMandiPrices());
  }

  // View switcher buttons
  const btnViewCards = document.getElementById('btn-view-cards');
  const btnViewTable = document.getElementById('btn-view-table');
  if (btnViewCards && btnViewTable) {
    btnViewCards.addEventListener('click', () => setViewMode('cards'));
    btnViewTable.addEventListener('click', () => setViewMode('table'));
  }

  // 2. Load dropdown metadata asynchronously in background
  loadFilterOptions();

  // 3. Initial load of mandis
  fetchMandiPrices();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initMandiPage);
} else {
  initMandiPage();
}

/**
 * Loads supported crops and state/district hierarchies from the backend.
 */
async function loadFilterOptions() {
  try {
    const [cropsRes, locRes] = await Promise.all([
      API.get('/api/mandi/crops'),
      API.get('/api/mandi/locations')
    ]);

    // Populate crops dropdown
    if (cropsRes && cropsRes.data) {
      const cropSelect = document.getElementById('mandi-crop-select');
      if (cropSelect) {
        cropSelect.innerHTML = `<option value="">🌾 All Crops</option>` +
          cropsRes.data.map(c => `<option value="${c}">${c}</option>`).join('');
      }
    }

    // Populate states and save district map
    if (locRes && locRes.data) {
      availableLocations = locRes.data;
      const stateSelect = document.getElementById('mandi-state-select');
      if (stateSelect) {
        const states = Object.keys(availableLocations);
        stateSelect.innerHTML = `<option value="">State: All States</option>` +
          states.map(s => `<option value="${s}">${s}</option>`).join('');
      }
    }
  } catch (err) {
    console.warn('Could not load filter metadata:', err);
  }
}

/**
 * Updates district dropdown when state changes.
 */
function handleStateChange() {
  const state = document.getElementById('mandi-state-select').value;
  const districtSelect = document.getElementById('mandi-district-select');

  // Clear GPS coordinates when manually switching state
  userLat = null;
  userLon = null;

  if (!districtSelect) return;

  if (state && availableLocations[state]) {
    districtSelect.innerHTML = `<option value="">District: All Districts</option>` +
      availableLocations[state].map(d => `<option value="${d}">${d}</option>`).join('');
  } else {
    districtSelect.innerHTML = `<option value="">District: All Districts</option>`;
  }

  fetchMandiPrices();
}

/**
 * Handles explicit farmer click on [ 📍 Use My Location ].
 * Uses the HTML5 Geolocation API with high accuracy and farmer-friendly error handling.
 */
function handleUseMyLocation() {
  const btn = document.getElementById('btn-use-location');
  const errorCard = document.getElementById('mandi-error-card');

  if (errorCard) errorCard.classList.add('d-none');

  if (!navigator.geolocation) {
    const msg = 'Geolocation is not supported by your browser. Please select state and district manually.';
    if (window.UI && UI.showToast) UI.showToast(msg, 'warning');
    showLocationError({ message: msg });
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.className = 'btn btn-success fw-semibold w-100 py-2';
    btn.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span>Detecting GPS Coordinates...`;
  }

  navigator.geolocation.getCurrentPosition(
    (pos) => {
      userLat = Math.round(pos.coords.latitude * 10000.0) / 10000.0;
      userLon = Math.round(pos.coords.longitude * 10000.0) / 10000.0;

      if (btn) {
        btn.disabled = false;
        btn.className = 'btn btn-success fw-semibold w-100 py-2';
        btn.innerHTML = `<i class="bi bi-geo-alt-fill me-1"></i>📍 GPS Active (${userLat.toFixed(2)}°, ${userLon.toFixed(2)}°)`;
      }

      // Reset manual dropdowns
      const stateSelect = document.getElementById('mandi-state-select');
      const districtSelect = document.getElementById('mandi-district-select');
      if (stateSelect) stateSelect.value = '';
      if (districtSelect) districtSelect.value = '';

      if (window.UI && UI.showToast) {
        UI.showToast(`GPS location locked: ${userLat}, ${userLon}`, 'success');
      }
      fetchMandiPrices();
    },
    (err) => {
      console.warn('Geolocation error:', err);
      if (btn) {
        btn.disabled = false;
        btn.className = 'btn btn-outline-success fw-semibold w-100 py-2';
        btn.innerHTML = `<i class="bi bi-crosshair me-2"></i>📍 Use My Location (GPS)`;
      }
      showLocationError(err);
    },
    { enableHighAccuracy: false, timeout: 10000, maximumAge: 300000 }
  );
}

/**
 * Handles location errors (permission denied, timeout, unavailable).
 */
function showLocationError(err) {
  const errorCard = document.getElementById('mandi-error-card');
  const errorMsg = document.getElementById('mandi-error-msg');
  const errorTitle = document.getElementById('mandi-error-title');

  let title = 'Location Unavailable';
  let message = err.message || 'Unable to retrieve GPS coordinates. Please select state and district manually.';

  if (err.code === 1) {
    title = 'Location Permission Denied';
    message = 'Location access is blocked in your browser. Please allow location permissions or choose your state and district manually.';
  } else if (err.code === 2) {
    title = 'Location Signal Unavailable';
    message = 'GPS signal unavailable. Please select your state and district manually.';
  } else if (err.code === 3) {
    title = 'Location Request Timed Out';
    message = 'Location request timed out. Please select your state and district manually.';
  }

  if (window.UI && UI.showToast) {
    UI.showToast(message, 'warning', 5000);
  }

  if (errorCard && errorMsg && errorTitle) {
    errorTitle.textContent = title;
    errorMsg.textContent = message;
    errorCard.classList.remove('d-none');
    errorCard.scrollIntoView({ behavior: 'smooth' });
  }
}

function focusManualLocation() {
  const stateSelect = document.getElementById('mandi-state-select');
  if (stateSelect) {
    stateSelect.focus();
    stateSelect.scrollIntoView({ behavior: 'smooth' });
  }
}

/**
 * Queries the backend GET /api/mandi/nearby endpoint.
 */
async function fetchMandiPrices() {
  const loading = document.getElementById('mandi-loading-state');
  const emptyBox = document.getElementById('mandi-empty-state');
  const statusBar = document.getElementById('mandi-status-bar');
  const cardsContainer = document.getElementById('mandi-cards-container');
  const tableContainer = document.getElementById('mandi-table-container');

  if (loading) loading.classList.remove('d-none');
  if (emptyBox) emptyBox.classList.add('d-none');

  const radius = document.getElementById('mandi-radius-select')?.value || '50';
  const crop = document.getElementById('mandi-crop-select')?.value || '';
  const state = document.getElementById('mandi-state-select')?.value || '';
  const district = document.getElementById('mandi-district-select')?.value || '';
  const sortBy = document.getElementById('mandi-sort-select')?.value || 'nearest';

  const params = {
    radius: radius,
    sortBy: sortBy
  };

  if (userLat != null && userLon != null) {
    params.latitude = userLat;
    params.longitude = userLon;
  }
  if (crop) params.crop = crop;
  if (state) params.state = state;
  if (district) params.district = district;

  try {
    const res = await API.get('/api/mandi/nearby', params);

    if (res && res.data) {
      const data = res.data;
      currentResults = data.results || [];

      // Update status bar
      if (statusBar) {
        statusBar.classList.remove('d-none');

        // Location text
        const locText = document.getElementById('mandi-location-text');
        if (locText) {
          if (data.location && data.location.resolvedLocation) {
            locText.textContent = `${data.location.resolvedLocation} (Radius: ${data.radiusKm} km)`;
          } else if (state || district) {
            locText.textContent = `${district ? district + ', ' : ''}${state ? state : 'India'}`;
          } else {
            locText.textContent = `All Regulated Mandis (Radius: ${data.radiusKm} km)`;
          }
        }

        // Source badge
        const sourceBadge = document.getElementById('mandi-source-badge');
        const sourceText = document.getElementById('mandi-source-text');
        if (sourceBadge && sourceText) {
          sourceText.textContent = data.sourceLabel || 'Latest Available Mandi Prices';
          if (data.sourceType === 'GOVERNMENT_API') {
            sourceBadge.className = 'badge badge-source-gov px-3 py-2 rounded-pill';
          } else if (data.sourceType === 'CACHED_GOVERNMENT') {
            sourceBadge.className = 'badge badge-source-cached px-3 py-2 rounded-pill';
          } else {
            sourceBadge.className = 'badge badge-source-db px-3 py-2 rounded-pill';
          }
        }

        // Last updated
        const updatedText = document.getElementById('mandi-updated-text');
        if (updatedText) {
          updatedText.innerHTML = `<i class="bi bi-clock-history me-1"></i>Updated: ${data.lastUpdated || '--'}`;
        }

        // Results count
        const countBadge = document.getElementById('mandi-results-count');
        if (countBadge) {
          countBadge.textContent = `${currentResults.length} mandis listed`;
        }

        // Notice alert
        const noticeBox = document.getElementById('mandi-notice-box');
        const noticeMsg = document.getElementById('mandi-notice-msg');
        if (noticeBox && noticeMsg) {
          if (data.notice) {
            noticeMsg.textContent = data.notice;
            noticeBox.classList.remove('d-none');
          } else {
            noticeBox.classList.add('d-none');
          }
        }
      }

      // Render results
      if (currentResults.length === 0) {
        if (emptyBox) {
          emptyBox.classList.remove('d-none');
          const emptyMsg = document.getElementById('mandi-empty-msg');
          if (emptyMsg) {
            emptyMsg.textContent = `No mandi prices found within ${radius} km for "${crop || 'All Crops'}". Try expanding your radius to 100 km or selecting another commodity.`;
          }
        }
        if (cardsContainer) cardsContainer.classList.add('d-none');
        if (tableContainer) tableContainer.classList.add('d-none');
      } else {
        if (cardsContainer) cardsContainer.classList.remove('d-none');
        renderCards(currentResults);
        renderTable(currentResults);
      }
    }
  } catch (err) {
    console.error('Failed to retrieve mandi prices:', err);
    UI.showToast(err.message || 'Could not retrieve nearby mandi prices', 'error');
  } finally {
    if (loading) loading.classList.add('d-none');
  }
}

/**
 * Renders the responsive Mandi Price Cards matching user specification.
 */
function renderCards(prices) {
  const grid = document.getElementById('mandi-cards-grid');
  if (!grid) return;

  grid.innerHTML = prices.map((p, idx) => {
    const distText = p.distanceKm != null ? `${p.distanceKm.toFixed(1)} km away` : 'Regional Market';
    const minFormatted = UI.formatCurrency(p.minPrice);
    const maxFormatted = UI.formatCurrency(p.maxPrice);
    const modalFormatted = UI.formatCurrency(p.modalPrice);
    const unitText = p.unit || '₹/quintal';

    let badgeClass = 'badge-source-gov';
    if (p.sourceType === 'CACHED_GOVERNMENT') badgeClass = 'badge-source-cached';
    if (p.sourceType === 'ADMIN_DATABASE') badgeClass = 'badge-source-db';

    return `
      <div class="col-lg-4 col-md-6 col-12">
        <div class="mandi-card p-4">
          <!-- Card Header: Crop & Distance -->
          <div class="d-flex justify-content-between align-items-start mb-2">
            <div>
              <span class="badge bg-success-subtle text-success fs-6 fw-bold px-2 py-1 mb-1">
                🌾 ${p.commodity}
              </span>
              <h5 class="fw-bold text-dark mb-0">${p.mandiName}</h5>
              <small class="text-muted"><i class="bi bi-geo-alt"></i> ${p.district}, ${p.state}</small>
            </div>
            <span class="distance-pill" title="Distance from your location">
              <i class="bi bi-signpost-2"></i> ${distText}
            </span>
          </div>

          <!-- Price Box Grid (Min, Max, Modal) -->
          <div class="price-box-grid my-3">
            <div class="price-box-item">
              <span class="label">Min Price</span>
              <span class="val">${minFormatted}</span>
            </div>
            <div class="price-box-item">
              <span class="label">Max Price</span>
              <span class="val">${maxFormatted}</span>
            </div>
            <div class="price-box-item">
              <span class="label">Modal Price</span>
              <span class="val modal-val">${modalFormatted}</span>
            </div>
          </div>

          <!-- Quality Details -->
          <div class="d-flex justify-content-between small text-muted border-top border-bottom py-2 my-2">
            <div><strong>Variety:</strong> ${p.variety || 'Local / Standard'}</div>
            <div><strong>Grade:</strong> ${p.grade || 'FAQ'}</div>
          </div>

          <!-- Unit and Date -->
          <div class="d-flex justify-content-between align-items-center small text-secondary mb-3">
            <span><strong>Unit:</strong> ${unitText}</span>
            <span class="badge ${badgeClass}" title="${p.source || ''}">
              ${p.sourceLabel || 'Government Data'}
            </span>
          </div>

          <div class="small text-muted mb-3">
            <i class="bi bi-calendar-event me-1"></i>Arrival / Updated: <strong>${p.arrivalDate || p.lastUpdated || 'Today'}</strong>
          </div>

          <!-- Action Buttons -->
          <div class="d-flex gap-2 mt-auto pt-2 border-top">
            <button type="button" class="btn btn-outline-secondary btn-sm flex-fill" onclick="openMandiModal(${idx})">
              <i class="bi bi-info-circle me-1"></i>View Mandi
            </button>
            <a href="${p.navigateUrl || '#'}" target="_blank" class="btn btn-success btn-sm flex-fill" title="Open Google Maps Directions">
              <i class="bi bi-compass me-1"></i>🗺 Navigate
            </a>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

/**
 * Renders the table view for tabular rate comparisons.
 */
function renderTable(prices) {
  const tbody = document.getElementById('mandi-table-tbody');
  if (!tbody) return;

  tbody.innerHTML = prices.map((p, idx) => {
    const distText = p.distanceKm != null ? `${p.distanceKm.toFixed(1)} km` : '-';
    let badgeClass = 'badge-source-gov';
    if (p.sourceType === 'CACHED_GOVERNMENT') badgeClass = 'badge-source-cached';
    if (p.sourceType === 'ADMIN_DATABASE') badgeClass = 'badge-source-db';

    return `
      <tr>
        <td><strong>#${idx + 1}</strong></td>
        <td>
          <div class="fw-bold text-dark">🌾 ${p.commodity}</div>
          <small class="text-muted">${p.variety || 'Standard Grade'}</small>
        </td>
        <td>
          <div class="fw-semibold text-secondary">${p.mandiName}</div>
          <small class="text-muted">${p.district}, ${p.state}</small>
        </td>
        <td>
          <span class="distance-pill">${distText}</span>
        </td>
        <td>${UI.formatCurrency(p.minPrice)}</td>
        <td>${UI.formatCurrency(p.maxPrice)}</td>
        <td><strong class="text-success fs-6">${UI.formatCurrency(p.modalPrice)}</strong></td>
        <td><span class="text-muted small">${p.unit || '₹/quintal'}</span></td>
        <td>
          <div class="small fw-semibold text-dark">${p.arrivalDate || 'Today'}</div>
          <span class="badge ${badgeClass} small">${p.sourceLabel || 'Government Data'}</span>
        </td>
        <td class="text-end">
          <div class="btn-group btn-group-sm">
            <button class="btn btn-outline-secondary" onclick="openMandiModal(${idx})" title="View Details">
              <i class="bi bi-eye"></i>
            </button>
            <a href="${p.navigateUrl || '#'}" target="_blank" class="btn btn-outline-success" title="Directions">
              <i class="bi bi-compass"></i>
            </a>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

/**
 * Opens the Mandi Details Modal.
 */
function openMandiModal(index) {
  const item = currentResults[index];
  if (!item) return;

  document.getElementById('modal-mandi-name').textContent = item.mandiName;
  document.getElementById('modal-crop-tag').textContent = `🌾 ${item.commodity}`;
  document.getElementById('modal-distance-text').textContent =
    item.distanceKm != null ? `${item.distanceKm.toFixed(1)} km away from farm` : 'Regional Market';

  document.getElementById('modal-min-price').textContent = `${UI.formatCurrency(item.minPrice)} / quintal`;
  document.getElementById('modal-max-price').textContent = `${UI.formatCurrency(item.maxPrice)} / quintal`;
  document.getElementById('modal-modal-price').textContent = `${UI.formatCurrency(item.modalPrice)} / quintal`;

  document.getElementById('modal-variety').textContent = item.variety || 'Standard / Local';
  document.getElementById('modal-grade').textContent = item.grade || 'FAQ';
  document.getElementById('modal-location').textContent = `${item.district}, ${item.state}`;
  document.getElementById('modal-arrival-date').textContent = item.arrivalDate || item.lastUpdated || 'Today';

  const sourceBadge = document.getElementById('modal-source-badge');
  if (sourceBadge) {
    sourceBadge.textContent = item.sourceLabel || 'Government Data';
    if (item.sourceType === 'GOVERNMENT_API') {
      sourceBadge.className = 'badge badge-source-gov';
    } else if (item.sourceType === 'CACHED_GOVERNMENT') {
      sourceBadge.className = 'badge badge-source-cached';
    } else {
      sourceBadge.className = 'badge badge-source-db';
    }
  }

  const navBtn = document.getElementById('modal-navigate-btn');
  if (navBtn) {
    navBtn.href = item.navigateUrl || '#';
  }

  const modalEl = document.getElementById('mandiDetailsModal');
  if (modalEl) {
    const bsModal = new bootstrap.Modal(modalEl);
    bsModal.show();
  }
}

/**
 * Toggles view between Cards and Table.
 */
function setViewMode(mode) {
  const cardsContainer = document.getElementById('mandi-cards-container');
  const tableContainer = document.getElementById('mandi-table-container');
  const btnCards = document.getElementById('btn-view-cards');
  const btnTable = document.getElementById('btn-view-table');

  if (mode === 'table') {
    if (cardsContainer) cardsContainer.classList.add('d-none');
    if (tableContainer) tableContainer.classList.remove('d-none');
    if (btnCards) btnCards.classList.remove('active');
    if (btnTable) btnTable.classList.add('active');
  } else {
    if (cardsContainer) cardsContainer.classList.remove('d-none');
    if (tableContainer) tableContainer.classList.add('d-none');
    if (btnCards) btnCards.classList.add('active');
    if (btnTable) btnTable.classList.remove('active');
  }
}

function expandRadius() {
  const radSelect = document.getElementById('mandi-radius-select');
  if (radSelect) {
    radSelect.value = '100';
    fetchMandiPrices();
  }
}

function resetCropFilter() {
  const cropSelect = document.getElementById('mandi-crop-select');
  if (cropSelect) {
    cropSelect.value = '';
    fetchMandiPrices();
  }
}

window.handleUseMyLocation = handleUseMyLocation;
window.fetchMandiPrices = fetchMandiPrices;
window.focusManualLocation = focusManualLocation;
window.openMandiModal = openMandiModal;
window.expandRadius = expandRadius;
window.resetCropFilter = resetCropFilter;
