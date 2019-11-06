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

package com.scienjus.model;

import java.util.Date;
import java.util.List;

/**
 * @author Scienjus
 * @date 2015/12/15.
 */
public class Rank {

    private String content;

    private String mode;

    private Date date;

    private List<RankWork> works;

//    get and set


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<RankWork> getWorks() {
        return works;
    }

    public void setWorks(List<RankWork> works) {
        this.works = works;
    }

    //    to string

    @Override
    public String toString() {
        return "Rank{" +
                "content='" + content + '\'' +
                ", mode='" + mode + '\'' +
                ", date=" + date +
                ", works=" + works +
                '}';
    }
}
