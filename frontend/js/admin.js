/**
 * SMART KRISHI - Admin Portal Controller
 */
document.addEventListener('DOMContentLoaded', async () => {
  UI.renderNavbar('admin');
  UI.renderFooter();

  // Guard: require admin authentication
  if (!AUTH.requireAuth(true)) return;

  loadAdminData();

  // Add Price Form
  const priceForm = document.getElementById('admin-add-price-form');
  if (priceForm) {
    priceForm.addEventListener('submit', handleAddPrice);
  }

  // Add Advisory Form
  const advisoryForm = document.getElementById('admin-add-advisory-form');
  if (advisoryForm) {
    advisoryForm.addEventListener('submit', handleAddAdvisory);
  }
});

async function loadAdminData() {
  // Load Stats
  try {
    const statsRes = await API.get('/api/admin/stats');
    if (statsRes && statsRes.data) {
      renderAdminStats(statsRes.data);
    }
  } catch (err) {
    console.error('Failed to load admin stats:', err);
    UI.showToast('Could not load admin stats', 'error');
  }

  // Load Farmers
  try {
    const farmersRes = await API.get('/api/admin/farmers');
    if (farmersRes && farmersRes.data) {
      renderFarmersTable(farmersRes.data);
    }
  } catch (err) {
    console.error('Failed to load farmers list:', err);
  }

  // Load Mandi Source Status
  try {
    const mandiStatusRes = await API.get('/api/mandi/status');
    if (mandiStatusRes && mandiStatusRes.data) {
      renderMandiSourceStatus(mandiStatusRes.data);
    }
  } catch (err) {
    console.warn('Could not load mandi status:', err);
  }
}

function renderMandiSourceStatus(status) {
  const statusBadge = document.getElementById('admin-gov-api-status');
  const syncText = document.getElementById('admin-gov-api-sync');
  if (statusBadge) {
    if (status.governmentApiConfigured) {
      statusBadge.className = 'badge bg-success-subtle text-success';
      statusBadge.textContent = 'Government API: Connected';
    } else {
      statusBadge.className = 'badge bg-warning-subtle text-dark';
      statusBadge.textContent = 'Government API: Not Configured (Using Database Fallback)';
    }
  }
  if (syncText) {
    syncText.textContent = `Last Sync: ${status.lastGovApiSync || 'Never'} (${status.databaseFallbackRecords || 0} DB fallback records ready)`;
  }
}

function renderAdminStats(stats) {
  document.getElementById('stat-farmers').textContent = stats.totalFarmers || 0;
  document.getElementById('stat-recs').textContent = stats.totalRecommendations || 0;
  document.getElementById('stat-prices').textContent = stats.totalMarketRecords || 0;
  document.getElementById('stat-advisories').textContent = stats.totalAdvisories || 0;
}

function renderFarmersTable(farmers) {
  const tbody = document.getElementById('admin-farmers-tbody');
  if (!tbody) return;

  if (farmers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No registered farmers found.</td></tr>`;
    return;
  }

  tbody.innerHTML = farmers.map((f, idx) => `
    <tr>
      <td><strong>#${idx + 1}</strong></td>
      <td>
        <div class="fw-bold text-dark">${f.name}</div>
        <small class="text-muted">${f.email}</small>
      </td>
      <td>${f.phone}</td>
      <td>${f.village || ''}, ${f.district || ''}, ${f.state || ''}</td>
      <td><strong>${f.landArea || 0}</strong> Acres</td>
      <td><span class="badge bg-light text-dark border">${f.soilType || 'N/A'}</span></td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-danger" onclick="deleteFarmerAccount(${f.userId})">
          <i class="bi bi-trash me-1"></i>Delete
        </button>
      </td>
    </tr>
  `).join('');
}

async function deleteFarmerAccount(userId) {
  if (!confirm('Are you sure you want to delete this farmer account and all associated farm data?')) return;
  try {
    await API.delete(`/api/admin/farmers/${userId}`);
    UI.showToast('Farmer account removed', 'info');
    loadAdminData();
  } catch (err) {
    console.error('Failed to delete farmer:', err);
    UI.showToast(err.message || 'Failed to delete farmer', 'error');
  }
}

async function handleAddPrice(e) {
  e.preventDefault();
  const payload = {
    cropName: document.getElementById('price-crop').value.trim(),
    variety: document.getElementById('price-variety').value.trim(),
    marketName: document.getElementById('price-mandi').value.trim(),
    district: document.getElementById('price-district').value.trim(),
    state: document.getElementById('price-state').value.trim(),
    minPrice: parseFloat(document.getElementById('price-min').value),
    maxPrice: parseFloat(document.getElementById('price-max').value),
    modalPrice: parseFloat(document.getElementById('price-modal').value),
    unit: document.getElementById('price-unit').value,
    trend: document.getElementById('price-trend').value
  };

  try {
    await API.post('/api/market/prices', payload);
    UI.showToast('Market commodity price added!', 'success');
    const modal = bootstrap.Modal.getInstance(document.getElementById('addPriceModal'));
    if (modal) modal.hide();
    document.getElementById('admin-add-price-form').reset();
    loadAdminData();
  } catch (err) {
    console.error('Add price error:', err);
    UI.showToast(err.message || 'Failed to add market price', 'error');
  }
}

async function handleAddAdvisory(e) {
  e.preventDefault();
  const payload = {
    title: document.getElementById('adv-title').value.trim(),
    category: document.getElementById('adv-category').value,
    season: document.getElementById('adv-season').value,
    targetCrop: document.getElementById('adv-crop').value.trim(),
    summary: document.getElementById('adv-summary').value.trim(),
    details: document.getElementById('adv-details').value.trim(),
    applicableState: document.getElementById('adv-state').value.trim(),
    officialLink: document.getElementById('adv-link').value.trim()
  };

  try {
    await API.post('/api/advisory', payload);
    UI.showToast('Advisory published successfully!', 'success');
    const modal = bootstrap.Modal.getInstance(document.getElementById('addAdvisoryModal'));
    if (modal) modal.hide();
    document.getElementById('admin-add-advisory-form').reset();
    loadAdminData();
  } catch (err) {
    console.error('Add advisory error:', err);
    UI.showToast(err.message || 'Failed to publish advisory', 'error');
  }
}
