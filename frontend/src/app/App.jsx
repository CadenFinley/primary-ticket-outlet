import { CssBaseline } from '@mui/material';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth, LoginPage } from '../features/auth';
import DashboardLayout from '../features/dashboard/components/DashboardLayout';
import {
  AdminDashboard,
  AttendeeDashboard,
  ManagerDashboard,
} from '../features/dashboard';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#7c3aed',
      dark: '#5b21b6',
      light: '#a855f7',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#c084fc',
      dark: '#7e22ce',
      light: '#e9d5ff',
      contrastText: '#2e1065',
    },
    background: {
      default: '#f5f0ff',
      paper: '#ffffff',
    },
  },
});

function ProtectedRoute({ children }) {
  const { token } = useAuth();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        <AuthProvider>
          <CssBaseline />
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <DashboardLayout />
                </ProtectedRoute>
              }
            >
              <Route index element={<AttendeeDashboard />} />
              <Route path="manager" element={<ManagerDashboard />} />
              <Route path="admin" element={<AdminDashboard />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}

