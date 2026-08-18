import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useRef, useState } from "react";
import { AlertCircle, CheckCircle2, FileVideo, Info, LogIn, ShieldCheck, UploadCloud } from "lucide-react";
import { AppLayout } from "@/layouts/AppLayout";
import { PageHeader } from "@/components/common/PageHeader";
import { GlassCard } from "@/components/common/GlassCard";
import { MagneticButton } from "@/components/common/MagneticButton";
import { useApp } from "@/context/AppContext";
import { useAuth } from "@/context/AuthContext";
import { uploadVideo, ALLOWED_MIME_TYPES, MAX_FILE_SIZE_BYTES, VideoUploadError } from "@/services/videoService";
import { ApiClientError } from "@/services/apiClient";
import { cn } from "@/lib/utils";
import { Link } from "@tanstack/react-router";
import type { VideoRecord } from "@/types";

export const Route = createFileRoute("/upload")({
  head: () => ({
    meta: [
      { title: "Upload match video — VisionSelect AI" },
      {
        name: "description",
        content:
          "Upload cricket match footage for pose tracking, event detection and an explainable AI evaluation.",
      },
      { property: "og:title", content: "Upload match video — VisionSelect AI" },
      {
        property: "og:description",
        content: "Drop in footage and watch the vision pipeline turn it into evidence.",
      },
    ],
  }),
  component: UploadPage,
});

type UploadState = "idle" | "uploading" | "ready" | "error";

