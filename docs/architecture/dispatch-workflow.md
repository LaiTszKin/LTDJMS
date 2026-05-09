# Dispatch Workflow

## Order State Machine

```
PENDING_CONFIRMATION
  -> confirmOrder (by assigned escort) -> CONFIRMED

CONFIRMED
  -> requestCompletion (by escort) -> PENDING_CUSTOMER_CONFIRMATION

PENDING_CUSTOMER_CONFIRMATION
  -> customerConfirmCompletion -> COMPLETED
  -> requestAfterSales (by customer) -> AFTER_SALES_REQUESTED
  -> timeout (24h) -> auto COMPLETED

COMPLETED
  -> requestAfterSales (by customer) -> AFTER_SALES_REQUESTED

AFTER_SALES_REQUESTED
  -> claimAfterSales (by staff) -> AFTER_SALES_IN_PROGRESS

AFTER_SALES_IN_PROGRESS
  -> closeAfterSales (by staff) -> AFTER_SALES_CLOSED
```

## Source Types

Orders originate from three paths:
- **MANUAL** — Created via `/dispatch-panel` by an administrator
- **CURRENCY_PURCHASE** — Auto-created when a currency-priced product with escort option is purchased
- **FIAT_PAYMENT** — Auto-created when a fiat payment with escort option completes

## Concurrency Controls

- `assignEscort`: atomic `UPDATE ... WHERE escort_user_id = 0 AND status = 'PENDING_CONFIRMATION'`
- `claimAfterSales`: atomic `UPDATE ... WHERE status = 'AFTER_SALES_REQUESTED' AND after_sales_assignee_user_id IS NULL`
- `closeAfterSales`: atomic `UPDATE ... WHERE status = 'AFTER_SALES_IN_PROGRESS' AND after_sales_assignee_user_id = ?`
- Handoff idempotency: source identity check before dispatch order creation

## Business Rules

- Escort user ID and customer user ID must be different
- Only the assigned escort can confirm or request completion
- Only the customer can confirm completion or request after-sales
- Customer confirmation has a 24-hour timeout; orders auto-complete after expiry
- After-sales claims use the first-to-claim pattern (not assignment)
- After-sales priority: online staff first, then all staff
