export type Phase = "waiting" | "work" | "break";

export type ProposalKind = "break" | "work";

export type Participant = {
  id: string;
  displayName: string;
  emoji: string;
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

export const MIN_PARTICIPANTS_TO_START = 1;
export const MAX_PARTICIPANTS = 4;
export const DEFAULT_WORK_MINUTES = 60;
export const DEFAULT_BREAK_MINUTES = 10;
export const SESSION_REJOIN_TTL_HOURS = 24;
export const SESSION_REJOIN_TTL_MS = SESSION_REJOIN_TTL_HOURS * 60 * 60 * 1000;

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

export const AUTO_DISPLAY_NAMES = [
  "ねこぱんつ",
  "うどん侍",
  "もちもち太郎",
  "かりんとう姫",
  "ささみ騎士",
  "ぷりん将軍",
  "やきとり船長",
  "めんだこ博士",
] as const;

export const AUTO_EMOJIS = [
  "😀", "😃", "😄", "😁", "😆", "😊", "😇", "🙂",
  "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙",
  "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐",
  "🤓", "😎", "🥳", "🤩", "😏", "😒", "😞", "😔",
  "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
  "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔",
  "🐧", "🐦", "🐤", "🦆", "🦅", "🦉", "🦇", "🐺",
  "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍑",
  "🍒", "🍍", "🥝", "🥑", "🍅", "🍆", "🥕", "🌽",
  "🌶️", "🥒", "🥬", "🥦", "🧄", "🧅", "🍄", "🥜",
  "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🥏",
  "🎱", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "⛳",
] as const;

export function generateRoomCode(length = 6): string {
  let code = "";
  for (let i = 0; i < length; i += 1) {
    code += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  }
  return code;
}

export function generateAutoDisplayName(
  random = Math.random,
): string {
  const index = Math.floor(random() * AUTO_DISPLAY_NAMES.length);
  return AUTO_DISPLAY_NAMES[index] ?? AUTO_DISPLAY_NAMES[0];
}

export function generateAutoEmoji(
  random = Math.random,
): string {
  const index = Math.floor(random() * AUTO_EMOJIS.length);
  return AUTO_EMOJIS[index] ?? AUTO_EMOJIS[0];
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

export function requireRoomMember(
  room: Room,
  participantId: string | undefined | null,
): string {
  const id = typeof participantId === "string" ? participantId.trim() : "";
  if (!id) {
    throw new Error("参加者IDは必須です");
  }
  if (!room.participants.some((p) => p.id === id)) {
    throw new Error("参加者が見つかりません");
  }
  return id;
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
  creator: Participant,
  code = generateRoomCode(),
  now = Date.now(),
): Room {
  return {
    code,
    workMinutes,
    breakMinutes,
    participants: [creator],
    phase: "waiting",
    phaseEndsAt: null,
    pendingProposal: null,
    lastActivityAt: now,
  };
}

export function joinRoomState(
  room: Room,
  participant: Participant,
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
  return {
    room: {
      ...room,
      participants: [...room.participants, participant],
      lastActivityAt: now,
    },
    participantId: participant.id,
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

export function updateRoomMinutesState(
  room: Room,
  workMinutes: number,
  breakMinutes: number,
  now = Date.now(),
): Room {
  if (room.phase !== "waiting") {
    throw new Error("待機中のみ分数を変更できます");
  }
  if (!Number.isFinite(workMinutes) || workMinutes <= 0) {
    throw new Error("作業時間が不正です");
  }
  if (!Number.isFinite(breakMinutes) || breakMinutes <= 0) {
    throw new Error("休憩時間が不正です");
  }
  return {
    ...room,
    workMinutes,
    breakMinutes,
    lastActivityAt: now,
  };
}

export function updateDisplayNameState(
  room: Room,
  participantId: string,
  displayName: string,
  now = Date.now(),
): Room {
  const trimmed = displayName.trim();
  if (!trimmed) {
    throw new Error("表示名は必須です");
  }
  const index = room.participants.findIndex((p) => p.id === participantId);
  if (index < 0) {
    throw new Error("参加者が見つかりません");
  }
  const participants = room.participants.map((p, i) =>
    i === index ? { ...p, displayName: trimmed } : p,
  );
  return { ...room, participants, lastActivityAt: now };
}

export function updateEmojiState(
  room: Room,
  participantId: string,
  emoji: string,
  now = Date.now(),
): Room {
  const trimmed = emoji.trim();
  if (!trimmed) {
    throw new Error("絵文字は必須です");
  }
  const index = room.participants.findIndex((p) => p.id === participantId);
  if (index < 0) {
    throw new Error("参加者が見つかりません");
  }
  const participants = room.participants.map((p, i) =>
    i === index ? { ...p, emoji: trimmed } : p,
  );
  return { ...room, participants, lastActivityAt: now };
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
