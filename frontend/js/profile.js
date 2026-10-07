/**
 * SMART KRISHI - Farmer Profile & Land Management Controller
 */
let currentFarmerProfile = null;

document.addEventListener('DOMContentLoaded', async () => {
  UI.renderNavbar('profile');
  UI.renderFooter();

  if (!AUTH.requireAuth()) return;

  const user = AUTH.getUser();
  await loadProfile(user.id);

  // Setup form listener
  const form = document.getElementById('profile-edit-form');
  if (form) {
    form.addEventListener('submit', handleProfileUpdate);
  }

  // Setup add farm form listener
  const addFarmForm = document.getElementById('add-farm-form');
  if (addFarmForm) {
    addFarmForm.addEventListener('submit', handleAddFarm);
  }
});

async function loadProfile(userId) {
  try {
    const res = await API.get(`/api/farmers/${userId}`);
    if (res && res.data) {
      currentFarmerProfile = res.data;
      renderProfileData(res.data);
      renderFarmsList(res.data.farms || []);
    }
  } catch (err) {
    console.error('Error loading farmer profile:', err);
    UI.showToast('Failed to load profile details', 'error');
  }
}

function renderProfileData(p) {
  document.getElementById('profile-name-header').textContent = p.name || 'Farmer';
  document.getElementById('profile-role-badge').textContent = p.role === 'ROLE_ADMIN' ? 'Administrator' : 'Registered Farmer';
  document.getElementById('profile-email-header').textContent = p.email || '';
  document.getElementById('profile-phone-header').textContent = p.phone || '';

  // Form inputs
  document.getElementById('input-name').value = p.name || '';
  document.getElementById('input-email').value = p.email || '';
  document.getElementById('input-phone').value = p.phone || '';
  document.getElementById('input-village').value = p.village || '';
  document.getElementById('input-district').value = p.district || '';
  document.getElementById('input-state').value = p.state || '';
  document.getElementById('input-landArea').value = p.landArea || 0;
  document.getElementById('input-soilType').value = p.soilType || 'Alluvial';
  document.getElementById('input-irrigationType').value = p.irrigationType || 'Tube Well';
  document.getElementById('input-primaryCrop').value = p.primaryCrop || '';
}

function renderFarmsList(farms) {
  const container = document.getElementById('farms-table-body');
  if (!container) return;

  if (farms.length === 0) {
    container.innerHTML = `
      <tr>
        <td colspan="6" class="text-center py-4 text-muted">
          <i class="bi bi-geo-alt fs-2 d-block mb-1"></i>
          No individual land plots added yet. Click "Add Farm / Plot" above to record your land holdings.
        </td>
      </tr>
    `;
    return;
  }

  container.innerHTML = farms.map((f, idx) => `
    <tr>
      <td><strong>#${idx + 1}</strong></td>
      <td><span class="fw-semibold text-dark">${f.farmName}</span></td>
      <td><code>${f.plotNumber || 'N/A'}</code></td>
      <td><strong>${f.areaAcres}</strong> Acres</td>
      <td><span class="badge bg-light text-dark border">${f.soilType || 'N/A'}</span></td>
      <td><span class="badge bg-info-subtle text-info border border-info-subtle">${f.waterSource || 'N/A'}</span></td>
      <td class="text-end">
        <button class="btn btn-sm btn-outline-danger" onclick="deleteFarm(${f.id})">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    </tr>
  `).join('');
}

async function handleProfileUpdate(e) {
  e.preventDefault();
  const btn = document.getElementById('btn-save-profile');
  btn.disabled = true;
  btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Saving...`;

  const user = AUTH.getUser();
  const payload = {
    name: document.getElementById('input-name').value.trim(),
    phone: document.getElementById('input-phone').value.trim(),
    village: document.getElementById('input-village').value.trim(),
    district: document.getElementById('input-district').value.trim(),
    state: document.getElementById('input-state').value.trim(),
    landArea: parseFloat(document.getElementById('input-landArea').value) || 0,
    soilType: document.getElementById('input-soilType').value,
    irrigationType: document.getElementById('input-irrigationType').value,
    primaryCrop: document.getElementById('input-primaryCrop').value.trim()
  };

  try {
    const res = await API.put(`/api/farmers/${user.id}`, payload);
    if (res && res.data) {
      UI.showToast('Profile updated successfully!', 'success');
      // Update cached session
      const updatedUser = {
        ...user,
        name: res.data.name,
        phone: res.data.phone,
        village: res.data.village,
        district: res.data.district,
        state: res.data.state,
        landArea: res.data.landArea,
        soilType: res.data.soilType
      };
      AUTH.setUser(updatedUser);
      renderProfileData(res.data);
    }
  } catch (err) {
    console.error('Update profile error:', err);
    UI.showToast(err.message || 'Error updating profile', 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = `<i class="bi bi-check2-circle me-1"></i>Save Changes`;
  }
}

async function handleAddFarm(e) {
  e.preventDefault();
  const user = AUTH.getUser();
  const payload = {
    farmName: document.getElementById('farm-name-input').value.trim(),
    plotNumber: document.getElementById('farm-plot-input').value.trim(),
    areaAcres: parseFloat(document.getElementById('farm-area-input').value) || 0,
    soilType: document.getElementById('farm-soil-input').value,
    waterSource: document.getElementById('farm-water-input').value
  };

  try {
    const res = await API.post(`/api/farmers/${user.id}/farms`, payload);
    if (res && res.data) {
      UI.showToast('Farm plot recorded successfully!', 'success');
      // Close modal
      const modalEl = document.getElementById('addFarmModal');
      const bsModal = bootstrap.Modal.getInstance(modalEl);
      if (bsModal) bsModal.hide();
      document.getElementById('add-farm-form').reset();
      // Reload profile
      await loadProfile(user.id);
    }
  } catch (err) {
    console.error('Add farm error:', err);
    UI.showToast(err.message || 'Failed to add farm', 'error');
  }
}

async function deleteFarm(farmId) {
  if (!confirm('Are you sure you want to remove this farm plot?')) return;
  try {
    await API.delete(`/api/farmers/farms/${farmId}`);
    UI.showToast('Farm plot removed', 'info');
    const user = AUTH.getUser();
    await loadProfile(user.id);
  } catch (err) {
    console.error('Delete farm error:', err);
    UI.showToast('Failed to delete farm plot', 'error');
  }
}
