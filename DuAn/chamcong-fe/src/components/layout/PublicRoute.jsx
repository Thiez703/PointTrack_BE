import { Navigate, Outlet } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { ROUTES } from '../../constants/routes'

export default function PublicRoute() {
  const { isAuthenticated, forcePasswordChange } = useAuthStore()

  if (isAuthenticated && forcePasswordChange) {
    return <Navigate to={ROUTES.FORCE_CHANGE_PASSWORD} replace />
  }

  if (isAuthenticated) {
    return <Navigate to={ROUTES.DASHBOARD} replace />
  }

  return <Outlet />
}

