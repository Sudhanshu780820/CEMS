import axios from 'axios';

// Resolve backend API URL from VITE_API_BASE_URL environment variable
// If unset, defaults to '/api' (routed to http://localhost:8080 by Vite proxy in dev)
// If set to e.g. 'https://backend.example.com', automatically normalizes to 'https://backend.example.com/api'
const resolveBaseURL = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (!envUrl || !envUrl.trim()) {
    return '/api';
  }
  const cleanUrl = envUrl.trim().replace(/\/+$/, '');
  return cleanUrl.endsWith('/api') ? cleanUrl : `${cleanUrl}/api`;
};

const api = axios.create({
  baseURL: resolveBaseURL(),
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      const isAuthPath = window.location.pathname.startsWith('/login') || 
                         window.location.pathname.startsWith('/register') ||
                         window.location.pathname === '/' ||
                         window.location.pathname.startsWith('/events');
      if (!isAuthPath) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
