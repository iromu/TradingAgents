---
title: "Shipping as Research"
status: "emerging"
authors: ["Nikola Balic (@nibzard)"]
based_on: ["AMP (Thorsten Ball, Quinn Slack)"]
category: "Learning & Adaptation"
source: "https://github.com/nibzard/awesome-agentic-patterns/blob/main/patterns/shipping-as-research.md"
tags: [research, experimentation, rapid-iteration, learning, shipping]
---

## Problem

In the rapidly evolving AI landscape, waiting for certainty before building means you're always behind. Traditional
product development emphasizes validation and certainty before release, but when the market changes every 3-6 weeks, you
can't afford to wait.

Research across major technology companies shows that 80-90% of product ideas fail to improve key metrics, even when
experts are confident they will work (Kohavi et al., 2007).

## Solution

**Treat shipping as research**: release features not because you're certain they'll work, but to learn whether they
work. Ship to figure out what works and doesn't work.

**Research mindset vs. Product mindset:**

| Product Mindset         | Research Mindset             |
|-------------------------|------------------------------|
| Ship when polished      | Ship to learn                |
| Validate before release | Release to validate          |
| Features must last      | Features may die in 3 months |
| Customer acquisition    | Customer learning            |

## Evidence

- **Evidence Grade:** `emerging`
- **Key Findings:**
    - AMP has ripped out multiple features that users loved (to-dos, forking, tabs, custom commands)
    - "People actually really like it when we remove stuff" — AMP users appreciate rapid iteration
    - Kohavi et al. (2007): 80-90% of product ideas fail to improve key metrics

## How to use it

**Principles for shipping as research:**

1. **Ship before you're certain** — as long as it doesn't break existing functionality and can be reversed
2. **Design for reversibility** — minimal dependencies, clean interfaces
3. **Communicate the experimental nature** — let users know they're part of the research
4. **Measure everything** — usage metrics, success/failure rates, user feedback
5. **Kill quickly** — when something isn't working, remove it

## Trade-offs

**Pros:** Speed, real-world validation, user goodwill, agility, innovation.
**Cons:** Wasted effort, user confusion, churn, brand risk, resource inefficiency.

## References

* [Raising an Agent Episode 10: The Assistant is Dead, Long Live the Factory](https://www.youtube.com/watch?v=4rx36wc9ugw) -
  AMP (Thorsten Ball, Quinn Slack, 2025)
* [Controlled Experiments on the Web](https://doi.org/10.1007/s10618-007-0061-3) - Kohavi, Henne, Sommerfield (2007)
*
Related: [Burn the Boats](../orchestration-control/burn-the-boats.md), [Disposable Scaffolding](../orchestration-control/disposable-scaffolding.md)
