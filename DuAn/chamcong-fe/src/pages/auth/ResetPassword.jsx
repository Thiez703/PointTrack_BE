import { useState, useEffect } from 'react'
import { Link, useSearchParams, useNavigate, Navigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import AuthLayout from '../../components/layout/AuthLayout'
import FormField from '../../components/ui/FormField'
import Button from '../../components/ui/Button'
import Alert from '../../components/ui/Alert'
import PasswordStrength from '../../components/ui/PasswordStrength'
import { authApi } from '../../api/authApi'
import { resetPasswordSchema } from '../../utils/validators'
import { ROUTES } from '../../constants/routes'

export default function ResetPassword() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')
  const [apiError, setApiError] = useState('')
  const [isExpiredToken, setIsExpiredToken] = useState(false)
  const [success, setSuccess] = useState(false)
  const [countdown, setCountdown] = useState(0)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  })

  const watchPassword = watch('newPassword')

  useEffect(() => {
    if (!success || countdown <= 0) return
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer)
          navigate(ROUTES.LOGIN, { replace: true })
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [success, countdown, navigate])

  if (!token) {
    return <Navigate to={ROUTES.FORGOT_PASSWORD} replace />
  }

  const onSubmit = async (values) => {
    setApiError('')
    setIsExpiredToken(false)
    try {
      await authApi.resetPassword({ token, ...values })
      setSuccess(true)
      setCountdown(3)
    } catch (err) {
      const msg = err.response?.data?.message || 'Đặt lại mật khẩu thất bại.'
      const code = err.response?.data?.code
      if (
        code === 'AUTH_009' ||
        code === 'AUTH_010' ||
        code === 'AUTH_012'
      ) {
        setIsExpiredToken(true)
      }
      setApiError(msg)
    }
  }

  return (
    <AuthLayout>
      <h2 className="mb-6 text-center text-xl font-semibold text-gray-900">
        Đặt lại mật khẩu
      </h2>

      {success ? (
        <div className="space-y-4">
          <Alert
            type="success"
            message={`Đặt lại mật khẩu thành công! Chuyển hướng đăng nhập sau ${countdown}s...`}
          />
          <div className="text-center">
            <Link
              to={ROUTES.LOGIN}
              className="inline-block rounded-lg bg-blue-600 px-6 py-2 text-sm font-medium text-white hover:bg-blue-700"
            >
              Đăng nhập ngay
            </Link>
          </div>
        </div>
      ) : (
        <>
          <Alert
            type="error"
            message={apiError}
            onClose={() => { setApiError(''); setIsExpiredToken(false) }}
          />

          {isExpiredToken && (
            <div className="mt-2 text-center">
              <Link
                to={ROUTES.FORGOT_PASSWORD}
                className="inline-block rounded-lg border border-blue-600 px-4 py-2 text-sm font-medium text-blue-600 hover:bg-blue-50"
              >
                Gửi lại email
              </Link>
            </div>
          )}

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
              Đặt lại mật khẩu
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  )
}

