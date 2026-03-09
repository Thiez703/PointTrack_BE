import { z } from 'zod'

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'Email không được để trống')
    .email('Email không hợp lệ'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})

export const passwordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .regex(
        /^(?=.*[a-zA-Z])(?=.*\d).+$/,
        'Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số'
      ),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'Email không được để trống')
    .email('Email không hợp lệ'),
})

export const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .regex(
        /^(?=.*[a-zA-Z])(?=.*\d).+$/,
        'Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số'
      ),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

export const profileSchema = z.object({
  fullName: z.string().min(1, 'Họ tên không được để trống').max(255),
  phone: z
    .string()
    .regex(
      /^(\+84|84|0)(3[2-9]|5[2689]|7[06-9]|8[1-9]|9[0-46-9])\d{7}$/,
      'Số điện thoại không hợp lệ'
    )
    .or(z.literal(''))
    .optional(),
  avatarUrl: z.string().url('URL không hợp lệ').or(z.literal('')).optional(),
})

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Vui lòng nhập mật khẩu hiện tại'),
    newPassword: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .regex(
        /^(?=.*[a-zA-Z])(?=.*\d).+$/,
        'Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số'
      ),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

