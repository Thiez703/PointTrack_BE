import useToastStore from '../../store/toastStore'

const styles = {
  success: 'bg-green-600',
  error: 'bg-red-600',
  warning: 'bg-yellow-500',
}

export default function Toast() {
  const { toasts, removeToast } = useToastStore()

  if (toasts.length === 0) return null

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm text-white shadow-lg
            animate-[slideIn_0.3s_ease-out] ${styles[toast.type]}`}
        >
          <span className="flex-1">{toast.message}</span>
          <button
            onClick={() => removeToast(toast.id)}
            className="text-white/80 hover:text-white font-bold"
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  )
}

