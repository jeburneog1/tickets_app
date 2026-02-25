package com.nequi.tickets.infrastructure.controller;

import com.nequi.tickets.infrastructure.dto.AvailabilityResponse;
import com.nequi.tickets.infrastructure.dto.CreateEventRequest;
import com.nequi.tickets.infrastructure.dto.DtoMapper;
import com.nequi.tickets.infrastructure.dto.EventResponse;
import com.nequi.tickets.usecase.port.CreateEventPort;
import com.nequi.tickets.usecase.port.GetEventPort;
import com.nequi.tickets.usecase.port.ListEventsPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/events")
public class EventController {
    
    private final CreateEventPort createEventPort;
    private final GetEventPort getEventPort;
    private final ListEventsPort listEventsPort;
    
    public EventController(
            CreateEventPort createEventPort,
            GetEventPort getEventPort,
            ListEventsPort listEventsPort) {
        this.createEventPort = createEventPort;
        this.getEventPort = getEventPort;
        this.listEventsPort = listEventsPort;
    }
    
    @PostMapping(
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> createEvent(@RequestBody CreateEventRequest request) {
        return createEventPort.execute(
                request.name(),
                request.date(),
                request.location(),
                request.totalCapacity()
            )
            .map(DtoMapper::toEventResponse);
    }

    
    @GetMapping(
        value = "/{eventId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<AvailabilityResponse> getEvent(@PathVariable String eventId) {
        return getEventPort.execute(eventId)
            .map(DtoMapper::toAvailabilityResponse);
    }
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public Flux<EventResponse> listEvents() {
        return listEventsPort.execute()
            .map(DtoMapper::toEventResponse);
    }
}
