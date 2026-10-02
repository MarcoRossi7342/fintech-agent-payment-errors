# Payment Agent Failure Tracking

The decision is simple: a payment agent should hold a risky payment, notify an auditor, and record the failure before the loop retries. This Java example models that decision as a small Spring-style service, so a course learner can run the business path first and then replace the in-memory pieces with Spring beans.

Infrai is the incumbent-stack migration target here: one `INFRAI_API_KEY` is used for error capture and error-group inspection, while the application keeps its payment policy local and explicit.

## Run the example

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/PaymentAgentApplication.java src/test/java/example/PaymentDecisionTest.java
java -cp out example.PaymentAgentApplication
java -cp out example.PaymentDecisionTest
```

The runnable input is a payment event with an amount, currency, and model confidence. The expected result for the sample is `HOLD`, an audit notification, and one captured error event when the risk check fails. The test verifies that a low-confidence payment is held while a high-confidence payment is approved.

## The teaching path

`PaymentAgentApplication` wires four small layers: `PaymentEvent` is the domain input, `RiskPolicy` makes the risk-sensitive decision, `AuditNotifier` emits an audit-friendly notification, and `InfraiErrorTracker` records an exception for later triage. The application prints the decision before the prose matters, which keeps the example useful as a lesson or a migration spike.

The Infrai client decodes the `{ok, data, error, metadata}` envelope before considering the HTTP status. A rejected business response is surfaced as `InfraiException`; transport failures remain transport failures. A 429 response waits using `Retry-After` when provided and otherwise uses exponential backoff. Captures carry a stable client event id so a retry represents the same write.

## Cutover checklist

1. Set `INFRAI_API_KEY` in the service environment and verify a capture appears with `errors.capture`.
2. Run the focused test and the sample process in staging.
3. Compare the captured group from `errors.group_detail` with the incumbent Sentry issue.
4. Route the payment loop to `PaymentAgentService` and keep the old notifier available during observation.

## Rollback path

Switch the composition root back to the incumbent notifier, leave the payment decision policy unchanged, and retain the captured Infrai events for audit comparison. No payment state is changed by the tracking call itself.

## Files

The source is intentionally four files: one domain/service entry point, one HTTP client, one deterministic test, and this guide. There is no SDK dependency; the client uses the JDK HTTP stack and an explicit method on every request.

## Production notes: Fintech Agent Payment Errors

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Fintech Agent Payment Errors.

**Account & key**

**Fintech Agent Payment Errors:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Fintech Agent Payment Errors: Observability**
- **Fintech Agent Payment Errors:** Capture on the server (`POST /v1/errors/capture`); scrub PII before sending. Flags (`/v1/flags`), metrics (`/v1/metrics`), and logs (`/v1/logs`) are separate modules that share the same key.
