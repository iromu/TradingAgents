---
title: "Tool Search Lazy Loading"
status: "emerging"
authors: ["Thariq (@trq212)"]
based_on: ["MCP Specification"]
category: "Context & Memory"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/tool-search-lazy-loading.md"
tags: [tool-discovery, lazy-loading, mcp, context-management, tool-search]
---

## Problem

As the Model Context Protocol (MCP) has grown, MCP servers may expose 50+ tools that consume significant context space.
Documented setups with 7+ servers have been documented consuming 67k+ tokens just for tool descriptions.

## Solution

Implement Tool Search: a lazy-loading mechanism where tools are dynamically loaded into context via search only when
needed, rather than preloaded on initialization.

**Core mechanics:**

1. **Threshold detection**: Monitor when tool descriptions would exceed a context threshold (e.g., 10% of context
   window)
2. **Search interface**: Provide a ToolSearchTool that allows agents to search tool metadata and selectively load tools
3. **Server instructions**: Leverage MCP server instruction fields to guide the agent on when to search for specific
   tools
4. **Agentic search**: Use intelligent search rather than basic RAG to find relevant tools

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
    - Dramatically reduces baseline context usage (67k+ tokens to just metadata)
    - Enables scaling to 100+ tools without context issues
    - Faster cold-start times when tools aren't needed

## How to use it

**For MCP server creators:**
- Enhance server instructions to help agents know when to search for your tools
- Include rich descriptions and tags to improve searchability
- Organize related tools to make discovery more intuitive

**For MCP client creators:**
- Implement ToolSearchTool for search-based discovery
- Use agentic search (not basic vector RAG)
- Set appropriate thresholds based on your use case (Claude Code uses 10%)
- Provide opt-out for users who prefer preloading

## Trade-offs

* **Pros:** Dramatically reduces baseline context usage, enables scaling to 100+ tools, faster cold-start times, better
  tool discovery.
* **Cons:** Adds latency when tools need to be dynamically loaded, requires search infrastructure, may miss
  serendipitous discovery, additional complexity.

## References

- [Original announcement tweet](https://x.com/trq212/status/2011523109871108570) by Thariq (@trq212)
- [MCP Documentation](https://modelcontextprotocol.io/)
