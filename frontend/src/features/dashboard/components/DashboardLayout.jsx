import {
  AppBar,
  Box,
  Button,
  Container,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import { RoleSwitcher } from '../../navigation';
import ContactInfoCard from './ContactInfoCard';

const ROLE_TO_PATH = {
  ROLE_USER: '/',
  ROLE_MANAGER: '/manager',
  ROLE_ADMIN: '/admin',
};

const PATH_TO_ROLE = Object.fromEntries(
  Object.entries(ROLE_TO_PATH).map(([role, path]) => [path, role])
);

export default function DashboardLayout() {
  const { token, user, roles, logout, setUser } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const availableRoles = roles?.length ? roles : ['ROLE_USER'];
  const currentRole =
    PATH_TO_ROLE[location.pathname] ??
    (availableRoles.includes('ROLE_USER') ? 'ROLE_USER' : availableRoles[0]);

  if (!token) {
    return null;
  }

  if (!availableRoles.includes(currentRole)) {
    navigate(ROLE_TO_PATH[availableRoles[0]], { replace: true });
    return null;
  }

  const handleRoleChange = (role) => {
    const target = ROLE_TO_PATH[role] ?? '/';
    navigate(target, { replace: true });
  };

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar position="static" color="primary" enableColorOnDark>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Primary Ticket Outlet
          </Typography>
          <Stack direction="row" spacing={2} alignItems="center">
            <Typography variant="body2">
              {user?.displayName || user?.email}
            </Typography>
            <Button color="inherit" onClick={logout}>
              Sign out
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container sx={{ py: 4 }}>
        <Stack spacing={4}>
          {user && (
            <ContactInfoCard token={token} user={user} onUpdated={setUser} />
          )}
          <RoleSwitcher
            roles={availableRoles}
            activeRole={currentRole}
            onRoleChange={handleRoleChange}
          />
          <Outlet />
        </Stack>
      </Container>
    </Box>
  );
}

