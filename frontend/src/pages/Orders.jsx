import { useEffect, useState } from 'react'
import { getOrders, getRoutes, assignOrderToRoute } from '../services/api.js'
import StatusBadge from '../components/StatusBadge.jsx'
import OrderModal from '../components/OrderModal.jsx'
import CreateOrderModal from '../components/CreateOrderModal.jsx'

const STATUS_FILTERS = [
  'ALL',
  'DRAFT',
  'CREATED',
  'READY',
  'MANIFESTED',
  'ASSIGNED',
  'DISPATCHED',
  'IN_TRANSIT',
  'COMPLETED',
  'BLOCKED',
]

export default function Orders({ notify }) {
  const [orders, setOrders] = useState([])
  const [routes, setRoutes] = useState([])
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('ALL')
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [showCreate, setShowCreate] = useState(false)

  async function load() {
    try {
      setError('')
      const data = await getOrders({ search, status: status === 'ALL' ? '' : status })
      setOrders(data)
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => {
    load()
  }, [search, status])

  useEffect(() => {
    getRoutes()
      .then(setRoutes)
      .catch(() => {})
  }, [])

  async function assignTo(routeId) {
    try {
      await assignOrderToRoute(routeId, selected.orderId)
      notify(`Order ${selected.orderId} assigned to route ${routeId}.`, 'success')
      setSelected(null)
      await load()
      getRoutes().then(setRoutes).catch(() => {})
    } catch (err) {
      notify(err.message, 'error')
    }
  }

  const assignable = routes.filter((route) =>
    ['DRAFT', 'READY', 'BLOCKED'].includes(String(route.status).toUpperCase())
  )
  const canAssign = selected && !selected.routeId && ['READY', 'CREATED', 'DRAFT', 'BLOCKED'].includes(
    String(selected.status).toUpperCase()
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h2 className="page-title">Orders</h2>
          <p className="page-sub">{orders.length} orders shown</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
          + Create Order
        </button>
      </div>

      <div className="filters">
        <input
          placeholder="Search by Order ID..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select value={status} onChange={(event) => setStatus(event.target.value)}>
          {STATUS_FILTERS.map((value) => (
            <option key={value} value={value}>
              {value === 'ALL' ? 'All statuses' : value.replace(/_/g, ' ')}
            </option>
          ))}
        </select>
      </div>

      {error ? <div className="error-text">{error}</div> : null}

      <div className="panel">
        {orders.length === 0 ? (
          <div className="empty">No orders found.</div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Origin</th>
                <th>Destination</th>
                <th>Weight</th>
                <th>Status</th>
                <th>ETA</th>
                <th>Route</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.orderId} className="clickable" onClick={() => setSelected(order)}>
                  <td>{order.orderId}</td>
                  <td>{order.customer}</td>
                  <td>{order.origin}</td>
                  <td>{order.destination}</td>
                  <td>{order.weight.toLocaleString()} lbs</td>
                  <td>
                    <StatusBadge status={order.status} />
                  </td>
                  <td>{order.eta || '-'}</td>
                  <td>{order.routeId || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selected ? (
        <OrderModal
          order={selected}
          onClose={() => setSelected(null)}
          footer={
            canAssign ? (
              <div className="field" style={{ minWidth: 220 }}>
                <label>Assign to route</label>
                <select
                  defaultValue=""
                  onChange={(event) => event.target.value && assignTo(event.target.value)}
                >
                  <option value="" disabled>
                    Choose a route...
                  </option>
                  {assignable.map((route) => (
                    <option key={route.routeId} value={route.routeId}>
                      {route.routeId} - {route.origin} &rarr; {route.destination} (
                      {route.capacityPercent}% full)
                    </option>
                  ))}
                </select>
              </div>
            ) : null
          }
        />
      ) : null}

      {showCreate ? (
        <CreateOrderModal
          notify={notify}
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false)
            load()
          }}
        />
      ) : null}
    </>
  )
}
