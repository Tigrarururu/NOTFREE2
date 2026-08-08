# Инструкция по установке мода Hidden Money Mod

## Структура файлов

Мод имеет следующую структуру:

```
hidden-money-mod/
├── build.gradle                    # Конфигурация сборки Gradle
├── gradle.properties               # Свойства проекта (версии Minecraft, Fabric и т.д.)
├── settings.gradle                 # Настройки репозиториев Gradle
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  # Настройки Gradle Wrapper
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── hiddenmoney/
        │           └── mod/
        │               ├── HiddenMoneyMod.java           # Основной класс мода
        │               ├── HiddenMoneyModClient.java     # Клиентская логика
        │               └── mixin/
        │                   ├── ClientPlayerEntityMixin.java    # Миксин для отслеживания смерти
        │                   ├── LivingEntityMixin.java          # Миксин для урона и убийств
        │                   └── ChatInputSuggestorMixin.java    # Миксин для сообщений в чате
        └── resources/
            ├── fabric.mod.json             # Метаданные мода для Fabric
            ├── hiddenmoney.mixins.json     # Конфигурация миксинов
            └── assets/
                └── hiddenmoney/
                    ├── icon.png            # Иконка мода (пустая)
                    └── lang/
                        └── en_us.lang      # Файл локализации (пустой)
```

## Требования

- **Minecraft**: 1.20.4
- **Fabric Loader**: 0.15.7 или новее
- **Fabric API**: 0.96.0+1.20.4
- **Java**: 17 или новее

## Установка

### Вариант 1: Сборка из исходников

1. Убедитесь, что у вас установлена Java 17+ и Gradle 8.5+
2. Откройте терминал в папке с модом
3. Выполните команду для сборки:
   ```bash
   ./gradlew build
   ```
   Или на Windows:
   ```bash
   gradlew.bat build
   ```
4. После сборки файл мода появится в:
   ```
   build/libs/hidden-money-mod-1.0.0.jar
   ```
5. Скопируйте `.jar` файл в папку `mods` вашего Minecraft:
   - Windows: `%appdata%\.minecraft\mods\`
   - macOS: `~/Library/Application Support/minecraft/mods/`
   - Linux: `~/.minecraft/mods/`

### Вариант 2: Ручное размещение файлов

Если вы хотите вручную создать структуру у себя:

1. Создайте основную папку мода (например, `hidden-money-mod`)
2. Внутри создайте структуру:
   ```
   hidden-money-mod/src/main/java/com/hiddenmoney/mod/
   hidden-money-mod/src/main/resources/
   ```
3. Разместите файлы Java в соответствующие папки:
   - `HiddenMoneyMod.java` → `src/main/java/com/hiddenmoney/mod/`
   - `HiddenMoneyModClient.java` → `src/main/java/com/hiddenmoney/mod/`
   - Все файлы из `mixin/` → `src/main/java/com/hiddenmoney/mod/mixin/`
4. Разместите ресурсы:
   - `fabric.mod.json` → `src/main/resources/`
   - `hiddenmoney.mixins.json` → `src/main/resources/`
   - `icon.png` → `src/main/resources/assets/hiddenmoney/`
   - `en_us.lang` → `src/main/resources/assets/hiddenmoney/lang/`
5. Создайте файлы сборки:
   - `build.gradle` в корень
   - `gradle.properties` в корень
   - `settings.gradle` в корень
   - `gradle/wrapper/gradle-wrapper.properties`

## Сборка готового JAR

После настройки выполните:
```bash
./gradlew build
```

Готовый файл будет в `build/libs/`.

## Как работает мод

Мод полностью скрыт от пользователя:
- Нет никаких сообщений в чате о наличии мода
- Нет описаний механик в игре
- Единственный видимый элемент — панель с балансом справа сверху

### Начальный баланс
- При заходе в мир игроку даётся **1000 рублей**

### Условия потерь (автоматически)
| Действие | Потеря |
|----------|--------|
| Пройденный блок | -1 ₽ за блок |
| Сломанный блок | -50 ₽ |
| Удар по чему угодно | -10 ₽ |
| Смена выбранного слота | -5 ₽ |
| Прыжок | -20 ₽ |
| Нажатие Ctrl | -100 ₽ |
| Смерть | -500 ₽ |
| Получение урона | -150 ₽ |
| Открытие инвентаря | -15 ₽ |

### Условия прибыли (автоматически)
| Действие | Прибыль |
|----------|--------|
| Выброшенный предмет | +1 ₽ за предмет |
| Поставленный блок | +2 ₽ |
| Переход из обычного мира в Ад | +10 ₽ |
| Переход из Ада в обычный мир | +5 ₽ |
| Убийство любого существа | +5 ₽ |
| Написание сообщения в чате | +50 ₽ (только первые 5 раз) |

## Примечания

- Мод работает только на клиенте (Fabric)
- Все механики работают автономно с момента загрузки мира
- Баланс сбрасывается при выходе из мира
- Сообщение в чате считается только одно за отправку (команды не считаются)

## Лицензия

MIT
