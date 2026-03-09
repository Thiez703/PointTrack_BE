import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import AuthLayout from '../../components/layout/AuthLayout'
import FormField from '../../components/ui/FormField'
import Button from '../../components/ui/Button'
import Alert from '../../components/ui/Alert'
import PasswordStrength from '../../components/ui/PasswordStrength'
import { useAuth } from '../../hooks/useAuth'
import { passwordSchema } from '../../utils/validators'
import { ROUTES } from '../../constants/routes'
import useAuthStore from '../../store/authStore'

export default function ForceChangePassword() {
  const [apiError, setApiError] = useState('')
  const { firstChangePassword } = useAuth()
  const forcePasswordChange = useAuthStore((s) => s.forcePasswordChange)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(passwordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  const watchPassword = watch('newPassword')

  if (!forcePasswordChange) {
    return <Navigate to={ROUTES.DASHBOARD} replace />
  }

  const onSubmit = async (values) => {
    setApiError('')
    try {
      await firstChangePassword(values)
    } catch (err) {
      const msg =
        err.response?.data?.message ||
        'Đổi mật khẩu thất bại. Vui lòng thử lại.'
      setApiError(msg)
    }
  }

  return (
    <AuthLayout>
      <div className="mb-4 rounded-lg bg-amber-50 border border-amber-300 px-4 py-3">
        <p className="text-sm font-medium text-amber-800">
          Đây là lần đăng nhập đầu tiên, vui lòng đổi mật khẩu để tiếp tục.
        </p>
      </div>

      <h2 className="mb-6 text-center text-xl font-semibold text-gray-900">
        Đổi mật khẩu
      </h2>

      <Alert
        type="error"
        message={apiError}
        onClose={() => setApiError('')}
      />

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="mt-4 space-y-4"
        noValidate
      >
        <div>
          <FormField
            label="Mật khẩu mới"
            type="password"
            placeholder="Ít nhất 8 ký tự, có chữ và số"
            autoComplete="new-password"
            error={errors.newPassword}
            register={register('newPassword')}
            disabled={isSubmitting}
          />
          <PasswordStrength password={watchPassword} />
        </div>

        <FormField
          label="Xác nhận mật khẩu"
          type="password"
          placeholder="Nhập lại mật khẩu mới"
          autoComplete="new-password"
          error={errors.confirmPassword}
          register={register('confirmPassword')}
          disabled={isSubmitting}
        />

        <Button
          type="submit"
          loading={isSubmitting}
          disabled={isSubmitting}
          className="w-full"
          size="lg"
        >
          Đổi mật khẩu
        </Button>
      </form>
    </AuthLayout>
  )
}

