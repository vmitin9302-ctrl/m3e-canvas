"use client";

import { useState } from "react";
import { Preview } from "@/components/Preview";
import { LangContext } from "@/lib/i18n";
import { ThemeContext } from "@/lib/theme";
import { buildPrompt } from "@/lib/prompt";
import { shareLink } from "@/lib/share";
import { saveProject } from "@/lib/project";
import { normalizeTheme, paletteOf, type Doc } from "@/lib/tokens";
import project from "@/prototypes/mitin-dev/mitin-dev.m3e.json";

const doc = project as Doc;
const theme = normalizeTheme(doc.theme);
const palette = paletteOf(doc.paletteKey, undefined, theme);
const base = process.env.NEXT_PUBLIC_BASE_PATH ?? "";

/** Local entry to the native Canvas Preview; no backend or persistence. */
export default function MitinDevPrototype() {
  const [preview, setPreview] = useState(false);
  const [prompt, setPrompt] = useState("");
  const [error, setError] = useState("");

  const openCanvas = async () => {
    try {
      window.location.assign(await shareLink(doc, `${window.location.origin}${base}/`));
    } catch {
      setError("Не удалось открыть ссылку. Сохраните JSON и откройте его через Open project в Canvas.");
    }
  };

  return (
    <LangContext.Provider value="en">
      <ThemeContext.Provider value={theme}>
        <main lang="ru" style={{ minHeight: "100dvh", background: palette.surface, color: palette.onSurface, padding: "clamp(24px, 5vw, 72px)", fontFamily: "system-ui, sans-serif" }}>
          <div style={{ maxWidth: 920, margin: "auto" }}>
            <p style={{ color: palette.primary, letterSpacing: 2, fontSize: 13, fontWeight: 700 }}>M3E CANVAS / ИНТЕРАКТИВНЫЙ ПРОТОТИП</p>
            <h1 style={{ fontSize: "clamp(40px, 7vw, 76px)", fontWeight: 750, letterSpacing: -3, margin: "32px 0 18px" }}>MITIN DEV</h1>
            <p style={{ fontSize: 24, maxWidth: 650, lineHeight: 1.5 }}>От первой идеи до работающего проекта. Клиент и владелец — две стороны одной системы.</p>
            <p style={{ color: palette.onSurfaceVariant, lineHeight: 1.7, maxWidth: 660, margin: "24px 0 36px" }}>52 связанных экрана в родном формате Canvas. Бриф, бюджет, статусы, сообщения и согласование демо работают в памяти Preview. Используйте вымышленные данные. Закрытие Preview сбрасывает введённые значения.</p>
            <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
              {[
                ["Открыть Preview", () => setPreview(true)],
                ["Редактировать в Canvas", openCanvas],
                ["Сохранить JSON", () => saveProject(doc)],
                ["Android prompt", () => setPrompt(buildPrompt(doc, {}, undefined, "en"))],
              ].map(([label, run], i) => <button key={label as string} onClick={run as () => void} style={{ minHeight: 56, padding: "0 24px", border: "none", borderRadius: 28, background: i === 0 ? palette.primary : palette.secondaryContainer, color: i === 0 ? palette.onPrimary : palette.onSecondaryContainer, cursor: "pointer", font: "inherit", fontWeight: 600 }}>{label as string}</button>)}
            </div>
            {error && <p role="alert">{error}</p>}
            {prompt && <section style={{ marginTop: 32 }}>
              <h2 style={{ fontSize: 24 }}>Android prompt из M3E Canvas</h2>
              <textarea aria-label="Android prompt" readOnly value={prompt} style={{ width: "100%", height: 360, marginTop: 16, padding: 20, borderRadius: 20, background: palette.surfaceContainer, color: palette.onSurface, border: `1px solid ${palette.outlineVariant}`, fontSize: 14 }} />
              <button onClick={async () => { try { await navigator.clipboard.writeText(prompt); } catch { setError("Выделите и скопируйте текст из поля вручную."); } }} style={{ marginTop: 12, padding: 16, borderRadius: 20, background: palette.primary, color: palette.onPrimary }}>Скопировать Android prompt</button>
            </section>}
            <p style={{ color: palette.onSurfaceVariant, marginTop: 48, fontSize: 14 }}>Клиентская часть приоритетна. Выбор роли находится только на отдельном demo-экране. Статистика рассчитана из вымышленной выборки на 06.09.2026.</p>
          </div>
        </main>
        {preview && <Preview doc={doc} widths={{}} palette={palette} startId="demo" onClose={() => setPreview(false)} />}
      </ThemeContext.Provider>
    </LangContext.Provider>
  );
}
