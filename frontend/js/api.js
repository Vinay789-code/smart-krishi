/**
 * SMART KRISHI - Centralized API Service
 */
const API = {
  async request(endpoint, options = {}) {
    const baseUrl = window.API_BASE_URL || (window.CONFIG && window.CONFIG.getApiBaseUrl ? window.CONFIG.getApiBaseUrl() : '');
    const url = `${baseUrl}${endpoint}`;

    const headers = { ...options.headers };

    // Inject token if available
    const token = localStorage.getItem('smart_krishi_token');
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Set JSON content-type if body is an object and not FormData
    if (options.body && !(options.body instanceof FormData) && typeof options.body === 'object') {
      headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(options.body);
    }

    try {
      const response = await fetch(url, {
        ...options,
        headers
      });

      // Handle 401 Unauthorized only for protected endpoints requiring user login
      if (response.status === 401) {
        const isPublicEndpoint = endpoint.includes('/disease/analyze') ||
                                 endpoint.includes('/crops/recommend') ||
                                 endpoint.includes('/weather') ||
                                 endpoint.includes('/market') ||
                                 endpoint.includes('/advisory');
        if (!isPublicEndpoint && !window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html')) {
          localStorage.removeItem('smart_krishi_token');
          localStorage.removeItem('smart_krishi_user');
          window.location.href = 'login.html?expired=true';
          return;
        }
      }

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMsg = data?.message || data?.error || `Request failed with status ${response.status}`;
        const err = new Error(errorMsg);
        err.status = response.status;
        err.validationErrors = data?.validationErrors;
        throw err;
      }

      return data;
    } catch (error) {
      console.error(`API Error on ${endpoint}:`, error);
      throw error;
    }
  },

  get(endpoint, params = null) {
    let url = endpoint;
    if (params) {
      const query = new URLSearchParams();
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined && params[key] !== '') {
          query.append(key, params[key]);
        }
      });
      const queryString = query.toString();
      if (queryString) {
        url += (url.includes('?') ? '&' : '?') + queryString;
      }
    }
    return this.request(url, { method: 'GET' });
  },

  post(endpoint, body) {
    return this.request(endpoint, {
      method: 'POST',
      body
    });
  },

  put(endpoint, body) {
    return this.request(endpoint, {
      method: 'PUT',
      body
    });
  },

  delete(endpoint) {
    return this.request(endpoint, {
      method: 'DELETE'
    });
  },

  upload(endpoint, formData, options = {}) {
    return this.request(endpoint, {
      method: 'POST',
      body: formData,
      ...options
    });
  }
};

window.API = API;
