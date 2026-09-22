package com.agenty.triage.agent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/* *
 * Where the original DevOps magic happens.
 * For now it utilizes "DUMB" logic classification 
 * via managing only preset 3 types of deterministic 
 * I/O. Will need to replace with a much more 
 * resilient approach soon.
 * */

@RestController
@RequestMapping("/api/v1/incidents")
public class TriageControl {

    private final MeterRegistry meterRegistry;
    private final Timer triageLatency;

    public TriageControl(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.triageLatency = Timer.builder("agenty.incident.triage.latency")
                .description("Latency incurred during incident payload classification")
                .publishPercentileHistogram()
                .register(meterRegistry);
    }

    @PostMapping("/triage")
    public ResponseEntity<TriageResult> triageIncident(@RequestBody IncidentPayload payload) {
        long startTime = System.nanoTime();

        String log = payload.logs() != null ? payload.logs().toUpperCase() : "";
        String severity;
        String category;
        String action;

        // Basically the logic of this application. Yes it's dumb.
        //
        if (log.contains("FATAL") || log.contains("OOM") || log.contains("PANIC")) {
            severity = "SEV-1";
            category = "INFRASTRUCTURE_FAILURE";
            action = "Trigger automated container restart and notify on-call SRE via PagerDuty.";
        } else if (log.contains("TIMEOUT") || log.contains("504") || log.contains("DEADLOCK")) {
            severity = "SEV-2";
            category = "DOWNSTREAM_DEGRADATION";
            action = "Scale upstream replicas and inspect downstream connection pools.";
        } else {
            severity = "SEV-3";
            category = "TRANSIENT_ANOMALY";
            action = "Route to service backlog; monitor error budget depletion rate.";
        }

        long durationNano = System.nanoTime() - startTime;
        triageLatency.record(durationNano, TimeUnit.NANOSECONDS);

        Counter.builder("agenty.incident.classified.total")
                .tag("service", payload.service() != null ? payload.service() : "unknown")
                .tag("severity", severity)
                .register(meterRegistry)
                .increment();

        TriageResult result = new TriageResult(
                severity,
                category,
                action,
                durationNano / 1_000_000.0);

        return ResponseEntity.ok(result);
    }
}
