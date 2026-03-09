import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import AuthLayout from '../../components/layout/AuthLayout'
import FormField from '../../components/ui/FormField'
import CountdownButton from '../../components/ui/CountdownButton'
import Alert from '../../components/ui/Alert'
import { authApi } from '../../api/authApi'
import { forgotPasswordSchema } from '../../utils/validators'
import { ROUTES } from '../../constants/routes'

export default function ForgotPassword() {
  const [apiError, setApiError] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  const onSubmit = async (values) => {
    setApiError('')
    try {
      await authApi.forgotPassword(values)
      setSubmitted(true)
    } catch (err) {
      setApiError(
        err.response?.data?.message ||
          'Gửi yêu cầu thất bại. Vui lòng thử lại.'
      )
    }
  }

  return (
    <AuthLayout>
      <h2 className="mb-2 text-center text-xl font-semibold text-gray-900">
        Quên mật khẩu
      </h2>
      <p className="mb-6 text-center text-sm text-gray-500">
        Nhập email để nhận link đặt lại mật khẩu
      </p>

      {submitted && (
        <Alert
          type="success"
          message="Nếu email tồn tại, chúng tôi đã gửi link đặt lại mật khẩu. Vui lòng kiểm tra hộp thư."
        />
      )}

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

        <CountdownButton
          type="submit"
          loading={isSubmitting}
          startCountdown={submitted}
          countdownSeconds={60}
          className="w-full"
          size="lg"
        >
          Gửi email reset
        </CountdownButton>
      </form>

      <div className="mt-4 text-center">
        <Link
          to={ROUTES.LOGIN}
          className="text-sm text-blue-600 hover:underline"
        >
          Quay lại đăng nhập
        </Link>
      </div>
    </AuthLayout>
  )
}

