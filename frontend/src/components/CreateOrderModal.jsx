import { useState } from 'react'
import Modal from './Modal.jsx'
import { createOrder } from '../services/api.js'

const EMPTY = {
  orderId: '',
  customer: '',
  origin: 'EXPINTL',
  destination: 'ATLTEST',
  weight: 500,
  pieces: 1,
  serviceDate: '2026-08-26',
  status: 'READY',
}

const STATUSES = ['DRAFT', 'CREATED', 'READY', 'BLOCKED']

export default function CreateOrderModal({ onClose, onCreated, notify }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const created = await createOrder({
        ...form,
        weight: Number(form.weight) || 0,
        pieces: Number(form.pieces) || 0,
      })
      notify(`Order ${created.orderId} created.`, 'success')
      onCreated(created)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Create Order"
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" form="order-form" disabled={saving}>
            {saving ? 'Saving...' : 'Save Order'}
          </button>
        </>
      }
    >
      {error ? <div className="error-text">{error}</div> : null}
      <form id="order-form" onSubmit={submit}>
        <div className="form-grid">
          <div className="field">
            <label>Order ID</label>
            <input value={form.orderId} onChange={set('orderId')} placeholder="LH-1050" required />
          </div>
          <div className="field">
            <label>Customer</label>
            <input value={form.customer} onChange={set('customer')} placeholder="ABC Logistics" required />
          </div>
          <div className="field">
            <label>Origin</label>
            <input value={form.origin} onChange={set('origin')} required />
          </div>
          <div className="field">
            <label>Destination</label>
            <input value={form.destination} onChange={set('destination')} required />
          </div>
          <div className="field">
            <label>Weight (lbs)</label>
            <input type="number" min="0" value={form.weight} onChange={set('weight')} required />
          </div>
          <div className="field">
            <label>Pieces</label>
            <input type="number" min="0" value={form.pieces} onChange={set('pieces')} required />
          </div>
          <div className="field">
            <label>Service date</label>
            <input type="date" value={form.serviceDate} onChange={set('serviceDate')} />
          </div>
          <div className="field">
            <label>Status</label>
            <select value={form.status} onChange={set('status')}>
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
        </div>
      </form>
    </Modal>
  )
}
