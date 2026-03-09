import { useMemo } from 'react'

function calcStrength(password) {
  if (!password) return { score: 0, label: '', color: 'bg-gray-200', textColor: 'text-gray-400' }
  let score = 0
  if (password.length >= 8) score++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++
  if (/\d/.test(password)) score++
  if (/[^a-zA-Z0-9]/.test(password)) score++

  const levels = [
    { label: 'Yếu', color: 'bg-red-500', textColor: 'text-red-600' },
    { label: 'Trung bình', color: 'bg-yellow-500', textColor: 'text-yellow-600' },
    { label: 'Khá', color: 'bg-blue-500', textColor: 'text-blue-600' },
    { label: 'Mạnh', color: 'bg-green-500', textColor: 'text-green-600' },
  ]
  const idx = Math.max(0, Math.min(score, levels.length) - 1)
  return { score, ...levels[idx] }
}

export default function PasswordStrength({ password }) {
  const strength = useMemo(() => calcStrength(password), [password])

  if (!password) return null

  return (
    <div className="mt-2">
      <div className="flex gap-1">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className={`h-1.5 flex-1 rounded-full transition-colors ${
              i <= strength.score ? strength.color : 'bg-gray-200'
            }`}
          />
        ))}
      </div>
      <p className={`mt-1 text-xs ${strength.textColor}`}>{strength.label}</p>
    </div>
  )
}

