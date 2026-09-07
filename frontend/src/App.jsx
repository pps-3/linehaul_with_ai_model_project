import { Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'
import Header from './components/Header.jsx'
import Toasts, { useToasts } from './components/Toasts.jsx'
import ChatAssistant from './components/ChatAssistant.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Orders from './pages/Orders.jsx'
import RoutesPage from './pages/Routes.jsx'
import Vehicles from './pages/Vehicles.jsx'
import Drivers from './pages/Drivers.jsx'

export default function App() {
  const { toasts, notify } = useToasts()

  return (
    <div className="app">
      <Sidebar />
      <div className="main">
        <Header />
        <div className="page">
          <Routes>
            <Route path="/" element={<Dashboard notify={notify} />} />
            <Route path="/orders" element={<Orders notify={notify} />} />
            <Route path="/routes" element={<RoutesPage notify={notify} />} />
            <Route path="/vehicles" element={<Vehicles notify={notify} />} />
            <Route path="/drivers" element={<Drivers notify={notify} />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </div>
      <ChatAssistant />
      <Toasts toasts={toasts} />
    </div>
  )
}
