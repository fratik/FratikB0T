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

package pl.fratik.starboard.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class StarData {
    private List<String> starredBy = new ArrayList<>();
    private Date starredOn;
    private String author;
    private String guild;
    private String channel;
    private String starboardMessageId;

    protected StarData() {
        LoggerFactory.getLogger(StarData.class).warn("Constructor StarData() jest przestarzały!", new Exception());
        starredOn = new Date();
    }

    public StarData(String author) {
        this.author = author;
        starredOn = new Date();
    }

}
