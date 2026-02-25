package com.nequi.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "business")
public class BusinessProperties {

    private Reservation reservation = new Reservation();
    private Order order = new Order();

    public static class Reservation {
        private int timeoutMinutes = 10;
        private long releaseCheckIntervalMs = 60000;
        private int releaseTimeoutSeconds = 30;

        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }

        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }

        public long getReleaseCheckIntervalMs() {
            return releaseCheckIntervalMs;
        }

        public void setReleaseCheckIntervalMs(long releaseCheckIntervalMs) {
            this.releaseCheckIntervalMs = releaseCheckIntervalMs;
        }

        public int getReleaseTimeoutSeconds() {
            return releaseTimeoutSeconds;
        }

        public void setReleaseTimeoutSeconds(int releaseTimeoutSeconds) {
            this.releaseTimeoutSeconds = releaseTimeoutSeconds;
        }
    }

    public static class Order {
        private int maxRetries = 3;
        private int maxTicketsPerOrder = 10;

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public int getMaxTicketsPerOrder() {
            return maxTicketsPerOrder;
        }

        public void setMaxTicketsPerOrder(int maxTicketsPerOrder) {
            this.maxTicketsPerOrder = maxTicketsPerOrder;
        }
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
}
