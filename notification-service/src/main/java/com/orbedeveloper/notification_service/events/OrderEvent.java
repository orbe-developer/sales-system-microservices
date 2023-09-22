package com.orbedeveloper.notification_service.events;


import com.orbedeveloper.notification_service.model.enums.OrderStatus;

public record OrderEvent(String orderNumber, int itemsCount, OrderStatus orderStatus) {
}