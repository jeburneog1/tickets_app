package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.infrastructure.dto.AssignComplimentaryTicketRequest;
import com.nequi.tickets.infrastructure.dto.DtoMapper;
import com.nequi.tickets.infrastructure.dto.TicketResponse;
import com.nequi.tickets.usecase.port.AssignComplimentaryTicketPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tickets")
public class ComplimentaryTicketController {
    
    private final AssignComplimentaryTicketPort assignComplimentaryTicketPort;
    
    public ComplimentaryTicketController(AssignComplimentaryTicketPort assignComplimentaryTicketPort) {
        this.assignComplimentaryTicketPort = assignComplimentaryTicketPort;
    }
    
    @PostMapping(
        value = "/complimentary",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TicketResponse> assignComplimentaryTicket(@RequestBody AssignComplimentaryTicketRequest request) {
        return assignComplimentaryTicketPort.execute(
                request.eventId(),
                request.customerId(),
                request.reason()
            )
            .map(DtoMapper::toTicketResponse);
    }
}
