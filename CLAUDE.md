@AGENTS.md

# Claude Code specifics

- Subagents you start follow these same rules.
- Background waits: at most one at a time, with long blocks or a single until-loop. Never stack short polls.
