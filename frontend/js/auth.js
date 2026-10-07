/**
 * SMART KRISHI - Authentication Service & Route Guards
 */
const AUTH = {
  getUser() {
    try {
      const data = localStorage.getItem('smart_krishi_user');
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  },

  setUser(user) {
    localStorage.setItem('smart_krishi_user', JSON.stringify(user));
  },

  getToken() {
    return localStorage.getItem('smart_krishi_token');
  },

  setSession(authResponse) {
    if (authResponse.token) {
      localStorage.setItem('smart_krishi_token', authResponse.token);
    }
    const user = {
      id: authResponse.id,
      name: authResponse.name,
      email: authResponse.email,
      phone: authResponse.phone,
      role: authResponse.role,
      village: authResponse.village,
      district: authResponse.district,
      state: authResponse.state,
      landArea: authResponse.landArea,
      soilType: authResponse.soilType
    };
    this.setUser(user);
  },

  isAuthenticated() {
    return !!this.getToken() && !!this.getUser();
  },

  isAdmin() {
    const user = this.getUser();
    return user && user.role === 'ROLE_ADMIN';
  },

  async login(email, password) {
    const res = await window.API.post('/api/auth/login', { email, password });
    if (res && res.data) {
      this.setSession(res.data);
      return res.data;
    }
    throw new Error('Invalid login response');
  },

  async register(data) {
    const res = await window.API.post('/api/auth/register', data);
    if (res && res.data) {
      this.setSession(res.data);
      return res.data;
    }
    throw new Error('Registration failed');
  },

  logout() {
    localStorage.removeItem('smart_krishi_token');
    localStorage.removeItem('smart_krishi_user');
    window.location.href = 'login.html?loggedOut=true';
  },

  requireAuth(requireAdmin = false) {
    if (!this.isAuthenticated()) {
      window.location.href = 'login.html?redirect=' + encodeURIComponent(window.location.pathname);
      return false;
    }
    if (requireAdmin && !this.isAdmin()) {
      window.location.href = 'dashboard.html?forbidden=true';
      return false;
    }
    return true;
  }
};

window.AUTH = AUTH;
