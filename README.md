# FratikB0T
[![TeamCity](https://ci.fratikbot.pl/app/rest/builds/buildType:FratikB0Ty_FratikB0T_Buildy_Build/statusIcon)](https://ci.fratikbot.pl/project/FratikB0Ty_FratikB0T)
[![Crowdin](https://badges.crowdin.net/fratikb0t/localized.svg)](https://crowdin.com/project/fratikb0t)
![ForksCound](https://img.shields.io/github/forks/fratik/FratikB0T)
![Contributors](https://img.shields.io/github/contributors/fratik/FratikB0T)
![StarBaterie](https://img.shields.io/github/stars/fratik/FratikB0T)
[![License: GPLv3](https://img.shields.io/github/license/fratik/FratikB0T)](https://www.gnu.org/licenses/gpl-3.0)

Wielofunkcyjny bot Discord. Teraz Open Source.  
_(bosh brzmię jak nadgryzione jabłko)_

## Zewnętrzne linki
[Strona główna](https://fratikbot.pl)  
[YouTrack (tablica bugów/feature requestów)](https://issues.fratikbot.pl)  
[TeamCity (CI)](https://ci.fratikbot.pl)  
[Discord Bot List](https://top.gg/bot/338359366891732993)  
[Serwer Discord](#serwer-discord)

## Co to być?
Bot na Discorda zawierajacy 131 komend, istniejący od 2 lat i posiadający 3000+ serwerów.
Całą wieczność na prywatnych repo, ale odechciewa mi się powoli to wszystko ogarniać więc boom, Open Source.

## Kompilacja
### Rdzeń
Użyj `gradlew.bat core:build` (Windows) / `./gradlew core:build` (Linux/macOS) by zbudować rdzeń w folderze `core/build/libs`.

### Pluginy
Użyj `gradlew.bat jar` (Windows) / `./gradlew jar` (Linux/macOS), a pluginy magicznie pojawią się w folderze `plugins`.

## Użycie
### Wymagania
- Java (w produkcji używane OpenJDK w wersji 8 lub 11)
- PostgreSQL (9/10/11 działają)
- [Lavalink](https://github.com/freyacodes/Lavalink)
- [Redis](https://redis.io/)
- klucze API: (opcjonalne)
    - image-server: https://api.fratikbot.pl oraz klucz lub ~~(Open Source wkrótce)~~ [self-hosted](https://github.com/fratik/FratikB0T-API)
    - yt/yt2: osobne klucze do YouTube API, używane w wyszukiwaniu utworów/zdobywaniu miniatur
    - sentry-dsn: (do apiKeys, nie apiUrls!) link do sentry
    - pixiv: do komendy fb!rule34, dane logowania w formacie `e-mail:hasło`
    
### Uruchomienie
```shell script
java -jar <zbudowany rdzeń>.jar <token>
```

### Konfiguracja
Po pierwszym uruchomieniu bot utworzy plik `config.json`, należy go ustawić.

## Specjalne podziękowania
Rdzeń wzorowany na Kyoko v2.  
Dashboard (Open Source wkrótce) dla wersji 3 stworzony przez [SebeeDev](https://github.com/SebeeDev). 

## Serwer Discord
[![https://discord.com/api/guilds/345655892882096139/embed.png?style=banner4](https://discord.com/api/guilds/345655892882096139/embed.png?style=banner4)](https://discord.gg/CZ8pXah)
