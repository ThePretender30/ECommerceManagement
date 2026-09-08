import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState, ErrorState, Loader, Pagination } from '../../components/Common'
import { PackageIcon } from '../../components/Icons'
import { ConfirmDialog } from '../../components/Modal'
import adminService from '../../services/adminService'
import { useDebounce, useToast } from '../../hooks'
import { formatCurrency, handleImageError, FALLBACK_IMAGE } from '../../utils/format'

export default function AdminProducts() {
  const toast = useToast()

  const [result, setResult] = useState(null)
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const debouncedSearch = useDebounce(search, 400)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [stockEdits, setStockEdits] = useState({})

  const load = () => {
    setLoading(true)
    setError(null)
    adminService
      .listProducts({
        q: debouncedSearch || undefined,
        categoryId: categoryId || undefined,
        page,
        size: 20,
      })
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    adminService.listCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  useEffect(load, [debouncedSearch, categoryId, page])
  useEffect(() => setPage(0), [debouncedSearch, categoryId])

  const handleStockSave = async (product) => {
    const nextStock = Number(stockEdits[product.id])
    if (!Number.isFinite(nextStock) || nextStock < 0) {
      toast.error('Stock must be zero or a positive number.')
      return
    }

    try {
      await adminService.updateStock(product.id, nextStock)
      toast.success(`Stock for "${product.name}" set to ${nextStock}.`)
      setStockEdits((current) => ({ ...current, [product.id]: undefined }))
      load()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      const response = await adminService.deleteProduct(deleteTarget.id)
      toast.success(response.message)
      setDeleteTarget(null)
      load()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Products</h1>
          <p className="admin-subtitle">
            {result ? `${result.totalElements} products in the catalogue` : 'Loading…'}
          </p>
        </div>
        <Link to="/admin/products/new" className="btn btn-primary">+ Add product</Link>
      </div>

      <div className="admin-toolbar">
        <input
          type="search"
          className="form-control"
          placeholder="Search by name, description or brand…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select
          className="form-control"
          value={categoryId}
          onChange={(event) => setCategoryId(event.target.value)}
        >
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>{category.name}</option>
          ))}
        </select>
      </div>

      {loading && <Loader label="Loading products…" />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && result?.empty && (
        <EmptyState
          icon={<PackageIcon size={48} />}
          title="No products found"
          message="Try a different search, or add your first product."
          action={<Link to="/admin/products/new" className="btn btn-primary">Add product</Link>}
        />
      )}

      {!loading && !error && result && !result.empty && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Category</th>
                  <th>Brand</th>
                  <th className="text-right">Price</th>
                  <th>Stock</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((product) => {
                  const editedStock = stockEdits[product.id]
                  const isEditing = editedStock !== undefined

                  return (
                    <tr key={product.id}>
                      <td>
                        <div className="table-product">
                          <img
                            src={product.imageUrl || FALLBACK_IMAGE}
                            alt={product.name}
                            onError={handleImageError}
                          />
                          <div>
                            <div className="table-product-name">{product.name}</div>
                            <div className="text-xs text-subtle">#{product.id}</div>
                          </div>
                        </div>
                      </td>
                      <td>{product.categoryName ?? '—'}</td>
                      <td>{product.brand ?? '—'}</td>
                      <td className="text-right font-semibold">{formatCurrency(product.price)}</td>
                      <td>
                        {isEditing ? (
                          <div className="row">
                            <input
                              type="number"
                              min="0"
                              className="form-control"
                              style={{ width: 80, padding: '4px 8px' }}
                              value={editedStock}
                              onChange={(event) =>
                                setStockEdits((current) => ({
                                  ...current,
                                  [product.id]: event.target.value,
                                }))
                              }
                            />
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => handleStockSave(product)}
                            >
                              Save
                            </button>
                            <button
                              type="button"
                              className="btn btn-ghost btn-sm"
                              onClick={() =>
                                setStockEdits((current) => ({ ...current, [product.id]: undefined }))
                              }
                            >
                              ×
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className={
                              product.stock === 0 ? 'stock-pill is-none'
                              : product.stock <= 5 ? 'stock-pill is-low'
                              : 'stock-pill is-ok'
                            }
                            onClick={() =>
                              setStockEdits((current) => ({
                                ...current,
                                [product.id]: String(product.stock),
                              }))
                            }
                            title="Click to edit stock"
                          >
                            {product.stock}
                          </button>
                        )}
                      </td>
                      <td>
                        <span className={product.active ? 'badge badge-success' : 'badge badge-neutral'}>
                          {product.active ? 'Active' : 'Hidden'}
                        </span>
                      </td>
                      <td>
                        <div className="table-actions">
                          <Link
                            to={`/admin/products/${product.id}/edit`}
                            className="btn btn-outline btn-sm"
                          >
                            Edit
                          </Link>
                          <button
                            type="button"
                            className="btn btn-ghost btn-sm"
                            style={{ color: 'var(--color-danger)' }}
                            onClick={() => setDeleteTarget(product)}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <Pagination page={result.page} totalPages={result.totalPages} onPageChange={setPage} />
        </>
      )}

      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={`Delete "${deleteTarget?.name}"?`}
        message="If this product appears in existing orders it will be deactivated and hidden from the store instead of deleted, so past invoices stay intact."
        confirmLabel="Delete product"
        danger
        busy={deleting}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  )
}
