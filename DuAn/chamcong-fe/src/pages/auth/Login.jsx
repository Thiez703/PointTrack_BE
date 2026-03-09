import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import AuthLayout from '../../components/layout/AuthLayout'
import FormField from '../../components/ui/FormField'
import Button from '../../components/ui/Button'
import Alert from '../../components/ui/Alert'
import { useAuth } from '../../hooks/useAuth'
import { loginSchema } from '../../utils/validators'
import { ROUTES } from '../../constants/routes'

export default function Login() {
  const [apiError, setApiError] = useState('')
  const { login } = useAuth()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const onSubmit = async (values) => {
    setApiError('')
    try {
      await login(values)
    } catch (err) {
      const msg =
        err.response?.data?.message || 'Đăng nhập thất bại. Vui lòng thử lại.'
      setApiError(msg)
    }
  }

  return (
    <AuthLayout>
      <h2 className="mb-6 text-center text-xl font-semibold text-gray-900">
        Đăng nhập
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
        <FormField
          label="Email"
          type="email"
          placeholder="email@example.com"
          autoComplete="email"
          error={errors.email}
          register={register('email')}
          disabled={isSubmitting}
        />

        <FormField
          label="Mật khẩu"
          type="password"
          placeholder="••••••••"
          autoComplete="current-password"
          error={errors.password}
          register={register('password')}
          disabled={isSubmitting}
        />

        <Button
          type="submit"
          loading={isSubmitting}
          disabled={isSubmitting}
          className="w-full"
          size="lg"
        >
          Đăng nhập
        </Button>
      </form>

      <div className="mt-4 text-center">
        <Link
          to={ROUTES.FORGOT_PASSWORD}
          className="text-sm text-blue-600 hover:text-blue-800 hover:underline"
        >
          Quên mật khẩu?
        </Link>
      </div>
    </AuthLayout>
  )
}

