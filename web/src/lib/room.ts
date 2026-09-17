export type Phase = "waiting" | "work" | "break";

export type ProposalKind = "break" | "work";

export type Participant = {
  id: string;
  displayName: string;
  emoji: string;
};

export type ParticipantInput = {
  id: string;
  displayName: string;
  emoji?: string;
};

export type Room = {
  code: string;
  workMinutes: number;
  breakMinutes: number;
  participants: Participant[];
  phase: Phase;
  phaseEndsAt: number | null;
  pendingProposal: ProposalKind | null;
  lastActivityAt: number;
};

export const MIN_PARTICIPANTS_TO_START = 2;
export const MAX_PARTICIPANTS = 4;
export const SESSION_REJOIN_TTL_HOURS = 24;
export const SESSION_REJOIN_TTL_MS = SESSION_REJOIN_TTL_HOURS * 60 * 60 * 1000;

export const PARTICIPANT_EMOJIS = ["🦊", "🐸", "🦉", "🐙"] as const;

export function pickUnusedEmoji(usedEmojis: readonly string[]): string {
  const used = new Set(usedEmojis);
  const next = PARTICIPANT_EMOJIS.find((emoji) => !used.has(emoji));
  if (!next) {
    throw new Error("利用可能な絵文字がありません");
  }
  return next;
}

function withAssignedEmoji(
  participant: ParticipantInput,
  usedEmojis: readonly string[],
): Participant {
  return {
    id: participant.id,
    displayName: participant.displayName,
    emoji: participant.emoji ?? pickUnusedEmoji(usedEmojis),
  };
}

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export function generateRoomCode(length = 6): string {
  let code = "";
  for (let i = 0; i < length; i += 1) {
    code += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  }
  return code;
}

export function formatRemainingMs(remainingMs: number): string {
  const clamped = Math.max(0, remainingMs);
  const totalSeconds = Math.ceil(clamped / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function remainingMs(room: Room, now = Date.now()): number | null {
  if (room.phaseEndsAt == null) return null;
  return room.phaseEndsAt - now;
}

export function phaseLabel(phase: Phase): string {
  switch (phase) {
    case "work":
      return "作業";
    case "break":
      return "休憩";
    default:
      return "待機";
  }
}

export function canStart(room: Room): boolean {
  return (
    room.phase === "waiting" &&
    room.participants.length >= MIN_PARTICIPANTS_TO_START
  );
}

export function isRoomExpired(
  room: Room,
  now = Date.now(),
  ttlMs = SESSION_REJOIN_TTL_MS,
): boolean {
  return now - room.lastActivityAt >= ttlMs;
}

export function createRoomState(
  workMinutes: number,
  breakMinutes: number,
  creator: ParticipantInput,
  code = generateRoomCode(),
  now = Date.now(),
): Room {
  return {
    code,
    workMinutes,
    breakMinutes,
    participants: [withAssignedEmoji(creator, [])],
    phase: "waiting",
    phaseEndsAt: null,
    pendingProposal: null,
    lastActivityAt: now,
  };
}

export function joinRoomState(
  room: Room,
  participant: ParticipantInput,
  now = Date.now(),
  ttlMs = SESSION_REJOIN_TTL_MS,
): { room: Room; participantId: string } {
  if (isRoomExpired(room, now, ttlMs)) {
    throw new Error("ルームが見つかりません");
  }
  const byId = room.participants.find((p) => p.id === participant.id);
  if (byId) {
    return { room: { ...room, lastActivityAt: now }, participantId: byId.id };
  }
  const byName = room.participants.find(
    (p) => p.displayName === participant.displayName,
  );
  if (byName) {
    return { room: { ...room, lastActivityAt: now }, participantId: byName.id };
  }
  if (room.participants.length >= MAX_PARTICIPANTS) {
    throw new Error("ルームは満員です");
  }
  const joined = withAssignedEmoji(
    participant,
    room.participants.map((p) => p.emoji),
  );
  return {
    room: {
      ...room,
      participants: [...room.participants, joined],
      lastActivityAt: now,
    },
    participantId: joined.id,
  };
}

export function startSessionState(room: Room, now = Date.now()): Room {
  if (!canStart(room)) {
    throw new Error("開始条件を満たしていません");
  }
  return {
    ...room,
    phase: "work",
    phaseEndsAt: now + room.workMinutes * 60_000,
    pendingProposal: null,
    lastActivityAt: now,
  };
}

export function proposeState(
  room: Room,
  kind: ProposalKind,
  now = Date.now(),
): Room {
  if (room.phase === "waiting") {
    throw new Error("セッション開始前は提案できません");
  }
  if (kind === "break" && room.phase !== "work") {
    throw new Error("作業中のみ休憩を提案できます");
  }
  if (kind === "work" && room.phase !== "break") {
    throw new Error("休憩中のみ作業再開を提案できます");
  }
  return { ...room, pendingProposal: kind, lastActivityAt: now };
}

export function confirmProposalState(room: Room, now = Date.now()): Room {
  if (room.pendingProposal == null) {
    throw new Error("確定する提案がありません");
  }
  if (room.pendingProposal === "break") {
    return {
      ...room,
      phase: "break",
      phaseEndsAt: now + room.breakMinutes * 60_000,
      pendingProposal: null,
      lastActivityAt: now,
    };
  }
  return {
    ...room,
    phase: "work",
    phaseEndsAt: now + room.workMinutes * 60_000,
    pendingProposal: null,
    lastActivityAt: now,
  };
}
