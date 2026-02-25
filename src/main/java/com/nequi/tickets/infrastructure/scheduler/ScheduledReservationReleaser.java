package com.nequi.tickets.infrastructure.scheduler;

import com.nequi.tickets.config.BusinessProperties;
import com.nequi.tickets.usecase.ReleaseExpiredReservationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class ScheduledReservationReleaser {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledReservationReleaser.class);
    
    private final ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase;
    private final BusinessProperties businessProperties;
    
    public ScheduledReservationReleaser(
            ReleaseExpiredReservationsUseCase releaseExpiredReservationsUseCase,
            BusinessProperties businessProperties) {
        this.releaseExpiredReservationsUseCase = releaseExpiredReservationsUseCase;
        this.businessProperties = businessProperties;
    }
    
    @Scheduled(
        initialDelayString = "${business.reservation.release-check-interval-ms:60000}",
        fixedRateString = "${business.reservation.release-check-interval-ms:60000}"
    )
    public void releaseExpiredReservations() {
        logger.info("⏰ Starting scheduled task: Release expired reservations");
        
        long startTime = System.currentTimeMillis();
        
        releaseExpiredReservationsUseCase.execute()
            .doOnSuccess(count -> {
                long duration = System.currentTimeMillis() - startTime;
                if (count > 0) {
                    logger.info("✅ Released {} expired reservations in {}ms", count, duration);
                } else {
                    logger.info("ℹ️  No expired reservations found. Duration: {}ms", duration);
                }
            })
            .doOnError(error -> 
                logger.error("Error releasing expired reservations", error))
            .onErrorResume(error -> {
                return Mono.just(0);
            })
            .timeout(Duration.ofSeconds(businessProperties.getReservation().getReleaseTimeoutSeconds()))
            .block();
        
        logger.debug("Completed scheduled task: Release expired reservations");
    }
}
