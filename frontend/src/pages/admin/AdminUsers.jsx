import { useEffect, useState } from 'react'
import { EmptyState, ErrorState, Loader, Pagination } from '../../components/Common'
import { ConfirmDialog } from '../../components/Modal'
import adminService from '../../services/adminService'
import { useAuth, useDebounce, useToast } from '../../hooks'
import { formatDate } from '../../utils/format'

export default function AdminUsers() {
  const { user: currentUser } = useAuth()
  const toast = useToast()

  const [result, setResult] = useState(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toggleTarget, setToggleTarget] = useState(null)

  const debouncedSearch = useDebounce(search, 400)

  const load = () => {
    setLoading(true)
    setError(null)
    adminService
      .listUsers({ q: debouncedSearch || undefined, page, size: 20 })
      .then(setResult)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(load, [debouncedSearch, page])
  useEffect(() => setPage(0), [debouncedSearch])

  const handleToggle = async () => {
    try {
      await adminService.setUserEnabled(toggleTarget.id, !toggleTarget.enabled)
      toast.success(
        `${toggleTarget.fullName} has been ${toggleTarget.enabled ? 'disabled' : 'enabled'}.`
      )
      setToggleTarget(null)
      load()
    } catch (err) {
      toast.error(err.message)
      setToggleTarget(null)
    }
  }

  return (
    <div>
      <div className="admin-header">
        <div>
          <h1 className="admin-title">Users</h1>
          <p className="admin-subtitle">
            {result ? `${result.totalElements} registered accounts` : 'Loading…'}
          </p>
        </div>
      </div>

      <div className="admin-toolbar">
        <input
          type="search"
          className="form-control"
          placeholder="Search by name, email or phone…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>

      {loading && <Loader label="Loading users…" />}
      {!loading && error && <ErrorState message={error} onRetry={load} />}

      {!loading && !error && result?.empty && (
        <EmptyState icon="👥" title="No users found" message="Try a different search term." />
      )}

      {!loading && !error && result && !result.empty && (
        <>
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Phone</th>
                  <th>Roles</th>
                  <th>Joined</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((user) => {
                  const isSelf = user.id === currentUser?.id
                  const isAdmin = user.roles.includes('ROLE_ADMIN')

                  return (
                    <tr key={user.id}>
                      <td>
                        <div className="table-product">
                          <span className="admin-avatar" aria-hidden="true">
                            {user.fullName.charAt(0).toUpperCase()}
                          </span>
                          <div>
                            <div className="table-product-name">
                              {user.fullName}
                              {isSelf && <span className="text-xs text-subtle"> (you)</span>}
                            </div>
                            <div className="text-xs text-subtle">{user.email}</div>
                          </div>
                        </div>
                      </td>
                      <td className="text-sm">{user.phoneNumber}</td>
                      <td>
                        <div className="row wrap">
                          {user.roles.map((role) => (
                            <span
                              key={role}
                              className={isAdmin && role === 'ROLE_ADMIN'
                                ? 'badge badge-warning'
                                : 'badge badge-neutral'}
                            >
                              {role.replace('ROLE_', '')}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="text-sm text-muted">{formatDate(user.createdAt)}</td>
                      <td>
                        <span className={user.enabled ? 'badge badge-success' : 'badge badge-danger'}>
                          {user.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-outline btn-sm"
                          onClick={() => setToggleTarget(user)}
                          disabled={isSelf}
                          title={isSelf ? 'You cannot disable your own account' : undefined}
                        >
                          {user.enabled ? 'Disable' : 'Enable'}
                        </button>
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
        open={Boolean(toggleTarget)}
        title={toggleTarget?.enabled ? 'Disable this account?' : 'Enable this account?'}
        message={
          toggleTarget?.enabled
            ? `${toggleTarget?.fullName} will be signed out on their next request and cannot sign in again until re-enabled. Their orders and reviews are kept.`
            : `${toggleTarget?.fullName} will be able to sign in and shop again.`
        }
        confirmLabel={toggleTarget?.enabled ? 'Disable account' : 'Enable account'}
        danger={toggleTarget?.enabled}
        onConfirm={handleToggle}
        onCancel={() => setToggleTarget(null)}
      />
    </div>
  )
}
