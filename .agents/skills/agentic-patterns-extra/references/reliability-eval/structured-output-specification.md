---
title: Structured Output Specification
status: established
authors:
  - Nikola Balic (@nibzard)
based_on:
  - Vercel AI Team
category: Reliability & Eval
source: 'https://vercel.com/blog/what-we-learned-building-agents-at-vercel'
tags:
  - structured-output
  - schema
  - validation
  - reliability
  - type-safety
  - integration
slug: structured-output-specification
id: structured-output-specification
summary: >-
  Constrain agent outputs using deterministic schemas that enforce structured,
  machine-readable results, enabling reliable validation, parsing, and integration
  with downstream systems.
updated_at: '2026-01-05'
---

## Problem

Free-form agent outputs are difficult to validate, parse, and integrate with downstream systems. When agents return
unstructured text, you face:

- Unpredictable output formats requiring complex parsing
- Difficult validation and error handling
- Brittle integration with automated workflows
- Inconsistent categorization and classification
- Manual post-processing to extract structured data

This makes it nearly impossible to build reliable multi-step workflows where one agent's output feeds into another
system or agent.

## Solution

Constrain agent outputs using deterministic schemas that enforce structured, machine-readable results. Instead of
allowing free-form text responses, specify exact output formats using type systems, JSON schemas, or framework-specific
structured output APIs.

**Core approach:**

**Define explicit output schemas:**

- Use TypeScript interfaces, JSON Schema, or Pydantic models
- Specify required fields, types, and constraints
- Define enumerations for categorical outputs
- Document field semantics and validation rules

**Leverage framework structured output APIs:**

- OpenAI's structured outputs with JSON schema (constrained decoding)
- Anthropic's tool use for structured results
- Vercel AI SDK's `generateObject` function
- LangChain's output parsers
- LlamaIndex Pydantic programs
- Instructor retry wrapper

**Validate at generation time:**

- Framework ensures LLM adheres to schema
- Type errors caught before reaching application code
- Guaranteed parseable outputs

**Example implementation:**

```typescript
import { generateObject } from 'ai';
import { z } from 'zod';

// Define strict output schema
const LeadQualificationSchema = z.object({
  qualification: z.enum(['qualified', 'unqualified', 'needs_review']),
  confidence: z.number().min(0).max(1),
  companySize: z.enum(['enterprise', 'mid-market', 'smb', 'unknown']),
  estimatedBudget: z.string().optional(),
  nextSteps: z.array(z.string()),
  reasoning: z.string()
});

// Agent returns structured, validated output
const result = await generateObject({
  model: openai('gpt-4'),
  schema: LeadQualificationSchema,
  prompt: `Analyze this lead: ${leadData}`
});

// TypeScript knows exact structure
if (result.object.qualification === 'qualified') {
  await sendToSalesTeam(result.object);
}
```

**Integration benefits:**

```mermaid
graph LR
    A[Agent Input] --> B[LLM + Schema]
    B --> C[Validated Structured Output]
    C --> D[Downstream System]
    C --> E[Database Storage]
    C --> F[Next Agent Phase]

    style C fill:#90EE90
```

## How to use it

**When to apply:**

- Multi-phase agent workflows requiring structured handoffs
- Classification and categorization tasks
- Data extraction and transformation
- Integration with databases or APIs
- Compliance and audit requirements
- Quality assurance and validation

**Implementation steps:**

**1. Identify output requirements:**

- What decisions does the agent make?
- What data must be extracted?
- What downstream systems consume this output?

**2. Design schema:**

```python
from pydantic import BaseModel, Field
from typing import Literal

class AbuseAnalysis(BaseModel):
    content_type: Literal['spam', 'abuse', 'legitimate', 'unclear']
    severity: Literal['critical', 'high', 'medium', 'low']
    recommended_action: Literal['remove', 'warn', 'ignore', 'escalate']
    confidence_score: float = Field(ge=0, le=1)
    evidence: list[str]
    requires_human_review: bool
```

**3. Integrate with agent framework:**

```python
result = client.generate(
    model="gpt-4",
