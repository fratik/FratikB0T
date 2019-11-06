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

package pl.fratik.moderation.entity;

import pl.fratik.api.entity.User;

public class FakeWiadomosc implements Wiadomosc {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public User getAuthor() {
        return null;
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public long getCreatedAt() {
        return 0;
    }

    @Override
    public Long getEditedAt() {
        return null;
    }

    @Override
    public boolean isFake() {
        return true;
    }
}
