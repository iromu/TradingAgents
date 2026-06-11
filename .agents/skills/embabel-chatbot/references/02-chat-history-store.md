# Chat History Store

Source: [embabel/embabel-agent-docs/chatbots.md](https://github.com/embabel/embabel/blob/main/embabel-agent-docs/chatbots.md)

## Overview

`ChatHistoryStore` provides persistent storage for conversation history. The default `InMemoryChatHistoryStore` keeps
messages in memory only — they're lost when the process restarts. For persistence across restarts, implement a custom
store.

## ChatHistory

Represents a single conversation's full history.

```java
public record ChatHistory(
    String conversationId,  // Unique ID for this conversation
    List<ChatMessage> messages  // Ordered list of messages
) {}
```

## ChatMessage

```java
public record ChatMessage(
    String role,      // "system", "user", "assistant", or "tool"
    String content,   // The message content
    String name,      // Optional name (for tool calls)
    String id,        // Optional message ID
    Object toolCall   // Optional tool call data
) {}
```

## InMemoryChatHistoryStore

Default implementation. Stores messages in a `ConcurrentHashMap`.

```java
ChatHistoryStore store = new InMemoryChatHistoryStore();
```

## Custom Store Example

Implement `ChatHistoryStore` to persist to a database:

```java
public class PostgresChatHistoryStore implements ChatHistoryStore {
    private final JdbcTemplate jdbcTemplate;

    public PostgresChatHistoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ChatHistory chatHistory) {
        // Delete existing messages and insert new ones
        jdbcTemplate.update(
            "DELETE FROM chat_messages WHERE conversation_id = ?",
            chatHistory.conversationId()
        );
        for (ChatMessage msg : chatHistory.messages()) {
            jdbcTemplate.update(
                "INSERT INTO chat_messages (conversation_id, role, content, name, id, tool_call) VALUES (?, ?, ?, ?, ?, ?)",
                chatHistory.conversationId(),
                msg.role(),
                msg.content(),
                msg.name(),
                msg.id(),
                msg.toolCall() != null ? objectMapper.writeValueAsString(msg.toolCall()) : null
            );
        }
    }

    @Override
    public Optional<ChatHistory> load(String conversationId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT role, content, name, id, tool_call FROM chat_messages WHERE conversation_id = ? ORDER BY id",
            conversationId
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<ChatMessage> messages = rows.stream()
            .map(row -> new ChatMessage(
                (String) row.get("role"),
                (String) row.get("content"),
                (String) row.get("name"),
                (String) row.get("id"),
                row.get("tool_call") != null ? objectMapper.readValue((String) row.get("tool_call"), Map.class) : null
            ))
            .toList();
        return Optional.of(new ChatHistory(conversationId, messages));
    }

    @Override
    public boolean delete(String conversationId) {
        int rows = jdbcTemplate.update(
            "DELETE FROM chat_messages WHERE conversation_id = ?",
            conversationId
        );
        return rows > 0;
    }
}
```

## Schema

```sql
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT,
    name VARCHAR(255),
    tool_call JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_conversation ON chat_messages(conversation_id);
```
