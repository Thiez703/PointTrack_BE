const sizeMap = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm', lg: 'h-16 w-16 text-lg' }

export default function Avatar({ src, name = '', size = 'md', className = '' }) {
  const initials = name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .slice(0, 2)
    .toUpperCase()

  if (src) {
    return (
      <img
        src={src}
        alt={name}
        className={`rounded-full object-cover ${sizeMap[size]} ${className}`}
      />
    )
  }

  return (
    <div
      className={`flex items-center justify-center rounded-full bg-blue-600
        text-white font-medium ${sizeMap[size]} ${className}`}
    >
      {initials || '?'}
    </div>
  )
}

