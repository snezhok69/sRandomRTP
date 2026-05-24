СОХРАНИТЕ ВСЕ ДАННЫЕ ПЛАГИНА (SRANDOMRTP)
ПЕРЕД ОБНОВЛЕНИЕМ!!!!!!!!!!!!!!!!



Новые команды!

1. Добавлены следующие админские/служебные команды:

/rtp settings
/rtp doctor
/rtp dump
/rtp stats
/rtp portal check
/rtp tpsbar

/rtp rambar
/rtp msptbar
/rtp allbars

1.1. /rtp settings - открывает кликабельное меню прямо в игре для включения или выключения подкоманд плагина.
Пермишион: sRandomRTP.Command.Settings
По умолчанию: выключена в Settings/commands.yml

1.2. /rtp doctor - показывает состояние плагина: версию Java, версию сервера, Paper/Folia, язык, optional-плагины, версии конфигов, активные RTP-поиски, задачи порталов и статус admin bossbar.
Пермишион: sRandomRTP.Command.Doctor
По умолчанию: выключена в Settings/commands.yml

1.3. /rtp dump - создаёт support zip в plugins/sRandomRTP/Diagnostics с диагностикой, последними ошибками и runtime-информацией.
Пермишион: sRandomRTP.Command.Dump
По умолчанию: выключена в Settings/commands.yml

1.4. /rtp stats - показывает живую статистику: активные поиски, общий RTP count, cooldowns, задачи порталов, completed/cancelled/refunded телепорты и среднее время поиска.
Пермишион: sRandomRTP.Command.Stats
По умолчанию: выключена в Settings/commands.yml

1.5. /rtp portal check - проверяет порталы на отсутствующие миры, дубли world + portalName и активные portal tasks.
Пермишион: sRandomRTP.Command.Portal.Check
По умолчанию: выключена в Settings/commands.yml

1.6. /rtp tpsbar, /rtp rambar, /rtp msptbar, /rtp allbars - дополнительные admin bossbar для TPS, RAM, MSPT или всех bar сразу.
Пермишионы: sRandomRTP.Command.TpsBar, sRandomRTP.Command.RamBar, sRandomRTP.Command.MsptBar, sRandomRTP.Command.AllBars
По умолчанию: выключены в Settings/commands.yml


Новая стабильность!

1. Chunk warming, частицы/триггеры порталов, admin bossbar и callbacks проверки обновлений теперь не трогают Bukkit API из небезопасных async-потоков.

2. Счётчик RTP теперь сохраняется пачками, а не пишет Data/rtpCount.yml после каждого телепорта.

3. RTP через cooldown bypass теперь тоже корректно считается.

4. Cooldown-пермишионы вроде sRandomRtp.Cooldown.N теперь работают стабильнее и также влияют на bossbar-отсчёт.

5. Reload теперь пишет сообщение об успешной перезагрузке после завершения reload.

6. /rtp cancel, отмена при движении, повторные RTP-запросы, выход игрока во время телепорта и возврат денег после отмены стали безопаснее.

7. Исправлено определение версии Minecraft для новой схемы Java Edition, включая 26.1, 26.1.1 и более новые 26.x сборки.


Обновление конфигов и локализации!

config.yml: добавлены diagnostic, Command-Aliases-Enabled, Command-Aliases, metrics.rtp.slow-request-threshold-ms и обновлённые комментарии по permissions.

Settings/commands.yml: добавлены переключатели для /rtp settings, /rtp doctor, /rtp dump, /rtp stats, /rtp portal check и admin bossbar-команд. Эти debug/admin команды выключены по умолчанию.

Settings/admin-bars.yml: добавлены настраиваемые bossbar для TPS, RAM и MSPT с заголовками, цветами, стилями, порогами и сообщениями.

Settings/biome.yml: добавлен отдельный профиль для поведения поиска через /rtp biome.

Settings/teleport.yml: добавлены более безопасные настройки поиска и чанков, включая parallel-search и prefer-generated-chunks.

plugin.yml: расширены permissions для новых команд, а статичные aliases удалены из plugin.yml.

Алиасы команд перенесены в config.yml:

Command-Aliases-Enabled: false
Command-Aliases:
  - randomtp
  - randomteleport

Алиасы выключены по умолчанию. Поставьте Command-Aliases-Enabled: true, если хотите включить /randomtp и /randomteleport.

Локализации в lang/*.yml расширены для:

/rtp settings, /rtp doctor, /rtp dump, /rtp stats, /rtp portal check, admin bossbar, invalid-command, сообщения успешного reload и обновлённых help-строк.


Мелкие изменения!

1. LuckPerms больше не является жёсткой зависимостью; optional-интеграции теперь обрабатываются мягче.

2. Сообщение о неизвестной команде теперь настраивается через локализацию, а не всегда берётся стандартный текст сервера.

3. При обновлении конфигов теперь создаются backup-файлы и пишется Diagnostics/config-changes.txt.

4. Диагностика startup, reload, config-changes, backup и slow RTP теперь работает только при diagnostic: true.

5. Улучшены хранение порталов, очистка порталов, частицы порталов и cooldown порталов.

6. Добавлены PlaceholderAPI значения и публичные RTP/portal events для интеграций.

7. Auto-reload теперь применяет изменения языка/diagnostic/алиасов из config.yml и переключатели команд из Settings/commands.yml после сохранения файла.


Важно для владельцев серверов!

1. Игровые RTP-команды для игроков остаются включены по умолчанию. Новые support/debug команды и admin bossbar выключены по умолчанию даже для OP, пока вы сами не включите их в Settings/commands.yml или через /rtp settings после включения этого меню.

2. /rtp settings тоже выключена по умолчанию. Чтобы управлять переключателями команд прямо из игры, сначала поставьте commands.admin.settings.enabled: true в Settings/commands.yml.

3. /rtp doctor, /rtp dump, /rtp stats, /rtp portal check, /rtp tpsbar, /rtp rambar, /rtp msptbar и /rtp allbars нужны владельцам сервера, администраторам и поддержке для проверки/отладки. Обычным игрокам они не нужны для стандартного RTP.

4. diagnostic: false стоит по умолчанию. При этом плагин не создаёт Diagnostics-файлы автоматически. Включайте diagnostic: true только если нужны полные отчёты запуска, reload, изменений конфигов и медленных RTP.

5. Алиасы команд тоже выключены по умолчанию. Включите Command-Aliases-Enabled: true в config.yml, если хотите использовать /randomtp и /randomteleport.

6. Старые конфиги мигрируются без перезаписи пользовательских значений. Как обычно, перед обновлением рабочего сервера лучше сделать backup.


Примечания

Обновление 3.1 в основном про стабильность, диагностику и совместимость после большого релиза 3.0. Большинство новых admin/support команд выключены по умолчанию, поэтому обычные игроки продолжат пользоваться плагином как раньше, пока вы сами не включите эти инструменты в Settings/commands.yml.

Если найдёте баги, пишите на Discord-сервер или создавайте GitHub issue.
