/**
 * videoService.ts
 *
 * Implements the two-step direct-to-storage upload flow from the backend contract.
 *
 * Backend contract (VideoController + VideoService):
 *
 *   Step 1 — POST /api/v1/videos/upload-url   [requires JWT, ADMIN or COACH role]
 *     Request:  { filename, mimeType, fileSizeBytes, playerId? }
 *     Response: ApiResponse<{ storageKey, uploadUrl, method:"PUT", expiresAt, requiredHeaders }>
 *
 *   Step 2 — PUT <uploadUrl> (the /local-storage/upload/{token} endpoint in dev)
 *     Body:    raw file bytes
 *     Headers: Content-Type: <mimeType> + any requiredHeaders from Step 1
 *     NOTE:    No Authorization header — the signed URL / token IS the credential.
 *
 *   Step 3 — POST /api/v1/videos              [requires JWT, ADMIN or COACH role]
 *     Request:  { storageKey, filename, mimeType, fileSizeBytes, playerId? }
 *     Response: ApiResponse<VideoRecord>  HTTP 201
 *
 * Allowed MIME types (from backend VideoService.ALLOWED_MIME_TYPES):
 *   - video/mp4
 *   - video/quicktime
 *
 * Max file size: 500 MB (524,288,000 bytes) — enforced by backend DTO @Max.
 * Enforced in this file before even calling Step 1.
 */

import { apiClient } from "@/services/apiClient";
import type {
  UploadUrlRequest,
  UploadUrlResponse,
  VideoCreateRequest,
  VideoRecord,
} from "@/types";

// ---------------------------------------------------------------------------
// Constants matching the backend contract
// ---------------------------------------------------------------------------

export const ALLOWED_MIME_TYPES = ["video/mp4", "video/quicktime"] as const;
export const MAX_FILE_SIZE_BYTES = 524_288_000; // 500 MB

export type AllowedMimeType = (typeof ALLOWED_MIME_TYPES)[number];

export class VideoUploadError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "VideoUploadError";
  }
}

// ---------------------------------------------------------------------------
// Step 1: Request a signed upload URL from the backend
// ---------------------------------------------------------------------------

export async function requestUploadUrl(
  payload: UploadUrlRequest,
): Promise<UploadUrlResponse> {
  return apiClient.post<UploadUrlResponse>(
    "/api/v1/videos/upload-url",
    payload,
    true, // JWT required
  );
}

// ---------------------------------------------------------------------------
// Step 2: PUT the file bytes directly to the storage URL
//         (local-storage shim in dev, S3/GCS in production)
// ---------------------------------------------------------------------------

export async function putFileToStorage(
  uploadUrl: string,
  file: File,
  requiredHeaders: Record<string, string>,
  onProgress?: (pct: number) => void,
): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", uploadUrl, true);

    // Apply MIME type and any headers the backend requires
    xhr.setRequestHeader("Content-Type", file.type);
    for (const [key, value] of Object.entries(requiredHeaders)) {
      xhr.setRequestHeader(key, value);
    }
    // No Authorization header — the uploadUrl/token is the credential

    if (onProgress) {
      xhr.upload.addEventListener("progress", (e) => {
        if (e.lengthComputable) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      });
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve();
      } else {
        reject(
          new VideoUploadError(
            `Storage PUT failed: HTTP ${xhr.status}. ` +
              "The upload URL may have expired. Please try again.",
          ),
        );
      }
    };

    xhr.onerror = () =>
      reject(new VideoUploadError("Network error while uploading the file."));

    xhr.send(file);
  });
}

// ---------------------------------------------------------------------------
// Step 3: Register the completed upload with the backend
// ---------------------------------------------------------------------------

export async function registerUpload(
  payload: VideoCreateRequest,
): Promise<VideoRecord> {
  return apiClient.post<VideoRecord>(
    "/api/v1/videos",
    payload,
    true, // JWT required
  );
}

// ---------------------------------------------------------------------------
// Composed flow: validate → get URL → PUT file → register
// Accepts an onProgress callback that reports 0-100% across the PUT phase.
// ---------------------------------------------------------------------------

export interface UploadOptions {
  file: File;
  playerId?: string;
  onProgress?: (pct: number) => void;
}

export async function uploadVideo({
  file,
  playerId,
  onProgress,
}: UploadOptions): Promise<VideoRecord> {
  // --- Pre-flight validation (match backend constraints) ---
  if (!ALLOWED_MIME_TYPES.includes(file.type as AllowedMimeType)) {
    throw new VideoUploadError(
      `Unsupported file type "${file.type}". ` +
        "Please upload an MP4 or QuickTime (.mov) file.",
    );
  }

  if (file.size > MAX_FILE_SIZE_BYTES) {
    const sizeMB = (file.size / 1_048_576).toFixed(0);
    throw new VideoUploadError(
      `File is ${sizeMB} MB. Maximum allowed size is 500 MB.`,
    );
  }

  // Step 1: get signed upload URL from the backend
  const urlResponse = await requestUploadUrl({
    filename: file.name,
    mimeType: file.type,
    fileSizeBytes: file.size,
    ...(playerId !== undefined ? { playerId } : {}),
  });

  // Step 2: PUT the raw bytes to the storage URL
  await putFileToStorage(
    urlResponse.uploadUrl,
    file,
    urlResponse.requiredHeaders,
    onProgress,
  );

  // Step 3: register the completed upload
  return registerUpload({
    storageKey: urlResponse.storageKey,
    filename: file.name,
    mimeType: file.type,
    fileSizeBytes: file.size,
    ...(playerId !== undefined ? { playerId } : {}),
  });
}
