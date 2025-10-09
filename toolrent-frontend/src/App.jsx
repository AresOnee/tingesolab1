import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Box from '@mui/material/Box'
import Toolbar from '@mui/material/Toolbar'
import PrivateRoute from "./PrivateRoute";
import Navbar from "./components/Navbar";
import Sidemenu, { drawerWidth } from "./components/Sidemenu";
import Home from "./components/Home";
import ToolsList from "./components/ToolsList";
import ClientsList from "./components/ClientsList";
import Loans from "./components/Loans";
import ConfigManagement from "./components/ConfigManagement";
import Unauthorized from "./components/Unauthorized";

export default function App() {
  return (
    <Router>
      <Navbar />
      <Sidemenu />
      <Box component="main" sx={{ flexGrow: 1, ml: `${drawerWidth}px`, p: 3 }}>
        <Toolbar /> {/* espacio bajo la AppBar */}
        <Routes>
          <Route path="/" element={<Navigate to="/home" replace />} />
          <Route path="/home" element={
            <PrivateRoute roles={['USER','ADMIN']}><Home/></PrivateRoute>
          }/>
          <Route path="/tools" element={
            <PrivateRoute roles={['ADMIN']}><ToolsList/></PrivateRoute>
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
          <Route path="/unauthorized" element={<Unauthorized />} />
          <Route path="*" element={<Navigate to="/home" replace />} />
        </Routes>
      </Box>
    </Router>
  );
}