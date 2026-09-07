import { useLocation } from 'react-router-dom'

const TITLES = {
  '/': ['Dashboard', 'Overview of the linehaul operation'],
  '/orders': ['Orders', 'All freight orders'],
  '/routes': ['Routes', 'Build routes, assign orders, trucks and drivers'],
  '/vehicles': ['Vehicles', 'Trucks available for assignment'],
  '/drivers': ['Drivers', 'Drivers available for assignment'],
}

export default function Header() {
  const { pathname } = useLocation()
  const [title, subtitle] = TITLES[pathname] || ['Linehaul', '']

  return (
    <header className="header">
      <div>
        <h1>{title}</h1>
        <div className="sub">{subtitle}</div>
      </div>
      <div className="header-right">
        <div>Dispatcher Console</div>
      </div>
    </header>
  )
}
