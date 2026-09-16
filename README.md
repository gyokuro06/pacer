# pacer

Pair-programming Assisted Cognitive Energy & Rest — login-free shared work/break sessions.

## Room store (local vs Vercel)

Room state is cached in-process and written to a durable backend so cold starts / multiple instances can still find a room.

| Env | Behavior |
| --- | --- |
| *(unset)* | JSON file at `web/.data/rooms.json` (local / CI default) |
| `PACER_ROOM_STORE=file` | Same JSON file (optional `PACER_ROOM_STORE_PATH`) |
| `PACER_ROOM_STORE=memory` | Process heap only (no durable layer) |
| `PACER_ROOM_STORE=postgres` or `DATABASE_URL` set | Neon/Postgres via `@neondatabase/serverless` |

### Vercel

1. Create a Neon project and copy the connection string.
2. In the Vercel project: set `DATABASE_URL` (and optionally `PACER_ROOM_STORE=postgres`).
3. Deploy `web/` — the store creates `pacer_rooms` on first use.
