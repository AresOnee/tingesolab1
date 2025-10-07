// src/keycloak.js
import Keycloak from "keycloak-js";

// Ajusta estos datos a tu realm/cliente
const keycloak = new Keycloak({
  url: "http://localhost:9090",    // servidor Keycloak
  realm: "sisgr-realm",            // tu realm
  clientId: "sisgr-frontend",      // id del cliente del frontend en Keycloak
});

export async function initKeycloak() {
  const authenticated = await keycloak.init({
    onLoad: "login-required",
    checkLoginIframe: false,
    pkceMethod: "S256",
  });

  if (!authenticated) {
    await keycloak.login();
  }

  // Guarda referencia global y token para que cualquier módulo lo pueda leer
  window.keycloak = keycloak;
  localStorage.setItem("kc_token", keycloak.token);

  // Mantén el token fresco
  setInterval(async () => {
    try {
      const refreshed = await keycloak.updateToken(30);
      if (refreshed) {
        localStorage.setItem("kc_token", keycloak.token);
      }
    } catch (e) {
      console.error("Fallo refrescando token:", e);
      keycloak.login();
    }
  }, 25_000);
}

export default keycloak;
