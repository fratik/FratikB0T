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

class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        main(null, args);
    }

    private static void main(Class<?> klasaLoadera, String[] args) {
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
        if (klasaLoadera == null) new FratikB0T(args[0]);
        else new FratikB0T(klasaLoadera, args[0]);

    }
}
