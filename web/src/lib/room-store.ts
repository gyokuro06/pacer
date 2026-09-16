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

const globalStore = globalThis as typeof globalThis & {
  __pacerRooms?: Map<string, Room>;
};

function rooms(): Map<string, Room> {
  if (!globalStore.__pacerRooms) {
    globalStore.__pacerRooms = new Map();
  }
  return globalStore.__pacerRooms;
}

export function getRoom(code: string, now = Date.now()): Room | undefined {
  const room = rooms().get(code.toUpperCase());
  if (!room) return undefined;
  if (isRoomExpired(room, now)) {
    rooms().delete(room.code);
    return undefined;
  }
  return room;
}

export function createRoom(
  workMinutes: number,
  breakMinutes: number,
  creator: Participant,
): Room {
  const room = createRoomState(workMinutes, breakMinutes, creator);
  rooms().set(room.code, room);
  return room;
}

export function joinRoom(
  code: string,
  participant: Participant,
): { room: Room; participantId: string } {
  const existing = getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const result = joinRoomState(existing, participant);
  rooms().set(result.room.code, result.room);
  return result;
}

export function startRoom(code: string): Room {
  const existing = getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = startSessionState(existing);
  rooms().set(updated.code, updated);
  return updated;
}

export function propose(code: string, kind: ProposalKind): Room {
  const existing = getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = proposeState(existing, kind);
  rooms().set(updated.code, updated);
  return updated;
}

export function confirm(code: string): Room {
  const existing = getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = confirmProposalState(existing);
  rooms().set(updated.code, updated);
  return updated;
}

export function newParticipantId(): string {
  return crypto.randomUUID();
}
