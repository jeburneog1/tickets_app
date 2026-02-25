package com.nequi.tickets.infrastructure.scheduler;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.usecase.ReleaseExpiredReservationsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledReservationReleaser Tests")
class ScheduledReservationReleaserTest {
    @Mock
    private ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;
    
    @Mock
    private BusinessProperties businessProperties;
    
    private ScheduledReservationReleaser scheduler;
    
    @BeforeEach
    void setUp() {
        BusinessProperties.Reservation reservation = new BusinessProperties.Reservation();
        reservation.setReleaseTimeoutSeconds(30);
        when(businessProperties.getReservation()).thenReturn(reservation);
        
        scheduler = new ScheduledReservationReleaser(releaseExpiredReservationsUseCase, businessProperties);
    }
    @Test
    @DisplayName("Should release expired reservations successfully")
    void shouldReleaseExpiredReservationsSuccessfully() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(5));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
    @Test
    @DisplayName("Should handle case when no expired reservations found")
    void shouldHandleCaseWhenNoExpiredReservations() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(0));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
    @Test
    @DisplayName("Should handle multiple expired reservations")
    void shouldHandleMultipleExpiredReservations() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(25));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
    @Test
    @DisplayName("Should handle errors gracefully without propagating")
    void shouldHandleErrorsGracefully() {
        when(releaseExpiredReservationsUseCase.execute())
            .thenReturn(Mono.error(new RuntimeException("Database connection failed")));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
    @Test
    @DisplayName("Should continue execution after error")
    void shouldContinueExecutionAfterError() {
        when(releaseExpiredReservationsUseCase.execute())
            .thenReturn(Mono.error(new RuntimeException("Temporary error")))
            .thenReturn(Mono.just(3));
        scheduler.releaseExpiredReservations();
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(2)).execute();
    }
    @Test
    @DisplayName("Should block and wait for reactive execution to complete")
    void shouldBlockAndWaitForCompletion() {
        when(releaseExpiredReservationsUseCase.execute())
            .thenReturn(Mono.just(10).delayElement(java.time.Duration.ofMillis(100)));
        long startTime = System.currentTimeMillis();
        scheduler.releaseExpiredReservations();
        long endTime = System.currentTimeMillis();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
        assert (endTime - startTime) >= 90;
    }
    @Test
    @DisplayName("Should handle large number of released reservations")
    void shouldHandleLargeNumberOfReleasedReservations() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(1000));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
    @Test
    @DisplayName("Should invoke use case exactly once per execution")
    void shouldInvokeUseCaseExactlyOnce() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(3));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
        verifyNoMoreInteractions(releaseExpiredReservationsUseCase);
    }
    @Test
    @DisplayName("Should handle long-running operations with timeout")
    void shouldHandleTimeout() {
        when(releaseExpiredReservationsUseCase.execute())
            .thenReturn(Mono.just(5).delayElement(java.time.Duration.ofMillis(100)));
        long startTime = System.currentTimeMillis();
        scheduler.releaseExpiredReservations();
        long endTime = System.currentTimeMillis();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
        long duration = endTime - startTime;
        assert duration < 30000; 
    }
    @Test
    @DisplayName("Should be callable multiple times independently")
    void shouldBeCallableMultipleTimes() {
        when(releaseExpiredReservationsUseCase.execute())
            .thenReturn(Mono.just(2))
            .thenReturn(Mono.just(0))
            .thenReturn(Mono.just(5));
        scheduler.releaseExpiredReservations();
        scheduler.releaseExpiredReservations();
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(3)).execute();
    }
    @Test
    @DisplayName("Should handle null or edge case returns gracefully")
    void shouldHandleEdgeCases() {
        when(releaseExpiredReservationsUseCase.execute()).thenReturn(Mono.just(0));
        scheduler.releaseExpiredReservations();
        verify(releaseExpiredReservationsUseCase, times(1)).execute();
    }
}