# MITIN DEV — native M3E Canvas prototype

52 экрана, две роли, только вымышленные данные. Backend, БД, реальные аккаунты и уведомления отсутствуют.

## Открыть

```bash
npm ci
npm run dev
```

Откройте `http://localhost:3000/mitin-dev/`, нажмите **Открыть Preview**, выберите демо-роль.

**Редактировать в Canvas** открывает тот же документ в нативном редакторе с подтверждением импорта. Либо на главной странице M3E выберите **Open project** и файл `prototypes/mitin-dev/mitin-dev.m3e.json`. Нажмите **Preview** / `P`. Перед просмотром другого сценария выберите `DEMO · роли` в списке экранов панели Preview. Это сохраняет локальные значения в рамках одного открытого Preview; Close или перезагрузка сбрасывают их.

## Данные и воспроизведение

- `demo-data.json` — вымышленные заявки и проекты, источник чисел аналитики.
- `mitin-dev.m3e.json` — готовый нативный документ, включая notes, actions и опциональные preview bindings.
- `../../scripts/generate-mitin-dev.py` — воспроизводимая генерация документа. Запуск: `python scripts/generate-mitin-dev.py` из корня репозитория.
- `../../docs/MITIN_DEV_APP_ANDROID_PROMPT.md` — экспорт нативного генератора M3E, целевая платформа Android.
- `../../docs/MITIN_DEV_APP_PRODUCT_SPEC.md` — продукт и список экранов.
- `../../docs/MITIN_DEV_APP_ARCHITECTURE.md` — план одного backend и переиспользования CRM.
- `../../docs/MITIN_DEV_APP_VERIFICATION.md` — проверенные сценарии и ограничения.

Для повторного Android export нажмите **Android prompt** на странице прототипа. Это `buildPrompt(doc, {}, undefined, "en")` исходного M3E, с русскими подписями и английским обрамлением. В редакторе тот же генератор доступен в панели Prompt → Android. Prompt — техническое задание для следующего шага, не готовый APK и не разрешение подключать production.

## Что добавлено в Preview

Опциональное поле `Item.preview` сохраняется вместе с нативным Item:

- `key`, `initial`: строковое значение в памяти Preview; связанные select/textField редактируют его.
- `text.label`, `text.supporting`: подстановка `{{key}}` в отображаемый текст.
- `set`: строковые изменения по нажатию; поддерживает ту же простую подстановку.
- `when`: показать Item, если значение `key` равно `equals`.

Значения не отправляются и не сохраняются в дизайн. Нет JavaScript expressions, eval, HTML injection, network actions или доступа к storage. У исходного upstream без расширения переходы сохраняются, но связанные формы остаются статическими. Настройка bindings пока в JSON/генераторе, визуального редактора для них нет. Обычные документы без `preview` продолжают работать по старой модели.

## Проверка

```bash
npm run typecheck
npm test
npm run build
```

`npm run dev` использует тонкий wrapper Next.js для совместимости с флагами supervised QA. Обычный запуск на 3000 сохраняется. Никакой deploy из этих команд не следует; workflow публикации репозитория запускается только на main или вручную. Не merge и не запускать deploy workflow в этой задаче.
