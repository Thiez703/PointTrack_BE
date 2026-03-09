import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import DashboardLayout from '../../components/layout/DashboardLayout'
import Avatar from '../../components/ui/Avatar'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'
import Button from '../../components/ui/Button'
import FormField from '../../components/ui/FormField'
import PasswordStrength from '../../components/ui/PasswordStrength'
import { userApi } from '../../api/userApi'
import { profileSchema, changePasswordSchema } from '../../utils/validators'
import useAuthStore from '../../store/authStore'
import useToastStore from '../../store/toastStore'

function ProfileInfo({ data }) {
  return (
    <div className="rounded-xl bg-white p-6 shadow">
      <div className="flex flex-col items-center text-center">
        <Avatar src={data.avatarUrl} name={data.fullName} size="lg" />
        <h2 className="mt-3 text-lg font-semibold text-gray-900">
          {data.fullName}
        </h2>
        <p className="text-sm text-gray-500">{data.email}</p>
        <span className="mt-2 inline-block rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800">
          {data.role}
        </span>
        {data.rank && (
          <span className="mt-1 text-sm text-gray-500">{data.rank}</span>
        )}
      </div>
      <div className="mt-4 border-t pt-4 text-sm text-gray-500">
        <p>
          Đăng nhập lần cuối:{' '}
          {data.lastLoginAt
            ? new Date(data.lastLoginAt).toLocaleString('vi-VN')
            : '—'}
        </p>
      </div>
    </div>
  )
}

function ProfileEditForm({ data }) {
  const queryClient = useQueryClient()
  const updateUser = useAuthStore((s) => s.updateUser)
  const addToast = useToastStore((s) => s.addToast)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: '',
      phone: '',
      avatarUrl: '',
    },
  })

  useEffect(() => {
    if (data) {
      reset({
        fullName: data.fullName || '',
        phone: data.phone || '',
        avatarUrl: data.avatarUrl || '',
      })
    }
  }, [data, reset])

  const mutation = useMutation({
    mutationFn: (values) => userApi.updateProfile(values),
    onSuccess: (res) => {
      const updated = res.data.data
      queryClient.invalidateQueries({ queryKey: ['profile'] })
      updateUser({
        fullName: updated.fullName,
        phone: updated.phone,
        avatarUrl: updated.avatarUrl,
      })
      addToast({ type: 'success', message: 'Cập nhật thông tin thành công!' })
    },
    onError: (err) => {
      addToast({
        type: 'error',
        message: err.response?.data?.message || 'Cập nhật thất bại.',
      })
    },
  })

  return (
    <div className="rounded-xl bg-white p-6 shadow">
      <h3 className="mb-4 text-lg font-semibold text-gray-900">
        Chỉnh sửa thông tin
      </h3>
      <form
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
        className="space-y-4"
        noValidate
      >
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Email
          </label>
          <input
            type="email"
            value={data?.email || ''}
            disabled
            className="block w-full rounded-lg border border-gray-200 bg-gray-50
              px-3 py-2 text-sm text-gray-500 cursor-not-allowed"
          />
        </div>

        <FormField
          label="Họ tên"
          type="text"
          placeholder="Nhập họ tên"
          error={errors.fullName}
          register={register('fullName')}
          disabled={mutation.isPending}
        />

        <FormField
          label="Số điện thoại"
          type="tel"
          placeholder="0912345678"
          error={errors.phone}
          register={register('phone')}
          disabled={mutation.isPending}
        />

        <FormField
          label="Avatar URL"
          type="url"
          placeholder="https://example.com/avatar.jpg"
          error={errors.avatarUrl}
          register={register('avatarUrl')}
          disabled={mutation.isPending}
        />

        <div className="flex gap-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Vai trò
          </label>
          <span className="text-sm text-gray-500">{data?.role}</span>
        </div>

        <div className="flex gap-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Cấp bậc
          </label>
          <span className="text-sm text-gray-500">{data?.rank || '—'}</span>
        </div>

        <Button
          type="submit"
          loading={mutation.isPending}
          disabled={mutation.isPending || !isDirty}
          className="w-full"
        >
          Cập nhật
        </Button>
      </form>
    </div>
  )
}

function ChangePasswordForm() {
  const updateTokens = useAuthStore((s) => s.updateTokens)
  const addToast = useToastStore((s) => s.addToast)

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  })

  const watchNewPassword = watch('newPassword')

  const mutation = useMutation({
    mutationFn: (values) => userApi.changePassword(values),
    onSuccess: (res) => {
      const result = res.data.data
      if (result?.accessToken && result?.refreshToken) {
        updateTokens({
          accessToken: result.accessToken,
          refreshToken: result.refreshToken,
        })
      }
      reset()
      addToast({ type: 'success', message: 'Đổi mật khẩu thành công!' })
    },
    onError: (err) => {
      addToast({
        type: 'error',
        message: err.response?.data?.message || 'Đổi mật khẩu thất bại.',
      })
    },
  })

  return (
    <div className="rounded-xl bg-white p-6 shadow">
      <h3 className="mb-4 text-lg font-semibold text-gray-900">
        Đổi mật khẩu
      </h3>
      <form
        onSubmit={handleSubmit((values) => mutation.mutate(values))}
        className="space-y-4"
        noValidate
      >
        <FormField
          label="Mật khẩu hiện tại"
          type="password"
          placeholder="Nhập mật khẩu hiện tại"
          autoComplete="current-password"
          error={errors.currentPassword}
          register={register('currentPassword')}
          disabled={mutation.isPending}
        />

        <div>
          <FormField
            label="Mật khẩu mới"
            type="password"
            placeholder="Ít nhất 8 ký tự, có chữ và số"
            autoComplete="new-password"
            error={errors.newPassword}
            register={register('newPassword')}
            disabled={mutation.isPending}
          />
          <PasswordStrength password={watchNewPassword} />
        </div>

        <FormField
          label="Xác nhận mật khẩu mới"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          autoComplete="new-password"
          error={errors.confirmPassword}
          register={register('confirmPassword')}
          disabled={mutation.isPending}
        />

        <Button
          type="submit"
          loading={mutation.isPending}
          disabled={mutation.isPending}
          className="w-full"
          variant="secondary"
        >
          Đổi mật khẩu
        </Button>
      </form>
    </div>
  )
}

export default function Profile() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['profile'],
    queryFn: async () => {
      const res = await userApi.getProfile()
      return res.data.data
    },
  })

  return (
    <DashboardLayout>
      <div className="mx-auto max-w-5xl">
        <h1 className="mb-6 text-2xl font-bold text-gray-900">
          Thông tin cá nhân
        </h1>

        {isLoading && (
          <div className="flex justify-center py-12">
            <Spinner size="lg" />
          </div>
        )}

        {error && (
          <Alert
            type="error"
            message={error.response?.data?.message || 'Tải thông tin thất bại'}
          />
        )}

        {data && (
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
            <div className="lg:col-span-1">
              <ProfileInfo data={data} />
            </div>
            <div className="lg:col-span-2 space-y-6">
              <ProfileEditForm data={data} />
              <ChangePasswordForm />
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}

