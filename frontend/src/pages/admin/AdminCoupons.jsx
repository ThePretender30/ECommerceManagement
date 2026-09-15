import { useEffect, useState } from 'react'
import { EmptyState, ErrorState, Loader } from '../../components/Common'
import Modal, { ConfirmDialog } from '../../components/Modal'
import couponService from '../../services/couponService'
import { useToast } from '../../hooks'
import { formatCurrency, formatDate } from '../../utils/format'
import './AdminCoupons.css'

const INITIAL_FORM = {
  code: '',
  discountType: 'PERCENTAGE',
  discountValue: '',
  minOrderAmount: '',
  maxDiscountAmount: '',
  active: true,
  expiresAt: '',
}

export default function AdminCoupons() {
  const toast = useToast()
  const [coupons, setCoupons] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [formData, setFormData] = useState(INITIAL_FORM)
  const [formErrors, setFormErrors] = useState({})
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const loadCoupons = () => {
    setLoading(true)
    setError(null)
    couponService
      .listAll()
      .then(setCoupons)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadCoupons()
  }, [])

  const handleOpenModal = () => {
    setFormData(INITIAL_FORM)
    setFormErrors({})
    setModalOpen(true)
  }

  const handleCloseModal = () => {
    setModalOpen(false)
    setFormData(INITIAL_FORM)
    setFormErrors({})
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
    setFormErrors((prev) => ({ ...prev, [name]: undefined }))
  }

  const handleCreateCoupon = async (e) => {
    e.preventDefault()
    setFormErrors({})

    const errors = {}
    if (!formData.code.trim()) {
      errors.code = 'Coupon code is required'
    }
    if (!formData.discountValue || Number(formData.discountValue) <= 0) {
      errors.discountValue = 'Discount value must be greater than 0'
    } else if (formData.discountType === 'PERCENTAGE' && Number(formData.discountValue) > 100) {
      errors.discountValue = 'Percentage discount cannot exceed 100%'
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors)
      return
    }

    setSaving(true)
    try {
      const payload = {
        code: formData.code.trim().toUpperCase(),
        discountType: formData.discountType,
        discountValue: Number(formData.discountValue),
        minOrderAmount: formData.minOrderAmount ? Number(formData.minOrderAmount) : null,
        maxDiscountAmount: formData.maxDiscountAmount ? Number(formData.maxDiscountAmount) : null,
        active: formData.active,
        expiresAt: formData.expiresAt ? new Date(formData.expiresAt).toISOString() : null,
      }

      const created = await couponService.create(payload)
      toast.success(`Coupon ${created.code} created successfully!`)
      handleCloseModal()
      loadCoupons()
    } catch (err) {
      toast.error(err.message || 'Failed to create coupon')
      if (err.fieldErrors) setFormErrors(err.fieldErrors)
    } finally {
      setSaving(false)
    }
  }

  const handleToggle = async (coupon) => {
    try {
      const updated = await couponService.toggle(coupon.id)
      toast.success(`Coupon ${updated.code} is now ${updated.active ? 'Active' : 'Inactive'}.`)
      setCoupons((prev) => prev.map((c) => (c.id === updated.id ? updated : c)))
    } catch (err) {
      toast.error(err.message || 'Failed to toggle coupon status')
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await couponService.delete(deleteTarget.id)
      toast.success(`Coupon ${deleteTarget.code} deleted permanently.`)
      setDeleteTarget(null)
      loadCoupons()
    } catch (err) {
      toast.error(err.message || 'Failed to delete coupon')
      setDeleteTarget(null)
    } finally {
      setDeleting(false)
    }
  }

  const activeCount = coupons.filter((c) => c.active).length

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Coupons & Discounts</h1>
          <p className="admin-subtitle">
            {coupons.length} total coupons ({activeCount} active)
          </p>
        </div>
        <button type="button" className="btn btn-primary" onClick={handleOpenModal}>
          + Create Coupon
        </button>
      </div>

      {loading && <Loader label="Loading coupons…" />}
      {!loading && error && <ErrorState message={error} onRetry={loadCoupons} />}

      {!loading && !error && coupons.length === 0 && (
        <EmptyState
          icon="🏷️"
          title="No coupons configured"
          message="Create your first discount coupon code to reward customers."
          action={
            <button type="button" className="btn btn-primary" onClick={handleOpenModal}>
              + Create Coupon
            </button>
          }
        />
      )}

      {!loading && !error && coupons.length > 0 && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Code</th>
                <th>Type</th>
                <th>Discount Value</th>
                <th>Min Order</th>
                <th>Max Cap</th>
                <th>Expiry</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {coupons.map((coupon) => (
                <tr key={coupon.id}>
                  <td>
                    <div className="coupon-code-cell">
                      <span className="coupon-badge">🏷️ {coupon.code}</span>
                    </div>
                  </td>
                  <td>
                    <span className="badge badge-neutral">
                      {coupon.discountType === 'PERCENTAGE' ? 'Percentage (%)' : 'Flat Amount (₹)'}
                    </span>
                  </td>
                  <td>
                    <strong className="text-success">
                      {coupon.discountType === 'PERCENTAGE'
                        ? `${coupon.discountValue}%`
                        : formatCurrency(coupon.discountValue)}
                    </strong>
                  </td>
                  <td className="text-sm">
                    {coupon.minOrderAmount ? formatCurrency(coupon.minOrderAmount) : <span className="text-muted">—</span>}
                  </td>
                  <td className="text-sm">
                    {coupon.maxDiscountAmount ? formatCurrency(coupon.maxDiscountAmount) : <span className="text-muted">No cap</span>}
                  </td>
                  <td className="text-sm text-muted">
                    {coupon.expiresAt ? formatDate(coupon.expiresAt) : 'Never'}
                  </td>
                  <td>
                    <span
                      className={`badge ${
                        coupon.active && !coupon.expired
                          ? 'badge-success'
                          : coupon.expired
                          ? 'badge-warning'
                          : 'badge-neutral'
                      }`}
                    >
                      {coupon.expired ? 'Expired' : coupon.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="table-actions">
                      <button
                        type="button"
                        className="btn btn-xs btn-outline"
                        onClick={() => handleToggle(coupon)}
                        title={coupon.active ? 'Deactivate' : 'Activate'}
                      >
                        {coupon.active ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-xs btn-danger"
                        onClick={() => setDeleteTarget(coupon)}
                        title="Delete coupon"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Coupon Modal */}
      <Modal
        open={modalOpen}
        title="Create New Coupon"
        onClose={handleCloseModal}
        size="md"
        footer={
          <>
            <button
              type="button"
              className="btn btn-outline"
              onClick={handleCloseModal}
              disabled={saving}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleCreateCoupon}
              disabled={saving}
            >
              {saving ? 'Creating…' : 'Create Coupon'}
            </button>
          </>
        }
      >
        <form onSubmit={handleCreateCoupon}>
          <div className="form-group">
            <label className="form-label" htmlFor="code">
              Coupon Code *
            </label>
            <input
              id="code"
              name="code"
              type="text"
              required
              className={formErrors.code ? 'form-control has-error' : 'form-control'}
              placeholder="e.g. SUMMER30"
              value={formData.code}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, code: e.target.value.toUpperCase() }))
              }
            />
            {formErrors.code && <span className="form-error">{formErrors.code}</span>}
          </div>

          <div className="coupon-modal-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="discountType">
                Discount Type *
              </label>
              <select
                id="discountType"
                name="discountType"
                className="form-control"
                value={formData.discountType}
                onChange={handleInputChange}
              >
                <option value="PERCENTAGE">Percentage (%)</option>
                <option value="FLAT">Flat Amount (₹)</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="discountValue">
                Discount Value *
              </label>
              <input
                id="discountValue"
                name="discountValue"
                type="number"
                step="0.01"
                min="0.01"
                required
                className={formErrors.discountValue ? 'form-control has-error' : 'form-control'}
                placeholder={formData.discountType === 'PERCENTAGE' ? 'e.g. 15' : 'e.g. 100'}
                value={formData.discountValue}
                onChange={handleInputChange}
              />
              {formErrors.discountValue && (
                <span className="form-error">{formErrors.discountValue}</span>
              )}
            </div>
          </div>

          <div className="coupon-modal-grid">
            <div className="form-group">
              <label className="form-label" htmlFor="minOrderAmount">
                Minimum Order Amount (Optional)
              </label>
              <input
                id="minOrderAmount"
                name="minOrderAmount"
                type="number"
                step="0.01"
                min="0"
                className="form-control"
                placeholder="e.g. 500"
                value={formData.minOrderAmount}
                onChange={handleInputChange}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="maxDiscountAmount">
                Maximum Discount Cap (Optional)
              </label>
              <input
                id="maxDiscountAmount"
                name="maxDiscountAmount"
                type="number"
                step="0.01"
                min="0"
                className="form-control"
                placeholder="e.g. 200"
                value={formData.maxDiscountAmount}
                onChange={handleInputChange}
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="expiresAt">
              Expiration Date & Time (Optional)
            </label>
            <input
              id="expiresAt"
              name="expiresAt"
              type="datetime-local"
              className="form-control"
              value={formData.expiresAt}
              onChange={handleInputChange}
            />
          </div>

          <div className="form-group">
            <label className="filter-checkbox">
              <input
                type="checkbox"
                name="active"
                checked={formData.active}
                onChange={handleInputChange}
              />
              <span>Activate immediately upon creation</span>
            </label>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete Coupon"
        message={`Are you sure you want to permanently delete coupon "${deleteTarget?.code}"? Customers will no longer be able to use this code.`}
        confirmLabel="Delete"
        danger
        busy={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
