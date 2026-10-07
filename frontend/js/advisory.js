/**
 * SMART KRISHI - Farming Advisory & Govt Schemes Controller
 */
let allAdvisories = [];
let currentCategory = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('advisory');
  UI.renderFooter();

  loadAdvisories();

  // Search input listener
  const searchInput = document.getElementById('advisory-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', applyAdvisoryFilters);
  }
});

async function loadAdvisories() {
  const loading = document.getElementById('advisory-loading');
  if (loading) loading.classList.remove('d-none');

  try {
    const res = await API.get('/api/advisory');
    if (res && res.data) {
      allAdvisories = res.data;
      applyAdvisoryFilters();
    }
  } catch (err) {
    console.error('Failed to load advisories:', err);
    UI.showToast('Could not load advisories', 'error');
  } finally {
    if (loading) loading.classList.add('d-none');
  }
}

function filterCategory(cat, btnEl) {
  currentCategory = cat;
  document.querySelectorAll('.advisory-cat-btn').forEach(btn => {
    btn.classList.remove('active', 'btn-agro-primary');
    btn.classList.add('btn-outline-secondary');
  });

  if (btnEl) {
    btnEl.classList.add('active', 'btn-agro-primary');
    btnEl.classList.remove('btn-outline-secondary');
  }

  applyAdvisoryFilters();
}

function applyAdvisoryFilters() {
  const query = (document.getElementById('advisory-search-input')?.value || '').toLowerCase().trim();

  const filtered = allAdvisories.filter(a => {
    const matchesCat = currentCategory === 'ALL' || a.category === currentCategory;
    const matchesQuery = !query ||
      a.title.toLowerCase().includes(query) ||
      (a.summary && a.summary.toLowerCase().includes(query)) ||
      (a.details && a.details.toLowerCase().includes(query)) ||
      (a.targetCrop && a.targetCrop.toLowerCase().includes(query));

    return matchesCat && matchesQuery;
  });

  renderAdvisoryCards(filtered);
}

function renderAdvisoryCards(items) {
  const container = document.getElementById('advisories-grid');
  if (!container) return;

  if (items.length === 0) {
    container.innerHTML = `
      <div class="col-12 text-center py-5 text-muted">
        <i class="bi bi-journal-x fs-2 d-block mb-2"></i>
        No advisories found for the selected filter or search term.
      </div>
    `;
    return;
  }

  container.innerHTML = items.map(a => {
    let iconClass = 'bi-info-circle text-primary';
    let badgeClass = 'bg-primary-subtle text-primary';
    if (a.category === 'SCHEME') {
      iconClass = 'bi-award text-success';
      badgeClass = 'bg-success-subtle text-success';
    } else if (a.category === 'FERTILIZER') {
      iconClass = 'bi-droplet-half text-warning';
      badgeClass = 'bg-warning-subtle text-warning-emphasis';
    } else if (a.category === 'PEST_CONTROL') {
      iconClass = 'bi-shield-exclamation text-danger';
      badgeClass = 'bg-danger-subtle text-danger';
    } else if (a.category === 'SEASONAL') {
      iconClass = 'bi-cloud-sun text-info';
      badgeClass = 'bg-info-subtle text-info';
    }

    return `
      <div class="col-lg-4 col-md-6">
        <div class="agro-card p-4">
          <div class="d-flex justify-content-between align-items-start mb-3">
            <span class="badge ${badgeClass} text-uppercase">${a.category.replace('_', ' ')}</span>
            <span class="badge bg-light text-dark border small">${a.season || 'All Season'}</span>
          </div>

          <h5 class="fw-bold mb-2 text-dark">${a.title}</h5>
          <div class="small text-success fw-semibold mb-2">
            <i class="bi bi-tag-fill me-1"></i>${a.targetCrop || 'All Agriculture'}
          </div>

          <p class="text-secondary small mb-4 flex-grow-1">
            ${a.summary || a.details.substring(0, 140) + '...'}
          </p>

          <div class="d-flex align-items-center justify-content-between pt-3 border-top mt-auto">
            <button class="btn btn-sm btn-agro-outline" onclick="openAdvisoryModal(${a.id})">
              Read Details <i class="bi bi-arrow-right ms-1"></i>
            </button>
            ${a.officialLink ? `
              <a href="${a.officialLink}" target="_blank" rel="noopener noreferrer" class="btn btn-sm btn-light border" title="Official Government Portal">
                <i class="bi bi-box-arrow-up-right text-success"></i> Portal
              </a>
            ` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function openAdvisoryModal(id) {
  const item = allAdvisories.find(a => a.id === id);
  if (!item) return;

  document.getElementById('adv-modal-title').textContent = item.title;
  document.getElementById('adv-modal-category').textContent = item.category.replace('_', ' ');
  document.getElementById('adv-modal-season').textContent = item.season || 'All Season';
  document.getElementById('adv-modal-targetCrop').textContent = item.targetCrop || 'General Agricultural';
  document.getElementById('adv-modal-state').textContent = item.applicableState || 'All India';
  document.getElementById('adv-modal-details').textContent = item.details;

  const linkBtn = document.getElementById('adv-modal-link');
  if (item.officialLink) {
    linkBtn.href = item.officialLink;
    linkBtn.classList.remove('d-none');
  } else {
    linkBtn.classList.add('d-none');
  }

  const modal = new bootstrap.Modal(document.getElementById('advisoryDetailModal'));
  modal.show();
}
