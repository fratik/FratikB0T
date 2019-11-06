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

package pl.fratik.core.manager.implementation;

import gg.amy.pgorm.PgStore;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.manager.ManagerBazyDanych;

public class ManagerBazyDanychImpl implements ManagerBazyDanych {
    private PgStore pgStore;

    @Override
    public void shutdown() {
        if (pgStore != null) pgStore.disconnect();
    }

    @Override
    public PgStore getPgStore() {
        if (pgStore == null) throw new IllegalStateException("pgStore == null");
        return pgStore;
    }

    @Override
    public void load() {
        Ustawienia ustawienia = Ustawienia.instance;
        try { //ładujemy klasę, problem z tym przy loaderze
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            /* fuck */
        }
        pgStore = new PgStore(ustawienia.postgres.jdbcUrl, ustawienia.postgres.user, ustawienia.postgres.password);
        pgStore.connect();
    }

}
