package com.nequi.tickets.usecase;

import com.nequi.tickets.domain.exception.OrderNotFoundException;
import com.nequi.tickets.domain.model.Order;
import com.nequi.tickets.domain.repository.OrderRepository;
import com.nequi.tickets.usecase.port.GetOrderStatusPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetOrderStatusUseCase implements GetOrderStatusPort {
    
    private final OrderRepository orderRepository;
    
    public GetOrderStatusUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
    
    public Mono<Order> execute(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return Mono.error(new IllegalArgumentException("Order ID is required"));
        }
        
        return orderRepository.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found with ID: " + orderId)));
    }
}
