package com.example.linehaul.service;

import com.example.linehaul.model.Driver;
import com.example.linehaul.model.Order;
import com.example.linehaul.model.Route;
import com.example.linehaul.model.Vehicle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Builds a plain-text snapshot of the current Linehaul data (routes, orders,
 * drivers, vehicles) that is handed to the AI model as context. This is the
 * "retrieval" step of a lightweight retrieval-augmented setup: no intent
 * parsing, no entity/pattern matching - the model reads the same table a
 * human dispatcher would and reasons over it directly.
 */
@Component
public class LinehaulContextBuilder {

    // Keep the context bounded so it stays well inside the model's token
    // limit even if the dataset grows. Raise this if your deployment needs
    // to expose more rows to the model.
    private static final int MAX_ROWS = 200;

    private final RouteService routeService;
    private final OrderService orderService;
    private final DriverService driverService;
    private final VehicleService vehicleService;

    public LinehaulContextBuilder(RouteService routeService,
                                   OrderService orderService,
                                   DriverService driverService,
                                   VehicleService vehicleService) {
        this.routeService = routeService;
        this.orderService = orderService;
        this.driverService = driverService;
        this.vehicleService = vehicleService;
    }

    public String build() {
        List<Route> routes = routeService.findAll(null);
        List<Order> orders = orderService.findAll(null, null);
        List<Driver> drivers = driverService.findAll();
        List<Vehicle> vehicles = vehicleService.findAll();

        StringBuilder text = new StringBuilder();

        text.append("ROUTES (").append(routes.size()).append(" total)\n");
        text.append("id | status | readiness | lane | orders | truck | driver | weight/capacity | departure | eta | reason\n");
        routes.stream().limit(MAX_ROWS).forEach(route -> text.append(routeRow(route)).append('\n'));
        if (routes.size() > MAX_ROWS) {
            text.append("... ").append(routes.size() - MAX_ROWS).append(" more routes not shown ...\n");
        }

        text.append("\nORDERS (").append(orders.size()).append(" total)\n");
        text.append("id | customer | status | weight | origin | destination | route | eta\n");
        orders.stream().limit(MAX_ROWS).forEach(order -> text.append(orderRow(order)).append('\n'));
        if (orders.size() > MAX_ROWS) {
            text.append("... ").append(orders.size() - MAX_ROWS).append(" more orders not shown ...\n");
        }

        text.append("\nDRIVERS (").append(drivers.size()).append(" total)\n");
        text.append("id | name | status | route\n");
        drivers.stream().limit(MAX_ROWS).forEach(driver -> text.append(driverRow(driver)).append('\n'));
        if (drivers.size() > MAX_ROWS) {
            text.append("... ").append(drivers.size() - MAX_ROWS).append(" more drivers not shown ...\n");
        }

        text.append("\nVEHICLES (").append(vehicles.size()).append(" total)\n");
        text.append("id | type | status | capacity | route\n");
        vehicles.stream().limit(MAX_ROWS).forEach(vehicle -> text.append(vehicleRow(vehicle)).append('\n'));
        if (vehicles.size() > MAX_ROWS) {
            text.append("... ").append(vehicles.size() - MAX_ROWS).append(" more vehicles not shown ...\n");
        }

        return text.toString();
    }

    private String routeRow(Route route) {
        return join(
                route.getRouteId(),
                up(route.getStatus()),
                up(route.getReadiness()),
                route.getOrigin() + " -> " + route.getDestination(),
                String.valueOf(route.getOrderIds().size()),
                route.isHasTruck() ? route.getTruckId() : "none",
                route.isHasDriver() ? driverLabel(route.getDriverId()) : "none",
                kg(route.getCurrentWeight()) + "/" + kg(route.getMaxCapacity()) + " (" + route.getCapacityPercent() + "%)",
                route.getDepartureTime(),
                blank(route.getEta()) ? "none" : route.getEta(),
                blank(route.getReadinessReason()) ? "-" : route.getReadinessReason()
        );
    }

    private String orderRow(Order order) {
        return join(
                order.getOrderId(),
                order.getCustomer(),
                up(order.getStatus()),
                kg(order.getWeight()),
                order.getOrigin(),
                order.getDestination(),
                blank(order.getRouteId()) ? "unassigned" : order.getRouteId(),
                blank(order.getEta()) ? "none" : order.getEta()
        );
    }

    private String driverRow(Driver driver) {
        return join(
                driver.getDriverId(),
                driver.getName(),
                up(driver.getStatus()),
                blank(driver.getRouteId()) ? "unassigned" : driver.getRouteId()
        );
    }

    private String vehicleRow(Vehicle vehicle) {
        return join(
                vehicle.getTruckId(),
                vehicle.getType(),
                up(vehicle.getStatus()),
                kg(vehicle.getCapacity()),
                blank(vehicle.getRouteId()) ? "unassigned" : vehicle.getRouteId()
        );
    }

    private String driverLabel(String driverId) {
        return driverService.findAll().stream()
                .filter(driver -> driverId.equalsIgnoreCase(driver.getDriverId()))
                .findFirst()
                .map(driver -> driverId + " (" + driver.getName() + ")")
                .orElse(driverId);
    }

    private static String join(String... columns) {
        return String.join(" | ", columns);
    }

    private static String kg(int value) {
        return String.format(Locale.ROOT, "%,d kg", value);
    }

    private static String up(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
