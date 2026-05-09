# Escort Dispatch

## Dispatch Order Management

### Create Dispatch Order (Admin)
Given the user is a guild administrator  
When they use `/dispatch-panel` and select a customer and escort option  
Then a dispatch order is created in `PENDING_CONFIRMATION` status

### Auto-Create Dispatch Order from Purchase
Given a user purchases a product with escort auto-creation enabled  
When the post-payment worker processes the order  
Then a dispatch order is automatically created  
And duplicate auto-creation for the same purchase is prevented

### Assign Escort (Admin)
Given a dispatch order exists with no escort assigned  
When an administrator assigns an escort via the dispatch panel  
Then the escort receives a direct message with a confirmation button

## Order Lifecycle

### Escort Confirms Order
Given an escort has been assigned to a dispatch order  
When they click the confirmation button in their direct message  
Then the order status transitions to `CONFIRMED`  
And the escort sees a completion request button

### Escort Requests Completion
Given a dispatch order is in `CONFIRMED` status  
When the escort clicks the completion button  
Then the order transitions to `PENDING_CUSTOMER_CONFIRMATION`  
And the customer receives a direct message with confirmation and after-sales request buttons

### Customer Confirms Completion
Given a dispatch order is in `PENDING_CUSTOMER_CONFIRMATION` status  
When the customer confirms completion  
Then the order transitions to `COMPLETED`  
And the escort is notified

### Auto-Complete on Timeout
Given a dispatch order is in `PENDING_CUSTOMER_CONFIRMATION` status  
And 24 hours have passed since completion was requested  
Then the order is automatically marked as `COMPLETED`

## After-Sales

### Request After-Sales
Given a dispatch order is in `COMPLETED` or `PENDING_CUSTOMER_CONFIRMATION` status  
When the customer clicks the after-sales request button  
Then the order transitions to `AFTER_SALES_REQUESTED`  
And online after-sales staff receive a claim notification

### Claim After-Sales Case
Given a dispatch order is in `AFTER_SALES_REQUESTED` status  
When an after-sales staff member clicks the claim button  
Then the case transitions to `AFTER_SALES_IN_PROGRESS`  
And the customer is notified that their case has been assigned

### Close After-Sales Case
Given a dispatch order is in `AFTER_SALES_IN_PROGRESS` status  
When the assigned after-sales staff member closes the case  
Then the order transitions to `AFTER_SALES_CLOSED`  
And the customer is notified

## Pricing and Staff Management

### Configure Escort Pricing (Admin)
Given the user is a guild administrator  
When they configure escort pricing in the admin panel  
Then guild-specific price overrides are applied for each escort option

### Manage After-Sales Staff (Admin)
Given the user is a guild administrator  
When they add or remove after-sales staff in the admin panel  
Then the staff list is updated

### Admin Notification on Purchase
Given a user purchases a product with escort auto-creation  
When the post-payment worker processes the order  
Then guild administrators with `ADMINISTRATOR` permission receive a direct message with customer and order details
