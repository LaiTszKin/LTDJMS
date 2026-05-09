# Notifications

## Fiat Order Notifications

### Order Confirmation DM
Given a user creates a fiat order  
When the order is successfully created  
Then the user receives a direct message with the order number, CVS payment code, amount, and payment deadline

### Payment Success DM
Given a fiat order transitions to `PAID` status  
When the post-payment worker processes the order  
Then the buyer receives a direct message confirming payment success

### Admin Order Notification
Given a fiat order with escort auto-creation is paid  
When the post-payment worker processes the notification step  
Then all guild administrators receive a direct message with customer details and order information

## Escort Order Notifications

### Escort Assignment DM
Given an escort is assigned to a dispatch order  
When the assignment is confirmed  
Then the escort receives a direct message with a confirmation button

### Completion Request DM
Given a dispatch order is confirmed  
When the escort requests completion  
Then the customer receives a direct message with confirmation and after-sales buttons

### After-Sales Assignment DM
Given a dispatch order is in after-sales requested status  
When an after-sales staff member is available  
Then the staff member receives a direct message with a claim button

## Error Handling

### Notification Failure
Given a notification cannot be delivered (e.g., user has DMs disabled)  
When the system attempts to send the notification  
Then the failure is logged  
And the notification is not retried (best-effort delivery)
