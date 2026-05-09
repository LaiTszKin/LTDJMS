# AI Chat and Agent

## AI Chat

### Mention-Based Chat
Given the bot is mentioned in an allowed channel  
When a user sends a message mentioning the bot  
Then the bot processes the message through the AI service  
And responds in the same channel with a streaming message

### Channel Allowlist
Given the guild has configured allowed AI channels or categories  
When a user mentions the bot in a disallowed channel  
Then the bot silently ignores the message

### Category Allowlist Inheritance
Given a category is in the AI allowlist  
When a user mentions the bot in any channel within that category  
Then the bot responds as if the channel were explicitly allowed

### Thread Support
Given a thread is created from an allowed channel  
When a user mentions the bot in the thread  
Then the bot responds in the thread with thread-aware conversation context

### Streaming Response
Given the bot is processing an AI chat request  
When the AI service produces a response  
Then the response is streamed to the Discord channel in chunks  
And reasoning content is displayed when configured

### Markdown Validation
Given the AI produces a response with Markdown content  
When the markdown validation is enabled  
Then the response is validated for Discord-compatible Markdown  
And invalid formatting is automatically corrected

## AI Agent

### Agent Channel Configuration (Admin)
Given the user is a guild administrator  
When they enable agent mode for a channel in the admin panel  
Then the channel becomes agent-enabled

### Agent Mode Routing
Given a channel is agent-enabled  
When a user mentions the bot  
Then the request is routed to the agent path regardless of the AI allowlist

### Channel Management (Agent)
Given the agent is active in a channel  
When the user asks the agent to create, list, or delete channels  
Then the agent performs the requested channel operation

### Category Management (Agent)
Given the agent is active in a channel  
When the user asks the agent to create, list, or delete categories  
Then the agent performs the requested category operation

### Role Management (Agent)
Given the agent is active in a channel  
When the user asks the agent to create, list, or delete roles  
Then the agent performs the requested role operation

### Permission Management (Agent)
Given the agent is active in a channel  
When the user asks the agent to view or modify permissions for channels, categories, or roles  
Then the agent performs the requested permission operation

### Message Management (Agent)
Given the agent is active in a channel  
When the user asks the agent to send, edit, search, pin, or delete messages  
Then the agent performs the requested message operation

### Channel Movement (Agent)
Given the agent is active in a channel  
When the user asks the agent to move a channel to a different category  
Then the agent moves the channel

### Agent Authorization
Given a user without `ADMINISTRATOR` permission  
When they ask the agent to execute a tool  
Then the agent rejects the request with an authorization error

### Tool Execution Audit
Given the agent executes a tool  
When the execution completes  
Then a tool execution log is recorded with the tool name, parameters, and result status
