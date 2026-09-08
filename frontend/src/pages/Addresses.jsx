import { useEffect, useState } from 'react'
import { EmptyState, ErrorState, Loader } from '../components/Common'
import { MapPinIcon } from '../components/Icons'
import Modal, { ConfirmDialog } from '../components/Modal'
import { addressService } from '../services/catalogService'
import { useToast } from '../hooks'
import './Addresses.css'

const EMPTY_FORM = {
  fullName: '',
  phone: '',
  line1: '',
  line2: '',
  city: '',
  state: '',
  postalCode: '',
  country: 'India',
  isDefault: false,
}

export default function Addresses() {
  const toast = useToast()

  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)

  const load = () => {
    setLoading(true)
    setError(null)
    addressService
      .list()
      .then(setAddresses)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setFieldErrors({})
    setModalOpen(true)
  }

  const openEdit = (address) => {
    setEditing(address)
    setForm({
      fullName: address.fullName,
      phone: address.phone,
      line1: address.line1,
      line2: address.line2 ?? '',
      city: address.city,
      state: address.state,
      postalCode: address.postalCode,
      country: address.country,
      isDefault: address.isDefault,
    })
    setFieldErrors({})
    setModalOpen(true)
  }

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFieldErrors({})
    setSaving(true)

    try {
      if (editing) {
        await addressService.update(editing.id, form)
        toast.success('Address updated.')
      } else {
        await addressService.create(form)
        toast.success('Address saved.')
      }
      setModalOpen(false)
      load()
    } catch (err) {
      toast.error(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
    } finally {
      setSaving(false)
    }
  }

  const handleSetDefault = async (address) => {
    try {
      await addressService.setDefault(address.id)
      toast.success(`${address.fullName}'s address is now your default.`)
      load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const handleDelete = async () => {
    try {
      await addressService.remove(deleteTarget.id)
      toast.info('Address removed.')
      setDeleteTarget(null)
      load()
    } catch (err) {
      toast.error(err.message)
      setDeleteTarget(null)
    }
  }

  if (loading) return <Loader fullPage label="Loading your addresses…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  return (
    <div className="page container">
      <div className="page-header row-between wrap">
        <div>
          <h1 className="page-title">Delivery addresses</h1>
          <p className="page-subtitle">Manage where your orders are sent.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          + Add address
        </button>
      </div>

      {addresses.length === 0 ? (
        <EmptyState
          icon={<MapPinIcon size={48} />}
          title="No saved addresses yet"
          message="Save an address to make checkout faster next time."
          action={
            <button type="button" className="btn btn-primary btn-lg" onClick={openCreate}>
              Add your first address
            </button>
          }
        />
      ) : (
        <div className="address-grid">
          {addresses.map((address) => (
            <article
              key={address.id}
              className={address.isDefault ? 'address-card is-default' : 'address-card'}
            >
              <div className="address-card-header">
                <h2 className="address-card-name">{address.fullName}</h2>
                {address.isDefault && <span className="badge badge-primary">Default</span>}
              </div>

              <p className="address-card-text">{address.formatted}</p>
              <p className="address-card-phone">{address.phone}</p>

              <div className="address-card-actions">
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={() => openEdit(address)}
                >
                  Edit
                </button>
                {!address.isDefault && (
                  <button
                    type="button"
                    className="btn btn-ghost btn-sm"
                    onClick={() => handleSetDefault(address)}
                  >
                    Set as default
                  </button>
                )}
                <button
                  type="button"
                  className="btn btn-ghost btn-sm address-delete"
                  onClick={() => setDeleteTarget(address)}
                >
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      <Modal
        open={modalOpen}
        title={editing ? 'Edit address' : 'Add a new address'}
        onClose={() => setModalOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button
              type="submit"
              form="address-form"
              className="btn btn-primary"
              disabled={saving}
            >
              {saving ? 'Saving…' : editing ? 'Save changes' : 'Add address'}
            </button>
          </>
        }
      >
        <form id="address-form" onSubmit={handleSubmit} noValidate>
          <div className="form-row cols-2">
            <div className="form-group">
              <label className="form-label" htmlFor="a-fullName">Recipient name</label>
              <input
                id="a-fullName" name="fullName" type="text" required
                className={fieldErrors.fullName ? 'form-control has-error' : 'form-control'}
                value={form.fullName} onChange={handleChange}
              />
              {fieldErrors.fullName && <span className="form-error">{fieldErrors.fullName}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="a-phone">Contact phone</label>
              <input
                id="a-phone" name="phone" type="tel" required
                className={fieldErrors.phone ? 'form-control has-error' : 'form-control'}
                value={form.phone} onChange={handleChange}
                placeholder="+919876543210"
              />
              {fieldErrors.phone ? (
                <span className="form-error">{fieldErrors.phone}</span>
              ) : (
                <span className="form-hint">Include the country code.</span>
              )}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="a-line1">Address line 1</label>
            <input
              id="a-line1" name="line1" type="text" required
              className={fieldErrors.line1 ? 'form-control has-error' : 'form-control'}
              value={form.line1} onChange={handleChange}
              placeholder="Flat / house number, building, street"
            />
            {fieldErrors.line1 && <span className="form-error">{fieldErrors.line1}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="a-line2">Address line 2 (optional)</label>
            <input
              id="a-line2" name="line2" type="text" className="form-control"
              value={form.line2} onChange={handleChange}
              placeholder="Area, landmark"
            />
          </div>

          <div className="form-row cols-3">
            <div className="form-group">
              <label className="form-label" htmlFor="a-city">City</label>
              <input
                id="a-city" name="city" type="text" required
                className={fieldErrors.city ? 'form-control has-error' : 'form-control'}
                value={form.city} onChange={handleChange}
              />
              {fieldErrors.city && <span className="form-error">{fieldErrors.city}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="a-state">State</label>
              <input
                id="a-state" name="state" type="text" required
                className={fieldErrors.state ? 'form-control has-error' : 'form-control'}
                value={form.state} onChange={handleChange}
              />
              {fieldErrors.state && <span className="form-error">{fieldErrors.state}</span>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="a-postalCode">Postal code</label>
              <input
                id="a-postalCode" name="postalCode" type="text" required
                className={fieldErrors.postalCode ? 'form-control has-error' : 'form-control'}
                value={form.postalCode} onChange={handleChange}
              />
              {fieldErrors.postalCode && <span className="form-error">{fieldErrors.postalCode}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="a-country">Country</label>
            <input
              id="a-country" name="country" type="text" className="form-control"
              value={form.country} onChange={handleChange}
            />
          </div>

          <label className="filter-checkbox">
            <input
              type="checkbox" name="isDefault"
              checked={form.isDefault} onChange={handleChange}
            />
            <span>Use this as my default delivery address</span>
          </label>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete this address?"
        message="Past orders keep their own copy of the delivery address, so your order history is unaffected."
        confirmLabel="Delete address"
        danger
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
