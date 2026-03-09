import { useState, useEffect } from 'react'
import Button from './Button'

export default function CountdownButton({
  children,
  countdownSeconds = 60,
  onCountdownEnd,
  startCountdown = false,
  ...props
}) {
  const [remaining, setRemaining] = useState(0)

  useEffect(() => {
    if (startCountdown && remaining === 0) {
      setRemaining(countdownSeconds)
    }
  }, [startCountdown, countdownSeconds])

  useEffect(() => {
    if (remaining <= 0) return
    const timer = setTimeout(() => {
      setRemaining((prev) => prev - 1)
    }, 1000)
    return () => clearTimeout(timer)
  }, [remaining])

  useEffect(() => {
    if (remaining === 0 && startCountdown) {
      onCountdownEnd?.()
    }
  }, [remaining])

  const isCountingDown = remaining > 0

  return (
    <Button disabled={isCountingDown} {...props}>
      {isCountingDown ? `${children} (${remaining}s)` : children}
    </Button>
  )
}

