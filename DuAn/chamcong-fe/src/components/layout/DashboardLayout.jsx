import { Navigate, useLocation } from 'react-router-dom'
import useAuthStore from '../../store/authStore'
import { ROUTES } from '../../constants/routes'

export default function DashboardLayout({ children }) {
  const { isAuthenticated, forcePasswordChange, user, clearAuth } = useAuthStore()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />
  }

  if (forcePasswordChange && location.pathname !== ROUTES.FORCE_CHANGE_PASSWORD) {
    return <Navigate to={ROUTES.FORCE_CHANGE_PASSWORD} replace />
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="border-b border-gray-200 bg-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600">
                <svg className="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <span className="text-lg font-bold text-gray-900">ChamCong</span>
            </div>
            <div className="flex items-center gap-4">
              <a href={ROUTES.PROFILE} className="text-sm text-gray-600 hover:text-gray-900">
                {user?.fullName || user?.email}
              </a>
              <button
                onClick={() => {
                  clearAuth()
                  window.location.href = ROUTES.LOGIN
                }}
                className="text-sm text-red-600 hover:text-red-800"
              >
                Đăng xuất
              </button>
            </div>
          </div>
        </div>
      </nav>
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  )
}

