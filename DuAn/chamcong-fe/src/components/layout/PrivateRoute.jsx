import { Navigate, Outlet, useLocation } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { ROUTES } from '../../constants/routes'

export default function PrivateRoute() {
  const { isAuthenticated, forcePasswordChange } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />
  }

  if (
    forcePasswordChange &&
    location.pathname !== ROUTES.FORCE_CHANGE_PASSWORD
  ) {
    return <Navigate to={ROUTES.FORCE_CHANGE_PASSWORD} replace />
  }

  return <Outlet />
}

