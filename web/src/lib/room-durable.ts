import fs from "node:fs";
import path from "node:path";
import type { Room } from "./room";

export type RoomStoreKind = "memory" | "file" | "postgres";

export type DurableRoomStore = {
  get(code: string): Promise<Room | undefined>;
  set(room: Room): Promise<void>;
  delete(code: string): Promise<void>;
};

export function resolveRoomStoreKind(
  env: NodeJS.ProcessEnv = process.env,
): RoomStoreKind {
  const explicit = env.PACER_ROOM_STORE?.trim().toLowerCase();
  if (explicit === "memory" || explicit === "file" || explicit === "postgres") {
    return explicit;
  }
  if (env.DATABASE_URL) return "postgres";
  return "file";
}

function filePath(env: NodeJS.ProcessEnv = process.env): string {
  return (
    env.PACER_ROOM_STORE_PATH?.trim() ||
    path.join(process.cwd(), ".data", "rooms.json")
  );
}

function readFileStore(storePath: string): Record<string, Room> {
  try {
    const raw = fs.readFileSync(storePath, "utf8");
    const parsed = JSON.parse(raw) as unknown;
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    return parsed as Record<string, Room>;
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === "ENOENT") return {};
    throw error;
  }
}

function writeFileStore(storePath: string, rooms: Record<string, Room>): void {
  fs.mkdirSync(path.dirname(storePath), { recursive: true });
  fs.writeFileSync(storePath, JSON.stringify(rooms), "utf8");
}

function createMemoryDurable(): DurableRoomStore {
  return {
    async get() {
      return undefined;
    },
    async set() {},
    async delete() {},
  };
}

function createFileDurable(storePath: string): DurableRoomStore {
  return {
    async get(code) {
      const rooms = readFileStore(storePath);
      return rooms[code];
    },
    async set(room) {
      const rooms = readFileStore(storePath);
      rooms[room.code] = room;
      writeFileStore(storePath, rooms);
    },
    async delete(code) {
      const rooms = readFileStore(storePath);
      if (!(code in rooms)) return;
      delete rooms[code];
      writeFileStore(storePath, rooms);
    },
  };
}

type SqlFn = (
  strings: TemplateStringsArray,
  ...values: unknown[]
) => Promise<unknown>;

function createPostgresDurable(databaseUrl: string): DurableRoomStore {
  let sqlPromise: Promise<SqlFn> | undefined;
  let ready: Promise<void> | undefined;

  async function sql(): Promise<SqlFn> {
    if (!sqlPromise) {
      sqlPromise = import("@neondatabase/serverless").then(({ neon }) => {
        return neon(databaseUrl) as SqlFn;
      });
    }
    return sqlPromise;
  }

  async function ensureSchema(): Promise<void> {
    if (!ready) {
      ready = (async () => {
        const run = await sql();
        await run`
          CREATE TABLE IF NOT EXISTS pacer_rooms (
            code TEXT PRIMARY KEY,
            payload JSONB NOT NULL
          )
        `;
      })();
    }
    await ready;
  }

  return {
    async get(code) {
      await ensureSchema();
      const run = await sql();
      const rows = (await run`
        SELECT payload FROM pacer_rooms WHERE code = ${code} LIMIT 1
      `) as Array<{ payload: Room }>;
      return rows[0]?.payload;
    },
    async set(room) {
      await ensureSchema();
      const run = await sql();
      const payload = JSON.stringify(room);
      await run`
        INSERT INTO pacer_rooms (code, payload)
        VALUES (${room.code}, ${payload}::jsonb)
        ON CONFLICT (code) DO UPDATE SET payload = EXCLUDED.payload
      `;
    },
    async delete(code) {
      await ensureSchema();
      const run = await sql();
      await run`DELETE FROM pacer_rooms WHERE code = ${code}`;
    },
  };
}

const globalDurable = globalThis as typeof globalThis & {
  __pacerDurable?: DurableRoomStore;
  __pacerDurableKey?: string;
};

export function getDurableRoomStore(
  env: NodeJS.ProcessEnv = process.env,
): DurableRoomStore {
  const kind = resolveRoomStoreKind(env);
  const key =
    kind === "file"
      ? `file:${filePath(env)}`
      : kind === "postgres"
        ? `postgres:${env.DATABASE_URL ?? ""}`
        : "memory";

  if (globalDurable.__pacerDurable && globalDurable.__pacerDurableKey === key) {
    return globalDurable.__pacerDurable;
  }

  let store: DurableRoomStore;
  if (kind === "postgres") {
    const url = env.DATABASE_URL?.trim();
    if (!url) {
      throw new Error("PACER_ROOM_STORE=postgres requires DATABASE_URL");
    }
    store = createPostgresDurable(url);
  } else if (kind === "memory") {
    store = createMemoryDurable();
  } else {
    store = createFileDurable(filePath(env));
  }

  globalDurable.__pacerDurable = store;
  globalDurable.__pacerDurableKey = key;
  return store;
}
