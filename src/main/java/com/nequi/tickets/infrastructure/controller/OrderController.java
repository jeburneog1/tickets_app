package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.infrastructure.dto.CreateOrderRequest;
import com.nequi.tickets.infrastructure.dto.DtoMapper;
import com.nequi.tickets.infrastructure.dto.OrderResponse;
import com.nequi.tickets.usecase.port.ConfirmOrderPort;
import com.nequi.tickets.usecase.port.CreateOrderPort;
import com.nequi.tickets.usecase.port.GetOrderStatusPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private final CreateOrderPort createOrderPort;
    private final GetOrderStatusPort getOrderStatusPort;
    private final ConfirmOrderPort confirmOrderPort;
    
    public OrderController(
            CreateOrderPort createOrderPort,
            GetOrderStatusPort getOrderStatusPort,
            ConfirmOrderPort confirmOrderPort) {
        this.createOrderPort = createOrderPort;
        this.getOrderStatusPort = getOrderStatusPort;
        this.confirmOrderPort = confirmOrderPort;
    }
    
    @PostMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        return createOrderPort.execute(
                request.eventId(),
                request.customerId(),
                request.numberOfTickets()
            )
            .map(DtoMapper::toOrderResponse);
    }
    
    @GetMapping(
        value = "/{orderId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderResponse> getOrderStatus(@PathVariable String orderId) {
        return getOrderStatusPort.execute(orderId)
            .map(DtoMapper::toOrderResponse);
    }
    
    @PostMapping(
        value = "/{orderId}/confirm",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<OrderResponse> confirmOrder(@PathVariable String orderId) {
        return confirmOrderPort.execute(orderId)
            .map(DtoMapper::toOrderResponse);
    }
}
