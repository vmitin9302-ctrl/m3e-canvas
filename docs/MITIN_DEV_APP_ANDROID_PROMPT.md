# MITIN DEV — Android prompt из M3E Canvas

Экспорт 06.09.2026, platform=android, language=en. Текст ниже получен нативным buildPrompt M3E Canvas из mitin-dev.m3e.json. Русские подписи сохранены. Это задание для прототипа, не разрешение подключать production или создавать миграции. Дополняющие требования: MITIN_DEV_APP_PRODUCT_SPEC.md и MITIN_DEV_APP_ARCHITECTURE.md.

---

Please implement MITIN DEV in the Material 3 Expressive design language. Russian-language client + owner mobile app prototype. Fictional data only. Client journey: interest, Lead, scripted AI brief, review, agreement, development, demo, launch, support. Both roles share one future MITIN DEV API / FastAPI backend and CRM; never create a second CRM. Prototype only: no API, credentials, accounts, notifications, payments, migrations or deployment. Role selection exists only on the developer/demo screen. Production role comes from backend authorization. Preserve budget_range. Use real stages, not invented progress percentages. Use local mock repositories for Android implementation; production integration requires a separate task.
Target a portrait phone screen (412×892dp), dark mode only.
Build it for Android, as a native app.
The layout below is a rough sketch that conveys intent, not a finished spec. Do not reproduce it as a static picture; build the complete, usable app that this kind of product is normally expected to be.

## Colors
The theme is Purple. Set these on the Material 3 dark color scheme and reference every UI color through its role.
- primary #D2BCFC / onPrimary #32226F / primaryContainer #4C3889 / onPrimaryContainer #E9DDFF
- secondaryContainer #4B425D / onSecondaryContainer #E9DDFD / tertiaryContainer #6C3644 / onTertiaryContainer #FDDAE1
- surface #141317 / surfaceContainerLow #1C1B1F / surfaceContainer #201F23 / surfaceContainerHigh #2B292D / surfaceContainerHighest #363438
- onSurface #E4E1E7 / onSurfaceVariant #C9C4D1 / outline #938F9B / outlineVariant #494550
- inverseSurface #E4E1E7 / inverseOnSurface #313034 / inversePrimary #6750A4
- error #F2B8B5 / onError #601410 / errorContainer #8C1D18 / onErrorContainer #F9DEDC

## Shape, type and motion
- Corners follow the M3 Expressive defaults (pill buttons, 20dp cards, 28dp dialogs).
- Use the device's system font as the typeface. Headlines, button labels and tabs use the M3 Expressive emphasized typography (the heavier headlineMediumEmphasized and similar styles).
- Motion uses MotionScheme.expressive(): a light spring bounce on transitions and state changes.

## Layout
There are 52 screens: "DEMO · роли", "CLIENT 01 · Добро пожаловать", "CLIENT · Демо-вход", "CLIENT 02 · Тип проекта", "CLIENT 03a · AI-бриф", "CLIENT 03b · Функции и бюджет", "CLIENT 04 · Проверка заявки", "CLIENT · Заявка принята", "CLIENT 05 · Главная", "CLIENT 06 · Мой проект", "CLIENT 07 · Этап проекта", "CLIENT 08 · Демо", "CLIENT · Макет демо", "CLIENT · Правки к демо", "CLIENT · Решение по демо", "CLIENT 09 · Материалы", "CLIENT · Добавление материалов", "CLIENT 10 · Стоимость", "CLIENT 11 · Сообщения", "CLIENT 12 · Поддержка", "CLIENT · Обращение", "CLIENT · Профиль", "CLIENT · Данные и доступ", "OWNER 01 · Dashboard", "OWNER 02 · Новые", "OWNER 02 · В работе", "OWNER 02 · Ожидание", "OWNER 02 · Закрытые", "OWNER 03 · Карточка заявки", "OWNER · Полное ТЗ", "OWNER · Статус заявки", "OWNER · Заявка 1043", "OWNER · Заявка 1044", "OWNER · Заявка 1042", "OWNER · Заявка 1041", "OWNER · Заявка 1040", "OWNER 04 · Проекты", "OWNER 04 · Ожидают клиента", "OWNER 05 · Карточка проекта", "OWNER 06 · Этапы", "OWNER · Проект из заявки", "OWNER · Этапы нового проекта", "OWNER · Студия «Линия»", "OWNER · Кабинет «Точка баланса»", "OWNER · Согласование демо", "OWNER · Материалы", "OWNER · Входящие", "OWNER · Чат проекта", "OWNER · Ещё", "OWNER · Поддержка", "OWNER 07 · Аналитика", "OWNER · Бюджеты".

Developer/demo-only entry. Role is not user-selectable in the production application. All names, projects, contacts and prices are fictional fixtures. Preview state resets when closed or reloaded.
The "DEMO · роли" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "MITIN DEV".
- Near the top, aligned left: bold text "ИНТЕРАКТИВНЫЙ ПРОТОТИП" at 13sp.
- Near the top, aligned left: bold text "Один проект." at 36sp.
- In the middle, aligned left: bold text "Две стороны." at 36sp.
- In the middle: a filled card (112dp tall) on primaryContainer with the headline "Клиент и владелец" and the body "Посмотрите путь от первой идеи до запуска и рабочую сторону MITIN DEV.".
- In the middle: a filled button "Я клиент" with a person icon (380dp wide).
- In the middle: a tonal button "Я владелец" with a business_center icon (380dp wide).
- In the middle: a filled card (112dp tall) with the headline "Только демонстрация" and the body "Вымышленные данные. Ввод хранится в памяти Preview. Выбор роли нужен только для просмотра сценариев.".

Client priority. Create project starts a Lead before a contract. Sign in uses a demo entry, no credentials and no account creation.
The "CLIENT 01 · Добро пожаловать" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "MITIN DEV".
- Near the top, aligned left: bold text "ОТ ИДЕИ ДО ЗАПУСКА" at 13sp.
- In the middle: a 380×228dp box (background primaryContainer, corner radius 28dp top / 28dp bottom).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - Near the top, aligned left: bold text "Ваш бизнес." at 38sp.
    - In the middle, aligned left: bold text "Следующий" at 38sp.
    - In the middle, aligned left: bold text "уровень." at 38sp.
- In the middle, aligned left: text "Сайты, боты и digital-системы" at 20sp.
- In the middle, aligned left: text "для бизнеса." at 20sp.
- In the middle: a filled card (108dp tall) with the headline "Всё о проекте — под рукой" and the body "Обсуждения, этапы, материалы и поддержка. Понятно, что происходит и какой следующий шаг.".
- In the middle: a filled button "Создать проект" with a add icon (380dp wide).
- Near the bottom: a tonal button "Войти" with a login icon (380dp wide).

Mock login only. Real app will obtain role from the authenticated backend. Do not add password or phone collection in this prototype.
The "CLIENT · Демо-вход" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Вход" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ПРОСМОТР КАБИНЕТА" at 13sp.
- Near the top, aligned left: bold text "Рады видеть вас" at 30sp.
- In the middle: a filled card (114dp tall) on primaryContainer with the headline "Алексей Смирнов" and the body "Вымышленный клиент. Активный проект уже согласован, договор заключён.".
- In the middle: a filled button "Открыть кабинет · демо" with a arrow_forward icon (380dp wide).

Choose project_type and continue to scripted AI brief. Budget keys must match existing Lead.budget_range; do not invent a second budget taxonomy.
The "CLIENT 02 · Тип проекта" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Новый проект" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ШАГ 1 / 3" at 13sp.
- Near the top, aligned left: bold text "Что хотите создать?" at 29sp.
- In the middle, in one row from left to right: a filled card (140dp tall) with a placeholder image (language icon) on top, the headline "Сайт" and the body "Ваш бизнес онлайн", a filled card (140dp tall) with a placeholder image (smart_toy icon) on top, the headline "Бот" and the body "Помощник в мессенджере" (keep them on the same line, vertically centered; never stack or wrap them; the "Бот" card stretches to fill the remaining width to the right edge).
- In the middle, in one row from left to right: a filled card (140dp tall) with a placeholder image (view_kanban icon) on top, the headline "CRM" and the body "Клиенты и задачи", a filled card (140dp tall) with a placeholder image (bolt icon) on top, the headline "Автоматизация" and the body "Меньше рутины" (keep them on the same line, vertically centered; never stack or wrap them; the "Автоматизация" card stretches to fill the remaining width to the right edge).
- In the middle, in one row from left to right: a filled card (140dp tall) with a placeholder image (auto_awesome icon) on top, the headline "AI" and the body "Умные инструменты", a filled card (140dp tall) with a placeholder image (lightbulb icon) on top, the headline "Своя идея" and the body "Обсудим вместе" (keep them on the same line, vertically centered; never stack or wrap them; the "Своя идея" card stretches to fill the remaining width to the right edge).
- Near the bottom, aligned left: text "Можно начать без готового ТЗ." at 15sp.

