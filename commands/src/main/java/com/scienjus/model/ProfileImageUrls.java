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
public class ProfileImageUrls {

    @SerializedName("px_170x170")
    private String px170x170;

    @SerializedName("px_50x50")
    private String px50x50;

//    get and set

    public String getPx170x170() {
        return px170x170;
    }

    public void setPx170x170(String px170x170) {
        this.px170x170 = px170x170;
    }

    public String getPx50x50() {
        return px50x50;
    }

    public void setPx50x50(String px50x50) {
        this.px50x50 = px50x50;
    }

//    to string


    @Override
    public String toString() {
        return "ProfileImageUrls{" +
                "px170x170='" + px170x170 + '\'' +
                ", px50x50='" + px50x50 + '\'' +
                '}';
    }
}
