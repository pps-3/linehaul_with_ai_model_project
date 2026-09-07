package com.example.linehaul.service;

import com.example.linehaul.exception.BusinessException;
import com.example.linehaul.exception.NotFoundException;
import com.example.linehaul.model.Driver;
import com.example.linehaul.repository.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> findAll() {
        return driverRepository.findAll().stream()
                .sorted((a, b) -> a.getDriverId().compareTo(b.getDriverId()))
                .toList();
    }

    public Driver findByDriverId(String driverId) {
        return driverRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver not found."));
    }

    public Driver create(Driver driver) {
        driver.setDriverId(LinehaulUtil.clean(driver.getDriverId()));

        if (driver.getDriverId().isEmpty()) {
            throw new BusinessException("Driver ID is required.");
        }
        if (driverRepository.existsByDriverId(driver.getDriverId())) {
            throw new BusinessException("Driver ID " + driver.getDriverId() + " already exists.");
        }
        if (driver.getName() == null || driver.getName().isBlank()) {
            throw new BusinessException("Driver name is required.");
        }
        if (driver.getStatus() == null || driver.getStatus().isBlank()) {
            driver.setStatus("AVAILABLE");
        }
        driver.setStatus(driver.getStatus().toUpperCase());
        driver.setRouteId(null);
        return driverRepository.save(driver);
    }
}
