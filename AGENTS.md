# Repository Guidelines

## Project Structure & Module Organization

This repository is a small Vite + React + TypeScript app with a TypeSpec API definition.

- `src/` contains the UI and client code, including `main.tsx`, `App.tsx`, and `api.ts`.
- `main.tsp` is the primary TypeSpec source.
- `scripts/` holds helper scripts such as `swagger-ui.mjs`.
- `tsp-output/` is generated output and should not be edited by hand.
- Root config files include `vite.config.ts`, `tsconfig*.json`, `Makefile`, and `package.json`.

## Build, Test, and Development Commands

- `npm run dev` starts the Vite dev server for local UI work.
- `npm run build` runs the TypeScript build and produces the production frontend bundle.
- `npm run preview` serves the built app locally for verification.
- `npm run api:build` compiles `main.tsp` into OpenAPI output under `tsp-output/schema/`.
- `make api-build` is a Make alias for the API generation step.
- `npm run api:swagger` or `make api-swagger` generates OpenAPI and launches Swagger UI.

There is no dedicated test runner configured yet. Use `npm run build` as the primary validation step.

## Coding Style & Naming Conventions

- Follow the existing TypeScript/React style: 2-space indentation, double quotes, semicolons, and trailing commas where already used.
- Keep components in `PascalCase` and helper functions/variables in `camelCase`.
- Prefer small, typed modules. Reuse shared API types from `src/api.ts` instead of duplicating shapes.
- The TypeScript config is strict, so fix unused variables, implicit `any`, and fallthrough issues before committing.

## Testing Guidelines

Automated tests are not currently set up. When adding tests, place them close to the code they cover and use clear names such as `App.test.tsx` or `api.test.ts`.

For now, validate changes by running the build and checking the generated API docs if TypeSpec changes were made.

## Commit & Pull Request Guidelines

Recent commits use Conventional Commits, for example `feat: add calendar booking TypeSpec API`.

- Keep commit subjects short and imperative.
- In pull requests, describe what changed, why it changed, and how you verified it.
- Include screenshots or short screen recordings for visible UI changes.
- Call out any generated output or API schema changes explicitly.

