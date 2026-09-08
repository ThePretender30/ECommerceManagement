import { useEffect, useState } from 'react'
import ProductCard from '../components/ProductCard'
import { Breadcrumbs, EmptyState, ErrorState, Loader, Pagination } from '../components/Common'
import { SearchIcon } from '../components/Icons'
import productService from '../services/productService'
import { categoryService } from '../services/catalogService'
import { useDebounce, useQueryParams } from '../hooks'
import './ProductList.css'

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest first' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'rating', label: 'Highest rated' },
  { value: 'popular', label: 'Most reviewed' },
  { value: 'name_asc', label: 'Name: A to Z' },
]

const PAGE_SIZE = 12

export default function ProductList() {
  const { get, setParams, clearAll } = useQueryParams()

  const [result, setResult] = useState(null)
  const [categories, setCategories] = useState([])
  const [brands, setBrands] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [filtersOpen, setFiltersOpen] = useState(false)

  const [searchInput, setSearchInput] = useState(get('q'))
  const debouncedSearch = useDebounce(searchInput, 450)

  const categorySlug = get('category')
  const sort = get('sort', 'newest')
  const brand = get('brand')
  const minPrice = get('minPrice')
  const maxPrice = get('maxPrice')
  const minRating = get('minRating')
  const inStock = get('inStock')
  const page = Number(get('page', '0'))
  const query = get('q')

  useEffect(() => {
    if (debouncedSearch !== query) {
      setParams({ q: debouncedSearch })
    }
  }, [debouncedSearch, query, setParams])

  useEffect(() => {
    setSearchInput(query)
  }, [query])

  useEffect(() => {
    categoryService.list().then(setCategories).catch(() => setCategories([]))
  }, [])

  useEffect(() => {
    const category = categories.find((c) => c.slug === categorySlug)
    productService
      .brands(category?.id)
      .then(setBrands)
      .catch(() => setBrands([]))
  }, [categorySlug, categories])

  useEffect(() => {
    setLoading(true)
    setError(null)

    productService
      .list({
        q: query,
        category: categorySlug,
        brand,
        minPrice,
        maxPrice,
        minRating,
        inStock: inStock === 'true' ? true : undefined,
        sort,
        page,
        size: PAGE_SIZE,
      })
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [query, categorySlug, brand, minPrice, maxPrice, minRating, inStock, sort, page])

  const activeCategory = categories.find((c) => c.slug === categorySlug)
  const hasActiveFilters = Boolean(
    brand || minPrice || maxPrice || minRating || inStock === 'true'
  )

  const heading = activeCategory
    ? activeCategory.name
    : query
      ? `Results for “${query}”`
      : 'All products'

  return (
    <div className="page container">
      <Breadcrumbs
        items={[
          { label: 'Home', to: '/' },
          ...(activeCategory
            ? [{ label: 'Categories', to: '/categories' }, { label: activeCategory.name }]
            : [{ label: 'Products' }]),
        ]}
      />

      <div className="page-header">
        <h1 className="page-title">{heading}</h1>
        <p className="page-subtitle">
          {result
            ? `${result.totalElements} ${result.totalElements === 1 ? 'product' : 'products'} found`
            : 'Loading…'}
        </p>
      </div>

      <div className="listing">
        <button
          type="button"
          className="btn btn-outline listing-filter-toggle"
          onClick={() => setFiltersOpen((open) => !open)}
          aria-expanded={filtersOpen}
        >
          {filtersOpen ? 'Hide filters' : 'Show filters'}
          {hasActiveFilters && <span className="badge badge-primary">Active</span>}
        </button>

        <aside className={filtersOpen ? 'filters is-open' : 'filters'}>
          <div className="filters-header">
            <h2 className="filters-title">Filters</h2>
            {(hasActiveFilters || categorySlug || query) && (
              <button type="button" className="filters-clear" onClick={clearAll}>
                Clear all
              </button>
            )}
          </div>

          <div className="filter-block">
            <label className="form-label" htmlFor="filter-search">Search</label>
            <input
              id="filter-search"
              type="search"
              className="form-control"
              placeholder="Search products…"
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
            />
          </div>

          <div className="filter-block">
            <span className="form-label">Category</span>
            <div className="filter-options">
              <button
                type="button"
                className={!categorySlug ? 'filter-option is-active' : 'filter-option'}
                onClick={() => setParams({ category: null, brand: null })}
              >
                All categories
              </button>
              {categories.map((category) => (
                <button
                  key={category.id}
                  type="button"
                  className={categorySlug === category.slug ? 'filter-option is-active' : 'filter-option'}
                  onClick={() => setParams({ category: category.slug, brand: null })}
                >
                  {category.name}
                  <span className="filter-option-count">{category.productCount ?? 0}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="filter-block">
            <span className="form-label">Price range</span>
            <div className="filter-price">
              <input
                type="number"
                className="form-control"
                placeholder="Min"
                min="0"
                value={minPrice}
                onChange={(event) => setParams({ minPrice: event.target.value })}
                aria-label="Minimum price"
              />
              <span className="filter-price-sep">to</span>
              <input
                type="number"
                className="form-control"
                placeholder="Max"
                min="0"
                value={maxPrice}
                onChange={(event) => setParams({ maxPrice: event.target.value })}
                aria-label="Maximum price"
              />
            </div>
          </div>

          {brands.length > 0 && (
            <div className="filter-block">
              <label className="form-label" htmlFor="filter-brand">Brand</label>
              <select
                id="filter-brand"
                className="form-control"
                value={brand}
                onChange={(event) => setParams({ brand: event.target.value })}
              >
                <option value="">All brands</option>
                {brands.map((b) => (
                  <option key={b} value={b}>{b}</option>
                ))}
              </select>
            </div>
          )}

          <div className="filter-block">
            <label className="form-label" htmlFor="filter-rating">Minimum rating</label>
            <select
              id="filter-rating"
              className="form-control"
              value={minRating}
              onChange={(event) => setParams({ minRating: event.target.value })}
            >
              <option value="">Any rating</option>
              <option value="4">4 stars and up</option>
              <option value="3">3 stars and up</option>
              <option value="2">2 stars and up</option>
            </select>
          </div>

          <div className="filter-block">
            <label className="filter-checkbox">
              <input
                type="checkbox"
                checked={inStock === 'true'}
                onChange={(event) => setParams({ inStock: event.target.checked ? 'true' : null })}
              />
              <span>In stock only</span>
            </label>
          </div>
        </aside>

        <div className="listing-results">
          <div className="listing-toolbar">
            <label className="listing-sort">
              <span className="text-sm text-muted">Sort by</span>
              <select
                className="form-control"
                value={sort}
                onChange={(event) => setParams({ sort: event.target.value })}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </div>

          {loading && <Loader label="Loading products…" />}

          {!loading && error && (
            <ErrorState message={error} onRetry={() => setParams({ _r: Date.now() }, { resetPage: false })} />
          )}

          {!loading && !error && result?.empty && (
            <EmptyState
              icon={<SearchIcon size={48} />}
              title="No products match your filters"
              message="Try widening your price range, choosing a different category, or clearing your filters."
              action={
                <button type="button" className="btn btn-primary" onClick={clearAll}>
                  Clear all filters
                </button>
              }
            />
          )}

          {!loading && !error && result && !result.empty && (
            <>
              <div className="product-grid">
                {result.content.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              <Pagination
                page={result.page}
                totalPages={result.totalPages}
                onPageChange={(nextPage) => {
                  setParams({ page: nextPage }, { resetPage: false })
                  window.scrollTo({ top: 0, behavior: 'smooth' })
                }}
              />
            </>
          )}
        </div>
      </div>
    </div>
  )
}
