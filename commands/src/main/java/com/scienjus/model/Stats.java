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
public class Stats {

    @SerializedName("scored_count")
    private int scoredCount;

    private int score;

    @SerializedName("views_count")
    private int viewsCount;

    @SerializedName("favorited_count")
    private FavoritedCount favoritedCount;

    @SerializedName("commented_count")
    private String commentedCount;

//    get and set

    public int getScoredCount() {
        return scoredCount;
    }

    public void setScoredCount(int scoredCount) {
        this.scoredCount = scoredCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(int viewsCount) {
        this.viewsCount = viewsCount;
    }

    public FavoritedCount getFavoritedCount() {
        return favoritedCount;
    }

    public void setFavoritedCount(FavoritedCount favoritedCount) {
        this.favoritedCount = favoritedCount;
    }

    public String getCommentedCount() {
        return commentedCount;
    }

    public void setCommentedCount(String commentedCount) {
        this.commentedCount = commentedCount;
    }

//    to string

    @Override
    public String toString() {
        return "Stats{" +
                "scoredCount=" + scoredCount +
                ", score=" + score +
                ", viewsCount=" + viewsCount +
                ", favoritedCount=" + favoritedCount +
                ", commentedCount='" + commentedCount + '\'' +
                '}';
    }
}
