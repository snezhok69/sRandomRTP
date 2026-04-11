# sRandomRTP 3.0 — Порталы, новые радиусы и поддержка Folia

Добавление новых команд!​
1.Были добавлены следующие команды:

/rtp far  
/rtp middle  
/rtp portal set <name> <radius> [circle|square]  
/rtp portal del <name>  
/rtp portal list [-p:<page>]  
/rtp chunky <radius>  
/rtp chunky stop  
/rtp player <player> [world]  
/rtp biome <biome|biome1,biome2>  

1.1. /rtp far — телепортирует в дальнюю зону по радиусам из far.yml. Пермишион: sRandomRTP.Command.Far  
1.2. /rtp middle — телепортирует в среднюю зону по радиусам из middle.yml. Пермишион: sRandomRTP.Command.Middle  
1.3. /rtp portal <set|del|list> — создаёт и управляет защищёнными порталами (круг/квадрат), с частицами, материалами из конфига и личным cooldown; данные хранятся в SQLite. Пермишион: sRandomRTP.Command.Portal  
1.4. /rtp chunky <radius|stop> — запускает или останавливает проген чанков через Chunky прямо из плагина. Пермишион: sRandomRTP.Command.Chunky  
1.5. /rtp player <player> [world] — может отправить игрока в другой мир, учитывает запретные миры и отправляет уведомления/редиректы. Пермишион: sRandomRTP.Command.Player  
1.6. /rtp biome <biome1,biome2> — принимает списки биомов и использует отдельные запреты блоков/биомов для этой команды. Пермишион: sRandomRTP.Command.Biome  
1.7. /rtp accept и /rtp deny используют новые тайм-ауты запросов и защиту от повторных заявок. Пермишионы: sRandomRTP.Command.Accept / sRandomRTP.Command.Deny  
1.8. /rtp back и /rtp base уважают новые правила высот, миров и предзагрузки чанков. Пермишионы: sRandomRTP.Command.Back / sRandomRTP.Command.Base  
1.9. Пермишион sRandomRtp.Cooldown.N задаёт персональный кулдаун, глобальный обход переименован в sRandomRTP.Command.bypass.

Новые возможности телепортации!​
1. Отдельные пресеты радиусов: базовый (teleport.yml), средний (middle.yml) и дальний (far.yml) с выбором круга/квадрата, абсолютными координатами и per-world настройками.  
2. Ограничения по Y для мира/Незера/Энда, автозапрет пещерных и океанических биомов и защита от выхода за границы мира.  
3. Тайм-ауты поиска (attempt/total), параллельный подбор координат и подсказка long-teleport-wait, чтобы поиск не зависал.  
4. Новые триггеры отмены: движение мышью, перемещение, урон и ломание блока — у каждого свой флаг кулдауна.  
5. Проверка достижений для /rtp world, гибкий редирект из запрещённых миров и возможность указать альтернативный мир прямо в /rtp player.  
6. Прогрев и предзагрузка чанков (chunk-warming/chunk-loading) для плавных телепортов на Paper и Folia.  
7. Порталы могут выполнять команды при входе и при желании сразу запускать RTP в заданный мир.

Обновление конфигурации и локализации!​
1. Настройки:
   - config.yml: Disable-Moved-Too-Quickly-Messages, короткие коды языков (en, ru, ...), обновлён список пермишионов.  
   - teleport.yml: minY, minY-nether, minY-end, block-cave-biomes, block-ocean-river-biomes, per-world радиусы, coordinate-generation, use-absolute-coordinates, parallel-search, teleport-timeout, break-block-cancel-rtp и redirect для запрещённых миров.  
   - far.yml и middle.yml: отдельные радиусы и формы генерации для новых команд.  
   - portal.yml: материалы/частицы портала, защита блоков, cooldown после прыжка и список команд при входе.  
   - chunk-loading.yml: предзагрузка вокруг точки, тайм-ауты, лимиты и планировщик прогрева.  
   - near.yml: упрощены радиусы для /rtp near.  
2. Локализации перенесены в lang/*.yml и пополнены ключами:
   - long-teleport-wait, titleMessage-loading/subtitleMessage-loading, worldborder-error, redirect-world/rederictworldnear-error;  
   - полный набор сообщений для порталов, защиты блоков, Chunky, расширенные подсказки /rtp player и плейсхолдер %y% в teleported/title/subtitle.  
3. Добавлен турецкий язык (tr.yml), имена всех файлов приведены к единому формату.

Платформа и зависимости!​
1. Версия плагина 3.0, API 1.16, отметка folia-supported и обязательный LuckPerms.  
2. Вшиты FoliaLib и PaperLib для безопасного планирования задач; SQLite хранит данные порталов.  
3. Chunky добавлен как softdepend, WorldEdit/Vault/PlaceholderAPI остаются опциональными.  
4. Maven: shade 3.4.1 с релокациями библиотек, репозитории tcoded/codemc для новых артефактов.

Мелкие изменения!​
1. Логирование стало гибче, можно подавлять спам “moved too quickly” в консоли.  
2. Сообщения о телепортации показывают координату Y и новые подсказки для долгого поиска.  
3. Исправлены повторные запросы /rtp player, отправка в запрещённые миры, удаление порталов и обработка отсутствия WorldGuard/Vault/Chunky.  
4. Добавлены проверки на неверные радиусы, имена порталов и пустые страницы списков, чтобы не словить краши или подвисания.  
5. Множество мелких оптимизаций в безопасности телепортации и управлении задачами.

Примечания
- Обновление 3.0 делалось очень долго, поэтому какие-то моменты могли быть упущены. Пишите о багах на Discord-сервере или создавайте GitHub issue.