Scripted AI, no model requests. User enters a fictional business task. The response is carried into review and the owner Lead card.
The "CLIENT 03a · AI-бриф" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "MITIN DEV AI" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ШАГ 2 / 3 · ЗНАКОМСТВО" at 13sp.
- In the middle: a filled card (112dp tall) on primaryContainer with the headline "MITIN DEV AI" and the body "Расскажите своими словами, что хотите создать и какую задачу бизнеса это должно решить.".
- In the middle: a filled card (105dp tall) with the headline "Вы · пример" and the body "Нужен сайт для автосервиса, чтобы клиенты видели услуги и оставляли заявки.".
- In the middle: an outlined text field labeled "Ваша задача".
- In the middle: a filled card (96dp tall) with the headline "Подсказка" and the body "Для кого проект? Что сейчас неудобно? Какой результат вы хотите получить?".
- Near the bottom: a filled button "Продолжить бриф" with a arrow_forward icon (380dp wide).

Scripted second AI step. Functional scope, business goal, budget_range, deadline and integrations are carried to review. Estimates are not agreed prices.
The "CLIENT 03b · Функции и бюджет" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Уточним детали" with a arrow_back icon button on the left.
- Near the top: a filled card (94dp tall) on primaryContainer with the headline "MITIN DEV AI" and the body "Что должно работать в первой версии? Какой бюджет и срок вы рассматриваете?".
- In the middle: an outlined text field labeled "Основные функции".
- In the middle: an outlined dropdown labeled "Бюджет" that opens a menu to pick one option (options "до 10 000 ₽", "10–20 тыс.", "20–40 тыс.", "40–70 тыс.", "70 тыс.+", "пока не знаю", initially "20–40 тыс.").
- In the middle: an outlined text field labeled "Желаемый срок".
- In the middle: an outlined text field labeled "Интеграции".
- In the middle: an outlined text field labeled "Комментарии".
- Near the bottom: a filled button "Проверить заявку" with a fact_check icon (380dp wide).

Structured brief is editable through the previous wizard. The demo submit only navigates and changes local state. No network calls, account creation, notification or real Lead creation.
The "CLIENT 04 · Проверка заявки" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Проверить заявку" with a arrow_back icon button on the left.
- Near the top: "Тип проекта" with supporting text "Сайт", a leading language icon.
- In the middle: "Задача" with supporting text "Сайт для автосервиса: услуги и заявки", a leading flag icon.
- In the middle: "Основные функции" with supporting text "Услуги, каталог, форма заявки", a leading checklist icon.
- In the middle: "Бюджет" with supporting text "20–40 тыс.", a leading payments icon.
- In the middle: "Сроки" with supporting text "В течение трёх недель", a leading event icon.
- In the middle: "Интеграции" with supporting text "Уведомления в Telegram", a leading hub icon.
- In the middle: "Комментарии" with supporting text "Фотографии подготовлю позже", a leading chat icon.
- Near the bottom, in one row from left to right: a tonal button "Изменить" (188dp wide), a filled button "Отправить заявку" (188dp wide) (keep them on the same line, vertically centered; never stack or wrap them; the "Отправить заявку" button stretches to fill the remaining width to the right edge).

Demo-only accepted Lead #1045. A Lead is not automatically a signed project. The next button deliberately moves the reviewer to the existing signed-project fixture p001.
The "CLIENT · Заявка принята" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка отправлена" with a arrow_back icon button on the left.
- In the middle: a 380×206dp box (background primaryContainer, corner radius 28dp top / 28dp bottom).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - Near the top, aligned left: bold text "Заявка №1045" at 30sp.
    - In the middle, aligned left: bold text "принята" at 38sp.
- In the middle: a filled card (120dp tall) with the headline "Что дальше" and the body "MITIN DEV изучит задачу, уточнит условия и предложит следующий шаг. Стоимость и срок пока не согласованы.".
- In the middle: a filled card (122dp tall) with the headline "Следующая часть демо" and the body "Посмотрим кабинет с уже согласованным проектом. В реальном приложении он появится после договорённости.".
- Near the bottom: a filled button "Главная клиента · демо" with a arrow_forward icon (380dp wide).

Signed project p001 belongs to the fictional client Alexey. Show the real current stage and next action; no invented percentage. This is distinct from submitted Lead #1045.
The "CLIENT 05 · Главная" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "MITIN DEV".
- Near the top, aligned left: bold text "ДОБРЫЙ ДЕНЬ" at 13sp.
- Near the top, aligned left: bold text "Алексей" at 38sp.
- In the middle: a filled card (96dp tall) on primaryContainer with the headline "Сайт автосервиса" and the body "Разработка · проект в работе".
- In the middle: "Следующее действие" with supporting text "Согласовать мобильную версию", a leading task_alt icon, a trailing chevron_right icon.
- In the middle, aligned left: bold text "ВАШ ПРОЕКТ" at 13sp.
- In the middle: "Мой проект" with supporting text "Этапы и договорённости", a leading work icon, a trailing chevron_right icon.
- In the middle: "Материалы" with supporting text "Ожидаем фотографии работ", a leading folder icon, a trailing chevron_right icon.
- In the middle: "Поддержка" with supporting text "Поможем с вашим сайтом", a leading support_agent icon, a trailing chevron_right icon.
- Near the bottom: a tonal button "Сообщения" with a chat_bubble icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); the first one is selected.

Shared project p001. Values reflect the same seeded project as owner. No progress percentage. Stage codes map to not_started / in_progress / waiting_client / completed.
The "CLIENT 06 · Мой проект" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Мой проект" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Сайт автосервиса" at 28sp.
- Near the top, aligned left: text "Сайт · начало 01.09.2026" at 14sp.
- In the middle: a filled card (84dp tall) on primaryContainer with the headline "30 000 ₽ · разработка" and the body "Оплачено 15 000 ₽ · остаток 15 000 ₽".
- In the middle: "Текущий этап" with supporting text "Разработка", a leading code icon, a trailing chevron_right icon.
- In the middle, centered, in one row from left to right: text "ТЗ" at 16sp, text "Завершён" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle, centered, in one row from left to right: text "Дизайн" at 16sp, text "Завершён" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle, centered, in one row from left to right: text "Разработка" at 16sp, text "В работе" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle, centered, in one row from left to right: text "Проверка" at 16sp, text "Не начат" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle, centered, in one row from left to right: text "Запуск" at 16sp, text "Не начат" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle, centered, in one row from left to right: text "Сопровождение" at 16sp, text "Не начат" at 14sp (keep them on the same line, vertically centered; never stack or wrap them).
- In the middle: a tonal button "Подробнее об этапе" (380dp wide).
- Near the bottom: a filled button "Посмотреть демо" with a open_in_new icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Development stage detail. Completed tasks, current work, client dependency and next action. Stage state follows the same stage_2 binding as owner.
The "CLIENT 07 · Этап проекта" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Этап проекта" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Разработка" at 32sp.
- Near the top, aligned left: text "В работе" at 16sp.
- In the middle: a filled card (92dp tall) with the headline "Что сделано" and the body "Главная, каталог услуг и мобильная версия.".
- In the middle: a filled card (100dp tall) with the headline "Что сейчас делаем" and the body "Настраиваем форму заявки и проверяем сообщения об ошибках.".
- In the middle: a filled card (94dp tall) on primaryContainer with the headline "Что ждём от вас" and the body "Фотографии работ для раздела с примерами.".
- In the middle: "Следующее действие" with supporting text "Добавить материалы проекта", a leading upload_file icon, a trailing chevron_right icon.
- Near the bottom: a filled button "Открыть демо" with a open_in_new icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Demo link opens an internal illustrative screen, never an external production site. Approve and feedback affect only local Preview data shared with owner.
The "CLIENT 08 · Демо" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Демо проекта" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ГОТОВО К ПРОСМОТРУ" at 13sp.
- Near the top, aligned left: bold text "Демо готово" at 36sp.
- In the middle: a filled card (120dp tall) on primaryContainer with the headline "Мобильная версия" and the body "Проверьте расположение блоков, тексты и кнопки. Ваше решение увидим в карточке проекта.".
- In the middle: "Согласование" with supporting text "Ожидается решение", a leading fact_check icon.
- In the middle: a filled button "Открыть демо ↗" with a language icon (380dp wide).
- In the middle: a filled button "Одобрить" with a check icon (380dp wide).
- In the middle: a tonal button "Есть правки" with a edit_note icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Internal fictional demo destination. No external site or production URL is opened. This frame is only an illustration of the linked demo action.
The "CLIENT · Макет демо" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Автосервис · макет" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "АВТОСЕРВИС «ИМПУЛЬС»" at 13sp.
- In the middle: a 380×252dp box (background primaryContainer, corner radius 28dp top / 28dp bottom).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - Near the top, aligned left: bold text "Ваш автомобиль" at 30sp.
    - In the middle, aligned left: bold text "в надёжных руках" at 30sp.
    - In the middle, aligned left: text "Диагностика · ремонт · обслуживание" at 15sp.
