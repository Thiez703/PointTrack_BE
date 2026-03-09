import Input from './Input'

export default function FormField({ label, error, register, ...props }) {
  return <Input label={label} error={error?.message} {...register} {...props} />
}

