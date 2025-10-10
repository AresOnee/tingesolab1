// src/main.jsx
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "./services/keycloak";

ReactDOM.createRoot(document.getElementById("root")).render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: "login-required",
      pkceMethod: "S256",
      checkLoginIframe: false,
    }}
    onTokens={({ token }) => {
      // deja el token disponible para axios/interceptores
      localStorage.setItem("kc_token", token);
      window.keycloak = keycloak;
    }}
  >
    <React.StrictMode>
      <App />
    </React.StrictMode>
  </ReactKeycloakProvider>
);
