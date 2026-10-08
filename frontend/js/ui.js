/**
 * SMART KRISHI - UI Components & Helpers
 */
const UI = {
  renderNavbar(activePage = '') {
    const navContainer = document.getElementById('navbar-container');
    if (!navContainer) return;

    const isAuth = window.AUTH && window.AUTH.isAuthenticated();
    const isAdmin = window.AUTH && window.AUTH.isAdmin();
    const user = isAuth ? window.AUTH.getUser() : null;

    navContainer.innerHTML = `
      <nav class="navbar navbar-expand-lg smart-navbar">
        <div class="container">
          <a class="navbar-brand" href="index.html">
            <span class="brand-icon"><i class="bi bi-flower1"></i></span>
            <span>Smart<span style="color: var(--primary-light);">Krishi</span></span>
          </a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#smartNavMenu" aria-controls="smartNavMenu" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
          </button>

          <div class="collapse navbar-collapse" id="smartNavMenu">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3">
              <li class="nav-item">
                <a class="nav-link ${activePage === 'home' ? 'active' : ''}" href="index.html">Home</a>
              </li>
              ${isAuth ? `
              <li class="nav-item">
                <a class="nav-link ${activePage === 'dashboard' ? 'active' : ''}" href="dashboard.html">Dashboard</a>
              </li>` : ''}
              <li class="nav-item">
                <a class="nav-link ${activePage === 'crops' ? 'active' : ''}" href="crop-recommendation.html">Crop Recommendation</a>
              </li>
              <li class="nav-item">
                <a class="nav-link ${activePage === 'weather' ? 'active' : ''}" href="weather.html">Weather & Irrigation</a>
              </li>
              <li class="nav-item">
                <a class="nav-link ${activePage === 'market' ? 'active' : ''}" href="market-prices.html">Market Prices</a>
              </li>
              <li class="nav-item">
                <a class="nav-link ${activePage === 'advisory' ? 'active' : ''}" href="advisory.html">Advisory & Schemes</a>
              </li>
              <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle ${['fertilizer', 'profit', 'crop-calendar', 'price-prediction'].includes(activePage) ? 'active' : ''}" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                  <i class="bi bi-grid-3x3-gap-fill me-1 text-success"></i>Smart Tools
                </a>
                <ul class="dropdown-menu shadow-sm">
                  <li><a class="dropdown-item ${activePage === 'fertilizer' ? 'active' : ''}" href="fertilizer.html"><i class="bi bi-droplet-half text-success me-2"></i>Fertilizer Advisor</a></li>
                  <li><a class="dropdown-item ${activePage === 'profit' ? 'active' : ''}" href="profit-calculator.html"><i class="bi bi-calculator text-primary me-2"></i>Crop Profit / ROI</a></li>
                  <li><a class="dropdown-item ${activePage === 'crop-calendar' ? 'active' : ''}" href="crop-calendar.html"><i class="bi bi-calendar3 text-warning me-2"></i>Crop Calendar</a></li>
                  <li><a class="dropdown-item ${activePage === 'price-prediction' ? 'active' : ''}" href="price-prediction.html"><i class="bi bi-graph-up-arrow text-danger me-2"></i>Price Trend & Prediction</a></li>
                </ul>
              </li>
              ${isAdmin ? `
              <li class="nav-item">
                <a class="nav-link text-danger ${activePage === 'admin' ? 'active' : ''}" href="admin.html"><i class="bi bi-shield-lock me-1"></i>Admin</a>
              </li>` : ''}
            </ul>

            <div class="d-flex align-items-center gap-2 mt-3 mt-lg-0">
              <button class="btn btn-sm btn-outline-secondary rounded-pill px-3" onclick="UI.showBackendModal()" title="API Backend Settings">
                <i class="bi bi-gear-fill me-1"></i>API Config
              </button>

              ${isAuth ? `
                <div class="dropdown">
                  <button class="btn btn-agro-outline dropdown-toggle d-flex align-items-center gap-2 rounded-pill px-3 py-1" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="bi bi-person-circle fs-5"></i>
                    <span>${user.name ? user.name.split(' ')[0] : 'Farmer'}</span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                    <li><h6 class="dropdown-header">${user.email} (${isAdmin ? 'Admin' : 'Farmer'})</h6></li>
                    <li><a class="dropdown-item" href="dashboard.html"><i class="bi bi-speedometer2 me-2"></i>Dashboard</a></li>
                    <li><a class="dropdown-item" href="profile.html"><i class="bi bi-person-lines-fill me-2"></i>Farm Profile</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item text-danger" href="javascript:void(0)" onclick="AUTH.logout()"><i class="bi bi-box-arrow-right me-2"></i>Logout</a></li>
                  </ul>
                </div>
              ` : `
                <a href="login.html" class="btn btn-agro-outline">Login</a>
                <a href="register.html" class="btn btn-agro-primary">Register</a>
              `}
            </div>
          </div>
        </div>
      </nav>
    `;
  },

  renderFooter() {
    const footerContainer = document.getElementById('footer-container');
    if (!footerContainer) return;

    footerContainer.innerHTML = `
      <footer class="smart-footer">
        <div class="container">
          <div class="row g-4">
            <div class="col-lg-4 col-md-6">
              <div class="d-flex align-items-center gap-2 mb-2">
                <span class="brand-icon" style="width: 32px; height: 32px; font-size: 1rem;"><i class="bi bi-flower1"></i></span>
                <span class="fw-bold fs-5 text-dark">Smart Krishi – Precision Agriculture Platform</span>
              </div>
              <p class="text-muted small mb-3">
                Empowering Indian farmers with data-driven crop recommendations, hyper-local weather & smart irrigation advisories, and transparent mandi market prices.
              </p>
              <div class="d-flex gap-2">
                <span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-shield-check me-1"></i>Production Ready</span>
                <span class="badge bg-light text-dark border"><i class="bi bi-cpu me-1"></i>Spring Boot + REST</span>
              </div>
            </div>

            <div class="col-lg-2 col-md-3 col-6">
              <h6 class="fw-bold text-dark mb-3">Modules</h6>
              <ul class="list-unstyled small d-flex flex-column gap-2 mb-0">
                <li><a href="crop-recommendation.html">Crop Recommendation</a></li>
                <li><a href="weather.html">Weather & Irrigation</a></li>
                <li><a href="market-prices.html">APMC Mandi Rates</a></li>
                <li><a href="advisory.html">Govt Schemes & Tips</a></li>
                <li><a href="fertilizer.html">Fertilizer Advisor</a></li>
                <li><a href="profit-calculator.html">ROI Calculator</a></li>
                <li><a href="crop-calendar.html">Crop Calendar</a></li>
                <li><a href="price-prediction.html">Price Prediction</a></li>
              </ul>
            </div>

            <div class="col-lg-2 col-md-3 col-6">
              <h6 class="fw-bold text-dark mb-3">Farmer Portal</h6>
              <ul class="list-unstyled small d-flex flex-column gap-2 mb-0">
                <li><a href="dashboard.html">Farmer Dashboard</a></li>
                <li><a href="profile.html">Land & Farm Profile</a></li>
                <li><a href="login.html">Sign In</a></li>
                <li><a href="register.html">Farmer Registration</a></li>
                <li><a href="admin.html">Admin Portal</a></li>
              </ul>
            </div>

            <div class="col-lg-4 col-md-6">
              <h6 class="fw-bold text-dark mb-3">Kisan Helpline & Assistance</h6>
              <div class="p-3 rounded bg-light border mb-2">
                <div class="d-flex align-items-center gap-2 text-success fw-bold">
                  <i class="bi bi-telephone-fill"></i> Kisan Call Centre: 1800-180-1551
                </div>
                <div class="small text-muted mt-1">Toll-free 24x7 government advisory line for all agricultural inquiries.</div>
              </div>
              <div class="small text-muted">
                Active Backend: <code id="active-backend-display" class="text-success">${window.API_BASE_URL || (window.CONFIG ? window.CONFIG.getApiBaseUrl() : '')}</code>
              </div>
            </div>
          </div>

          <hr class="my-4 text-muted">

          <div class="d-flex flex-column flex-md-row justify-content-between align-items-center small text-muted">
            <div>© ${new Date().getFullYear()} Smart Krishi – Precision Agriculture Platform.</div>
            <div class="d-flex gap-3 mt-2 mt-md-0">
              <span>Deployable on Netlify & Render</span>
              <span>•</span>
              <a href="javascript:void(0)" onclick="UI.showBackendModal()">Configure Backend</a>
            </div>
          </div>
        </div>
      </footer>
    `;
  },

  showToast(message, type = 'success', duration = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container-agro';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `agro-toast toast-${type}`;

    let icon = 'bi-check-circle-fill text-success';
    if (type === 'error') icon = 'bi-exclamation-triangle-fill text-danger';
    if (type === 'warning') icon = 'bi-exclamation-circle-fill text-warning';
    if (type === 'info') icon = 'bi-info-circle-fill text-primary';

    toast.innerHTML = `
      <i class="bi ${icon} fs-5 mt-1"></i>
      <div class="flex-grow-1">
        <div class="fw-semibold text-dark mb-0">${message}</div>
      </div>
      <button type="button" class="btn-close btn-close-sm ms-2" aria-label="Close"></button>
    `;

    toast.querySelector('.btn-close').addEventListener('click', () => {
      toast.remove();
    });

    container.appendChild(toast);

    setTimeout(() => {
      if (toast.parentNode) {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
      }
    }, duration);
  },

  showBackendModal() {
    let modalEl = document.getElementById('backendConfigModal');
    if (!modalEl) {
      const modalHtml = `
        <div class="modal fade" id="backendConfigModal" tabindex="-1" aria-labelledby="backendConfigModalLabel" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title fw-bold" id="backendConfigModalLabel"><i class="bi bi-hdd-network me-2 text-success"></i>Backend API URL</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
              </div>
              <div class="modal-body">
                <p class="small text-muted">
                  Configure the Spring Boot backend server URL. Perfect for switching between your local server and your live Render cloud backend.
                </p>
                <div class="mb-3">
                  <label class="form-label">API Base URL</label>
                  <input type="url" id="modalBackendUrlInput" class="form-control" placeholder="https://smart-krishi-backend.onrender.com">
                </div>
                <div class="d-flex gap-2">
                  <button type="button" class="btn btn-sm btn-outline-secondary" onclick="document.getElementById('modalBackendUrlInput').value=window.location.protocol+'//'+window.location.hostname+':8080'">Local Backend (:8080)</button>
                  <button type="button" class="btn btn-sm btn-outline-secondary" onclick="document.getElementById('modalBackendUrlInput').value='https://smart-krishi-backend.onrender.com'">Render Cloud</button>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-agro-primary" onclick="UI.saveBackendUrl()">Save & Reload</button>
              </div>
            </div>
          </div>
        </div>
      `;
      document.body.insertAdjacentHTML('beforeend', modalHtml);
      modalEl = document.getElementById('backendConfigModal');
    }

    document.getElementById('modalBackendUrlInput').value = window.CONFIG.getApiBaseUrl();
    const bsModal = new bootstrap.Modal(modalEl);
    bsModal.show();
  },

  saveBackendUrl() {
    const val = document.getElementById('modalBackendUrlInput').value;
    if (val && val.trim()) {
      window.CONFIG.setApiBaseUrl(val.trim());
    } else {
      window.CONFIG.resetApiBaseUrl();
    }
    window.location.reload();
  },

  formatCurrency(num) {
    if (num == null) return '₹0';
    return '₹' + Number(num).toLocaleString('en-IN');
  }
};

window.UI = UI;
