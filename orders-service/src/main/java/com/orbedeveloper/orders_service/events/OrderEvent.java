package com.orbedeveloper.orders_service.events;

import com.orbedeveloper.orders_service.model.enums.OrderStatus;

public record OrderEvent(String orderNumber, int itemsCount, OrderStatus orderStatus) {
}