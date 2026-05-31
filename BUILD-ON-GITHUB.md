# Сборка MinerSeller через GitHub Actions

Не требует Java и Maven на твоём ПК — всё собирается в облаке GitHub.

## Шаги

1. Зайди на https://github.com → **New** → создай репозиторий (например `MinerSeller`).
   - Ничего не добавляй при создании (ни README, ни .gitignore) — просто **Create repository**.

2. Загрузи файлы проекта: **Add file → Upload files**.
   - Перетащи СОДЕРЖИМОЕ папки MinerSeller-src (файл `pom.xml`, папки `src` и `.github`).
   - ВАЖНО: папка `.github/workflows/build.yml` должна сохраниться (легко потерять при drag&drop — проверь).
   - Нажми **Commit changes**.

3. Открой вкладку **Actions** → дождись зелёной галочки у задачи «Build MinerSeller» (~1–2 мин).

4. Скачай jar: открой завершённый запуск → внизу **Artifacts** → `MinerSeller-jar` → внутри `MinerSeller-1.0.4.jar`.

5. Кинь jar в папку `plugins` сервера 1.19.4 и перезапусти сервер.

## Команды
- `/miner` (алиасы: `minebuyer`, `шахтер`, `шахтёр`, `руды`) — открыть меню.
- `/miner reload` — перезагрузить config.yml (нужно право `minerseller.admin`).

## Важно про /miner
Если на сервере стоит DeluxeMenus и у него тоже команда `miner` — будет конфликт.
Либо убери её из DeluxeMenus, либо поменяй команду в `plugin.yml`.
