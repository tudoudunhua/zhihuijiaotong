package com.zhihui.bishe.repository;

import com.zhihui.bishe.model.CarOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarOwnerRepository extends JpaRepository<CarOwner, Long> {
    Optional<CarOwner> findByPlateNumber(String plateNumber);
}

