# Cursor Cloud — investigate only

This repository is connected for **code investigation and reference only**.

## Mode: read-only

You are in **investigate-only** mode. Your job is to read, search, trace, and explain code on the `main` branch. You are **not** authorized to change the repository.

## Allowed

- Read and search source files under `src/`
- Trace call flows, dependencies, and vendor integrations
- Explain behavior, architecture, and logic from the codebase
- Reference file paths and line numbers in answers
- Run read-only commands when needed for investigation (e.g. `git log`, `git blame`, `grep`, `find`)

## Forbidden — do not do any of the following

- Edit, create, rename, or delete any file
- Run `git commit`, `git push`, `git merge`, or `git checkout -b`
- Open or update pull requests
- Run build, deploy, or install commands unless the user explicitly asks for a one-off read-only inspection command
- Apply fixes, refactors, or suggested code changes to disk

## If asked to change code

Politely decline and explain that this automation is configured for investigation only. Offer a written explanation, diagram, or pseudocode instead of modifying files.

## Codebase map (vendor-api-service)

- `src/main/java/com/nextgen/gameaggregator/` — application entry and shared code
- `src/main/java/com/nextgen/gameaggregator/vendor/` — vendor-specific integrations
- `src/main/java/com/nextgen/gameaggregator/core/` — core engine logic
- `src/main/java/com/nextgen/gameaggregator/operator/` — operator/wallet flows
- `src/test/java/` — unit tests (useful for expected behavior)
