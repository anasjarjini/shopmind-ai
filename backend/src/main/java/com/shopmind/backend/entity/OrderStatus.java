package com.shopmind.order;

public enum OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED, RETURNED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED || next == REFUNDED;
            case SHIPPED -> next == DELIVERED || next == REFUNDED;
            case DELIVERED -> next == RETURNED || next == REFUNDED;
            case CANCELLED, REFUNDED, RETURNED -> false;
        };
    }

    /** These statuses must say why (it becomes a training signal for the recommender). */
    public boolean requiresReason() {
        return this == CANCELLED || this == REFUNDED || this == RETURNED;
    }
}
