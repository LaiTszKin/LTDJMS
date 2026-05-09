# State Transition Patterns

## Idempotent Updates

Most state transitions use the pattern `UPDATE ... WHERE <current_state_condition> AND <transition_target_IS_NULL> RETURNING *`.

This ensures:
- Duplicate calls are safe (no-ops)
- Concurrent conflicting updates are prevented (first writer wins)
- The caller can detect whether the transition actually occurred

**Evidence**: See `JdbcFiatOrderRepository.java` — all `mark*` methods use this pattern.

## Claim/Release Locking

Background workers use a `processing_at` column for lightweight row-level locks:

```
worker claims row:
  UPDATE ... SET processing_at = ? WHERE processing_at IS NULL

if success: process (each step is idempotent)
if any step fails: release
  UPDATE ... SET processing_at = NULL WHERE id = ?
```

Three independent lock pairs:
- `claimFulfillmentProcessing` / `releaseFulfillmentProcessing` — post-payment worker
- `claimAdminNotificationProcessing` / `releaseAdminNotificationProcessing` — admin notifications
- `claimReconciliationProcessing` / `releaseReconciliationProcessing` — reconciliation worker

**Evidence**: See `JdbcFiatOrderRepository.java`, `FiatOrderPostPaymentWorker.java`, `FiatPaymentReconciliationService.java`

## Two-Phase Redemption

Redemption codes use an optimistic mark-then-grant pattern:

1. `markAsRedeemedIfAvailable()` — atomic DB update, returns false if already claimed
2. `grantReward()` — may fail (product deleted, overflow, DB error)
3. On grant failure: `clearRedeemedIfMatches()` — compensating transaction reverts the code

If the compensating revert fails, the code is stuck in a claimed-but-not-rewarded state with an explicit error message.

**Evidence**: See `RedemptionService.java`, `RedemptionCodeRepository.java`

## Escort Order Transitions

All escort order state transitions use atomic conditional updates:
- `assignEscort`: `WHERE escort_user_id = 0 AND status = 'PENDING_CONFIRMATION'`
- `claimAfterSales`: `WHERE status = 'AFTER_SALES_REQUESTED' AND after_sales_assignee_user_id IS NULL`
- `closeAfterSales`: `WHERE status = 'AFTER_SALES_IN_PROGRESS' AND after_sales_assignee_user_id = ?`

**Evidence**: See `JdbcEscortDispatchOrderRepository.java`, `EscortDispatchOrderService.java`

## Handoff Idempotency

Cross-module handoffs (shop → dispatch) check for existing orders before creating new ones:

`EscortDispatchHandoffService.handoff()` calls `findBySourceIdentity(sourceType, sourceReference)` first. If a dispatch order already exists for the purchase, it returns the existing order instead of creating a duplicate.

**Evidence**: See `EscortDispatchHandoffService.java`

## Fiat Order States

```
PENDING_PAYMENT → PAID      (via callback or reconciliation query)
PENDING_PAYMENT → EXPIRED   (via reconciliation expiry check)
PAID            → (fulfilled after post-payment pipeline)
```

All transitions are conditional: `markPaidIfPending` only fires from PENDING_PAYMENT, `markExpiredIfPending` likewise. Once PAID, the order progresses through independent idempotent steps (notification, reward, fulfillment) in the background worker.

**Evidence**: See `FiatOrder.java`, `JdbcFiatOrderRepository.java`