- In the middle: a filled card (108dp tall) with the headline "Услуги сервиса" and the body "Техническое обслуживание, диагностика, ремонт ходовой части.".
- In the middle: a filled card (108dp tall) with the headline "Пример раздела работ" and the body "Здесь будут фотографии. Это внутренний макет для проверки перехода.".
- Near the bottom: a tonal button "Вернуться к согласованию" (380dp wide).

Editable comment kept only in Preview memory. Sending updates the shared demo_feedback binding; no message or notification leaves this browser.
The "CLIENT · Правки к демо" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Есть правки" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Что поправить?" at 30sp.
- In the middle: a filled card (110dp tall) with the headline "Лучше с примером" and the body "Укажите блок и желаемое изменение. Все записи здесь демонстрационные.".
- In the middle: an outlined text field labeled "Комментарий к демо".
- In the middle: a filled button "Сохранить комментарий" with a check icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Local confirmation only. Owner reads the same demo_status and demo_feedback keys.
The "CLIENT · Решение по демо" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Решение сохранено" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Спасибо!" at 38sp.
- In the middle: a filled card (96dp tall) on primaryContainer with the headline "Результат" and the body "Одобрено".
- In the middle: a filled card (128dp tall) with the headline "Комментарий" and the body "Пока без комментариев".
- In the middle: a tonal button "Вернуться к проекту" (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Only fixture metadata; no file input, file reading or upload. The add action marks a mock material as attached, shared with owner.
The "CLIENT 09 · Материалы" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Материалы" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ФАЙЛЫ ПРОЕКТА" at 13sp.
- Near the top, aligned left: bold text "Всё для работы" at 30sp.
- In the middle: "Логотип" with supporting text "Готово", a leading check_circle icon.
- In the middle: "Фотографии" with supporting text "12 файлов", a leading photo_library icon.
- In the middle: "Тексты" with supporting text "Готово", a leading description icon.
- In the middle: "Реквизиты" with supporting text "Ожидаются", a leading article icon.
- In the middle: "Новые материалы" with supporting text "Пока не добавлены", a leading attach_file icon.
- Near the bottom: a filled button "Добавить материалы" with a add icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Simulates a file attachment using predefined metadata. Does not open the device picker or read any real file.
The "CLIENT · Добавление материалов" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Добавить материалы" with a arrow_back icon button on the left.
- In the middle: a filled card (122dp tall) on primaryContainer with the headline "Фотографии работ" and the body "Демо-файл: work-example.jpg · 240 КБ. Содержимое не загружается.".
- In the middle: a filled button "Добавить пример файла" with a upload_file icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Fictional financial breakdown. Development, paid, remainder, third-party annual charges, infrastructure monthly charges and paid APIs stay separate. No checkout or payment actions.
The "CLIENT 10 · Стоимость" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Стоимость" with a arrow_back icon button on the left.
- Near the top: a 380×166dp box (background primaryContainer, corner radius 28dp top / 28dp bottom).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - Near the top, aligned left: bold text "РАЗРАБОТКА" at 13sp.
    - In the middle, aligned left: bold text "30 000 ₽" at 42sp.
    - Near the bottom, aligned left: text "Оплачено 15 000 ₽ · осталось 15 000 ₽" at 15sp.
- In the middle: "Сторонние расходы" with supporting text "Домен · 199 ₽ / год", a leading language icon.
- In the middle: "Инфраструктура ежемесячно" with supporting text "Хостинг · 500 ₽ / мес.", a leading dns icon.
- In the middle: "Платные API и сервисы" with supporting text "Нет", a leading api icon.
- In the middle: a filled card (112dp tall) with the headline "Условия проекта" and the body "Суммы демонстрационные. Разработка оплачивается отдельно от домена и инфраструктуры.".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Проект" is selected.

Simple project chat, not MAX/VK UI or a promised messenger proxy. One shared last-message slot per side, memory only. Production channel model remains to be decided.
The "CLIENT 11 · Сообщения" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Сообщения" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "САЙТ АВТОСЕРВИСА" at 13sp.
- In the middle: a filled card (108dp tall) on primaryContainer with the headline "MITIN DEV · 10:30" and the body "Мобильная версия готова. Посмотрите демо, когда будет удобно.".
- In the middle: a filled card (90dp tall) with the headline "Алексей · 10:35" and the body "Спасибо, посмотрю сегодня.".
- In the middle: a filled card (96dp tall) with the headline "MITIN DEV · новый ответ" and the body "Ждём ваши материалы.".
- In the middle: a filled card (90dp tall) with the headline "Вы · последнее сообщение" and the body "Фотографии подготовлю вечером.".
- In the middle: an outlined text field labeled "Сообщение по проекту".
- Near the bottom: a filled button "Отправить" with a send icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Сообщения" is selected.

Separate post-launch scenario for p001, demonstrating its future lifecycle; the current development fixture is not silently changed. Production badge is demo status, not a health check. Support is fictional until 30.09.2026.
The "CLIENT 12 · Поддержка" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Поддержка" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ПОСЛЕ ЗАПУСКА · ДЕМО" at 13sp.
- Near the top, aligned left: bold text "Мой сайт работает" at 28sp.
- In the middle: a filled card (92dp tall) on primaryContainer with the headline "Production" and the body "Пример состояния после запуска.".
- In the middle: a filled card (104dp tall) with the headline "Сопровождение активно" and the body "До 30 сентября 2026 · контроль доступности и помощь по работе сайта.".
- In the middle: a filled button "Сообщить о проблеме" with a report icon (380dp wide).
- In the middle: a tonal button "Заказать доработку" with a construction icon (380dp wide).
- In the middle: a outlined button "Новый проект" with a add icon (380dp wide).
- Near the bottom: "Последнее обращение" with supporting text "Обращений пока нет", a leading support_agent icon.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); the first one is selected.

Mock SupportRequest. No notification or ticket is created outside local memory.
The "CLIENT · Обращение" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Поддержка проекта" with a arrow_back icon button on the left.
- Near the top: "Тип обращения" with supporting text "Проблема", a leading support_agent icon.
- In the middle: an outlined text field labeled "Опишите задачу".
- In the middle: a filled button "Сохранить обращение" with a check icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); the first one is selected.

Fictional demo profile; no real personal data. Privacy/consent placeholders describe the future flow but are not legal documents. There is no in-product role toggle.
The "CLIENT · Профиль" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Профиль".
- Near the top, aligned left: bold text "Алексей Смирнов" at 28sp.
- Near the top, aligned left: text "Демонстрационный профиль" at 15sp.
- In the middle: a filled card (98dp tall) with the headline "Данные аккаунта" and the body "alexey@example.test · телефон не указан".
- In the middle: "Данные и доступ" with supporting text "Будущая privacy и настройки", a leading shield icon, a trailing chevron_right icon.
- In the middle: "Поддержка" with supporting text "Обратиться по проекту", a leading support_agent icon, a trailing chevron_right icon.
- In the middle: a outlined button "Выйти" with a logout icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Профиль" is selected.

Architecture placeholder only. This is not a privacy policy, consent or legal document. No legal/РКН content is modified.
The "CLIENT · Данные и доступ" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Данные и доступ" with a arrow_back icon button on the left.
- In the middle: a filled card (112dp tall) on primaryContainer with the headline "Только ваши проекты" and the body "После авторизации доступ определяет сервер. Чужие проекты и файлы недоступны.".
- In the middle: a filled card (126dp tall) with the headline "Перед реальным запуском" and the body "Здесь появятся утверждённая политика, необходимые согласия и действия с данными.".
- In the middle: a filled card (114dp tall) with the headline "Сейчас — прототип" and the body "Ввод в Preview остаётся в памяти до закрытия. Используйте только вымышленные данные.".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (home), "Проект" (work), "Сообщения" (chat_bubble), "Профиль" (person); "Профиль" is selected.

