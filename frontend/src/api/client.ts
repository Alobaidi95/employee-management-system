import axios from "axios";

// Every request goes to the Spring Boot backend. In dev, that's
// localhost:8080 (matches the docker-compose.yml port mapping). We'll
// make this configurable via an env variable once we get to deployment -
// hardcoded for now since we're only running locally.
const apiClient = axios.create({
  baseURL: "http://localhost:8080/api",
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