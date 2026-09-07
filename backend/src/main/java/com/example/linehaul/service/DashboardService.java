package com.example.linehaul.service;

import com.example.linehaul.model.Order;
import com.example.linehaul.model.Route;
import com.example.linehaul.model.Status;
import com.example.linehaul.repository.DriverRepository;
import com.example.linehaul.repository.OrderRepository;
import com.example.linehaul.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final RouteService routeService;

    public DashboardService(OrderRepository orderRepository,
                            VehicleRepository vehicleRepository,
                            DriverRepository driverRepository,
                            RouteService routeService) {
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.routeService = routeService;
    }

    public Summary summary() {
        List<Route> routes = routeService.findAll(null);

        long readyRoutes = routes.stream().filter(r -> Status.READY.equals(r.getReadiness())).count();
        long blockedRoutes = routes.stream().filter(r -> Status.BLOCKED.equals(r.getReadiness())).count();
        long activeRoutes = routes.stream()
                .filter(r -> Status.DISPATCHED.equalsIgnoreCase(r.getStatus())
                        || Status.IN_TRANSIT.equalsIgnoreCase(r.getStatus()))
                .count();

        List<Route> activeRouteList = routes.stream()
                .filter(r -> Status.DISPATCHED.equalsIgnoreCase(r.getStatus())
                        || Status.IN_TRANSIT.equalsIgnoreCase(r.getStatus())
                        || Status.READY.equalsIgnoreCase(r.getStatus()))
                .toList();

        List<Order> toDispatch = orderRepository.findByStatusIn(List.of(
                        Status.CREATED, Status.READY, Status.MANIFESTED, Status.ASSIGNED)).stream()
                .sorted((a, b) -> a.getOrderId().compareTo(b.getOrderId()))
                .toList();

        Summary summary = new Summary();
        summary.setTotalOrders((int) orderRepository.count());
        summary.setTotalRoutes(routes.size());
        summary.setActiveRoutes((int) activeRoutes);
        summary.setReadyRoutes((int) readyRoutes);
        summary.setBlockedRoutes((int) blockedRoutes);
        summary.setTotalVehicles((int) vehicleRepository.count());
        summary.setTotalDrivers((int) driverRepository.count());
        summary.setOrdersToDispatch(toDispatch);
        summary.setActiveRouteList(activeRouteList);
        return summary;
    }

    public static class Summary {
        private int totalOrders;
        private int totalRoutes;
        private int activeRoutes;
        private int readyRoutes;
        private int blockedRoutes;
        private int totalVehicles;
        private int totalDrivers;
        private List<Order> ordersToDispatch;
        private List<Route> activeRouteList;

        public int getTotalOrders() {
            return totalOrders;
        }

        public void setTotalOrders(int totalOrders) {
            this.totalOrders = totalOrders;
        }

        public int getTotalRoutes() {
            return totalRoutes;
        }

        public void setTotalRoutes(int totalRoutes) {
            this.totalRoutes = totalRoutes;
        }

        public int getActiveRoutes() {
            return activeRoutes;
        }

        public void setActiveRoutes(int activeRoutes) {
            this.activeRoutes = activeRoutes;
        }

        public int getReadyRoutes() {
            return readyRoutes;
        }

        public void setReadyRoutes(int readyRoutes) {
            this.readyRoutes = readyRoutes;
        }

        public int getBlockedRoutes() {
            return blockedRoutes;
        }

        public void setBlockedRoutes(int blockedRoutes) {
            this.blockedRoutes = blockedRoutes;
        }

        public int getTotalVehicles() {
            return totalVehicles;
        }

        public void setTotalVehicles(int totalVehicles) {
            this.totalVehicles = totalVehicles;
        }

        public int getTotalDrivers() {
            return totalDrivers;
        }

        public void setTotalDrivers(int totalDrivers) {
            this.totalDrivers = totalDrivers;
        }

        public List<Order> getOrdersToDispatch() {
            return ordersToDispatch;
        }

        public void setOrdersToDispatch(List<Order> ordersToDispatch) {
            this.ordersToDispatch = ordersToDispatch;
        }

        public List<Route> getActiveRouteList() {
            return activeRouteList;
        }

        public void setActiveRouteList(List<Route> activeRouteList) {
            this.activeRouteList = activeRouteList;
        }
    }
}
