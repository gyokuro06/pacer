import {
  confirmProposalState,
  createRoomState,
  isRoomExpired,
  joinRoomState,
  proposeState,
  startSessionState,
  type Participant,
  type ProposalKind,
  type Room,
} from "./room";
import { getDurableRoomStore } from "./room-durable";

const globalStore = globalThis as typeof globalThis & {
  __pacerRooms?: Map<string, Room>;
};

function rooms(): Map<string, Room> {
  if (!globalStore.__pacerRooms) {
    globalStore.__pacerRooms = new Map();
  }
  return globalStore.__pacerRooms;
}

async function persist(room: Room): Promise<void> {
  rooms().set(room.code, room);
  await getDurableRoomStore().set(room);
}

async function remove(code: string): Promise<void> {
  rooms().delete(code);
  await getDurableRoomStore().delete(code);
}

export async function getRoom(
  code: string,
  now = Date.now(),
): Promise<Room | undefined> {
  const key = code.toUpperCase();
  let room = rooms().get(key);
  if (!room) {
    room = await getDurableRoomStore().get(key);
    if (room) {
      rooms().set(room.code, room);
    }
  }
  if (!room) return undefined;
  if (isRoomExpired(room, now)) {
    await remove(room.code);
    return undefined;
  }
  return room;
}

export async function createRoom(
  workMinutes: number,
  breakMinutes: number,
  creator: Participant,
): Promise<Room> {
  const room = createRoomState(workMinutes, breakMinutes, creator);
  await persist(room);
  return room;
}

export async function joinRoom(
  code: string,
  participant: Participant,
): Promise<{ room: Room; participantId: string }> {
  const existing = await getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const result = joinRoomState(existing, participant);
  await persist(result.room);
  return result;
}

export async function startRoom(code: string): Promise<Room> {
  const existing = await getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = startSessionState(existing);
  await persist(updated);
  return updated;
}

export async function propose(code: string, kind: ProposalKind): Promise<Room> {
  const existing = await getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = proposeState(existing, kind);
  await persist(updated);
  return updated;
}

export async function confirm(code: string): Promise<Room> {
  const existing = await getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = confirmProposalState(existing);
  await persist(updated);
  return updated;
}

export function newParticipantId(): string {
  return crypto.randomUUID();
}
