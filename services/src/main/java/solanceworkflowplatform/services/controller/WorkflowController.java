package solanceworkflowplatform.services.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import solanceworkflowplatform.services.model.*;
import solanceworkflowplatform.services.service.WorkflowService;


import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WorkflowController {
    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, String>>> register(
            @RequestBody RegisterRequest req) {
        return service.submit(RegisterRequest.EVENT_TYPE, req)
                .map(id -> ResponseEntity.ok(Map.of("eventId", id)));
    }

    @PostMapping("/open-account")
    public Mono<ResponseEntity<Map<String, String>>> openAccount(
            @RequestBody OpenAccountRequest req) {
        return service.submit(OpenAccountRequest.EVENT_TYPE, req)
                .map(id -> ResponseEntity.ok(Map.of("eventId", id)));
    }

    @PostMapping("/deposit")
    public Mono<ResponseEntity<Map<String, String>>> deposit(
            @RequestBody DepositRequest req) {
        return service.submit(DepositRequest.EVENT_TYPE, req)
                .map(id -> ResponseEntity.ok(Map.of("eventId", id)));
    }

    @PostMapping("/payout")
    public Mono<ResponseEntity<Map<String, String>>> payout(
            @RequestBody PaymentInstructionRequest req) {
        return service.submit(PaymentInstructionRequest.EVENT_TYPE, req)
                .map(id -> ResponseEntity.ok(Map.of("eventId", id)));
    }
    @GetMapping("/events")
    public Flux<WorkflowEventRecord> listEvents() {
        return service.listEvents();
    }
}
