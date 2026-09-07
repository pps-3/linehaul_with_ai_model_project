package com.example.linehaul.service;

import com.example.linehaul.exception.BusinessException;
import com.example.linehaul.exception.NotFoundException;
import com.example.linehaul.model.Driver;
import com.example.linehaul.model.Order;
import com.example.linehaul.model.Route;
import com.example.linehaul.model.Status;
import com.example.linehaul.model.Vehicle;
import com.example.linehaul.repository.DriverRepository;
import com.example.linehaul.repository.OrderRepository;
import com.example.linehaul.repository.RouteRepository;
import com.example.linehaul.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final ResourceLookup lookup;

    public RouteService(RouteRepository routeRepository,
                        OrderRepository orderRepository,
                        VehicleRepository vehicleRepository,
                        DriverRepository driverRepository,
                        ResourceLookup lookup) {
        this.routeRepository = routeRepository;
        this.orderRepository = orderRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.lookup = lookup;
    }


    public List<Route> findAll(String search) {
        String needle = LinehaulUtil.clean(search).toLowerCase();
        return routeRepository.findAll().stream()
                .filter(r -> needle.isEmpty() || r.getRouteId().toLowerCase().contains(needle))
                .sorted((a, b) -> a.getRouteId().compareTo(b.getRouteId()))
                .map(this::decorate)
                .toList();
    }

    public Route findByRouteId(String routeId) {
        return decorate(routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found.")));
    }

    public List<Order> findOrders(String routeId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        List<Order> orders = new ArrayList<>();
        for (String orderId : route.getOrderIds()) {
            orderRepository.findByOrderId(orderId).ifPresent(orders::add);
        }
        return orders;
    }

    public Route create(Route route) {
        route.setRouteId(LinehaulUtil.clean(route.getRouteId()));

        if (route.getRouteId().isEmpty()) {
            throw new BusinessException("Route ID is required.");
        }
        if (routeRepository.existsByRouteId(route.getRouteId())) {
            throw new BusinessException("Route ID " + route.getRouteId() + " already exists.");
        }
        if (route.getMaxCapacity() <= 0) {
            throw new BusinessException("Maximum capacity must be greater than zero.");
        }
        if (LinehaulUtil.clean(route.getDepartureTime()).isEmpty()) {
            route.setDepartureTime("20:00");
        }

        route.setOrderIds(new ArrayList<>());
        route.setCurrentWeight(0);
        route.setTruckId(null);
        route.setDriverId(null);
        route.setStatus(Status.DRAFT);
        route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
        return decorate(routeRepository.save(route));
    }

    public Route update(String routeId, Route changes) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));

        if (changes.getOrigin() != null) route.setOrigin(changes.getOrigin());
        if (changes.getDestination() != null) route.setDestination(changes.getDestination());
        if (changes.getDepartureTime() != null && !changes.getDepartureTime().isBlank()) {
            route.setDepartureTime(changes.getDepartureTime());
        }
        if (changes.getTravelDuration() >= 0) {
            route.setTravelDuration(changes.getTravelDuration());
        }
        if (changes.getMaxCapacity() > 0) {
            if (changes.getMaxCapacity() < route.getCurrentWeight()) {
                throw new BusinessException("Maximum capacity cannot be lower than the current weight.");
            }
            route.setMaxCapacity(changes.getMaxCapacity());
        }

        route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
        for (String orderId : route.getOrderIds()) {
            orderRepository.findByOrderId(orderId).ifPresent(order -> {
                order.setEta(route.getEta());
                orderRepository.save(order);
            });
        }
        return decorate(routeRepository.save(route));
    }

    public Route assignOrder(String routeId, String orderId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found."));

        requireEditable(route);

        if (order.getRouteId() != null && !order.getRouteId().isEmpty()) {
            if (order.getRouteId().equals(route.getRouteId())) {
                throw new BusinessException("Order " + orderId + " is already on route " + routeId + ".");
            }
            throw new BusinessException("Order " + orderId + " is already assigned to route "
                    + order.getRouteId() + ".");
        }

        int newWeight = route.getCurrentWeight() + order.getWeight();
        if (newWeight > route.getMaxCapacity()) {
            throw new BusinessException("Route capacity exceeded.");
        }

        route.getOrderIds().add(order.getOrderId());
        route.setCurrentWeight(newWeight);

     
        route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
        routeRepository.save(route);

        order.setRouteId(route.getRouteId());
        order.setStatus(Status.ASSIGNED);
        order.setEta(route.getEta());
        orderRepository.save(order);

        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route unassignOrder(String routeId, String orderId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found."));

        requireEditable(route);

        if (!route.getOrderIds().contains(order.getOrderId())) {
            throw new BusinessException("Order " + orderId + " is not on route " + routeId + ".");
        }

        route.getOrderIds().remove(order.getOrderId());
        route.setCurrentWeight(Math.max(0, route.getCurrentWeight() - order.getWeight()));
        routeRepository.save(route);

        order.setRouteId(null);
        order.setEta(null);
        order.setStatus(Status.READY);
        orderRepository.save(order);

        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route assignTruck(String routeId, String truckId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        Vehicle truck = vehicleRepository.findByTruckId(truckId)
                .orElseThrow(() -> new NotFoundException("Truck not found."));

        requireEditable(route);

        String truckRoute = truck.getRouteId();
        if (truckRoute != null && !truckRoute.isEmpty() && !truckRoute.equals(route.getRouteId())) {
            throw new BusinessException("Truck is already assigned.");
        }
        if (!Status.AVAILABLE.equalsIgnoreCase(truck.getStatus())) {
            throw new BusinessException("Truck is not available.");
        }
        if (truck.getCapacity() < route.getCurrentWeight()) {
            throw new BusinessException("Truck capacity is smaller than the route weight.");
        }

        route.setTruckId(truck.getTruckId());
        truck.setRouteId(route.getRouteId());
        truck.setStatus(Status.ASSIGNED);
        vehicleRepository.save(truck);

        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route unassignTruck(String routeId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        requireEditable(route);

        if (route.getTruckId() != null) {
            lookup.findTruck(route.getTruckId()).ifPresent(truck -> {
                truck.setRouteId(null);
                truck.setStatus(Status.AVAILABLE);
                vehicleRepository.save(truck);
            });
        }
        route.setTruckId(null);
        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route assignDriver(String routeId, String driverId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        Driver driver = driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found."));

        requireEditable(route);

        String driverRoute = driver.getRouteId();
        if (driverRoute != null && !driverRoute.isEmpty() && !driverRoute.equals(route.getRouteId())) {
            throw new BusinessException("Driver is already assigned.");
        }
        if (!Status.AVAILABLE.equalsIgnoreCase(driver.getStatus())) {
            throw new BusinessException("Driver is not available.");
        }

        route.setDriverId(driver.getDriverId());
        driver.setRouteId(route.getRouteId());
        driver.setStatus(Status.ASSIGNED);
        driverRepository.save(driver);

        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route unassignDriver(String routeId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));
        requireEditable(route);

        if (route.getDriverId() != null) {
            lookup.findDriver(route.getDriverId()).ifPresent(driver -> {
                driver.setRouteId(null);
                driver.setStatus(Status.AVAILABLE);
                driverRepository.save(driver);
            });
        }
        route.setDriverId(null);
        return decorate(routeRepository.save(refreshStatus(route)));
    }

    public Route dispatch(String routeId) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new NotFoundException("Route not found."));

        if (Status.DISPATCHED.equalsIgnoreCase(route.getStatus())) {
            throw new BusinessException("Route is already dispatched.");
        }
        if (Status.IN_TRANSIT.equalsIgnoreCase(route.getStatus())
                || Status.COMPLETED.equalsIgnoreCase(route.getStatus())) {
            throw new BusinessException("Route cannot be dispatched.");
        }

        if (route.getOrderIds().isEmpty()) {
            throw new BusinessException("Route cannot be dispatched. Reason: no orders assigned.");
        }
        if (route.getTruckId() == null || route.getTruckId().isBlank()) {
            throw new BusinessException("Route cannot be dispatched. Reason: truck not assigned.");
        }
        if (route.getDriverId() == null || route.getDriverId().isBlank()) {
            throw new BusinessException("Route cannot be dispatched. Reason: driver not assigned.");
        }
        if (route.getCurrentWeight() > route.getMaxCapacity()) {
            throw new BusinessException("Route cannot be dispatched. Reason: route capacity exceeded.");
        }

        route.setStatus(Status.DISPATCHED);
        routeRepository.save(route);

        for (String orderId : route.getOrderIds()) {
            orderRepository.findByOrderId(orderId).ifPresent(order -> {
                order.setStatus(Status.DISPATCHED);
                orderRepository.save(order);
            });
        }

        lookup.findTruck(route.getTruckId()).ifPresent(truck -> {
            truck.setStatus(Status.IN_TRANSIT);
            vehicleRepository.save(truck);
        });
        lookup.findDriver(route.getDriverId()).ifPresent(driver -> {
            driver.setStatus(Status.IN_TRANSIT);
            driverRepository.save(driver);
        });

        return decorate(route);
    }

    private void requireEditable(Route route) {
        String status = route.getStatus() == null ? "" : route.getStatus().toUpperCase();
        if (status.equals(Status.DISPATCHED) || status.equals(Status.IN_TRANSIT)
                || status.equals(Status.COMPLETED)) {
            throw new BusinessException("Route " + route.getRouteId()
                    + " is already " + status.replace('_', ' ').toLowerCase()
                    + " and cannot be changed.");
        }
    }

 
    private Route refreshStatus(Route route) {
        route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
        decorate(route);

        String status = route.getStatus() == null ? "" : route.getStatus().toUpperCase();
        if (status.equals(Status.DISPATCHED) || status.equals(Status.IN_TRANSIT)
                || status.equals(Status.COMPLETED)) {
            return route;
        }
        route.setStatus(route.getReadiness().equals(Status.READY) ? Status.READY : Status.BLOCKED);
        return route;
    }

    private Route decorate(Route route) {
        if (route.getOrderIds() == null) {
            route.setOrderIds(new ArrayList<>());
        }
        route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
        route.setCapacityPercent(LinehaulUtil.capacityPercent(route.getCurrentWeight(), route.getMaxCapacity()));

        boolean hasOrders = !route.getOrderIds().isEmpty();
        boolean hasTruck = route.getTruckId() != null && !route.getTruckId().isBlank();
        boolean hasDriver = route.getDriverId() != null && !route.getDriverId().isBlank();
        boolean capacityOk = route.getCurrentWeight() <= route.getMaxCapacity();

        route.setHasOrders(hasOrders);
        route.setHasTruck(hasTruck);
        route.setHasDriver(hasDriver);
        route.setCapacityOk(capacityOk);

        String status = route.getStatus() == null ? "" : route.getStatus().toUpperCase();
        if (status.equals(Status.DISPATCHED) || status.equals(Status.IN_TRANSIT)
                || status.equals(Status.COMPLETED)) {
            route.setReadiness(status);
            route.setReadinessReason("Route " + status.replace('_', ' ').toLowerCase() + ".");
            return route;
        }

        if (hasOrders && hasTruck && hasDriver && capacityOk) {
            route.setReadiness(Status.READY);
            route.setReadinessReason("Ready to dispatch");
        } else {
            route.setReadiness(Status.BLOCKED);
            if (!hasOrders) route.setReadinessReason("No orders assigned");
            else if (!hasTruck) route.setReadinessReason("Truck not assigned");
            else if (!hasDriver) route.setReadinessReason("Driver not assigned");
            else route.setReadinessReason("Route capacity exceeded");
        }
        return route;
    }
}
