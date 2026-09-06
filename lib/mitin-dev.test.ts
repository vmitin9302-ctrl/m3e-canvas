import { describe, expect, it } from "vitest";
import raw from "../prototypes/mitin-dev/mitin-dev.m3e.json";
import fixtures from "../prototypes/mitin-dev/demo-data.json";
import { isProject } from "./project";
import { initialPreviewState, patchPreviewState, previewItem, previewText } from "./preview-state";
import { actionsOf, groupsInFrame, type Doc } from "./tokens";

const doc = raw as Doc;
const items = doc.groups.flatMap(g => g.items);

describe("MITIN DEV native Canvas prototype", () => {
  it("opens as a native project with unique ids and no broken navigation targets", () => {
    expect(isProject(doc)).toBe(true);
    const ids = [...doc.frames.map(f => f.id), ...doc.groups.map(g => g.id), ...items.map(i => i.id)];
    expect(new Set(ids).size).toBe(ids.length);
    const targets = new Set([...doc.frames.map(f => f.id), "back"]);
    for (const item of items) for (const a of actionsOf(item)) expect(targets.has(a.action.to)).toBe(true);
    // Every authored screen is reachable through buttons/slots from the demo entry.
    const seen = new Set<string>();
    const visit = (id: string) => {
      if (seen.has(id) || id === "back") return;
      seen.add(id);
      const frame = doc.frames.find(f => f.id === id)!;
      for (const g of groupsInFrame(doc.groups, frame, doc.frames, {})) for (const item of g.items) for (const a of actionsOf(item)) visit(a.action.to);
    };
    visit("demo");
    expect([...doc.frames.filter(f => !seen.has(f.id))]).toEqual([]);
  });

  it("carries a changed budget and task from client to owner without mutating the saved design", () => {
    const original = JSON.stringify(doc);
    const state = patchPreviewState(initialPreviewState(doc), { budget_range: "40–70 тыс.", task: "Сайт для тестового сервиса" });
    const review = items.find(i => i.label === "Бюджет" && i.preview?.text)!;
    expect(previewItem(review, state).supporting).toBe("40–70 тыс.");
    const owner = items.find(i => i.label === "AI-бриф / задача")!;
    expect(previewItem(owner, state).supporting).toContain("Сайт для тестового сервиса");
    expect(JSON.stringify(doc)).toBe(original);
    expect(initialPreviewState(doc).budget_range).toBe("20–40 тыс.");
  });

  it("shares comments, messages and stage statuses using plain text, never executable expressions", () => {
    const state = patchPreviewState(initialPreviewState(doc), { feedback_input: "Кнопка записи", stage_2: "Ожидаем клиента" });
    const next = patchPreviewState(state, { demo_feedback: "{{feedback_input}}", demo_status: "Есть правки" });
    expect(previewText("{{demo_status}}: {{demo_feedback}}", next)).toBe("Есть правки: Кнопка записи");
    expect(next.stage_2).toBe("Ожидаем клиента");
    expect(previewText("<script>literal</script>", next)).toBe("<script>literal</script>");
    expect(patchPreviewState(next, JSON.parse('{"__proto__":"unsafe"}'))).toEqual(next);
  });

  it("keeps exactly the required budget ranges and derives the analytics cohort", () => {
    expect(fixtures.budget_ranges).toEqual(["до 10 000 ₽", "10–20 тыс.", "20–40 тыс.", "40–70 тыс.", "70 тыс.+", "пока не знаю"]);
    const budget = items.find(i => i.kind === "select" && i.preview?.key === "budget_range")!;
    expect(budget.tabs?.map(t => t.label)).toEqual(fixtures.budget_ranges);
    expect(fixtures.leads.filter(l => l.project_id).length / fixtures.leads.length).toBe(0.5);
    for (const p of fixtures.projects) expect(fixtures.leads.find(l => l.id === p.lead_id)?.project_id).toBe(p.id);
    expect(items.some(i => i.src)).toBe(false);
  });

  it("rejects malformed optional preview bindings on import", () => {
    const bad = structuredClone(doc);
    (bad.groups[0].items[0] as unknown as Record<string, unknown>).preview = { text: { label: 42 } };
    expect(isProject(bad)).toBe(false);
  });
});