Owner-only production destination. Metrics are derived from the fictional cohort snapshot on 06.09.2026, not live analytics; no real CRM access.
The "OWNER 01 · Dashboard" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "MITIN DEV".
- Near the top, aligned left: bold text "РАБОЧИЙ ДЕНЬ" at 13sp.
- Near the top, aligned left: bold text "Всё под контролем" at 29sp.
- In the middle, in one row from left to right: a 184×120dp box (background primaryContainer, corner radius 28dp top / 28dp bottom), a 184×120dp box (background surfaceContainerLow, corner radius 28dp top / 28dp bottom) (keep them on the same line, vertically centered; never stack or wrap them; the box stretches to fill the remaining width to the right edge).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - In the middle, centered: bold text "1" at 38sp.
    - Near the bottom, centered: text "Новые заявки" at 14sp.
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - In the middle, centered: bold text "2" at 38sp.
    - Near the bottom, centered: text "Активные проекты" at 14sp.
- In the middle, in one row from left to right: a 184×120dp box (background surfaceContainerLow, corner radius 28dp top / 28dp bottom), a 184×120dp box (background surfaceContainerLow, corner radius 28dp top / 28dp bottom) (keep them on the same line, vertically centered; never stack or wrap them; the box stretches to fill the remaining width to the right edge).
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - In the middle, centered: bold text "2" at 38sp.
    - Near the bottom, centered: text "Ожидают клиента" at 14sp.
  - Inside the box, layered on top of it (the container is the background; positions are relative to it):
    - In the middle, centered: bold text "2" at 38sp.
    - Near the bottom, centered: text "Новые сообщения" at 14sp.
- In the middle, aligned left: bold text "ПОСЛЕДНЯЯ ЗАЯВКА" at 13sp.
- In the middle: "Алексей Смирнов · №1045" with supporting text "Сайт · 20–40 тыс. · приложение", a leading person icon, a trailing chevron_right icon.
- In the middle: a filled card (88dp tall) with the headline "Ближайшее действие" and the body "Автосервис: получить фотографии работ.".
- Near the bottom, aligned left: text "Срез demo data · 06.09.2026" at 12sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); the first one is selected.