function UploadPage() {
  const { markUploaded } = useApp();
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  const [dragging, setDragging] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const [state, setState] = useState<UploadState>("idle");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [videoRecord, setVideoRecord] = useState<VideoRecord | null>(null);

  // Backend constraints (matching VideoService and UploadUrlRequest DTO)
  const MAX_SIZE_MB = MAX_FILE_SIZE_BYTES / 1_048_576; // 500 MB
  const ACCEPT = ALLOWED_MIME_TYPES.join(", "); // "video/mp4, video/quicktime"

  const start = async (selectedFile: File) => {
    setFile(selectedFile);
    setState("uploading");
    setProgress(0);
    setErrorMessage(null);
    setVideoRecord(null);

    try {
      const record = await uploadVideo({
        file: selectedFile,
        onProgress: setProgress,
      });
      setVideoRecord(record);
      setState("ready");
      markUploaded(selectedFile.name);
    } catch (err) {
      setState("error");
      if (err instanceof VideoUploadError) {
        setErrorMessage(err.message);
      } else if (err instanceof ApiClientError && err.status === 401) {
        setErrorMessage("Your session has expired. Please sign in again to upload.");
      } else if (err instanceof ApiClientError && err.status === 403) {
        setErrorMessage(
          "You need a Coach or Admin account to upload videos. " +
          `Your current role is ${user?.role ?? "unknown"}.`
        );
      } else if (err instanceof ApiClientError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage("An unexpected error occurred. Please try again.");
      }
    }
  };

  const handleFileSelected = (selectedFile: File | undefined) => {
    if (!selectedFile) return;
    void start(selectedFile);
  };

  // --- Not authenticated: show sign-in prompt ---
  if (!isAuthenticated) {
    return (
      <AppLayout>
        <PageHeader
          eyebrow="Step 1 · Ingest"
          title="Upload match video"
          description="Sign in to your account to upload match footage for AI analysis."
        />
        <GlassCard className="mx-auto max-w-md p-10 text-center">
          <LogIn className="mx-auto h-10 w-10 text-cyan" />
          <h2 className="telemetry mt-4 text-xl text-foreground">Authentication required</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            You need to be signed in with a Coach or Admin account to upload videos.
          </p>
          <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
            <Link to="/login">
              <MagneticButton>Sign in</MagneticButton>
            </Link>
            <Link to="/register">
              <MagneticButton variant="outline">Create account</MagneticButton>
            </Link>
          </div>
        </GlassCard>
      </AppLayout>
    );
  }

  return (
    <AppLayout>
      <PageHeader
        eyebrow="Step 1 · Ingest"
        title="Upload match video"
        description={`MP4 or QuickTime (.mov), up to ${MAX_SIZE_MB} MB. Footage is uploaded directly to secure storage.`}
      />

      <div className="grid gap-6 lg:grid-cols-[1.5fr_1fr]">
        <GlassCard className="p-6">
          {/* Drop zone — hidden when a file is selected and processing */}
          {state === "idle" && (
            <div
              role="button"
              tabIndex={0}
              onClick={() => inputRef.current?.click()}
              onKeyDown={(e) => e.key === "Enter" && inputRef.current?.click()}
              onDragOver={(e) => {
                e.preventDefault();
                setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(e) => {
                e.preventDefault();
                setDragging(false);
                handleFileSelected(e.dataTransfer.files[0]);
              }}
              className={cn(
                "focus-ring grid cursor-pointer place-items-center rounded-2xl border-2 border-dashed px-6 py-20 text-center transition-colors",
                dragging
                  ? "border-primary bg-primary/10"
                  : "border-border bg-surface/40 hover:border-primary/50",
              )}
            >
              <input
                ref={inputRef}
                type="file"
                accept={ACCEPT}
                className="sr-only"
                onChange={(e) => handleFileSelected(e.target.files?.[0])}
              />
              <UploadCloud className="h-10 w-10 text-cyan" />
              <p className="telemetry mt-5 text-xl text-foreground">Drop match footage here</p>
              <p className="mt-2 max-w-sm text-sm text-muted-foreground">
                Or click to browse. Best results from a fixed side-on or behind-the-stumps camera
                at 1080p or higher.
              </p>
              <p className="mt-3 text-xs text-muted-foreground">
                MP4 or QuickTime · up to {MAX_SIZE_MB} MB
              </p>
            </div>
          )}

          {/* Progress / success / error card */}
          {file && state !== "idle" && (
            <div className="rounded-xl border border-border bg-surface/50 p-5">
              <div className="flex items-center gap-3">
                <FileVideo className="h-5 w-5 shrink-0 text-cyan" />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm text-foreground">{file.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {state === "uploading" && `Uploading… ${progress}%`}
                    {state === "ready" && "Upload complete · ready to analyse"}
                    {state === "error" && "Upload failed"}
                  </p>
                </div>
                {state === "uploading" && (
                  <span className="telemetry text-sm text-foreground">{progress}%</span>
                )}
                {state === "ready" && (
                  <CheckCircle2 className="h-5 w-5 shrink-0 text-success" />
                )}
                {state === "error" && (
                  <AlertCircle className="h-5 w-5 shrink-0 text-danger" />
                )}
              </div>

              {/* Progress bar */}
              {state === "uploading" && (
                <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-surface-2">
                  <div
                    className="h-full rounded-full bg-primary transition-[width] duration-200"
                    style={{ width: `${progress}%` }}
                  />
                </div>
              )}

              {/* Error message */}
              {state === "error" && errorMessage && (
                <div className="mt-4 rounded-lg border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
                  {errorMessage}
                </div>
              )}

              {/* Success actions */}
              {state === "ready" && (
                <div className="mt-5 flex flex-wrap items-center gap-3">
                  <MagneticButton onClick={() => void navigate({ to: "/analysis" })}>
                    Run AI analysis
                  </MagneticButton>
                  {videoRecord && (
                    <span className="flex items-center gap-1.5 text-xs text-success">
                      <CheckCircle2 className="h-3.5 w-3.5" />
                      Registered · ID {videoRecord.id.slice(0, 8)}…
                    </span>
                  )}
                </div>
              )}

              {/* Retry on error */}
              {state === "error" && (
                <div className="mt-4">
                  <MagneticButton
                    variant="outline"
                    onClick={() => {
                      setState("idle");
                      setFile(null);
                      setErrorMessage(null);
                    }}
                  >
                    Try again
                  </MagneticButton>
                </div>
              )}
            </div>
          )}
        </GlassCard>

        <div className="space-y-6">
          <GlassCard className="p-6">
            <ShieldCheck className="h-5 w-5 text-cyan" />
            <h2 className="telemetry mt-4 text-lg text-foreground">What we extract</h2>
            <ul className="mt-4 space-y-3 text-sm text-muted-foreground">
              {[
                "17-point pose skeleton per frame",
                "Bat and ball trajectory tracking",
                "Shot and delivery event segmentation",
                "Footwork, balance and timing windows",
              ].map((x) => (
                <li key={x} className="flex gap-2.5">
                  <span className="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-cyan" />
                  {x}
                </li>
              ))}
            </ul>
          </GlassCard>
          <GlassCard className="p-6">
            <Info className="h-5 w-5 text-warning" />
            <h2 className="telemetry mt-4 text-lg text-foreground">Never extracted</h2>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
              Face identity, skin tone, kit branding, crowd audio and commentary are all discarded
              before scoring. The fairness report documents every excluded signal.
            </p>
          </GlassCard>
        </div>
      </div>
    </AppLayout>
  );
}