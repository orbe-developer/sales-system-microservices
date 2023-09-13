package com.orbedeveloper.orders_service;

import com.orbedeveloper.orders_service.model.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
