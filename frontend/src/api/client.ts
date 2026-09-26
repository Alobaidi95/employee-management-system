import axios from "axios";

// VITE_API_URL is baked in at BUILD time, not runtime - Vite only exposes
// env vars prefixed with VITE_ to client code, and it substitutes them
// when `npm run build` runs, not when the resulting container later
// starts. Falls back to localhost for local `npm run dev`, where no env
// var is set at all.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080/api",
});

// Runs before every outgoing request. Reads the JWT from localStorage (if
// present) and attaches it as a Bearer token - this is what replaces
// manually adding an Authorization header to every single API call.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Runs after every response. If the backend ever returns 401 (token
// missing/expired/invalid), we clear the stale token and send the user
// back to login - otherwise they'd be stuck seeing broken pages with no
// clear reason why.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default apiClient;