import type { Doc, Item } from "./tokens";

export type PreviewState = Record<string, string>;
const validKey = (key: string) => /^[a-z][a-z0-9_]{0,79}$/i.test(key) && !["constructor", "prototype", "__proto__"].includes(key);

/** Preview values live only for the lifetime of an open Preview. */
export function initialPreviewState(doc: Doc): PreviewState {
  const state: PreviewState = {};
  for (const group of doc.groups) for (const item of group.items) {
    const p = item.preview;
    if (p?.key && validKey(p.key) && !Object.hasOwn(state, p.key)) state[p.key] = p.initial ?? "";
  }
  return state;
}

/** Plain text interpolation only: no expressions, HTML, URLs or evaluation. */
export function previewText(text: string, state: PreviewState) {
  return text.replace(/\{\{([a-z][a-z0-9_]*)\}\}/gi, (_, key: string) => Object.hasOwn(state, key) ? state[key] : "");
}

export function patchPreviewState(state: PreviewState, patch: PreviewState): PreviewState {
  const next = { ...state };
  for (const [key, value] of Object.entries(patch)) {
    if (validKey(key) && typeof value === "string") next[key] = previewText(value, state).slice(0, 1000);
  }
  return next;
}

export function previewItem(item: Item, state: PreviewState): Item {
  const p = item.preview;
  if (!p) return item;
  return {
    ...item,
    ...(p.text?.label !== undefined ? { label: previewText(p.text.label, state) } : {}),
    ...(p.text?.supporting !== undefined ? { supporting: previewText(p.text.supporting, state) } : {}),
    ...(p.key && item.kind === "select" ? { selected: item.tabs?.findIndex(t => t.label === state[p.key!]) } : {}),
  };
}

export const previewVisible = (item: Item, state: PreviewState) => !item.preview?.when || state[item.preview.when.key] === item.preview.when.equals;
