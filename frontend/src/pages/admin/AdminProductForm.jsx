import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorState, Loader } from '../../components/Common'
import adminService from '../../services/adminService'
import { useToast } from '../../hooks'
import { handleImageError, FALLBACK_IMAGE } from '../../utils/format'

const EMPTY_FORM = {
  name: '',
  description: '',
  price: '',
  categoryId: '',
  brand: '',
  imageUrl: '',
  stock: '0',
  active: true,
}

/**
 * Create/edit form for a product. One component serves both because the fields
 * are identical - the presence of an `:id` route param decides the mode.
 *
 * Rating and review count are deliberately absent: they are derived from real
 * customer reviews and are not something an admin can set.
 */
export default function AdminProductForm() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const toast = useToast()

  const [form, setForm] = useState(EMPTY_FORM)
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(isEdit)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    adminService
      .listCategories()
      .then((data) => {
        setCategories(data)
        // Default the category selector so creating a product is one less click.
        if (!isEdit && data.length > 0) {
          setForm((current) => ({ ...current, categoryId: String(data[0].id) }))
        }
      })
      .catch(() => setCategories([]))
  }, [isEdit])

  useEffect(() => {
    if (!isEdit) return

    adminService
      .getProduct(id)
      .then((product) =>
        setForm({
          name: product.name,
          description: product.description ?? '',
          price: String(product.price),
          categoryId: String(product.categoryId),
          brand: product.brand ?? '',
          imageUrl: product.imageUrl ?? '',
          stock: String(product.stock),
          active: product.active,
        })
      )
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFieldErrors({})
    setSaving(true)

    const payload = {
      name: form.name,
      description: form.description || null,
      price: Number(form.price),
      categoryId: Number(form.categoryId),
      brand: form.brand || null,
      imageUrl: form.imageUrl || null,
      stock: Number(form.stock),
      active: form.active,
    }

    try {
      if (isEdit) {
        await adminService.updateProduct(id, payload)
        toast.success('Product updated.')
      } else {
        await adminService.createProduct(payload)
        toast.success('Product created.')
      }
      navigate('/admin/products')
    } catch (err) {
      toast.error(err.message)
      if (err.fieldErrors) setFieldErrors(err.fieldErrors)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Loader fullPage label="Loading product…" />
  if (error) return <ErrorState message={error} />

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">{isEdit ? 'Edit product' : 'Add a product'}</h1>
          <p className="admin-subtitle">
            {isEdit ? 'Update the details of this product.' : 'Add a new product to the catalogue.'}
          </p>
        </div>
        <Link to="/admin/products" className="btn btn-outline">← Back to products</Link>
      </div>

      <form onSubmit={handleSubmit} noValidate>
        <div className="admin-grid split">
          <section className="card">
            <div className="card-header">Product details</div>
            <div className="card-body">
              <div className="form-group">
                <label className="form-label" htmlFor="name">Product name</label>
                <input
                  id="name" name="name" type="text" required
                  className={fieldErrors.name ? 'form-control has-error' : 'form-control'}
                  value={form.name} onChange={handleChange}
                  placeholder="e.g. Stainless Steel Pressure Cooker 5L"
                />
                {fieldErrors.name && <span className="form-error">{fieldErrors.name}</span>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="description">Description</label>
                <textarea
                  id="description" name="description" rows="5"
                  className={fieldErrors.description ? 'form-control has-error' : 'form-control'}
                  value={form.description} onChange={handleChange}
                  placeholder="What is it, what is it made of, who is it for?"
                />
                {fieldErrors.description && (
                  <span className="form-error">{fieldErrors.description}</span>
                )}
              </div>

              <div className="form-row cols-2">
                <div className="form-group">
                  <label className="form-label" htmlFor="categoryId">Category</label>
                  <select
                    id="categoryId" name="categoryId" required
                    className={fieldErrors.categoryId ? 'form-control has-error' : 'form-control'}
                    value={form.categoryId} onChange={handleChange}
                  >
                    <option value="">Select a category</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>{category.name}</option>
                    ))}
                  </select>
                  {fieldErrors.categoryId && (
                    <span className="form-error">{fieldErrors.categoryId}</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="brand">Brand (optional)</label>
                  <input
                    id="brand" name="brand" type="text" className="form-control"
                    value={form.brand} onChange={handleChange}
                    placeholder="e.g. ChefLine"
                  />
                </div>
              </div>

              <div className="form-row cols-2">
                <div className="form-group">
                  <label className="form-label" htmlFor="price">Price (₹)</label>
                  <input
                    id="price" name="price" type="number" step="0.01" min="0.01" required
                    className={fieldErrors.price ? 'form-control has-error' : 'form-control'}
                    value={form.price} onChange={handleChange}
                    placeholder="0.00"
                  />
                  {fieldErrors.price && <span className="form-error">{fieldErrors.price}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="stock">Stock</label>
                  <input
                    id="stock" name="stock" type="number" min="0" required
                    className={fieldErrors.stock ? 'form-control has-error' : 'form-control'}
                    value={form.stock} onChange={handleChange}
                  />
                  {fieldErrors.stock ? (
                    <span className="form-error">{fieldErrors.stock}</span>
                  ) : (
                    <span className="form-hint">Products with 5 or fewer appear in the low-stock list.</span>
                  )}
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="imageUrl">Image URL</label>
                <input
                  id="imageUrl" name="imageUrl" type="url" className="form-control"
                  value={form.imageUrl} onChange={handleChange}
                  placeholder="https://…"
                />
                <span className="form-hint">Paste a direct link to a product photograph.</span>
              </div>

              <label className="filter-checkbox">
                <input
                  type="checkbox" name="active"
                  checked={form.active} onChange={handleChange}
                />
                <span>Visible in the store</span>
              </label>
            </div>
          </section>

          {/* Live preview so an admin can see the image before saving. */}
          <aside className="card">
            <div className="card-header">Preview</div>
            <div className="card-body">
              <div className="product-card" style={{ maxWidth: 260, margin: '0 auto' }}>
                <div className="product-card-image">
                  <img
                    src={form.imageUrl || FALLBACK_IMAGE}
                    alt={form.name || 'Product preview'}
                    onError={handleImageError}
                  />
                </div>
                <div className="product-card-body">
                  <span className="product-card-category">
                    {categories.find((c) => String(c.id) === form.categoryId)?.name ?? 'Category'}
                  </span>
                  <h3 className="product-card-name">{form.name || 'Product name'}</h3>
                  {form.brand && <p className="product-card-brand">{form.brand}</p>}
                  <div className="product-card-footer">
                    <span className="product-card-price">
                      ₹{form.price ? Number(form.price).toFixed(2) : '0.00'}
                    </span>
                  </div>
                </div>
              </div>

              <div className="stack mt-6">
                <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                  {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create product'}
                </button>
                <Link to="/admin/products" className="btn btn-outline btn-block">Cancel</Link>
              </div>
            </div>
          </aside>
        </div>
      </form>
    </div>
  )
}
