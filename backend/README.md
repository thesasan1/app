# Backend

NestJS + TypeScript API for the Personal OS (see `../docs/ARCHITECTURE.md` §4).

## Requirements

Node.js 22+ and npm.

## Commands

```bash
npm ci                 # install exact dependencies
npm run start:dev      # run with auto-restart (http://localhost:3000)
npm run build          # compile to dist/
npm start              # run compiled build
npm run lint           # ESLint (type-aware)
npm run format:check   # Prettier
npm run typecheck      # tsc --noEmit
npm test               # unit tests
npm run test:e2e       # HTTP e2e tests (supertest)
npm run openapi        # regenerate openapi/openapi.v1.json
npm run openapi:check  # fail if the committed spec is stale
```

Configuration comes from environment variables validated at startup (`src/config/env.ts`); copy `.env.example` for local values. Never commit `.env`.

## Endpoints (current)

| Method | Path               | Notes                                                           |
| ------ | ------------------ | --------------------------------------------------------------- |
| GET    | `/v1/health/live`  | Liveness                                                        |
| GET    | `/v1/health/ready` | Readiness (database and Redis checks arrive with those modules) |
| GET    | `/v1/docs`         | Swagger UI, disabled when `NODE_ENV=production`                 |

All errors are RFC 9457 `application/problem+json` with a stable `code`.
