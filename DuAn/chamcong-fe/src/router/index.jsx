import { Routes, Route, Navigate } from 'react-router-dom'
import PublicRoute from '../components/layout/PublicRoute'
import PrivateRoute from '../components/layout/PrivateRoute'
import Login from '../pages/auth/Login'
import ForceChangePassword from '../pages/auth/ForceChangePassword'
import ForgotPassword from '../pages/auth/ForgotPassword'
import ResetPassword from '../pages/auth/ResetPassword'
import Dashboard from '../pages/Dashboard'
import Profile from '../pages/profile/Profile'
import { ROUTES } from '../constants/routes'

export default function AppRouter() {
  return (
    <Routes>
      <Route element={<PublicRoute />}>
        <Route path={ROUTES.LOGIN} element={<Login />} />
        <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPassword />} />
        <Route path={ROUTES.RESET_PASSWORD} element={<ResetPassword />} />
      </Route>

      <Route element={<PrivateRoute />}>
        <Route
          path={ROUTES.FORCE_CHANGE_PASSWORD}
          element={<ForceChangePassword />}
        />
        <Route path={ROUTES.DASHBOARD} element={<Dashboard />} />
        <Route path={ROUTES.PROFILE} element={<Profile />} />
      </Route>

      <Route path="*" element={<Navigate to={ROUTES.LOGIN} replace />} />
    </Routes>
  )
}

