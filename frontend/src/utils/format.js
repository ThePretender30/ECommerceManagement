const currencyFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
})

export const formatCurrency = (value) => {
  const number = Number(value ?? 0)
  return Number.isFinite(number) ? currencyFormatter.format(number) : '—'
}

export const formatDate = (isoString) => {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

export const formatDateTime = (isoString) => {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export const statusBadgeClass = (status) => {
  switch (status) {
    case 'DELIVERED':
      return 'badge-success'
    case 'CANCELLED':
      return 'badge-danger'
    case 'DISPATCHED':
    case 'OUT_FOR_DELIVERY':
      return 'badge-info'
    case 'PROCESSING':
    case 'ORDER_CONFIRMED':
      return 'badge-primary'
    default:
      return 'badge-warning'
  }
}

export const FALLBACK_IMAGE =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400">
       <rect width="400" height="400" fill="#f1f5f9"/>
       <text x="50%" y="50%" font-family="system-ui, sans-serif" font-size="18"
             fill="#94a3b8" text-anchor="middle" dominant-baseline="middle">No image</text>
     </svg>`
  )

export const handleImageError = (event) => {
  if (event.target.src !== FALLBACK_IMAGE) {
    event.target.src = FALLBACK_IMAGE
  }
}
