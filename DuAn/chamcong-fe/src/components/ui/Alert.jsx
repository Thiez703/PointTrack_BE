const styles = {
  success: 'bg-green-50 border-green-400 text-green-800',
  error: 'bg-red-50 border-red-400 text-red-800',
  warning: 'bg-yellow-50 border-yellow-400 text-yellow-800',
}

export default function Alert({ type = 'error', message, onClose }) {
  if (!message) return null

  return (
    <div
      role="alert"
      className={`flex items-center justify-between rounded-lg border
        px-4 py-3 text-sm ${styles[type]}`}
    >
      <span>{message}</span>
      {onClose && (
        <button
          onClick={onClose}
          className="ml-3 text-lg font-bold leading-none hover:opacity-70"
        >
          &times;
        </button>
      )}
    </div>
  )
}

