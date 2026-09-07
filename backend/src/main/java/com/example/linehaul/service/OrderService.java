package com.example.linehaul.service;

import com.example.linehaul.exception.BusinessException;
import com.example.linehaul.exception.NotFoundException;
import com.example.linehaul.model.Order;
import com.example.linehaul.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> findAll(String search, String status) {
        List<Order> orders = orderRepository.findAll();
        String needle = LinehaulUtil.clean(search).toLowerCase();
        String wanted = LinehaulUtil.clean(status).toUpperCase();

        return orders.stream()
                .filter(o -> needle.isEmpty() || o.getOrderId().toLowerCase().contains(needle))
                .filter(o -> wanted.isEmpty() || wanted.equalsIgnoreCase(o.getStatus()))
                .sorted((a, b) -> a.getOrderId().compareTo(b.getOrderId()))
                .toList();
    }

    public Order findByOrderId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found."));
    }

    public Order create(Order order) {
        order.setOrderId(LinehaulUtil.clean(order.getOrderId()));

        if (order.getOrderId().isEmpty()) {
            throw new BusinessException("Order ID is required.");
        }
        if (orderRepository.existsByOrderId(order.getOrderId())) {
            throw new BusinessException("Order ID " + order.getOrderId() + " already exists.");
        }
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("READY");
        }
        order.setStatus(order.getStatus().toUpperCase());
        order.setRouteId(null);
        order.setEta(null);
        return orderRepository.save(order);
    }

    public Order update(String orderId, Order changes) {
        Order order = findByOrderId(orderId);

        if (changes.getCustomer() != null) order.setCustomer(changes.getCustomer());
        if (changes.getOrigin() != null) order.setOrigin(changes.getOrigin());
        if (changes.getDestination() != null) order.setDestination(changes.getDestination());
        if (changes.getServiceDate() != null) order.setServiceDate(changes.getServiceDate());
        order.setWeight(changes.getWeight());
        order.setPieces(changes.getPieces());
        if (changes.getStatus() != null && !changes.getStatus().isBlank()) {
            order.setStatus(changes.getStatus().toUpperCase());
        }
        return orderRepository.save(order);
    }
}
