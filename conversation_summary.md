# Conversation Summary: Ecore IPFS Persistence Project

This document summarizes our discussions and decisions in this thread regarding the implementation and publication strategy of EMF Ecore IPFS persistence.

## 1. Research Questions Evolution
We started with three initial research questions from a previous thread and iteratively refined them to meet **Q1 Journal** and **MODELS Conference** standards.

The revised focus areas:
* **RQ1 (Architecture):** Architectural challenges of extending model persistence frameworks (EMF) to support content-addressed decentralized storage.
* **RQ2 (Cross-References):** Maintaining and resolving cross-resource references in an immutable environment.
* **RQ3 (Performance):** Quantifying the performance overhead and establishing a practicality threshold.

*(The full text of the refined RQs is saved in `research_questions.md`)*

## 2. Key Insights & Novel Contributions
* **The "Problem" is Actually a Feature:** We realized that content-addressed storage naturally solves the dependency explicitly. Since CIDs are immutable, a reference to a dependency is inherently "version-pinned" to the exact version it was originally tested against. It behaves exactly like modern lockfiles (npm, Go modules, Git).
* **Reference Management Strategies:** We identified 4 strategies for handling IPFS cross-references: 
  1. Direct CID referencing
  2. IPFS Directories (relative paths)
  3. IPNS indirection
  4. Manifest/Registry
* **Publication Strategy:** We determined that **RQ2 is the strongest, most novel contribution** for a premier conference like **MODELS**, while RQ1 and RQ3 are better suited to round out an extended **Q1 journal** submission (e.g., SoSyM).

## 3. Architecture Proposal: `cascadeSave()`
We discussed implementing an explicit API mechanism (or EMF save option) to manage reference upgrades intentionally:
* **`resource.save()`:** Saves only the current resource, returning its new CID without forcing referencing resources to update.
* **`resource.cascadeSave()`:** A "Git-like" tree save. It saves the resource, utilizes an EMF CrossReferencer to find every other resource in the `ResourceSet` that references it, updates those references to the new CID, and saves them recursively up the chain.

## 4. Unrelated Notes
* Discussed how to gain immediate access to Anthropic's Claude 3 Opus model (bypassing waiting lists) via Claude Pro, API tier prepayments, or third-party platforms like Poe and OpenRouter.
