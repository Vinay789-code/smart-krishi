/**
 * SMART KRISHI – Precision Agriculture Platform
 * Single Configurable API_BASE_URL Configuration
 * Supports Netlify, Render, and Localhost environments seamlessly
 */

const API_BASE_URL = (function() {
  // 1. Explicit window override
  if (typeof window !== 'undefined' && window.__SMART_KRISHI_API_URL__) {
    return window.__SMART_KRISHI_API_URL__.replace(/\/$/, '');
  }

  // 2. User/Admin configured URL in browser storage
  const stored = typeof localStorage !== 'undefined' ? localStorage.getItem('smart_krishi_api_url') : null;
  if (stored && stored.trim() !== '') {
    return stored.trim().replace(/\/$/, '');
  }

  // 3. Environment-aware runtime detection
  if (typeof window !== 'undefined' && window.location) {
    const hostname = window.location.hostname;
    const isLocal = hostname === 'localhost' || hostname === '127.0.0.1';
    
    // When running on Netlify (*.netlify.app) or external domain, target Render backend
    if (!isLocal) {
      return 'https://smart-krishi-backend.onrender.com';
    }

    // Dynamic local origin without hardcoding
    const protocol = window.location.protocol || 'http:';
    return `${protocol}//${hostname}:8080`;
  }

  return 'https://smart-krishi-backend.onrender.com';
})();

// Export globally for all modules
window.API_BASE_URL = API_BASE_URL;

const CONFIG = {
  getApiBaseUrl() {
    return window.API_BASE_URL || API_BASE_URL;
  },

  setApiBaseUrl(url) {
    if (url && url.trim() !== '') {
      const clean = url.trim().replace(/\/$/, '');
      localStorage.setItem('smart_krishi_api_url', clean);
      window.API_BASE_URL = clean;
    } else {
      localStorage.removeItem('smart_krishi_api_url');
      window.API_BASE_URL = API_BASE_URL;
    }
  },

  resetApiBaseUrl() {
    localStorage.removeItem('smart_krishi_api_url');
  }
};

window.CONFIG = CONFIG;
