import { useEffect, useState } from 'react'
import { ErrorState, Loader } from '../../components/Common'
import Modal, { ConfirmDialog } from '../../components/Modal'
import adminService from '../../services/adminService'
import { useToast } from '../../hooks'
import { handleImageError, FALLBACK_IMAGE } from '../../utils/format'

const EMPTY_FORM = { name: '', slug: '', description: '', imageUrl: '' }

export default function AdminCategories() {
  const toast = useToast()

  const [categories, setCategories] = useState([])
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
    adminService
      .listCategories()
      .then(setCategories)
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

  const openEdit = (category) => {
    setEditing(category)
    setForm({
      name: category.name,
      slug: category.slug,
      description: category.description ?? '',
      imageUrl: category.imageUrl ?? '',
    })
    setFieldErrors({})
    setModalOpen(true)
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFieldErrors({})
    setSaving(true)

    try {
      if (editing) {
        await adminService.updateCategory(editing.id, form)
        toast.success('Category updated.')
      } else {
        await adminService.createCategory(form)
        toast.success('Category created.')
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

  const handleDelete = async () => {
    try {
      await adminService.deleteCategory(deleteTarget.id)
      toast.success('Category deleted.')
      setDeleteTarget(null)
      load()
    } catch (err) {
      toast.error(err.message)
      setDeleteTarget(null)
    }
  }

  if (loading) return <Loader fullPage label="Loading categories…" />
  if (error) return <ErrorState message={error} onRetry={load} />

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Categories</h1>
          <p className="admin-subtitle">{categories.length} product sections.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          + Add category
        </button>
      </div>

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Category</th>
              <th>URL slug</th>
              <th>Description</th>
              <th className="text-right">Products</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {categories.map((category) => (
              <tr key={category.id}>
                <td>
                  <div className="table-product">
                    <img
                      src={category.imageUrl || FALLBACK_IMAGE}
                      alt={category.name}
                      onError={handleImageError}
                    />
                    <span className="table-product-name">{category.name}</span>
                  </div>
                </td>
                <td><code className="text-xs">{category.slug}</code></td>
                <td className="text-sm text-muted" style={{ maxWidth: 320 }}>
                  {category.description ?? '—'}
                </td>
                <td className="text-right font-semibold">{category.productCount ?? 0}</td>
                <td>
                  <div className="table-actions">
                    <button
                      type="button"
                      className="btn btn-outline btn-sm"
                      onClick={() => openEdit(category)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      style={{ color: 'var(--color-danger)' }}
                      onClick={() => setDeleteTarget(category)}
                      disabled={category.productCount > 0}
                      title={
                        category.productCount > 0
                          ? 'Move or delete this category’s products first'
                          : 'Delete this category'
                      }
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

      <Modal
        open={modalOpen}
        title={editing ? 'Edit category' : 'Add a category'}
        onClose={() => setModalOpen(false)}
        footer={
          <>
            <button type="button" className="btn btn-outline" onClick={() => setModalOpen(false)}>
              Cancel
            </button>
            <button type="submit" form="category-form" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : editing ? 'Save changes' : 'Create category'}
            </button>
          </>
        }
      >
        <form id="category-form" onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="c-name">Category name</label>
            <input
              id="c-name" name="name" type="text" required
              className={fieldErrors.name ? 'form-control has-error' : 'form-control'}
              value={form.name} onChange={handleChange}
              placeholder="e.g. Sports Equipment"
            />
            {fieldErrors.name && <span className="form-error">{fieldErrors.name}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="c-slug">URL slug (optional)</label>
            <input
              id="c-slug" name="slug" type="text"
              className={fieldErrors.slug ? 'form-control has-error' : 'form-control'}
              value={form.slug} onChange={handleChange}
              placeholder="sports-equipment"
            />
            {fieldErrors.slug ? (
              <span className="form-error">{fieldErrors.slug}</span>
            ) : (
              <span className="form-hint">
                Leave blank to generate it from the name. Lowercase letters, numbers and hyphens only.
              </span>
            )}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="c-description">Description</label>
            <textarea
              id="c-description" name="description" rows="3"
              className={fieldErrors.description ? 'form-control has-error' : 'form-control'}
              value={form.description} onChange={handleChange}
              placeholder="A short line shown on the categories page."
            />
            {fieldErrors.description && <span className="form-error">{fieldErrors.description}</span>}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="c-imageUrl">Image URL</label>
            <input
              id="c-imageUrl" name="imageUrl" type="url" className="form-control"
              value={form.imageUrl} onChange={handleChange}
              placeholder="https://…"
            />
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={`Delete "${deleteTarget?.name}"?`}
        message="This permanently removes the category. It is only possible because no products belong to it."
        confirmLabel="Delete category"
        danger
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
