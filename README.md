README (Russian)


DiscordIPC-Java23
Перенос оригинальной библиотеки jagrosh/DiscordIPC на Java 21 / Java 23, полностью совместимый с JitPack и современными версиями JDK.
Эта библиотека позволяет легко интегрировать Discord Rich Presence в ваши Java-приложения, Minecraft-клиенты, лаунчеры, desktop-утилиты и любые другие программы.
🚀 Возможности
Подключение к Discord IPC
Отправка Rich Presence
Кастомные кнопки (2 шт.)
Поддержка больших/малых изображений
Поддержка timestamps
Совместимость с Java 21 и Java 23
Работает на Windows / macOS / Linux
📦 Установка через Gradle (JitPack)
1. Добавьте репозиторий
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
2. Добавьте зависимость
dependencies {
    implementation 'com.github.fadu335:DiscordIPC-Java23:<version>'
}
Последняя версия смотрите здесь:
👉 https://jitpack.io/#fadu335/DiscordIPC-Java23
💡 Пример использования
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;

import java.time.OffsetDateTime;

public class DiscordManager {

    // Ваш Discord application ID
    private final IPCClient client = new IPCClient(123456789012345678L);

    private RichPresence.Builder createPresence() {
        return new RichPresence.Builder()
                .setDetails("Example Application")
                .setState("Running demo mode")
                .setStartTimestamp(OffsetDateTime.now())
                .setLargeImage("large_icon", "Example tooltip")
                .setButton1Text("Website")
                .setButton1Url("https://example.com")
                .setButton2Text("GitHub")
                .setButton2Url("https://github.com/example");
    }

    public void connect() {
        try {
            client.setListener(new IPCListener() {
                @Override
                public void onReady(IPCClient client) {
                    client.subscribe(IPCClient.Event.CURRENT_USER_UPDATE);
                    String discordUsername = client.getDiscordUsername();
                    String discordNickname = client.getDiscordNickname();
                    String discordAvatarUrl = client.getDiscordAvatarUrl();
                    System.out.println("Discord username: " + discordUsername);
                    System.out.println("Discord nickname: " + discordNickname);
                    System.out.println("Discord avatar: " + discordAvatarUrl);
                    client.sendRichPresence(createPresence().build());
                }
            });

            client.connect();
        } catch (Exception ignored) {}
    }
}
🧩 Структура RichPresence Builder
Метод	Описание
setDetails(String)	Верхняя строка RPC
setState(String)	Нижняя строка
setStartTimestamp(OffsetDateTime)	Время начала
setEndTimestamp(OffsetDateTime)	Время окончания
setLargeImage(key, text)	Большое изображение + подсказка
setSmallImage(key, text)	Маленькое изображение + подсказка
setButton1Text/Url	Первая кнопка
setButton2Text/Url	Вторая кнопка
⚠️ Важно
Для работы кнопок в Rich Presence:
У вас должен быть создан Discord Application
В разделе Rich Presence > Assets должны быть загружены картинки
Кнопки отображаются только в самом Discord клиенте, не на мобильных
📄 Лицензия
Apache License 2.0
Оригинальный автор — jagrosh

🇺🇸 README (English)


DiscordIPC-Java23
A modernized and updated fork of jagrosh/DiscordIPC, fully compatible with Java 21 / Java 23, packaged for easy use via JitPack.
This library allows Java applications, game clients, and tools to integrate Discord Rich Presence.
🚀 Features
Discord IPC connection
Sending Rich Presence payloads
Support for 2 custom buttons
Support for timestamps
Large/small assets
Updated for Java 21 / Java 23
Works on Windows / macOS / Linux
📦 Installation (Gradle + JitPack)
1. Add repository
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
2. Add dependency
dependencies {
    implementation 'com.github.fadu335:DiscordIPC-Java23:<version>'
}
Latest version:
👉 https://jitpack.io/#fadu335/DiscordIPC-Java23
💡 Usage Example
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;

import java.time.OffsetDateTime;

public class DiscordManager {

    private final IPCClient client = new IPCClient(123456789012345678L);

    private RichPresence.Builder createPresence() {
        return new RichPresence.Builder()
                .setDetails("Example Application")
                .setState("Running demo mode")
                .setStartTimestamp(OffsetDateTime.now())
                .setLargeImage("large_icon", "Example tooltip")
                .setButton1Text("Website")
                .setButton1Url("https://example.com")
                .setButton2Text("GitHub")
                .setButton2Url("https://github.com/example");
    }

    public void connect() {
        try {
            client.setListener(new IPCListener() {
                @Override
                public void onReady(IPCClient client) {
                    client.subscribe(IPCClient.Event.CURRENT_USER_UPDATE);
                    String discordUsername = client.getDiscordUsername();
                    String discordNickname = client.getDiscordNickname();
                    String discordAvatarUrl = client.getDiscordAvatarUrl();
                    System.out.println("Discord username: " + discordUsername);
                    System.out.println("Discord nickname: " + discordNickname);
                    System.out.println("Discord avatar: " + discordAvatarUrl);
                    client.sendRichPresence(createPresence().build());
                }
            });

            client.connect();
        } catch (Exception ignored) {}
    }
}
🧩 RichPresence Fields Overview
Method	Description
setDetails(String)	First line of RPC
setState(String)	Second line
setStartTimestamp()	Start time
setEndTimestamp()	End time
setLargeImage(key, text)	Big asset
setSmallImage(key, text)	Small asset
setButton1Text/Url	First button
setButton2Text/Url	Second button
⚠️ Notes
You must create a Discord Application
Upload images to Rich Presence > Assets
Buttons show only on the desktop Discord client, not mobile
📄 License
Apache License 2.0
Original project by jagrosh
Если хочешь — могу оформить это с красивыми бейджами, логотипом, примерами скриншотов или сделать стиль в твоём фирменном стиле Rockstar.
