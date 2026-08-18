import { players, getPlayer } from "@/data/players";
import { recentAnalyses, activityFeed, trainingPlan } from "@/data/analysis";
import type { Player, Role } from "@/types";

/** Placeholder data service — swap for REST calls to the Spring Boot API later. */

const delay = <T>(value: T, ms = 220) =>
  new Promise<T>((resolve) => setTimeout(() => resolve(value), ms));

export const listPlayers = () => delay(players);
export const fetchPlayer = (id: string) => delay(getPlayer(id));
export const listAnalyses = () => delay(recentAnalyses);
export const listActivity = () => delay(activityFeed);
export const fetchTrainingPlan = (_playerId: string) => delay(trainingPlan);

export function searchPlayers(query: string, roleFilter: string): Player[] {
  const q = query.trim().toLowerCase();
  return players.filter((p) => {
    const matchesQuery =
      !q ||
      p.name.toLowerCase().includes(q) ||
      p.team.toLowerCase().includes(q) ||
      p.role.toLowerCase().includes(q);
    const matchesRole = roleFilter === "all" || p.role.toLowerCase().includes(roleFilter);
    return matchesQuery && matchesRole;
  });
}

export function rankedPlayers(): Player[] {
  return [...players].sort((a, b) => b.overallScore - a.overallScore);
}

export const roleCopy: Record<Role, { title: string; blurb: string; focus: string[] }> = {
  coach: {
    title: "Coach",
    blurb: "Turn every session into a measurable development plan.",
    focus: ["Training plans", "Weakness drill-downs", "Session-over-session progress"],
  },
  selector: {
    title: "Selector",
    blurb: "Rank talent on evidence, and prove the call was fair.",
    focus: ["Cohort rankings", "Fairness audits", "Comparison verdicts"],
  },
  player: {
    title: "Player",
    blurb: "See exactly why your score is what it is — and what moves it.",
    focus: ["Personal progress", "Evidence timeline", "Next drills"],
  },
};