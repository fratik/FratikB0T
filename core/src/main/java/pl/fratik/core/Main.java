/*
 * Copyright (C) 2019 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.core;

import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.requests.RestAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.crypto.AES;
import pl.fratik.core.crypto.CryptoException;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (System.getenv("SZYFROWANY_CONFIG") == null) {
            if (args.length == 0) {
                LOGGER.error("Nie podano tokenu");
                System.exit(1);
            }

            if (args.length > 1 && args[1].equals("debug")) {
                LOGGER.warn("= TRYB DEBUG WŁĄCZONY =");
                LOGGER.warn("Stacktrace będzie wysyłany do każdego RestAction, ma to negatywny wpływ na wydajność!");
                RestAction.setPassContext(true);
                RestAction.setDefaultFailure(ContextException.herePrintingTrace());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            new FratikB0T(args[0], false);
        } else {
            if (args.length > 0 && args[0].equals("edit")) {
                File cfg = new File("config.json");
                if (!cfg.exists()) {
                    LOGGER.error("Aby stworzyć zaszyfrowany config, uruchom bota normalnie, edytuj config," +
                            "następnie odpal go tak jak teraz w celu zaszyfrowania.");
                    System.exit(1);
                }
                Console console = System.console();
                if (console == null) {
                    LOGGER.error("Nie znaleziono instancji konsoli. Użycie szyfrowanych configów jest nie możliwe.");
                    System.exit(1);
                }
                byte[] conf = readConfig(cfg);
                ByteBuffer byteBuffer = ByteBuffer.wrap(conf);
                int noonceSize = byteBuffer.getInt();
                byteBuffer.clear();
                if (noonceSize < 12 || noonceSize >= 16) {
                    LOGGER.info("Wykryto niezaszyfrowany config.");
                    char[] chars = console.readPassword("Utwórz hasło: ");
                    byte[] encrypted;
                    try {
                        encrypted = AES.encrypt(conf, chars);
                    } catch (CryptoException e) {
                        LOGGER.error("Nie udało się zaszyfrować configu.", e);
                        System.exit(1);
                        return;
                    }
                    try {
                        Files.write(cfg.toPath(), encrypted);
                    } catch (IOException e) {
                        LOGGER.error("Nie udało się zapisać zaszyfrowanego configu.", e);
                        System.exit(1);
                        return;
                    }
                    LOGGER.info("Pomyślnie zaszyfrowano config. Aby go edytować, uruchom bota w ten sam sposób ponownie.");
                    System.exit(0);
                    return;
                }
                LOGGER.info("Teraz config zostanie odszyfrowany.");
                char[] chars = console.readPassword("Podaj hasło: ");
                byte[] decrypted;
                try {
                    decrypted = AES.decrypt(conf, chars);
                } catch (CryptoException e) {
                    LOGGER.error("Nie udało się odszyfrować configu. Prawdopodobnie podano nieprawidłowe hasło.");
                    System.exit(1);
                    return;
                }
                try {
                    Files.write(cfg.toPath(), decrypted);
                } catch (IOException e) {
                    LOGGER.error("Nie udało się zapisać odszyfrowanego configu.", e);
                    System.exit(1);
                    return;
                }
                LOGGER.info("Config został odszyfrowany. Edytuj go i naciśnij enter by go zaszyfrować.\n" +
                        "Jeżeli chcesz zmienić hasło napisz 'zmien' (bez ') i potwierdź enterem.\n" +
                        "Jeżeli chcesz wyłączyć szyfrowanie configu, napisz 'wylacz' (bez ') i potwierdź enterem.");
                String potwierdzenie = console.readLine();
                if (potwierdzenie.equals("zmien")) {
                    chars = console.readPassword("Nowe hasło: ");
                }
                if (potwierdzenie.equals("wylacz")) {
                    LOGGER.info("Config nie zostanie zaszyfrowany.");
                    System.exit(0);
                    return;
                }
                conf = readConfig(cfg);
                try {
                    Files.write(cfg.toPath(), AES.encrypt(conf, chars));
                } catch (IOException e) {
                    LOGGER.error("Nie udało się zapisać zaszyfrowanego configu.", e);
                    System.exit(1);
                    return;
                } catch (CryptoException e) {
                    LOGGER.error("Nie udało się zaszyfrować configu.", e);
                    System.exit(1);
                    return;
                }
                LOGGER.info("Pomyślnie zaszyfrowano config. Aby go edytować, uruchom bota w ten sam sposób ponownie.");
                System.exit(0);
                return;
            }
            new FratikB0T(null, true);
        }
    }

    private static byte[] readConfig(File cfg) {
        try {
            return Files.readAllBytes(cfg.toPath());
        } catch (IOException e) {
            LOGGER.error("Nie udało się odczytać configu.", e);
            System.exit(1);
            return null;
        }
    }
}
