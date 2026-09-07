package com.example.linehaul.config;

import com.example.linehaul.model.Driver;
import com.example.linehaul.model.Order;
import com.example.linehaul.model.Route;
import com.example.linehaul.model.Status;
import com.example.linehaul.model.Vehicle;
import com.example.linehaul.repository.DriverRepository;
import com.example.linehaul.repository.OrderRepository;
import com.example.linehaul.repository.RouteRepository;
import com.example.linehaul.repository.VehicleRepository;
import com.example.linehaul.service.LinehaulUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public DataSeeder(OrderRepository orderRepository,
                      RouteRepository routeRepository,
                      VehicleRepository vehicleRepository,
                      DriverRepository driverRepository) {
        this.orderRepository = orderRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public void run(String... args) {
        if (orderRepository.count() > 0 || routeRepository.count() > 0) {
            log.info("Database already has data - skipping sample data.");
            return;
        }

        log.info("Database is empty - creating sample data...");

     
        vehicleRepository.saveAll(List.of(
                truck("T-182", "Truck", 10000, Status.AVAILABLE, null),
                truck("T-204", "Truck", 8000, Status.AVAILABLE, null),
                truck("T-311", "Tractor", 12000, Status.IN_TRANSIT, "LH-1031"),
                truck("T-407", "Van", 4000, Status.AVAILABLE, null),
                truck("T-509", "Truck", 9000, Status.MAINTENANCE, null)
        ));

        driverRepository.saveAll(List.of(
                driver("D-101", "John Smith", Status.AVAILABLE, null),
                driver("D-102", "Maria Garcia", Status.AVAILABLE, null),
                driver("D-103", "David Chen", Status.IN_TRANSIT, "LH-1031"),
                driver("D-104", "Priya Nair", Status.ASSIGNED, "LH-1032"),
                driver("D-105", "Sam Wilson", Status.AVAILABLE, null)
        ));

        List<Route> routes = new ArrayList<>();
        routes.add(route("LH-1029", "EXPINTL", "ATLTEST", "20:00", 9, 10000, "T-182", "D-101", Status.READY));
        routes.add(route("LH-1030", "Dallas", "Chicago", "21:00", 14, 8000, "T-204", null, Status.BLOCKED));
        routes.add(route("LH-1031", "Austin", "Dallas", "06:00", 4, 12000, "T-311", "D-103", Status.DISPATCHED));
        routes.add(route("LH-1032", "Atlanta", "EXPINTL", "22:30", 11, 9000, null, "D-104", Status.BLOCKED));
        routes.add(route("LH-1033", "Chicago", "Austin", "19:00", 16, 7000, null, null, Status.DRAFT));
        routeRepository.saveAll(routes);

        List<Order> orders = new ArrayList<>();
        int n = 1001;

        for (int i = 0; i < 8; i++) {
            orders.add(order("LH-" + n++, "ABC Logistics", "EXPINTL", "ATLTEST",
                    700 + i * 120, 8 + i * 2, "2026-08-25", Status.ASSIGNED, "LH-1029"));
        }
        for (int i = 0; i < 4; i++) {
            orders.add(order("LH-" + n++, "Fast Freight Co", "Dallas", "Chicago",
                    650 + i * 250, 10 + i, "2026-08-25", Status.ASSIGNED, "LH-1030"));
        }
        for (int i = 0; i < 5; i++) {
            orders.add(order("LH-" + n++, "Sunrise Cargo", "Austin", "Dallas",
                    500 + i * 180, 6 + i, "2026-08-24", Status.DISPATCHED, "LH-1031"));
        }
        for (int i = 0; i < 3; i++) {
            orders.add(order("LH-" + n++, "Delta Shipping", "Atlanta", "EXPINTL",
                    800 + i * 300, 12 + i, "2026-08-26", Status.ASSIGNED, "LH-1032"));
        }
        for (int i = 0; i < 2; i++) {
            orders.add(order("LH-" + n++, "Blue Line Transport", "Chicago", "Austin",
                    900 + i * 200, 14, "2026-08-20", Status.COMPLETED, null));
        }
        for (int i = 0; i < 2; i++) {
            orders.add(order("LH-" + n++, "Northwind Carriers", "Dallas", "Atlanta",
                    1100 + i * 150, 18, "2026-08-25", Status.BLOCKED, null));
        }
        for (int i = 0; i < 8; i++) {
            orders.add(order("LH-" + n++, customers[i], origins[i], destinations[i],
                    400 + i * 210, 5 + i * 3, "2026-08-26", Status.READY, null));
        }
        orderRepository.saveAll(orders);

        for (Route route : routeRepository.findAll()) {
            List<Order> onRoute = orderRepository.findByRouteId(route.getRouteId());
            route.setOrderIds(onRoute.stream().map(Order::getOrderId).toList());
            route.setCurrentWeight(onRoute.stream().mapToInt(Order::getWeight).sum());
            route.setEta(LinehaulUtil.calculateEta(route.getDepartureTime(), route.getTravelDuration()));
            routeRepository.save(route);

            for (Order order : onRoute) {
                order.setEta(route.getEta());
                orderRepository.save(order);
            }
        }

        log.info("Sample data created: {} orders, {} routes, {} trucks, {} drivers.",
                orderRepository.count(), routeRepository.count(),
                vehicleRepository.count(), driverRepository.count());
    }

    private static final String[] customers = {
            "ABC Logistics", "Fast Freight Co", "Sunrise Cargo", "Delta Shipping",
            "Blue Line Transport", "Northwind Carriers", "Metro Haulers", "Peak Distribution"
    };
    private static final String[] origins = {
            "EXPINTL", "Dallas", "Austin", "Atlanta", "Chicago", "EXPINTL", "Dallas", "ATLTEST"
    };
    private static final String[] destinations = {
            "ATLTEST", "Chicago", "Dallas", "EXPINTL", "Austin", "Atlanta", "ATLTEST", "Chicago"
    };

    private Vehicle truck(String truckId, String type, int capacity, String status, String routeId) {
        Vehicle v = new Vehicle();
        v.setTruckId(truckId);
        v.setType(type);
        v.setCapacity(capacity);
        v.setStatus(status);
        v.setRouteId(routeId);
        return v;
    }

    private Driver driver(String driverId, String name, String status, String routeId) {
        Driver d = new Driver();
        d.setDriverId(driverId);
        d.setName(name);
        d.setStatus(status);
        d.setRouteId(routeId);
        return d;
    }

    private Route route(String routeId, String origin, String destination, String departureTime,
                        int travelDuration, int maxCapacity, String truckId, String driverId, String status) {
        Route r = new Route();
        r.setRouteId(routeId);
        r.setOrigin(origin);
        r.setDestination(destination);
        r.setDepartureTime(departureTime);
        r.setTravelDuration(travelDuration);
        r.setMaxCapacity(maxCapacity);
        r.setTruckId(truckId);
        r.setDriverId(driverId);
        r.setStatus(status);
        r.setOrderIds(new ArrayList<>());
        r.setCurrentWeight(0);
        r.setEta(LinehaulUtil.calculateEta(departureTime, travelDuration));
        return r;
    }

    private Order order(String orderId, String customer, String origin, String destination,
                        int weight, int pieces, String serviceDate, String status, String routeId) {
        Order o = new Order();
        o.setOrderId(orderId);
        o.setCustomer(customer);
        o.setOrigin(origin);
        o.setDestination(destination);
        o.setWeight(weight);
        o.setPieces(pieces);
        o.setServiceDate(serviceDate);
        o.setStatus(status);
        o.setRouteId(routeId);
        return o;
    }
}
