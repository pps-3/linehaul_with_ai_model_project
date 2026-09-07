import { useState } from 'react'
import Modal from './Modal.jsx'
import { createRoute } from '../services/api.js'

const EMPTY = {
  routeId: '',
  origin: 'EXPINTL',
  destination: 'ATLTEST',
  departureTime: '20:00',
  travelDuration: 9,
  maxCapacity: 10000,
}

export default function CreateRouteModal({ onClose, onCreated, notify }) {
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  async function submit(event) {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const created = await createRoute({
        ...form,
        travelDuration: Number(form.travelDuration) || 0,
        maxCapacity: Number(form.maxCapacity) || 0,
      })
      notify(`Route ${created.routeId} created.`, 'success')
      onCreated(created)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      title="Create Route"
      onClose={onClose}
      footer={
        <>
          <button className="btn" onClick={onClose}>
            Cancel
          </button>
          <button className="btn btn-primary" form="route-form" disabled={saving}>
            {saving ? 'Saving...' : 'Save Route'}
          </button>
        </>
      }
    >
      {error ? <div className="error-text">{error}</div> : null}
      <form id="route-form" onSubmit={submit}>
        <div className="form-grid">
          <div className="field">
            <label>Route ID</label>
            <input value={form.routeId} onChange={set('routeId')} placeholder="LH-1040" required />
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
            <label>Departure time</label>
            <input type="time" value={form.departureTime} onChange={set('departureTime')} required />
          </div>
          <div className="field">
            <label>Travel duration (hours)</label>
            <input
              type="number"
              min="0"
              value={form.travelDuration}
              onChange={set('travelDuration')}
              required
            />
          </div>
          <div className="field">
            <label>Maximum capacity (lbs)</label>
            <input
              type="number"
              min="1"
              value={form.maxCapacity}
              onChange={set('maxCapacity')}
              required
            />
          </div>
        </div>
      </form>
    </Modal>
  )
}
