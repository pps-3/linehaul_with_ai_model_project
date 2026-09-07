package com.example.linehaul.service;

import com.example.linehaul.exception.BusinessException;
import com.example.linehaul.exception.NotFoundException;
import com.example.linehaul.model.Vehicle;
import com.example.linehaul.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll().stream()
                .sorted((a, b) -> a.getTruckId().compareTo(b.getTruckId()))
                .toList();
    }

    public Vehicle findByTruckId(String truckId) {
        return vehicleRepository.findByTruckId(truckId)
                .orElseThrow(() -> new NotFoundException("Truck not found."));
    }

    public Vehicle create(Vehicle vehicle) {
        vehicle.setTruckId(LinehaulUtil.clean(vehicle.getTruckId()));

        if (vehicle.getTruckId().isEmpty()) {
            throw new BusinessException("Truck ID is required.");
        }
        if (vehicleRepository.existsByTruckId(vehicle.getTruckId())) {
            throw new BusinessException("Truck ID " + vehicle.getTruckId() + " already exists.");
        }
        if (vehicle.getCapacity() <= 0) {
            throw new BusinessException("Capacity must be greater than zero.");
        }
        if (vehicle.getType() == null || vehicle.getType().isBlank()) {
            vehicle.setType("Truck");
        }
        if (vehicle.getStatus() == null || vehicle.getStatus().isBlank()) {
            vehicle.setStatus("AVAILABLE");
        }
        vehicle.setStatus(vehicle.getStatus().toUpperCase());
        vehicle.setRouteId(null);
        return vehicleRepository.save(vehicle);
    }
}
