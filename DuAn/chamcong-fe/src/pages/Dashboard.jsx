import DashboardLayout from '../components/layout/DashboardLayout'
import useAuthStore from '../store/authStore'

export default function Dashboard() {
  const user = useAuthStore((s) => s.user)

  return (
    <DashboardLayout>
      <div className="rounded-xl bg-white p-8 shadow">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">
          Xin chào, <span className="font-medium">{user?.fullName}</span>!
        </p>
        <p className="mt-1 text-sm text-gray-500">
          Chào mừng bạn đến với hệ thống Chấm Công & Home Services.
        </p>
      </div>
    </DashboardLayout>
  )
}

