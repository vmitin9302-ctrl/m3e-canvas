"""Build the native M3E Canvas document from fictional fixtures. No network or APIs."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = json.loads((ROOT / 'prototypes/mitin-dev/demo-data.json').read_text())
D = dict(title='MITIN DEV', brief='Russian-language client + owner mobile app prototype. Fictional data only. Client journey: interest, Lead, scripted AI brief, review, agreement, development, demo, launch, support. Both roles share one future MITIN DEV API / FastAPI backend and CRM; never create a second CRM. Prototype only: no API, credentials, accounts, notifications, payments, migrations or deployment. Role selection exists only on the developer/demo screen. Production role comes from backend authorization. Preserve budget_range. Use real stages, not invented progress percentages. Use local mock repositories for Android implementation; production integration requires a separate task.', frame='phone', platform='android', paletteKey='purple', dynamicColor=False, theme=dict(dark=True,bothModes=False,contrast='standard',shape='rounded',font='system',emphasized=True,motion='expressive'),frames=[],groups=[])
F = None
N = 0

def add(kind, label='', x=20, y=100, **kw):
    global N
    N += 1
    it=dict(id=f'{F["id"]}-{N}', kind=kind, label=label, icon=None,variant='filled',**kw)
    D['groups'].append(dict(id='g'+str(N),x=F['x']+x,y=F['y']+y,axis='x',items=[it]))
    return it

def binding(key, initial, **rest): return dict(key=key,initial=initial,**rest)
def live(text=None, supporting=None, **rest):
    return dict(text={**({'label':text} if text is not None else {}),**({'supporting':supporting} if supporting is not None else {})},**rest)
def action(to): return dict(to=to,transition='slide')
def txt(label,y,size=16,x=24,bold=False,**kw): return add('text',label,x,y,size=size,bold=bold,**kw)
def box(y,h,fill='surfaceContainerLow',x=16,w=380,r=28): return add('box','',x,y,size=w,size2=h,fill=fill,radiusTop=r,radiusBottom=r)
def button(label,y,to,x=16,w=380,variant='filled',icon=None,preview=None,note=None):
    kw=dict(size=w,action=action(to));
    if preview: kw['preview']=preview
    if note: kw['note']=note
    it=add('button',label,x,y,**kw);it['variant']=variant;it['icon']=icon
    return it

def card(label,body,y,h=100,to=None,fill=None,x=16,w=380,preview=None):
    kw=dict(supporting=body,size=w,size2=h,noImage=True)
    if to: kw['action']=action(to)
    if fill: kw['fill']=fill
    if preview: kw['preview']=preview
    return add('card',label,x,y,**kw)
def row(label,body,y,to=None,icon='chevron_right',preview=None):
    it=add('listItem',label,16,y,size=380,supporting=body,icon2='chevron_right' if to else None)
    it['icon']=icon
    if to: it['action']=action(to)
    if preview: it['preview']=preview
    return it

def field(label,key,initial,y):
    it=add('textField',label,16,y,size=380,preview=binding(key,initial),note=f'Editable local demo field {key}. Memory only; never transmit or persist entered content.')
    it['variant']='outlined'
    return it

def select(label,key,initial,options,y):
    it=add('select',label,16,y,size=380,tabs=[dict(label=o) for o in options],selected=options.index(initial),preview=binding(key,initial),note=f'Local shared field {key}; selected text appears on both role screens. Backend later owns the canonical value.')
    it['variant']='outlined'
    return it

def nav(role='client',selected=0):
    entries=([('Главная','home','c-home'),('Проект','work','c-project'),('Сообщения','chat_bubble','c-chat'),('Профиль','person','c-profile')] if role=='client' else [('Главная','space_dashboard','o-home'),('Заявки','inbox','o-leads'),('Проекты','work','o-projects'),('Ещё','more_horiz','o-more')])
    add('bottomNav','',0,788,size=412,tabs=[dict(label=l,icon=i) for l,i,_ in entries],selected=selected,actions={f'tab:{i}':dict(to=to,transition='fade') for i,(_,_,to) in enumerate(entries)})

def screen(id,name,title=None,back=None,role=None,selected=0,note=''):
    global F
    i=len(D['frames']);F=dict(id=id,name=name,x=(i%6)*532,y=(i//6)*1040,note=note or name,bg='surface')
    D['frames'].append(F)
    if title is not None:
        bar=add('topAppBar',title,0,0,size=412)
        if back: bar['icon']='arrow_back';bar['actions']={'icon':action(back)}
    if role: nav(role,selected)
    return F

def kicker(label,y=106): txt(label,y,13,bold=True)
def pair(a,b,y): button(a[0],y,a[1],w=188,variant='tonal');button(b[0],y,b[1],x=208,w=188)

screen('demo','DEMO · роли','MITIN DEV',note='Developer/demo-only entry. Role is not user-selectable in the production application. All names, projects, contacts and prices are fictional fixtures. Preview state resets when closed or reloaded.')
kicker('ИНТЕРАКТИВНЫЙ ПРОТОТИП',118)
txt('Один проект.',170,36,bold=True);txt('Две стороны.',218,36,bold=True)
card('Клиент и владелец','Посмотрите путь от первой идеи до запуска и рабочую сторону MITIN DEV.',298,112,fill='primaryContainer')
button('Я клиент',460,'c-welcome',icon='person')
button('Я владелец',532,'o-home',variant='tonal',icon='business_center')
card('Только демонстрация','Вымышленные данные. Ввод хранится в памяти Preview. Выбор роли нужен только для просмотра сценариев.',644,112)

screen('c-welcome','CLIENT 01 · Добро пожаловать','MITIN DEV',note='Client priority. Create project starts a Lead before a contract. Sign in uses a demo entry, no credentials and no account creation.')
kicker('ОТ ИДЕИ ДО ЗАПУСКА',116)
box(166,228,'primaryContainer');txt('Ваш бизнес.',195,38,bold=True);txt('Следующий',245,38,bold=True);txt('уровень.',295,38,bold=True)
txt('Сайты, боты и digital-системы',434,20);txt('для бизнеса.',464,20)
card('Всё о проекте — под рукой','Обсуждения, этапы, материалы и поддержка. Понятно, что происходит и какой следующий шаг.',524,108)
button('Создать проект',674,'c-type',icon='add');button('Войти',742,'c-login',variant='tonal',icon='login')

screen('c-login','CLIENT · Демо-вход','Вход',back='c-welcome',note='Mock login only. Real app will obtain role from the authenticated backend. Do not add password or phone collection in this prototype.')
kicker('ПРОСМОТР КАБИНЕТА');txt('Рады видеть вас',154,30,bold=True)
card('Алексей Смирнов','Вымышленный клиент. Активный проект уже согласован, договор заключён.',238,114,fill='primaryContainer')
button('Открыть кабинет · демо',428,'c-home',icon='arrow_forward')

screen('c-type','CLIENT 02 · Тип проекта','Новый проект',back='c-welcome',note='Choose project_type and continue to scripted AI brief. Budget keys must match existing Lead.budget_range; do not invent a second budget taxonomy.')
kicker('ШАГ 1 / 3');txt('Что хотите создать?',150,29,bold=True)
for i,(label,desc,icon) in enumerate([('Сайт','Ваш бизнес онлайн','language'),('Бот','Помощник в мессенджере','smart_toy'),('CRM','Клиенты и задачи','view_kanban'),('Автоматизация','Меньше рутины','bolt'),('AI','Умные инструменты','auto_awesome'),('Своя идея','Обсудим вместе','lightbulb')]):
    x=16+(i%2)*196;y=222+(i//2)*160
    c=card(label,desc,y,140,'c-brief',x=x,w=184);c['noImage']=False;c['icon']=icon;c['preview']={'set':{'project_type':label}}
txt('Можно начать без готового ТЗ.',724,15)

screen('c-brief','CLIENT 03a · AI-бриф','MITIN DEV AI',back='c-type',note='Scripted AI, no model requests. User enters a fictional business task. The response is carried into review and the owner Lead card.')
kicker('ШАГ 2 / 3 · ЗНАКОМСТВО')
card('MITIN DEV AI','Расскажите своими словами, что хотите создать и какую задачу бизнеса это должно решить.',160,112,fill='primaryContainer')
card('Вы · пример','Нужен сайт для автосервиса, чтобы клиенты видели услуги и оставляли заявки.',294,105)
field('Ваша задача','task','Сайт для автосервиса: услуги и заявки',444)
card('Подсказка','Для кого проект? Что сейчас неудобно? Какой результат вы хотите получить?',540,96)
button('Продолжить бриф',702,'c-brief-details',icon='arrow_forward')

screen('c-brief-details','CLIENT 03b · Функции и бюджет','Уточним детали',back='c-brief',note='Scripted second AI step. Functional scope, business goal, budget_range, deadline and integrations are carried to review. Estimates are not agreed prices.')
card('MITIN DEV AI','Что должно работать в первой версии? Какой бюджет и срок вы рассматриваете?',110,94,fill='primaryContainer')
field('Основные функции','features','Услуги, каталог, форма заявки',234)
select('Бюджет','budget_range','20–40 тыс.',DATA['budget_ranges'],330)
field('Желаемый срок','deadline','В течение трёх недель',426)
field('Интеграции','integrations','Уведомления в Telegram',522)
field('Комментарии','comments','Фотографии подготовлю позже',618)
button('Проверить заявку',714,'c-review',icon='fact_check')

screen('c-review','CLIENT 04 · Проверка заявки','Проверить заявку',back='c-brief-details',note='Structured brief is editable through the previous wizard. The demo submit only navigates and changes local state. No network calls, account creation, notification or real Lead creation.')
for y,label,body,key,initial,icon in [(112,'Тип проекта','Сайт','project_type','Сайт','language'),(190,'Задача','Сайт для автосервиса: услуги и заявки','task','Сайт для автосервиса: услуги и заявки','flag'),(268,'Основные функции','Услуги, каталог, форма заявки','features','Услуги, каталог, форма заявки','checklist'),(346,'Бюджет','20–40 тыс.','budget_range','20–40 тыс.','payments'),(424,'Сроки','В течение трёх недель','deadline','В течение трёх недель','event'),(502,'Интеграции','Уведомления в Telegram','integrations','Уведомления в Telegram','hub'),(580,'Комментарии','Фотографии подготовлю позже','comments','Фотографии подготовлю позже','chat')]:
    row(label,body,y,icon=icon,preview=binding(key,initial,text={'supporting':'{{'+key+'}}'}))
pair(('Изменить','c-brief'),('Отправить заявку','c-sent'),716)
D['groups'][-1]['items'][0]['preview']={'set':{'lead_status':'Новая'}}

screen('c-sent','CLIENT · Заявка принята','Заявка отправлена',back='c-review',note='Demo-only accepted Lead #1045. A Lead is not automatically a signed project. The next button deliberately moves the reviewer to the existing signed-project fixture p001.')
box(144,206,'primaryContainer');txt('Заявка №1045',184,30,bold=True);txt('принята',230,38,bold=True)
card('Что дальше','MITIN DEV изучит задачу, уточнит условия и предложит следующий шаг. Стоимость и срок пока не согласованы.',402,120)
card('Следующая часть демо','Посмотрим кабинет с уже согласованным проектом. В реальном приложении он появится после договорённости.',546,122)
button('Главная клиента · демо',714,'c-home',icon='arrow_forward')

screen('c-home','CLIENT 05 · Главная','MITIN DEV',role='client',note='Signed project p001 belongs to the fictional client Alexey. Show the real current stage and next action; no invented percentage. This is distinct from submitted Lead #1045.')
kicker('ДОБРЫЙ ДЕНЬ');txt('Алексей',139,38,bold=True)
card('Сайт автосервиса','Разработка · проект в работе',218,96,'c-project','primaryContainer',preview=live(supporting='{{current_stage}} · проект в работе'))
row('Следующее действие','Согласовать мобильную версию',334,'c-demo','task_alt')
kicker('ВАШ ПРОЕКТ',446)
for y,l,b,to,ic in [(482,'Мой проект','Этапы и договорённости','c-project','work'),(558,'Материалы','Ожидаем фотографии работ','c-materials','folder'),(634,'Поддержка','Поможем с вашим сайтом','c-support','support_agent')]: row(l,b,y,to,ic)
button('Сообщения',716,'c-chat',variant='tonal',icon='chat_bubble')

screen('c-project','CLIENT 06 · Мой проект','Мой проект',back='c-home',role='client',selected=1,note='Shared project p001. Values reflect the same seeded project as owner. No progress percentage. Stage codes map to not_started / in_progress / waiting_client / completed.')
txt('Сайт автосервиса',112,28,bold=True);txt('Сайт · начало 01.09.2026',153,14)
card('30 000 ₽ · разработка','Оплачено 15 000 ₽ · остаток 15 000 ₽',192,84,'c-cost','primaryContainer')
row('Текущий этап','Разработка',290,'c-stage','code',preview=binding('current_stage','Разработка',text={'supporting':'{{current_stage}}'}))
for i,(name,default,icon) in enumerate([('ТЗ','Завершён','check_circle'),('Дизайн','Завершён','check_circle'),('Разработка','В работе','radio_button_checked'),('Проверка','Не начат','radio_button_unchecked'),('Запуск','Не начат','radio_button_unchecked'),('Сопровождение','Не начат','radio_button_unchecked')]):
    key=f'stage_{i}';y=382+i*44
    txt(name,y,16);txt(default,y,14,x=206,preview=binding(key,default,text={'label':'{{'+key+'}}'}))
button('Подробнее об этапе',662,'c-stage',variant='tonal');button('Посмотреть демо',726,'c-demo',icon='open_in_new')

screen('c-stage','CLIENT 07 · Этап проекта','Этап проекта',back='c-project',role='client',selected=1,note='Development stage detail. Completed tasks, current work, client dependency and next action. Stage state follows the same stage_2 binding as owner.')
txt('Разработка',114,32,bold=True);txt('В работе',158,16,preview=live(text='{{stage_2}}'))
card('Что сделано','Главная, каталог услуг и мобильная версия.',214,92)
card('Что сейчас делаем','Настраиваем форму заявки и проверяем сообщения об ошибках.',326,100)
card('Что ждём от вас','Фотографии работ для раздела с примерами.',446,94,fill='primaryContainer')
row('Следующее действие','Добавить материалы проекта',560,'c-materials','upload_file')
button('Открыть демо',686,'c-demo',icon='open_in_new')

screen('c-demo','CLIENT 08 · Демо','Демо проекта',back='c-project',role='client',selected=1,note='Demo link opens an internal illustrative screen, never an external production site. Approve and feedback affect only local Preview data shared with owner.')
kicker('ГОТОВО К ПРОСМОТРУ');txt('Демо готово',150,36,bold=True)
card('Мобильная версия','Проверьте расположение блоков, тексты и кнопки. Ваше решение увидим в карточке проекта.',234,120,fill='primaryContainer')
row('Согласование','Ожидается решение',378,icon='fact_check',preview=binding('demo_status','Ожидается решение',text={'supporting':'{{demo_status}}'}))
button('Открыть демо ↗',492,'c-demo-site',icon='language')
button('Одобрить',602,'c-demo-result',icon='check',preview={'set':{'demo_status':'Одобрено'}})
button('Есть правки',670,'c-feedback',variant='tonal',icon='edit_note')

screen('c-demo-site','CLIENT · Макет демо','Автосервис · макет',back='c-demo',note='Internal fictional demo destination. No external site or production URL is opened. This frame is only an illustration of the linked demo action.')
kicker('АВТОСЕРВИС «ИМПУЛЬС»');box(160,252,'primaryContainer');txt('Ваш автомобиль',186,30,bold=True);txt('в надёжных руках',228,30,bold=True);txt('Диагностика · ремонт · обслуживание',302,15)
card('Услуги сервиса','Техническое обслуживание, диагностика, ремонт ходовой части.',450,108)
card('Пример раздела работ','Здесь будут фотографии. Это внутренний макет для проверки перехода.',580,108)
button('Вернуться к согласованию',734,'c-demo',variant='tonal')

screen('c-feedback','CLIENT · Правки к демо','Есть правки',back='c-demo',role='client',selected=1,note='Editable comment kept only in Preview memory. Sending updates the shared demo_feedback binding; no message or notification leaves this browser.')
txt('Что поправить?',128,30,bold=True);card('Лучше с примером','Укажите блок и желаемое изменение. Все записи здесь демонстрационные.',208,110)
field('Комментарий к демо','feedback_input','Сделать кнопку записи заметнее',386)
button('Сохранить комментарий',528,'c-demo-result',preview={'set':{'demo_status':'Есть правки','demo_feedback':'{{feedback_input}}'}},icon='check')

screen('c-demo-result','CLIENT · Решение по демо','Решение сохранено',back='c-demo',role='client',selected=1,note='Local confirmation only. Owner reads the same demo_status and demo_feedback keys.')
txt('Спасибо!',152,38,bold=True);card('Результат','Одобрено',244,96,fill='primaryContainer',preview=live(supporting='{{demo_status}}'))
card('Комментарий','Пока без комментариев',362,128,preview=binding('demo_feedback','Пока без комментариев',text={'supporting':'{{demo_feedback}}'}))
button('Вернуться к проекту',588,'c-project',variant='tonal')

screen('c-materials','CLIENT 09 · Материалы','Материалы',back='c-project',role='client',selected=1,note='Only fixture metadata; no file input, file reading or upload. The add action marks a mock material as attached, shared with owner.')
kicker('ФАЙЛЫ ПРОЕКТА');txt('Всё для работы',152,30,bold=True)
for i,(l,b,ic) in enumerate([('Логотип','Готово','check_circle'),('Фотографии','12 файлов','photo_library'),('Тексты','Готово','description'),('Реквизиты','Ожидаются','article')]):row(l,b,226+i*84,icon=ic)
row('Новые материалы','Пока не добавлены',580,icon='attach_file',preview=binding('materials_status','Пока не добавлены',text={'supporting':'{{materials_status}}'}))
button('Добавить материалы',702,'c-add-materials',icon='add')

screen('c-add-materials','CLIENT · Добавление материалов','Добавить материалы',back='c-materials',role='client',selected=1,note='Simulates a file attachment using predefined metadata. Does not open the device picker or read any real file.')
card('Фотографии работ','Демо-файл: work-example.jpg · 240 КБ. Содержимое не загружается.',166,122,fill='primaryContainer')
button('Добавить пример файла',376,'c-materials',preview={'set':{'materials_status':'work-example.jpg · добавлен'}},icon='upload_file')

screen('c-cost','CLIENT 10 · Стоимость','Стоимость',back='c-project',role='client',selected=1,note='Fictional financial breakdown. Development, paid, remainder, third-party annual charges, infrastructure monthly charges and paid APIs stay separate. No checkout or payment actions.')
box(110,166,'primaryContainer');kicker('РАЗРАБОТКА',136);txt('30 000 ₽',174,42,bold=True);txt('Оплачено 15 000 ₽ · осталось 15 000 ₽',236,15)
row('Сторонние расходы','Домен · 199 ₽ / год',300,icon='language')
row('Инфраструктура ежемесячно','Хостинг · 500 ₽ / мес.',390,icon='dns')
row('Платные API и сервисы','Нет',480,icon='api')
card('Условия проекта','Суммы демонстрационные. Разработка оплачивается отдельно от домена и инфраструктуры.',594,112)

screen('c-chat','CLIENT 11 · Сообщения','Сообщения',back='c-home',role='client',selected=2,note='Simple project chat, not MAX/VK UI or a promised messenger proxy. One shared last-message slot per side, memory only. Production channel model remains to be decided.')
kicker('САЙТ АВТОСЕРВИСА')
card('MITIN DEV · 10:30','Мобильная версия готова. Посмотрите демо, когда будет удобно.',152,108,fill='primaryContainer')
card('Алексей · 10:35','Спасибо, посмотрю сегодня.',282,90)
card('MITIN DEV · новый ответ','Ждём ваши материалы.',394,96,preview=binding('owner_message','Ждём ваши материалы.',text={'supporting':'{{owner_message}}'}))
card('Вы · последнее сообщение','Фотографии подготовлю вечером.',510,90,preview=binding('client_message','Фотографии подготовлю вечером.',text={'supporting':'{{client_message}}'}))
field('Сообщение по проекту','chat_input','Отправлю фотографии сегодня',628)
button('Отправить',712,'c-chat',icon='send',preview={'set':{'client_message':'{{chat_input}}','chat_input':''}})

screen('c-support','CLIENT 12 · Поддержка','Поддержка',back='c-home',role='client',note='Separate post-launch scenario for p001, demonstrating its future lifecycle; the current development fixture is not silently changed. Production badge is demo status, not a health check. Support is fictional until 30.09.2026.')
kicker('ПОСЛЕ ЗАПУСКА · ДЕМО');txt('Мой сайт работает',154,28,bold=True)
card('Production','Пример состояния после запуска.',226,92,fill='primaryContainer')
card('Сопровождение активно','До 30 сентября 2026 · контроль доступности и помощь по работе сайта.',338,104)
button('Сообщить о проблеме',480,'c-support-form',icon='report',preview={'set':{'support_type':'Проблема'}})
button('Заказать доработку',552,'c-support-form',variant='tonal',icon='construction',preview={'set':{'support_type':'Доработка'}})
button('Новый проект',624,'c-type',variant='outlined',icon='add')
row('Последнее обращение','Обращений пока нет',702,icon='support_agent',preview=binding('support_last','Обращений пока нет',text={'supporting':'{{support_last}}'}))

screen('c-support-form','CLIENT · Обращение','Поддержка проекта',back='c-support',role='client',note='Mock SupportRequest. No notification or ticket is created outside local memory.')
row('Тип обращения','Проблема',130,icon='support_agent',preview=binding('support_type','Проблема',text={'supporting':'{{support_type}}'}))
field('Опишите задачу','support_input','Нужно изменить часы работы',274)
button('Сохранить обращение',398,'c-support',preview={'set':{'support_last':'{{support_type}}: {{support_input}}'}},icon='check')

screen('c-profile','CLIENT · Профиль','Профиль',role='client',selected=3,note='Fictional demo profile; no real personal data. Privacy/consent placeholders describe the future flow but are not legal documents. There is no in-product role toggle.')
txt('Алексей Смирнов',132,28,bold=True);txt('Демонстрационный профиль',180,15)
card('Данные аккаунта','alexey@example.test · телефон не указан',238,98)
row('Данные и доступ','Будущая privacy и настройки',368,'c-privacy','shield')
row('Поддержка','Обратиться по проекту',460,'c-support','support_agent')
button('Выйти',650,'c-welcome',variant='outlined',icon='logout')

screen('c-privacy','CLIENT · Данные и доступ','Данные и доступ',back='c-profile',role='client',selected=3,note='Architecture placeholder only. This is not a privacy policy, consent or legal document. No legal/РКН content is modified.')
card('Только ваши проекты','После авторизации доступ определяет сервер. Чужие проекты и файлы недоступны.',150,112,fill='primaryContainer')
card('Перед реальным запуском','Здесь появятся утверждённая политика, необходимые согласия и действия с данными.',290,126)
card('Сейчас — прототип','Ввод в Preview остаётся в памяти до закрытия. Используйте только вымышленные данные.',450,114)

# Owner screens use the same fixture identifiers and Preview keys.
counts={'new':sum(l['status']=='new' for l in DATA['leads']),'active':sum(p['status']=='active' for p in DATA['projects']),'waiting':sum(p['waiting_client'] for p in DATA['projects']),'messages':len(DATA['unread_messages'])}
screen('o-home','OWNER 01 · Dashboard','MITIN DEV',role='owner',note='Owner-only production destination. Metrics are derived from the fictional cohort snapshot on 06.09.2026, not live analytics; no real CRM access.')
kicker('РАБОЧИЙ ДЕНЬ');txt('Всё под контролем',145,29,bold=True)
for i,(label,num,to) in enumerate([('Новые заявки',counts['new'],'o-leads'),('Активные проекты',counts['active'],'o-projects'),('Ожидают клиента',counts['waiting'],'o-waiting-projects'),('Новые сообщения',counts['messages'],'o-messages')]):
    x=16+(i%2)*196;y=218+(i//2)*136
    b=box(y,120,'primaryContainer' if i==0 else 'surfaceContainerLow',x,184)
    b['action']=action(to);b['note']=label
    txt(str(num),y+14,38,x+16,True,action=action(to))
    txt(label,y+76,14,x+14,action=action(to))
kicker('ПОСЛЕДНЯЯ ЗАЯВКА',516)
row('Алексей Смирнов · №1045','Сайт · 20–40 тыс. · приложение',556,'o-lead','person',preview=live(supporting='{{project_type}} · {{budget_range}} · приложение'))
card('Ближайшее действие','Автосервис: получить фотографии работ.',646,88,'o-project')
txt('Срез demo data · 06.09.2026',748,12)

statuses={'new':'Новые','in_progress':'В работе','waiting':'Ожидание','closed':'Закрытые'}
lead_frames={'new':'o-leads','in_progress':'o-leads-work','waiting':'o-leads-wait','closed':'o-leads-closed'}
for sk in statuses:
    screen(lead_frames[sk],'OWNER 02 · '+statuses[sk],'Заявки',back='o-home',role='owner',selected=1,note='Filter over seeded fictional Lead cohort. Each row opens its own Lead/project; no backend query. Snapshot filters are not recalculated by local status edits.')
    add('tabs','',0,102,size=412,tabs=[dict(label=l) for l in statuses.values()],selected=list(statuses).index(sk),actions={f'tab:{i}':dict(to=lead_frames[k],transition='fade') for i,k in enumerate(statuses)})
    for i,l in enumerate([l for l in DATA['leads'] if l['status']==sk]):
        to='o-lead' if l['id']=='1045' else 'o-lead-'+l['id']
        c=card(l['client']+' · №'+l['id'],f'{l["type"]} · {l["budget_range"]} · {l["source"]} · {statuses[l["status"]]} · {l["date"]}',180+i*148,126,to)
        if l['id']=='1045': c['preview']=live(supporting='{{project_type}} · {{budget_range}} · Приложение · {{lead_status}} · 06.09.2026')
    txt('Демонстрационный список заявок',736,13)

screen('o-lead','OWNER 03 · Карточка заявки','Заявка №1045',back='o-leads',role='owner',selected=1,note='Same Lead #1045 as client brief, shared local bindings. Contact is reserved .test address; no phone number. Create project does not duplicate Lead. Budget range is not a contract amount.')
txt('Алексей Смирнов',114,28,bold=True);txt('alexey@example.test',157,14)
row('Источник · бюджет','Приложение · 20–40 тыс.',202,icon='inbox',preview=live(supporting='Приложение · {{budget_range}}'))
card('AI-бриф / задача','Сайт для автосервиса: услуги и заявки',290,102,preview=live(supporting='{{project_type}}. {{task}}'))
card('ТЗ и комментарии','Услуги, каталог, форма заявки',408,124,'o-brief-full',preview=live(supporting='{{features}}. {{integrations}}. {{comments}}'))
row('Статус заявки','Новая',550,'o-lead-status','flag',preview=binding('lead_status','Новая',text={'supporting':'{{lead_status}}'}))
button('Принять в работу',646,'o-lead',variant='tonal',preview={'set':{'lead_status':'В работе'}})
button('Создать проект',714,'o-new-project',icon='add',preview={'set':{'lead_status':'Проект создан','new_project_created':'Да'}})

screen('o-brief-full','OWNER · Полное ТЗ','Бриф и ТЗ',back='o-lead',role='owner',selected=1,note='Full-length brief values remain readable; cards wrap text. Inputs are demo text, not real personal data.')
for i,(label,key,initial) in enumerate([('Задача','task','Сайт для автосервиса: услуги и заявки'),('Функции','features','Услуги, каталог, форма заявки'),('Интеграции','integrations','Уведомления в Telegram'),('Сроки','deadline','В течение трёх недель'),('Комментарии','comments','Фотографии подготовлю позже')]):card(label,initial,108+i*130,114,preview=live(supporting='{{'+key+'}}'))

screen('o-lead-status','OWNER · Статус заявки','Изменить статус',back='o-lead',role='owner',selected=1,note='Local status edit only. Labels map to future existing Lead status enum after CRM schema verification.')
select('Статус заявки','lead_status','Новая',['Новая','В работе','Ожидание','Закрыта','Проект создан'],180)
button('Готово',394,'o-lead',icon='check')

for l in DATA['leads']:
    if l['id']=='1045':continue
    screen('o-lead-'+l['id'],'OWNER · Заявка '+l['id'],'Заявка №'+l['id'],back=lead_frames[l['status']],role='owner',selected=1,note='Distinct fictional Lead. No real client contact data. A converted Lead opens the linked Project without duplication.')
    txt(l['client'],126,28,bold=True)
    for i,(lab,val) in enumerate([('Тип / бюджет',l['type']+' · '+l['budget_range']),('Источник / дата',l['source']+' · '+l['date']),('Статус',statuses[l['status']])]):row(lab,val,204+i*100,icon='description')
    card('Краткий бриф','Демонстрационная задача: услуги, описание и удобная связь с клиентами.',534,110)
    if l['project_id']: button('Открыть проект',696,'o-project' if l['project_id']=='p001' else 'o-'+l['project_id'],icon='work')
    else:button('К списку заявок',696,lead_frames[l['status']],variant='tonal')

for fid,waiting in [('o-projects',False),('o-waiting-projects',True)]:
    screen(fid,'OWNER 04 · '+('Ожидают клиента' if waiting else 'Проекты'),'Проекты',back='o-home',role='owner',selected=2,note='Only fictional projects from demo-data.json. Real РиТМассаж and DIVEEV STUDIO are intentionally replaced with fictional equivalents. New-project route uses the same Lead binding, not a new CRM.')
    kicker('ОЖИДАЮТ КЛИЕНТА' if waiting else 'ПРОЕКТЫ В РАБОТЕ')
    for i,p in enumerate([p for p in DATA['projects'] if not waiting or p['waiting_client']]):
        card(p['name'],p['client']+' · '+p['stage']+'. '+p['next'],160+i*154,138,'o-project' if p['id']=='p001' else 'o-'+p['id'])
    button('Проект из заявки №1045',656,'o-new-project',variant='tonal',preview={'when':{'key':'new_project_created','equals':'Да'}})
    txt('Вымышленные проекты · demo data',746,12)

screen('o-project','OWNER 05 · Карточка проекта','Проект',back='o-projects',role='owner',selected=2,note='Main owner work model: status / agreed / cost / waiting for client / next action. Same p001 as client. Stage and demo decisions are shared within Preview.')
txt('Сайт автосервиса',110,28,bold=True);txt('Алексей Смирнов · до 18.09.2026',153,14)
card('Разработка · 30 000 ₽','Оплачено 15 000 ₽ · остаток 15 000 ₽',190,90,fill='primaryContainer')
row('Этап проекта','Разработка',296,'o-stages','checklist',preview=live(supporting='{{current_stage}} · {{stage_2}}'))
card('Что согласовано','Главная, услуги, каталог и форма заявки. Мобильная версия входит в стоимость.',380,102)
row('Что ждём от клиента','Фотографии работ',502,'o-materials','folder')
row('Демо · решение клиента','Ожидается решение',582,'o-demo-review','fact_check',preview=live(supporting='{{demo_status}}'))
row('Следующее действие','Получить материалы',662,'o-chat','arrow_forward')

screen('o-stages','OWNER 06 · Этапы','Этапы проекта',back='o-project',role='owner',selected=2,note='All six stages support not_started, in_progress, waiting_client, completed. Local selections immediately appear in client Project. No migration or database mutation. Current stage is a separate explicit field, never inferred from percentages.')
select('Текущий этап','current_stage','Разработка',['ТЗ','Дизайн','Разработка','Проверка','Запуск','Сопровождение'],108)
for i,(name,default) in enumerate([('ТЗ','Завершён'),('Дизайн','Завершён'),('Разработка','В работе'),('Проверка','Не начат'),('Запуск','Не начат'),('Сопровождение','Не начат')]):select(name,f'stage_{i}',default,DATA['stage_statuses'],202+i*82)
button('Готово',716,'o-project',icon='check')

screen('o-new-project','OWNER · Проект из заявки','Новый проект',back='o-lead',role='owner',selected=2,note='Created mock project p004 links Lead #1045; repeated taps open this same project, no duplicate. Range is not a price. Cost, payment, deadline and agreement remain unset until negotiated.')
txt('Проект · Сайт',118,28,bold=True,preview=live(text='Проект · {{project_type}}'))
row('Основание','Заявка №1045 · Алексей Смирнов',174,icon='link')
card('Статус: согласование','Сначала подтвердить ТЗ, стоимость и сроки.',264,98,fill='primaryContainer')
row('Стоимость / оплата','Не согласована / нет оплаты',384,icon='payments')
row('Бюджет клиента','20–40 тыс.',470,icon='account_balance_wallet',preview=live(supporting='{{budget_range}}'))
row('Срок','Не согласован',556,icon='event')
button('Этапы нового проекта',660,'o-new-stages',icon='checklist')
button('Открыть исходную заявку',726,'o-lead',variant='tonal')

screen('o-new-stages','OWNER · Этапы нового проекта','Этапы нового проекта',back='o-new-project',role='owner',selected=2,note='Independent stages of new mock project p004, not p001. Every status editable locally; no backend or database.')
for i,name in enumerate(['ТЗ','Дизайн','Разработка','Проверка','Запуск','Сопровождение']):select(name,f'new_stage_{i}','Не начат',DATA['stage_statuses'],128+i*88)
button('Готово',706,'o-new-project',icon='check')

for p in DATA['projects'][1:]:
    screen('o-'+p['id'],'OWNER · '+p['name'],'Карточка проекта',back='o-projects',role='owner',selected=2,note='Distinct fictional project linked to its existing demo Lead. Financial values come from the fixture, not live CRM.')
    txt(p['name'],122,25,bold=True);txt(p['client'],164,16)
    card(p['stage'],f'Стоимость {p["cost"]} ₽ · оплачено {p["paid"]} ₽',226,96,fill='primaryContainer')
    card('Что согласовано','Сайт с услугами, примерами работ и формой связи.',348,96)
    card('Что ждём от клиента','Согласование демо' if p['waiting_client'] else 'Сейчас ничего',466,96)
    card('Следующее действие',p['next'],584,102)

screen('o-demo-review','OWNER · Согласование демо','Решение клиента',back='o-project',role='owner',selected=2,note='Read the same demo decision and comment that the client edited; this is not a real approval audit log.')
card('Согласование','Ожидается решение',156,110,fill='primaryContainer',preview=live(supporting='{{demo_status}}'))
card('Комментарий клиента','Пока без комментариев',300,144,preview=live(supporting='{{demo_feedback}}'))
button('Обсудить с клиентом',530,'o-chat',variant='tonal',icon='chat')

screen('o-materials','OWNER · Материалы','Материалы клиента',back='o-project',role='owner',selected=2,note='Metadata-only view of the same mock upload state as the client. No file content or actual uploads.')
row('Логотип и тексты','Готово',144,icon='check_circle');row('Фотографии','12 файлов',246,icon='photo_library');row('Реквизиты','Ожидаются',348,icon='article')
card('Новые материалы','Пока не добавлены',466,116,preview=live(supporting='{{materials_status}}'))
button('Написать клиенту',658,'o-chat',variant='tonal',icon='chat')

screen('o-messages','OWNER · Входящие','Сообщения',back='o-home',role='owner',note='Demo inbox derived from fixture unread_messages. Last messages are shared with client in memory.')
row('Алексей · Сайт автосервиса','Новое сообщение',156,'o-chat','chat_bubble',preview=live(supporting='{{client_message}}'))
row('Павел · Точка баланса','Посмотрел демо. Обсудим в проекте.',256,'o-p003','chat_bubble')

screen('o-chat','OWNER · Чат проекта','Алексей · проект',back='o-project',role='owner',note='Project chat mock. Reuses client_message / owner_message local state, not existing MessengerMessage or a new external channel.')
card('Алексей · последнее сообщение','Фотографии подготовлю вечером.',156,126,fill='primaryContainer',preview=live(supporting='{{client_message}}'))
card('MITIN DEV · последний ответ','Ждём ваши материалы.',316,110,preview=live(supporting='{{owner_message}}'))
field('Ответ клиенту','owner_input','Спасибо, буду ждать фотографии',494)
button('Отправить ответ',610,'o-chat',icon='send',preview={'set':{'owner_message':'{{owner_input}}','owner_input':''}})

screen('o-more','OWNER · Ещё','Ещё',role='owner',selected=3,note='Owner admin-only menu. No production role switch. Analytics uses a reproducible demo cohort; client support is a shared mock request.')
kicker('MITIN DEV · ВЛАДЕЛЕЦ')
row('Аналитика','Заявки, источники и конверсия',178,'o-analytics','analytics')
row('Сообщения','Проектные обсуждения',276,'o-messages','forum')
row('Поддержка','Обращения клиентов',374,'o-support','support_agent')
card('Доступ владельца','В будущем — только для администратора. Роль и разрешения проверяет backend.',514,120,fill='primaryContainer')

screen('o-support','OWNER · Поддержка','Обращения',back='o-more',role='owner',selected=3,note='Same support_last binding as client; no notifications are sent and no real support ticket exists.')
card('Алексей · Сайт автосервиса','Обращений пока нет',176,160,preview=live(supporting='{{support_last}}'))
button('Обсудить обращение',450,'o-chat',variant='tonal')

converted=sum(l['project_id'] is not None for l in DATA['leads'])
screen('o-analytics','OWNER 07 · Аналитика','Аналитика',back='o-more',role='owner',selected=3,note='All values are derived from demo-data.json snapshot, not invented operational metrics. Cohort 6 Leads / 3 converted / 50%. Denominator includes all 6 cohort Leads, including closed. Local edits do not recalculate this snapshot.')
kicker('DEMO DATA · СРЕЗ 06.09.2026')
card('Заявки → проекты',f'{len(DATA["leads"])} заявок → {converted} проекта · {converted/len(DATA["leads"]):.0%}',152,104,fill='primaryContainer')
kicker('ИСТОЧНИКИ',286)
from collections import Counter
sources=Counter(l['source'] for l in DATA['leads'])
for i,(source,count) in enumerate(sources.items()):txt(source,324+i*38,16);txt(str(count),324+i*38,18,x=354,bold=True)
row('Бюджеты','Все 6 диапазонов budget_range',512,'o-budgets','payments')
row('Проекты','2 активных · 1 на сопровождении',602,'o-projects','work')
card('Как считаем','Конверсия = заявки с project_id / все заявки выборки. 3 / 6 = 50%.',702,72)

screen('o-budgets','OWNER · Бюджеты','Бюджеты заявок',back='o-analytics',role='owner',selected=3,note='Counts from the same fictional cohort, including zero buckets; no assumptions about live Lead budget values.')
for i,b in enumerate(DATA['budget_ranges']):row(b,str(sum(l['budget_range']==b for l in DATA['leads']))+' заявок',116+i*98,icon='payments')

out=ROOT/'prototypes/mitin-dev/mitin-dev.m3e.json'
out.write_text(json.dumps(D,ensure_ascii=False,indent=2)+'\n')
print(f'{len(D["frames"])} screens, {len(D["groups"])} native groups → {out.relative_to(ROOT)}')
