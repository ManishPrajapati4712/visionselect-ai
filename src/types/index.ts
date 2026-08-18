export type Role = "coach" | "selector" | "player";

export type MetricKey = "batting" | "bowling" | "fielding" | "fitness" | "consistency";

export interface PlayerMetrics {
  batting: number;
  bowling: number;
  fielding: number;
  fitness: number;
  consistency: number;
}

export interface Player {
  id: string;
  name: string;
  age: number;
  role: string;
  region: string;
  team: string;
  battingStyle: string;
  bowlingStyle: string;
  overallScore: number;
  confidence: number;
  metrics: PlayerMetrics;
  matches: number;
  lastAnalyzed: string;
  trend: { label: string; score: number }[];
  strengths: string[];
  weaknesses: string[];
  initials: string;
}

export interface EvidenceEntry {
  id: string;
  timestamp: string;
  seconds: number;
  title: string;
  detail: string;
  metric: MetricKey;
  impact: number;
  confidence: number;
}

export interface MetricContribution {
  metric: MetricKey;
  label: string;
  weight: number;
  contribution: number;
  note: string;
}

export interface AnalysisStage {
  id: string;
  label: string;
  description: string;
  durationMs: number;
  geminiSnippet: string;
}

export interface TrainingItem {
  id: string;
  focus: string;
  drill: string;
  frequency: string;
  expectedGain: string;
  priority: "high" | "medium" | "low";
}

export interface FairnessFactor {
  label: string;
  description: string;
  included: boolean;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  streaming?: boolean;
}

export interface AnalysisRecord {
  id: string;
  playerId: string;
  playerName: string;
  venue: string;
  date: string;
  score: number;
  confidence: number;
  status: "complete" | "processing" | "review";
  duration: string;
}

export type FeedbackVerdict = "agree" | "review" | "disagree";

export interface FeedbackEntry {
  verdict: FeedbackVerdict;
  reason: string;
  comments?: string;
  priority?: "normal" | "high";
  submittedAt: string;
  playerId: string;
}

// ---------------------------------------------------------------------------
// Backend API types — mirror the Spring Boot DTOs exactly.
// Never add DB credentials or JWT secrets to these types.
// ---------------------------------------------------------------------------

/** Matches the backend ApiResponse<T> envelope. */
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  errors: string[] | null;
}

/** Authenticated user returned inside AuthResponse. */
export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  role: string; // "ADMIN" | "COACH" | "SELECTOR" | "PLAYER" — backend enum
}

/** POST /api/v1/auth/login request body. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** POST /api/v1/auth/register request body. */
export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
  role: string; // "COACH" | "SELECTOR" | "PLAYER"
}

/** POST /api/v1/auth/login and /register response data. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

/** POST /api/v1/auth/refresh request body. */
export interface RefreshRequest {
  refreshToken: string;
}

/** POST /api/v1/videos/upload-url request body. */
export interface UploadUrlRequest {
  filename: string;
  mimeType: string;
  fileSizeBytes: number;
  playerId?: string;
}

/** POST /api/v1/videos/upload-url response data. */
export interface UploadUrlResponse {
  storageKey: string;
  uploadUrl: string;
  method: string; // always "PUT"
  expiresAt: string;
  requiredHeaders: Record<string, string>;
}

/** POST /api/v1/videos request body (Step 2 — register completed upload). */
export interface VideoCreateRequest {
  storageKey: string;
  filename: string;
  mimeType: string;
  fileSizeBytes: number;
  playerId?: string;
}

/** POST /api/v1/videos response data. */
export interface VideoRecord {
  id: string;
  filename: string;
  fileSizeBytes: number;
  mimeType: string;
  storageKey: string;
  durationSeconds: number | null;
  status: 'PENDING_UPLOAD' | 'UPLOADED' | 'DELETED';
  uploadedBy: string;
  playerId: string | null;
  createdAt: string;
}