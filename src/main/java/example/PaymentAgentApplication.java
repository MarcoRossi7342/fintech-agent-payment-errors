package example;

import java.time.Instant;
import java.util.UUID;

public class PaymentAgentApplication {
    public static void main(String[] args) {
        InfraiErrorTracker tracker = new InfraiErrorTracker(System.getenv("INFRAI_API_KEY"));
        PaymentAgentService service = new PaymentAgentService(new RiskPolicy(0.75), new AuditNotifier(), tracker);
        PaymentEvent event = new PaymentEvent("pay-1001", 4200L, "USD", 0.42, Instant.parse("2026-09-13T00:00:00Z"));
        Decision decision = service.handle(event);
        System.out.println(decision.status() + " " + decision.message());
    }
}

record PaymentEvent(String id, long amountMinor, String currency, double modelConfidence, Instant occurredAt) {}
record Decision(String status, String message) {}

class RiskPolicy {
    private final double threshold;
    RiskPolicy(double threshold) { this.threshold = threshold; }
    Decision decide(PaymentEvent event) {
        if (event.modelConfidence() < threshold) return new Decision("HOLD", "manual review required");
        return new Decision("APPROVE", "risk checks passed");
    }
}

class AuditNotifier {
    void notify(PaymentEvent event, Decision decision) {
        System.out.println("AUDIT payment=" + event.id() + " status=" + decision.status());
    }
}

class PaymentAgentService {
    private final RiskPolicy policy;
    private final AuditNotifier notifier;
    private final InfraiErrorTracker tracker;
    PaymentAgentService(RiskPolicy policy, AuditNotifier notifier, InfraiErrorTracker tracker) {
        this.policy = policy; this.notifier = notifier; this.tracker = tracker;
    }
    Decision handle(PaymentEvent event) {
        Decision decision = policy.decide(event);
        notifier.notify(event, decision);
        if ("HOLD".equals(decision.status())) {
            tracker.capture(event, new IllegalStateException("risk confidence below policy threshold"));
        }
        return decision;
    }
}

class InfraiErrorTracker {
    private final InfraiClient client;
    InfraiErrorTracker(String key) { this.client = new InfraiClient(key); }
    void capture(PaymentEvent event, Exception exception) {
        if (client.key().isBlank()) return;
        String payload = "{\"title\":\"payment agent held\",\"message\":\"" + exception.getMessage()
                + "\",\"level\":\"error\",\"fingerprint\":[\"payment-agent\",\"risk\"],"
                + "\"exception\":\"" + exception.getClass().getName() + "\",\"context\":{\"payment_id\":\""
                + event.id() + "\",\"amount_minor\":" + event.amountMinor() + "}}";
        client.post("/v1/errors/capture", payload, UUID.nameUUIDFromBytes(event.id().getBytes()).toString());
    }
}
