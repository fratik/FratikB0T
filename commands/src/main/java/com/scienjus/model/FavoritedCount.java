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

import com.google.gson.annotations.SerializedName;

/**
 * @author Scienjus
 * @date 2015/12/15.
 */
public class FavoritedCount {

    @SerializedName("public")
    private int publicCount;

    @SerializedName("private")
    private int privateCount;

//    get and set

    public int getPublicCount() {
        return publicCount;
    }

    public void setPublicCount(int publicCount) {
        this.publicCount = publicCount;
    }

    public int getPrivateCount() {
        return privateCount;
    }

    public void setPrivateCount(int privateCount) {
        this.privateCount = privateCount;
    }

//    to string

    @Override
    public String toString() {
        return "FavoritedCount{" +
                "publicCount=" + publicCount +
                ", privateCount=" + privateCount +
                '}';
    }
}