Filter over seeded fictional Lead cohort. Each row opens its own Lead/project; no backend query. Snapshot filters are not recalculated by local status edits.
The "OWNER 02 · Новые" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявки" with a arrow_back icon button on the left.
- Near the top: a tab row with 4 tabs: "Новые", "В работе", "Ожидание", "Закрытые"; the first one is selected.
- In the middle: a filled card (126dp tall) with the headline "Алексей Смирнов · №1045" and the body "Сайт · 20–40 тыс. · Приложение · Новые · 06.09.2026".
- Near the bottom, aligned left: text "Демонстрационный список заявок" at 13sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Filter over seeded fictional Lead cohort. Each row opens its own Lead/project; no backend query. Snapshot filters are not recalculated by local status edits.
The "OWNER 02 · В работе" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявки" with a arrow_back icon button on the left.
- Near the top: a tab row with 4 tabs: "Новые", "В работе", "Ожидание", "Закрытые"; "В работе" is selected.
- In the middle: a filled card (126dp tall) with the headline "Алексей Смирнов · №1042" and the body "Сайт · 20–40 тыс. · Сайт · В работе · 01.09.2026".
- In the middle: a filled card (126dp tall) with the headline "Павел Ильин · №1041" and the body "Сайт · 20–40 тыс. · Рекомендация · В работе · 28.08.2026".
- Near the bottom, aligned left: text "Демонстрационный список заявок" at 13sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Filter over seeded fictional Lead cohort. Each row opens its own Lead/project; no backend query. Snapshot filters are not recalculated by local status edits.
The "OWNER 02 · Ожидание" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявки" with a arrow_back icon button on the left.
- Near the top: a tab row with 4 tabs: "Новые", "В работе", "Ожидание", "Закрытые"; "Ожидание" is selected.
- In the middle: a filled card (126dp tall) with the headline "Ольга Лебедева · №1043" and the body "Бот · 10–20 тыс. · Telegram · Ожидание · 05.09.2026".
- Near the bottom, aligned left: text "Демонстрационный список заявок" at 13sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Filter over seeded fictional Lead cohort. Each row opens its own Lead/project; no backend query. Snapshot filters are not recalculated by local status edits.
The "OWNER 02 · Закрытые" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявки" with a arrow_back icon button on the left.
- Near the top: a tab row with 4 tabs: "Новые", "В работе", "Ожидание", "Закрытые"; "Закрытые" is selected.
- In the middle: a filled card (126dp tall) with the headline "Игорь Соколов · №1044" and the body "CRM · 40–70 тыс. · Сайт · Закрытые · 04.09.2026".
- In the middle: a filled card (126dp tall) with the headline "Марина Орлова · №1040" and the body "Сайт · до 10 000 ₽ · Рекомендация · Закрытые · 20.08.2026".
- Near the bottom, aligned left: text "Демонстрационный список заявок" at 13sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Same Lead #1045 as client brief, shared local bindings. Contact is reserved .test address; no phone number. Create project does not duplicate Lead. Budget range is not a contract amount.
The "OWNER 03 · Карточка заявки" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1045" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Алексей Смирнов" at 28sp.
- Near the top, aligned left: text "alexey@example.test" at 14sp.
- In the middle: "Источник · бюджет" with supporting text "Приложение · 20–40 тыс.", a leading inbox icon.
- In the middle: a filled card (102dp tall) with the headline "AI-бриф / задача" and the body "Сайт для автосервиса: услуги и заявки".
- In the middle: a filled card (124dp tall) with the headline "ТЗ и комментарии" and the body "Услуги, каталог, форма заявки".
- In the middle: "Статус заявки" with supporting text "Новая", a leading flag icon, a trailing chevron_right icon.
- In the middle: a tonal button "Принять в работу" (380dp wide).
- Near the bottom: a filled button "Создать проект" with a add icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Full-length brief values remain readable; cards wrap text. Inputs are demo text, not real personal data.
The "OWNER · Полное ТЗ" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Бриф и ТЗ" with a arrow_back icon button on the left.
- Near the top: a filled card (114dp tall) with the headline "Задача" and the body "Сайт для автосервиса: услуги и заявки".
- In the middle: a filled card (114dp tall) with the headline "Функции" and the body "Услуги, каталог, форма заявки".
- In the middle: a filled card (114dp tall) with the headline "Интеграции" and the body "Уведомления в Telegram".
- In the middle: a filled card (114dp tall) with the headline "Сроки" and the body "В течение трёх недель".
- In the middle: a filled card (114dp tall) with the headline "Комментарии" and the body "Фотографии подготовлю позже".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Local status edit only. Labels map to future existing Lead status enum after CRM schema verification.
The "OWNER · Статус заявки" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Изменить статус" with a arrow_back icon button on the left.
- In the middle: an outlined dropdown labeled "Статус заявки" that opens a menu to pick one option (options "Новая", "В работе", "Ожидание", "Закрыта", "Проект создан", initially "Новая").
- In the middle: a filled button "Готово" with a check icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.
The "OWNER · Заявка 1043" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1043" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Ольга Лебедева" at 28sp.
- In the middle: "Тип / бюджет" with supporting text "Бот · 10–20 тыс.", a leading description icon.
- In the middle: "Источник / дата" with supporting text "Telegram · 05.09.2026", a leading description icon.
- In the middle: "Статус" with supporting text "Ожидание", a leading description icon.
- In the middle: a filled card (110dp tall) with the headline "Краткий бриф" and the body "Демонстрационная задача: услуги, описание и удобная связь с клиентами.".
- Near the bottom: a tonal button "К списку заявок" (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.
The "OWNER · Заявка 1044" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1044" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Игорь Соколов" at 28sp.
- In the middle: "Тип / бюджет" with supporting text "CRM · 40–70 тыс.", a leading description icon.
- In the middle: "Источник / дата" with supporting text "Сайт · 04.09.2026", a leading description icon.
- In the middle: "Статус" with supporting text "Закрытые", a leading description icon.
- In the middle: a filled card (110dp tall) with the headline "Краткий бриф" and the body "Демонстрационная задача: услуги, описание и удобная связь с клиентами.".
- Near the bottom: a tonal button "К списку заявок" (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.
The "OWNER · Заявка 1042" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1042" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Алексей Смирнов" at 28sp.
- In the middle: "Тип / бюджет" with supporting text "Сайт · 20–40 тыс.", a leading description icon.
- In the middle: "Источник / дата" with supporting text "Сайт · 01.09.2026", a leading description icon.
- In the middle: "Статус" with supporting text "В работе", a leading description icon.
- In the middle: a filled card (110dp tall) with the headline "Краткий бриф" and the body "Демонстрационная задача: услуги, описание и удобная связь с клиентами.".
- Near the bottom: a filled button "Открыть проект" with a work icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.
The "OWNER · Заявка 1041" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1041" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Павел Ильин" at 28sp.
- In the middle: "Тип / бюджет" with supporting text "Сайт · 20–40 тыс.", a leading description icon.
- In the middle: "Источник / дата" with supporting text "Рекомендация · 28.08.2026", a leading description icon.
- In the middle: "Статус" with supporting text "В работе", a leading description icon.
- In the middle: a filled card (110dp tall) with the headline "Краткий бриф" and the body "Демонстрационная задача: услуги, описание и удобная связь с клиентами.".
- Near the bottom: a filled button "Открыть проект" with a work icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.
The "OWNER · Заявка 1040" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Заявка №1040" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Марина Орлова" at 28sp.
- In the middle: "Тип / бюджет" with supporting text "Сайт · до 10 000 ₽", a leading description icon.
- In the middle: "Источник / дата" with supporting text "Рекомендация · 20.08.2026", a leading description icon.
- In the middle: "Статус" with supporting text "Закрытые", a leading description icon.
- In the middle: a filled card (110dp tall) with the headline "Краткий бриф" and the body "Демонстрационная задача: услуги, описание и удобная связь с клиентами.".
- Near the bottom: a filled button "Открыть проект" with a work icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Заявки" is selected.

Only fictional projects from demo-data.json. Real РиТМассаж and DIVEEV STUDIO are intentionally replaced with fictional equivalents. New-project route uses the same Lead binding, not a new CRM.
The "OWNER 04 · Проекты" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Проекты" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ПРОЕКТЫ В РАБОТЕ" at 13sp.
- In the middle: a filled card (138dp tall) with the headline "Сайт автосервиса" and the body "Алексей Смирнов · Разработка. Получить фотографии работ".
- In the middle: a filled card (138dp tall) with the headline "Студия «Линия»" and the body "Марина Орлова · Сопровождение. Плановая проверка 10 сентября".
- In the middle: a filled card (138dp tall) with the headline "Кабинет «Точка баланса»" and the body "Павел Ильин · Проверка. Получить согласование демо".
- In the middle: a tonal button "Проект из заявки №1045" (380dp wide).
- Near the bottom, aligned left: text "Вымышленные проекты · demo data" at 12sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Only fictional projects from demo-data.json. Real РиТМассаж and DIVEEV STUDIO are intentionally replaced with fictional equivalents. New-project route uses the same Lead binding, not a new CRM.
The "OWNER 04 · Ожидают клиента" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Проекты" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "ОЖИДАЮТ КЛИЕНТА" at 13sp.
- In the middle: a filled card (138dp tall) with the headline "Сайт автосервиса" and the body "Алексей Смирнов · Разработка. Получить фотографии работ".
- In the middle: a filled card (138dp tall) with the headline "Кабинет «Точка баланса»" and the body "Павел Ильин · Проверка. Получить согласование демо".
- In the middle: a tonal button "Проект из заявки №1045" (380dp wide).
- Near the bottom, aligned left: text "Вымышленные проекты · demo data" at 12sp.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Main owner work model: status / agreed / cost / waiting for client / next action. Same p001 as client. Stage and demo decisions are shared within Preview.
The "OWNER 05 · Карточка проекта" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Проект" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Сайт автосервиса" at 28sp.
- Near the top, aligned left: text "Алексей Смирнов · до 18.09.2026" at 14sp.
- In the middle: a filled card (90dp tall) on primaryContainer with the headline "Разработка · 30 000 ₽" and the body "Оплачено 15 000 ₽ · остаток 15 000 ₽".
- In the middle: "Этап проекта" with supporting text "Разработка", a leading checklist icon, a trailing chevron_right icon.
- In the middle: a filled card (102dp tall) with the headline "Что согласовано" and the body "Главная, услуги, каталог и форма заявки. Мобильная версия входит в стоимость.".
- In the middle: "Что ждём от клиента" with supporting text "Фотографии работ", a leading folder icon, a trailing chevron_right icon.
- In the middle: "Демо · решение клиента" with supporting text "Ожидается решение", a leading fact_check icon, a trailing chevron_right icon.
- In the middle: "Следующее действие" with supporting text "Получить материалы", a leading arrow_forward icon, a trailing chevron_right icon.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

All six stages support not_started, in_progress, waiting_client, completed. Local selections immediately appear in client Project. No migration or database mutation. Current stage is a separate explicit field, never inferred from percentages.
The "OWNER 06 · Этапы" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Этапы проекта" with a arrow_back icon button on the left.
- Near the top: an outlined dropdown labeled "Текущий этап" that opens a menu to pick one option (options "ТЗ", "Дизайн", "Разработка", "Проверка", "Запуск", "Сопровождение", initially "Разработка").
- In the middle: an outlined dropdown labeled "ТЗ" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Завершён").
- In the middle: an outlined dropdown labeled "Дизайн" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Завершён").
- In the middle: an outlined dropdown labeled "Разработка" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "В работе").
- In the middle: an outlined dropdown labeled "Проверка" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Запуск" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Сопровождение" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- Near the bottom: a filled button "Готово" with a check icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Created mock project p004 links Lead #1045; repeated taps open this same project, no duplicate. Range is not a price. Cost, payment, deadline and agreement remain unset until negotiated.
The "OWNER · Проект из заявки" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Новый проект" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Проект · Сайт" at 28sp.
- In the middle: "Основание" with supporting text "Заявка №1045 · Алексей Смирнов", a leading link icon.
- In the middle: a filled card (98dp tall) on primaryContainer with the headline "Статус: согласование" and the body "Сначала подтвердить ТЗ, стоимость и сроки.".
- In the middle: "Стоимость / оплата" with supporting text "Не согласована / нет оплаты", a leading payments icon.
- In the middle: "Бюджет клиента" with supporting text "20–40 тыс.", a leading account_balance_wallet icon.
- In the middle: "Срок" with supporting text "Не согласован", a leading event icon.
- In the middle: a filled button "Этапы нового проекта" with a checklist icon (380dp wide).
- Near the bottom: a tonal button "Открыть исходную заявку" (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Independent stages of new mock project p004, not p001. Every status editable locally; no backend or database.
The "OWNER · Этапы нового проекта" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Этапы нового проекта" with a arrow_back icon button on the left.
- Near the top: an outlined dropdown labeled "ТЗ" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Дизайн" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Разработка" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Проверка" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Запуск" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- In the middle: an outlined dropdown labeled "Сопровождение" that opens a menu to pick one option (options "Не начат", "В работе", "Ожидаем клиента", "Завершён", initially "Не начат").
- Near the bottom: a filled button "Готово" with a check icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Distinct fictional project linked to its existing demo Lead. Financial values come from the fixture, not live CRM.
The "OWNER · Студия «Линия»" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Карточка проекта" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Студия «Линия»" at 25sp.
- Near the top, aligned left: text "Марина Орлова" at 16sp.
- In the middle: a filled card (96dp tall) on primaryContainer with the headline "Сопровождение" and the body "Стоимость 9000 ₽ · оплачено 9000 ₽".
- In the middle: a filled card (96dp tall) with the headline "Что согласовано" and the body "Сайт с услугами, примерами работ и формой связи.".
- In the middle: a filled card (96dp tall) with the headline "Что ждём от клиента" and the body "Сейчас ничего".
- In the middle: a filled card (102dp tall) with the headline "Следующее действие" and the body "Плановая проверка 10 сентября".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Distinct fictional project linked to its existing demo Lead. Financial values come from the fixture, not live CRM.
The "OWNER · Кабинет «Точка баланса»" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Карточка проекта" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "Кабинет «Точка баланса»" at 25sp.
- Near the top, aligned left: text "Павел Ильин" at 16sp.
- In the middle: a filled card (96dp tall) on primaryContainer with the headline "Проверка" and the body "Стоимость 25000 ₽ · оплачено 12500 ₽".
- In the middle: a filled card (96dp tall) with the headline "Что согласовано" and the body "Сайт с услугами, примерами работ и формой связи.".
- In the middle: a filled card (96dp tall) with the headline "Что ждём от клиента" and the body "Согласование демо".
- In the middle: a filled card (102dp tall) with the headline "Следующее действие" and the body "Получить согласование демо".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Read the same demo decision and comment that the client edited; this is not a real approval audit log.
The "OWNER · Согласование демо" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Решение клиента" with a arrow_back icon button on the left.
- In the middle: a filled card (110dp tall) on primaryContainer with the headline "Согласование" and the body "Ожидается решение".
- In the middle: a filled card (144dp tall) with the headline "Комментарий клиента" and the body "Пока без комментариев".
- In the middle: a tonal button "Обсудить с клиентом" with a chat icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Metadata-only view of the same mock upload state as the client. No file content or actual uploads.
The "OWNER · Материалы" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Материалы клиента" with a arrow_back icon button on the left.
- Near the top: "Логотип и тексты" with supporting text "Готово", a leading check_circle icon.
- In the middle: "Фотографии" with supporting text "12 файлов", a leading photo_library icon.
- In the middle: "Реквизиты" with supporting text "Ожидаются", a leading article icon.
- In the middle: a filled card (116dp tall) with the headline "Новые материалы" and the body "Пока не добавлены".
- In the middle: a tonal button "Написать клиенту" with a chat icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Проекты" is selected.

Demo inbox derived from fixture unread_messages. Last messages are shared with client in memory.
The "OWNER · Входящие" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Сообщения" with a arrow_back icon button on the left.
- Near the top: "Алексей · Сайт автосервиса" with supporting text "Новое сообщение", a leading chat_bubble icon, a trailing chevron_right icon.
- In the middle: "Павел · Точка баланса" with supporting text "Посмотрел демо. Обсудим в проекте.", a leading chat_bubble icon, a trailing chevron_right icon.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); the first one is selected.

Project chat mock. Reuses client_message / owner_message local state, not existing MessengerMessage or a new external channel.
The "OWNER · Чат проекта" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Алексей · проект" with a arrow_back icon button on the left.
- In the middle: a filled card (126dp tall) on primaryContainer with the headline "Алексей · последнее сообщение" and the body "Фотографии подготовлю вечером.".
- In the middle: a filled card (110dp tall) with the headline "MITIN DEV · последний ответ" and the body "Ждём ваши материалы.".
- In the middle: an outlined text field labeled "Ответ клиенту".
- In the middle: a filled button "Отправить ответ" with a send icon (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); the first one is selected.

Owner admin-only menu. No production role switch. Analytics uses a reproducible demo cohort; client support is a shared mock request.
The "OWNER · Ещё" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Ещё".
- Near the top, aligned left: bold text "MITIN DEV · ВЛАДЕЛЕЦ" at 13sp.
- In the middle: "Аналитика" with supporting text "Заявки, источники и конверсия", a leading analytics icon, a trailing chevron_right icon.
- In the middle: "Сообщения" with supporting text "Проектные обсуждения", a leading forum icon, a trailing chevron_right icon.
- In the middle: "Поддержка" with supporting text "Обращения клиентов", a leading support_agent icon, a trailing chevron_right icon.
- In the middle: a filled card (120dp tall) on primaryContainer with the headline "Доступ владельца" and the body "В будущем — только для администратора. Роль и разрешения проверяет backend.".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Ещё" is selected.

Same support_last binding as client; no notifications are sent and no real support ticket exists.
The "OWNER · Поддержка" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Обращения" with a arrow_back icon button on the left.
- In the middle: a filled card (160dp tall) with the headline "Алексей · Сайт автосервиса" and the body "Обращений пока нет".
- In the middle: a tonal button "Обсудить обращение" (380dp wide).
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Ещё" is selected.

All values are derived from demo-data.json snapshot, not invented operational metrics. Cohort 6 Leads / 3 converted / 50%. Denominator includes all 6 cohort Leads, including closed. Local edits do not recalculate this snapshot.
The "OWNER 07 · Аналитика" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Аналитика" with a arrow_back icon button on the left.
- Near the top, aligned left: bold text "DEMO DATA · СРЕЗ 06.09.2026" at 13sp.
- In the middle: a filled card (104dp tall) on primaryContainer with the headline "Заявки → проекты" and the body "6 заявок → 3 проекта · 50%".
- In the middle, aligned left: bold text "ИСТОЧНИКИ" at 13sp.
- In the middle, aligned left: text "Приложение" at 16sp.
- In the middle, aligned left: text "Telegram" at 16sp.
- In the middle, aligned left: text "Сайт" at 16sp.
- In the middle, aligned left: text "Рекомендация" at 16sp.
- In the middle: "Бюджеты" with supporting text "Все 6 диапазонов budget_range", a leading payments icon, a trailing chevron_right icon.
- In the middle: "Проекты" with supporting text "2 активных · 1 на сопровождении", a leading work icon, a trailing chevron_right icon.
- Near the bottom: a filled card (72dp tall) with the headline "Как считаем" and the body "Конверсия = заявки с project_id / все заявки выборки. 3 / 6 = 50%.".
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Ещё" is selected.

Counts from the same fictional cohort, including zero buckets; no assumptions about live Lead budget values.
The "OWNER · Бюджеты" screen, from top to bottom (overlapping parts are called out as such):
- Near the top: a top app bar titled "Бюджеты заявок" with a arrow_back icon button on the left.
- Near the top: "до 10 000 ₽" with supporting text "1 заявок", a leading payments icon.
- In the middle: "10–20 тыс." with supporting text "1 заявок", a leading payments icon.
- In the middle: "20–40 тыс." with supporting text "3 заявок", a leading payments icon.
- In the middle: "40–70 тыс." with supporting text "1 заявок", a leading payments icon.
- In the middle: "70 тыс.+" with supporting text "0 заявок", a leading payments icon.
- In the middle: "пока не знаю" with supporting text "0 заявок", a leading payments icon.
- Near the bottom: a navigation bar with 4 destinations: "Главная" (space_dashboard), "Заявки" (inbox), "Проекты" (work), "Ещё" (more_horiz); "Ещё" is selected.

Parts placed outside the screens (shared parts or references):
- First: bold text "1" at 18sp.
- Below that: bold text "1" at 18sp.
- Below that: bold text "2" at 18sp.
- Below that: bold text "2" at 18sp.

## Behavior and navigation
- The "Я клиент" button opens the "CLIENT 01 · Добро пожаловать" screen with a slide in from the right when tapped.
- The "Я владелец" button opens the "OWNER 01 · Dashboard" screen with a slide in from the right when tapped.
- The "Создать проект" button opens the "CLIENT 02 · Тип проекта" screen with a slide in from the right when tapped.
- The "Войти" button opens the "CLIENT · Демо-вход" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Вход" top app bar opens the "CLIENT 01 · Добро пожаловать" screen with a slide in from the right.
- The "Открыть кабинет · демо" button opens the "CLIENT 05 · Главная" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Новый проект" top app bar opens the "CLIENT 01 · Добро пожаловать" screen with a slide in from the right.
- The "Сайт" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "Бот" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "CRM" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "Автоматизация" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "AI" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "Своя идея" card opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "MITIN DEV AI" top app bar opens the "CLIENT 02 · Тип проекта" screen with a slide in from the right.
- The "Ваша задача" text field Editable local demo field task. Memory only; never transmit or persist entered content.
- The "Продолжить бриф" button opens the "CLIENT 03b · Функции и бюджет" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Уточним детали" top app bar opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right.
- The "Основные функции" text field Editable local demo field features. Memory only; never transmit or persist entered content.
- The "Бюджет" dropdown Local shared field budget_range; selected text appears on both role screens. Backend later owns the canonical value.
- The "Желаемый срок" text field Editable local demo field deadline. Memory only; never transmit or persist entered content.
- The "Интеграции" text field Editable local demo field integrations. Memory only; never transmit or persist entered content.
- The "Комментарии" text field Editable local demo field comments. Memory only; never transmit or persist entered content.
- The "Проверить заявку" button opens the "CLIENT 04 · Проверка заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Проверить заявку" top app bar opens the "CLIENT 03b · Функции и бюджет" screen with a slide in from the right.
- The "Изменить" button opens the "CLIENT 03a · AI-бриф" screen with a slide in from the right when tapped.
- The "Отправить заявку" button opens the "CLIENT · Заявка принята" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка отправлена" top app bar opens the "CLIENT 04 · Проверка заявки" screen with a slide in from the right.
- The "Главная клиента · демо" button opens the "CLIENT 05 · Главная" screen with a slide in from the right when tapped.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Сайт автосервиса" card opens the "CLIENT 06 · Мой проект" screen with a slide in from the right when tapped.
- The "Следующее действие" list item opens the "CLIENT 08 · Демо" screen with a slide in from the right when tapped.
- The "Мой проект" list item opens the "CLIENT 06 · Мой проект" screen with a slide in from the right when tapped.
- The "Материалы" list item opens the "CLIENT 09 · Материалы" screen with a slide in from the right when tapped.
- The "Поддержка" list item opens the "CLIENT 12 · Поддержка" screen with a slide in from the right when tapped.
- The "Сообщения" button opens the "CLIENT 11 · Сообщения" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Мой проект" top app bar opens the "CLIENT 05 · Главная" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "30 000 ₽ · разработка" card opens the "CLIENT 10 · Стоимость" screen with a slide in from the right when tapped.
- The "Текущий этап" list item opens the "CLIENT 07 · Этап проекта" screen with a slide in from the right when tapped.
- The "Подробнее об этапе" button opens the "CLIENT 07 · Этап проекта" screen with a slide in from the right when tapped.
- The "Посмотреть демо" button opens the "CLIENT 08 · Демо" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Этап проекта" top app bar opens the "CLIENT 06 · Мой проект" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Следующее действие" list item opens the "CLIENT 09 · Материалы" screen with a slide in from the right when tapped.
- The "Открыть демо" button opens the "CLIENT 08 · Демо" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Демо проекта" top app bar opens the "CLIENT 06 · Мой проект" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Открыть демо ↗" button opens the "CLIENT · Макет демо" screen with a slide in from the right when tapped.
- The "Одобрить" button opens the "CLIENT · Решение по демо" screen with a slide in from the right when tapped.
- The "Есть правки" button opens the "CLIENT · Правки к демо" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Автосервис · макет" top app bar opens the "CLIENT 08 · Демо" screen with a slide in from the right.
- The "Вернуться к согласованию" button opens the "CLIENT 08 · Демо" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Есть правки" top app bar opens the "CLIENT 08 · Демо" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Комментарий к демо" text field Editable local demo field feedback_input. Memory only; never transmit or persist entered content.
- The "Сохранить комментарий" button opens the "CLIENT · Решение по демо" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Решение сохранено" top app bar opens the "CLIENT 08 · Демо" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Вернуться к проекту" button opens the "CLIENT 06 · Мой проект" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Материалы" top app bar opens the "CLIENT 06 · Мой проект" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Добавить материалы" button opens the "CLIENT · Добавление материалов" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Добавить материалы" top app bar opens the "CLIENT 09 · Материалы" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Добавить пример файла" button opens the "CLIENT 09 · Материалы" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Стоимость" top app bar opens the "CLIENT 06 · Мой проект" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- Tapping the arrow_back icon button on the left of the "Сообщения" top app bar opens the "CLIENT 05 · Главная" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Сообщение по проекту" text field Editable local demo field chat_input. Memory only; never transmit or persist entered content.
- The "Отправить" button opens the "CLIENT 11 · Сообщения" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Поддержка" top app bar opens the "CLIENT 05 · Главная" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Сообщить о проблеме" button opens the "CLIENT · Обращение" screen with a slide in from the right when tapped.
- The "Заказать доработку" button opens the "CLIENT · Обращение" screen with a slide in from the right when tapped.
- The "Новый проект" button opens the "CLIENT 02 · Тип проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Поддержка проекта" top app bar opens the "CLIENT 12 · Поддержка" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Опишите задачу" text field Editable local demo field support_input. Memory only; never transmit or persist entered content.
- The "Сохранить обращение" button opens the "CLIENT 12 · Поддержка" screen with a slide in from the right when tapped.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- The "Данные и доступ" list item opens the "CLIENT · Данные и доступ" screen with a slide in from the right when tapped.
- The "Поддержка" list item opens the "CLIENT 12 · Поддержка" screen with a slide in from the right when tapped.
- The "Выйти" button opens the "CLIENT 01 · Добро пожаловать" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Данные и доступ" top app bar opens the "CLIENT · Профиль" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "CLIENT 05 · Главная" screen with a fade.
- Tapping the "Проект" destination of the navigation bar opens the "CLIENT 06 · Мой проект" screen with a fade.
- Tapping the "Сообщения" destination of the navigation bar opens the "CLIENT 11 · Сообщения" screen with a fade.
- Tapping the "Профиль" destination of the navigation bar opens the "CLIENT · Профиль" screen with a fade.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The box opens the "OWNER 02 · Новые" screen with a slide in from the right when tapped. It also Новые заявки.
- The text opens the "OWNER 02 · Новые" screen with a slide in from the right when tapped.
- The text opens the "OWNER 02 · Новые" screen with a slide in from the right when tapped.
- The box opens the "OWNER 04 · Проекты" screen with a slide in from the right when tapped. It also Активные проекты.
- The text opens the "OWNER 04 · Проекты" screen with a slide in from the right when tapped.
- The text opens the "OWNER 04 · Проекты" screen with a slide in from the right when tapped.
- The box opens the "OWNER 04 · Ожидают клиента" screen with a slide in from the right when tapped. It also Ожидают клиента.
- The text opens the "OWNER 04 · Ожидают клиента" screen with a slide in from the right when tapped.
- The text opens the "OWNER 04 · Ожидают клиента" screen with a slide in from the right when tapped.
- The box opens the "OWNER · Входящие" screen with a slide in from the right when tapped. It also Новые сообщения.
- The text opens the "OWNER · Входящие" screen with a slide in from the right when tapped.
- The text opens the "OWNER · Входящие" screen with a slide in from the right when tapped.
- The "Алексей Смирнов · №1045" list item opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right when tapped.
- The "Ближайшее действие" card opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявки" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the "Новые" destination of the tabs opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "В работе" destination of the tabs opens the "OWNER 02 · В работе" screen with a fade.
- Tapping the "Ожидание" destination of the tabs opens the "OWNER 02 · Ожидание" screen with a fade.
- Tapping the "Закрытые" destination of the tabs opens the "OWNER 02 · Закрытые" screen with a fade.
- The "Алексей Смирнов · №1045" card opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявки" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the "Новые" destination of the tabs opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "В работе" destination of the tabs opens the "OWNER 02 · В работе" screen with a fade.
- Tapping the "Ожидание" destination of the tabs opens the "OWNER 02 · Ожидание" screen with a fade.
- Tapping the "Закрытые" destination of the tabs opens the "OWNER 02 · Закрытые" screen with a fade.
- The "Алексей Смирнов · №1042" card opens the "OWNER · Заявка 1042" screen with a slide in from the right when tapped.
- The "Павел Ильин · №1041" card opens the "OWNER · Заявка 1041" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявки" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the "Новые" destination of the tabs opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "В работе" destination of the tabs opens the "OWNER 02 · В работе" screen with a fade.
- Tapping the "Ожидание" destination of the tabs opens the "OWNER 02 · Ожидание" screen with a fade.
- Tapping the "Закрытые" destination of the tabs opens the "OWNER 02 · Закрытые" screen with a fade.
- The "Ольга Лебедева · №1043" card opens the "OWNER · Заявка 1043" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявки" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the "Новые" destination of the tabs opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "В работе" destination of the tabs opens the "OWNER 02 · В работе" screen with a fade.
- Tapping the "Ожидание" destination of the tabs opens the "OWNER 02 · Ожидание" screen with a fade.
- Tapping the "Закрытые" destination of the tabs opens the "OWNER 02 · Закрытые" screen with a fade.
- The "Игорь Соколов · №1044" card opens the "OWNER · Заявка 1044" screen with a slide in from the right when tapped.
- The "Марина Орлова · №1040" card opens the "OWNER · Заявка 1040" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1045" top app bar opens the "OWNER 02 · Новые" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "ТЗ и комментарии" card opens the "OWNER · Полное ТЗ" screen with a slide in from the right when tapped.
- The "Статус заявки" list item opens the "OWNER · Статус заявки" screen with a slide in from the right when tapped.
- The "Принять в работу" button opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right when tapped.
- The "Создать проект" button opens the "OWNER · Проект из заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Бриф и ТЗ" top app bar opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the arrow_back icon button on the left of the "Изменить статус" top app bar opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Статус заявки" dropdown Local shared field lead_status; selected text appears on both role screens. Backend later owns the canonical value.
- The "Готово" button opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1043" top app bar opens the "OWNER 02 · Ожидание" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "К списку заявок" button opens the "OWNER 02 · Ожидание" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1044" top app bar opens the "OWNER 02 · Закрытые" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "К списку заявок" button opens the "OWNER 02 · Закрытые" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1042" top app bar opens the "OWNER 02 · В работе" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Открыть проект" button opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1041" top app bar opens the "OWNER 02 · В работе" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Открыть проект" button opens the "OWNER · Кабинет «Точка баланса»" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Заявка №1040" top app bar opens the "OWNER 02 · Закрытые" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Открыть проект" button opens the "OWNER · Студия «Линия»" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Проекты" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Сайт автосервиса" card opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right when tapped.
- The "Студия «Линия»" card opens the "OWNER · Студия «Линия»" screen with a slide in from the right when tapped.
- The "Кабинет «Точка баланса»" card opens the "OWNER · Кабинет «Точка баланса»" screen with a slide in from the right when tapped.
- The "Проект из заявки №1045" button opens the "OWNER · Проект из заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Проекты" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Сайт автосервиса" card opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right when tapped.
- The "Кабинет «Точка баланса»" card opens the "OWNER · Кабинет «Точка баланса»" screen with a slide in from the right when tapped.
- The "Проект из заявки №1045" button opens the "OWNER · Проект из заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Проект" top app bar opens the "OWNER 04 · Проекты" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Этап проекта" list item opens the "OWNER 06 · Этапы" screen with a slide in from the right when tapped.
- The "Что ждём от клиента" list item opens the "OWNER · Материалы" screen with a slide in from the right when tapped.
- The "Демо · решение клиента" list item opens the "OWNER · Согласование демо" screen with a slide in from the right when tapped.
- The "Следующее действие" list item opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Этапы проекта" top app bar opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Текущий этап" dropdown Local shared field current_stage; selected text appears on both role screens. Backend later owns the canonical value.
- The "ТЗ" dropdown Local shared field stage_0; selected text appears on both role screens. Backend later owns the canonical value.
- The "Дизайн" dropdown Local shared field stage_1; selected text appears on both role screens. Backend later owns the canonical value.
- The "Разработка" dropdown Local shared field stage_2; selected text appears on both role screens. Backend later owns the canonical value.
- The "Проверка" dropdown Local shared field stage_3; selected text appears on both role screens. Backend later owns the canonical value.
- The "Запуск" dropdown Local shared field stage_4; selected text appears on both role screens. Backend later owns the canonical value.
- The "Сопровождение" dropdown Local shared field stage_5; selected text appears on both role screens. Backend later owns the canonical value.
- The "Готово" button opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Новый проект" top app bar opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Этапы нового проекта" button opens the "OWNER · Этапы нового проекта" screen with a slide in from the right when tapped.
- The "Открыть исходную заявку" button opens the "OWNER 03 · Карточка заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Этапы нового проекта" top app bar opens the "OWNER · Проект из заявки" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "ТЗ" dropdown Local shared field new_stage_0; selected text appears on both role screens. Backend later owns the canonical value.
- The "Дизайн" dropdown Local shared field new_stage_1; selected text appears on both role screens. Backend later owns the canonical value.
- The "Разработка" dropdown Local shared field new_stage_2; selected text appears on both role screens. Backend later owns the canonical value.
- The "Проверка" dropdown Local shared field new_stage_3; selected text appears on both role screens. Backend later owns the canonical value.
- The "Запуск" dropdown Local shared field new_stage_4; selected text appears on both role screens. Backend later owns the canonical value.
- The "Сопровождение" dropdown Local shared field new_stage_5; selected text appears on both role screens. Backend later owns the canonical value.
- The "Готово" button opens the "OWNER · Проект из заявки" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Карточка проекта" top app bar opens the "OWNER 04 · Проекты" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the arrow_back icon button on the left of the "Карточка проекта" top app bar opens the "OWNER 04 · Проекты" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- Tapping the arrow_back icon button on the left of the "Решение клиента" top app bar opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Обсудить с клиентом" button opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Материалы клиента" top app bar opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Написать клиенту" button opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Сообщения" top app bar opens the "OWNER 01 · Dashboard" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Алексей · Сайт автосервиса" list item opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- The "Павел · Точка баланса" list item opens the "OWNER · Кабинет «Точка баланса»" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Алексей · проект" top app bar opens the "OWNER 05 · Карточка проекта" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Ответ клиенту" text field Editable local demo field owner_input. Memory only; never transmit or persist entered content.
- The "Отправить ответ" button opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Аналитика" list item opens the "OWNER 07 · Аналитика" screen with a slide in from the right when tapped.
- The "Сообщения" list item opens the "OWNER · Входящие" screen with a slide in from the right when tapped.
- The "Поддержка" list item opens the "OWNER · Поддержка" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Обращения" top app bar opens the "OWNER · Ещё" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Обсудить обращение" button opens the "OWNER · Чат проекта" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Аналитика" top app bar opens the "OWNER · Ещё" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.
- The "Бюджеты" list item opens the "OWNER · Бюджеты" screen with a slide in from the right when tapped.
- The "Проекты" list item opens the "OWNER 04 · Проекты" screen with a slide in from the right when tapped.
- Tapping the arrow_back icon button on the left of the "Бюджеты заявок" top app bar opens the "OWNER 07 · Аналитика" screen with a slide in from the right.
- Tapping the "Главная" destination of the navigation bar opens the "OWNER 01 · Dashboard" screen with a fade.
- Tapping the "Заявки" destination of the navigation bar opens the "OWNER 02 · Новые" screen with a fade.
- Tapping the "Проекты" destination of the navigation bar opens the "OWNER 04 · Проекты" screen with a fade.
- Tapping the "Ещё" destination of the navigation bar opens the "OWNER · Ещё" screen with a fade.

## Component styles
Per-component guidance for the parts in use. The numbers are the M3 Expressive defaults: let the standard components handle whatever they already do, and adjust where the content calls for it.
- Top app bar: 64dp tall on surface, with its background extended behind the status bar (pad the top by the system inset). Title in titleLarge, 48dp icon buttons on each side. The standard tint to surfaceContainer on scroll is fine.
- Text: the specified sp size; headings on onSurface, descriptions on onSurfaceVariant, line height 1.3–1.5× the size. No ripple or press feedback on tap.
- Cards: 20dp corners with an image area on top. Filled uses surfaceContainerHighest, elevated uses surfaceContainerLow with a level 1 shadow, outlined has a 1dp outlineVariant border. Headline in titleMedium, body in bodyMedium, 16dp inner padding.
- Buttons: medium size, 56dp tall, fully rounded (pill). Filled uses primary, tonal uses secondaryContainer, outlined has a 1dp outline border. A connected button group is a row with 3dp gaps where only the inner adjoining corners shrink to 8dp and the outer corners stay round (the M3 Expressive connected button group).
- Boxes: plain containers with the specified background token and corner radii. They are the background for whatever is layered on them and have no behavior of their own.
- Text fields: 56dp tall. Outlined has 16dp corners and an outline border; filled sits on surfaceContainerHighest with an underline. On focus the label floats up and the border becomes 2dp primary. Supporting text goes underneath in bodySmall.
- Dropdowns: look like a text field (56dp tall, outlined or filled) with a trailing arrow_drop_down icon. Implement as an exposed dropdown menu: tapping opens a menu below (surfaceContainer, 4dp corners, 48dp items) and the chosen value shows in the field.
- List items: 72dp tall, 24dp leading icon (on a 40dp primaryContainer circle unless stated), headline in bodyLarge, supporting text in bodyMedium on onSurfaceVariant, on the specified background role (surfaceContainerLow unless stated). A stacked list is a vertical run with 3dp gaps, 28dp outer corners and 8dp inner corners (the M3 Expressive list treatment).
- Navigation bar: 80dp tall on surfaceContainer, with its background extended down through the gesture navigation area (pad the bottom by the system inset). The active destination shows a secondaryContainer pill indicator (64×32dp), a filled icon and a labelMedium label.
- Tabs: M3 primary tabs. 48dp tall, labels in titleSmall; the selected tab has primary text and a 3dp label-width indicator with rounded top corners, with an outlineVariant divider underneath. Tapping a tab switches the content.

## General guidance
- Work out what kind of app this is from the purpose of the screens, and implement the features such an app is normally expected to have (create, list, detail, edit, delete, search, settings, whichever apply) even where the sketch does not show them.
- Treat the data as real. Persist what the user creates on the device (Room, DataStore or similar) so it survives restarts. Do not ship dummy or sample data; show an empty state when there is nothing yet. Validate input, and confirm or report failures and deletions appropriately.
- Fill in behavior the sketch leaves out from the purpose of the screen and the labels of the parts. A button or item with no behavior specified should do what its label implies (save, send, open a detail screen, and so on), never nothing.
- The layout only needs to keep the intent (order, grouping, relative placement); sizes and spacing may be adjusted to fit the content. If something would break on a device, prefer working over matching the sketch.
- Use the standard components from Jetpack Compose material3 (latest, including the Expressive APIs); do not custom-draw parts the library provides.
- Always reference colors through the scheme roles above (primary, surfaceContainer, …) instead of hard-coded values.
- Keep 16dp screen margins and 8–16dp between parts, and use the M3 type styles (titleLarge, bodyMedium, …).
- Parts described as "in one row" must share a single Row (horizontal container) on the same line; never stack them vertically or wrap them. The row is as tall as its tallest part and the others are vertically centered in it.
- Parts described as "layered inside" a container are drawn on top of that container (a Box with the container as its background). The overlap is intentional: do not separate or reorder them for layout reasons. Later items in the description are drawn in front of earlier ones.
- Give every tappable part ripple plus a slight press-scale. "Back" plays the entry transition in reverse, and the system back gesture / button must do the same.
- Use Material Symbols Rounded for icons.
- Do not verify on an emulator or a device. When the implementation is done, produce a signed release APK as the deliverable.
