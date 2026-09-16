import {
  confirmProposalState,
  createRoomState,
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

export function getRoom(code: string): Room | undefined {
  return rooms().get(code.toUpperCase());
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

export function joinRoom(code: string, participant: Participant): Room {
  const existing = getRoom(code);
  if (!existing) {
    throw new Error("ルームが見つかりません");
  }
  const updated = joinRoomState(existing, participant);
  rooms().set(updated.code, updated);
  return updated;
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
