// src/App.jsx
// VERSION CORREGIDA - Con setupErrorHandler configurado

import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import { useEffect } from "react";
import PrivateRoute from "./PrivateRoute";
import Navbar from "./components/Navbar";
import Sidemenu, { drawerWidth } from "./components/Sidemenu";
import Home from "./components/Home";
import ToolsList from "./components/ToolsList";
import ClientsList from "./components/ClientsList";
import Loans from "./components/Loans";
import ConfigManagement from "./components/ConfigManagement";
import KardexList from "./components/KardexList";
import Unauthorized from "./components/Unauthorized";
import Reports from "./components/Reports";
import { SnackbarProvider, useSnackbar } from "./contexts/SnackbarContext";
import { setupErrorHandler } from "./http-common";

// Componente interno para configurar el interceptor
function AppContent() {
  const { showError } = useSnackbar();

  // 🔥 CRÍTICO: Configurar el interceptor HTTP para usar el Snackbar
  useEffect(() => {
    console.log("✅ Configurando interceptor HTTP con Snackbar");
    setupErrorHandler(showError);
  }, [showError]);

  return (
    <Router>
      <Navbar />
      <Sidemenu />
      <Box component="main" sx={{ flexGrow: 1, ml: `${drawerWidth}px`, p: 3 }}>
        <Toolbar />
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />
          
          <Route path="/home" element={
            <PrivateRoute roles={['USER','ADMIN']}><Home/></PrivateRoute>
          }/>
          
          <Route path="/tools" element={
            <PrivateRoute roles={['USER','ADMIN']}><ToolsList/></PrivateRoute>
          }/>
          
          <Route path="/clients" element={
            <PrivateRoute roles={['ADMIN']}><ClientsList/></PrivateRoute>
          }/>
          
          <Route path="/loans" element={
            <PrivateRoute roles={['USER','ADMIN']}><Loans/></PrivateRoute>
          }/>
          
          <Route path="/config" element={
            <PrivateRoute roles={['ADMIN']}><ConfigManagement/></PrivateRoute>
          }/>
          
          <Route path="/kardex" element={
            <PrivateRoute roles={['USER','ADMIN']}><KardexList/></PrivateRoute>
          }/>

          <Route path="/reports" element={
            <PrivateRoute roles={['USER','ADMIN']}><Reports/></PrivateRoute>
          }/>
          
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
      </Box>
    </Router>
  );
}

export default function App() {
  return (
    <SnackbarProvider>
      <AppContent />
    </SnackbarProvider>
  );
}