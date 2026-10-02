package example;

import java.time.Instant;

public class PaymentDecisionTest {
    public static void main(String[] args) {
        RiskPolicy policy = new RiskPolicy(0.75);
        PaymentEvent low = new PaymentEvent("test-low", 100, "USD", 0.2, Instant.EPOCH);
        PaymentEvent high = new PaymentEvent("test-high", 100, "USD", 0.9, Instant.EPOCH);
        check("HOLD".equals(policy.decide(low).status()), "low confidence must be held");
        check("APPROVE".equals(policy.decide(high).status()), "high confidence must be approved");
        System.out.println("PaymentDecisionTest passed");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
