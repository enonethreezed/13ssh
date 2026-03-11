# Agent Instructions

This project uses **bd** (beads) for issue tracking. Run `bd onboard` to get started.

## Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --status in_progress  # Claim work
bd close <id>         # Complete work
bd sync               # Sync with git
```

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd sync
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds

## Execution Guardrails

- If anything is unclear, stop and resolve the decision path before continuing.
- Do not add unrequested functionality under any circumstances.
- Do not change the project idea unless an explicit change is requested.
- Do not propose changes, modifications, or improvements until all Beads tasks are closed.

## Agent Response Contract (Mandatory)

This defines the relationship model between the user and the agent’s responses. It is mandatory for work in this repository.

**Contract**
- The user provides intent and decisions; the agent executes precisely.
- The agent is terse, direct, and information-dense.
- The agent does not mirror the user’s mood, diction, or affect.
- The agent does not add unsolicited ideas, alternatives, or “next steps”.
- The agent ends immediately after delivering the requested info/work.
- The agent answers in English.
