import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import type { FeedbackEntry, Role } from "@/types";

interface AppState {
  role: Role;
  setRole: (r: Role) => void;
  activePlayerId: string;
  setActivePlayerId: (id: string) => void;
  hasUpload: boolean;
  markUploaded: (name: string) => void;
  uploadName: string | null;
  analysisComplete: boolean;
  markAnalysisComplete: () => void;
  feedback: FeedbackEntry | null;
  submitFeedback: (f: FeedbackEntry) => void;
}

const Ctx = createContext<AppState | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  const [role, setRole] = useState<Role>("coach");
  const [activePlayerId, setActivePlayerId] = useState("arjun-mehra");
  const [uploadName, setUploadName] = useState<string | null>(null);
  const [analysisComplete, setAnalysisComplete] = useState(false);
  const [feedback, setFeedback] = useState<FeedbackEntry | null>(null);

  const markUploaded = useCallback((name: string) => setUploadName(name), []);
  const markAnalysisComplete = useCallback(() => setAnalysisComplete(true), []);

  const value = useMemo(
    () => ({
      role,
      setRole,
      activePlayerId,
      setActivePlayerId,
      hasUpload: uploadName !== null,
      uploadName,
      markUploaded,
      analysisComplete,
      markAnalysisComplete,
      feedback,
      submitFeedback: setFeedback,
    }),
    [role, activePlayerId, uploadName, analysisComplete, feedback, markUploaded, markAnalysisComplete],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useApp() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useApp must be used inside AppProvider");
  return ctx;
}