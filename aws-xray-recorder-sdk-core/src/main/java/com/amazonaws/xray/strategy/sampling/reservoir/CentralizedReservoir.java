package com.amazonaws.xray.strategy.sampling.reservoir;

import com.amazonaws.services.xray.model.SamplingRule;
import com.amazonaws.services.xray.model.SamplingTargetDocument;

import java.time.Instant;

public class CentralizedReservoir {
    private static final long DEFAULT_INTERVAL = 10; // Seconds

    private long capacity;
    private long quota;
    private long used;

    private long currentEpoch;
    private long interval;
    private boolean borrow;
    private Instant refreshedAt;
    private Instant expiresAt;

    public CentralizedReservoir(long capacity) {
        this.capacity = capacity;
        this.expiresAt = Instant.EPOCH;
        this.refreshedAt = Instant.EPOCH;
        this.interval = DEFAULT_INTERVAL;
    }

    public void update(SamplingRule r) {
        capacity = r.getReservoirSize().longValue();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isBorrow(Instant now) {
        if (now.getEpochSecond() != currentEpoch) {
            reset(now);
        }
        boolean b = borrow;
        borrow = true;
        return !b && capacity != 0;
    }

    public boolean isStale(Instant now) {
        return now.isAfter(refreshedAt.plusSeconds(interval));
    }

    public void update(SamplingTargetDocument target, Instant now) {
        if (target.getReservoirQuota() != null) {
            quota = target.getReservoirQuota();
        }

        if (target.getReservoirQuotaTTL() != null) {
            expiresAt = target.getReservoirQuotaTTL().toInstant();
        }

        if (target.getInterval() != null) {
            interval = target.getInterval();
        }

        refreshedAt = now;
    }

    public boolean take(Instant now) {
        // We have moved to a new epoch. Reset reservoir.
        if (now.getEpochSecond() != currentEpoch) {
            reset(now);
        }

        if (quota > used) {
            used++;

            return true;
        }

        return false;
    }

    void reset(Instant now) {
        currentEpoch = now.getEpochSecond();
        used = 0;
        borrow = false;
    }

    public long getQuota() {
        return quota;
    }

    public long getUsed() {
        return used;
    }

    public long getCurrentEpoch() {
        return currentEpoch;
    }

    public long getInterval() {
        return interval;
    }

}
