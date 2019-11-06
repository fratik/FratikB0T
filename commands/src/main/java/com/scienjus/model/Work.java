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

import java.util.List;

/**
 * @author Scienjus
 * @date 2015/12/15.
 */
public class Work {
    private int id;

    private String title;

    private String caption;

    private List<String> tags;

    private List<String> tools;

    @SerializedName("image_urls")
    private ImageUrls imageUrls;

    private int width;

    private int height;

    private Stats stats;

    private int publicity;

    @SerializedName("age_limit")
    private String ageLimit;

    @SerializedName("created_time")
    private String createdTime;

    @SerializedName("reuploaded_time")
    private String reuploadedTime;

    private User user;

    @SerializedName("is_manga")
    private boolean manga;

    @SerializedName("is_liked")
    private boolean liked;

    @SerializedName("favorite_id")
    private String favoriteId;

    @SerializedName("page_count")
    private int pageCount;

    @SerializedName("book_style")
    private String bookStyle;

    private String type;

    private Metadata metadata;

    @SerializedName("content_type")
    private String contentType;

    @SerializedName("sanity_level")
    private String sanityLevel;

    //detail



    //    get and set

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    public ImageUrls getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(ImageUrls imageUrls) {
        this.imageUrls = imageUrls;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public int getPublicity() {
        return publicity;
    }

    public void setPublicity(int publicity) {
        this.publicity = publicity;
    }

    public String getAgeLimit() {
        return ageLimit;
    }

    public void setAgeLimit(String ageLimit) {
        this.ageLimit = ageLimit;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getReuploadedTime() {
        return reuploadedTime;
    }

    public void setReuploadedTime(String reuploadedTime) {
        this.reuploadedTime = reuploadedTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isManga() {
        return manga;
    }

    public void setManga(boolean manga) {
        this.manga = manga;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public String getFavoriteId() {
        return favoriteId;
    }

    public void setFavoriteId(String favoriteId) {
        this.favoriteId = favoriteId;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getBookStyle() {
        return bookStyle;
    }

    public void setBookStyle(String bookStyle) {
        this.bookStyle = bookStyle;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSanityLevel() {
        return sanityLevel;
    }

    public void setSanityLevel(String sanityLevel) {
        this.sanityLevel = sanityLevel;
    }

//    to string

    @Override
    public String toString() {
        return "Work{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", caption='" + caption + '\'' +
                ", tags=" + tags +
                ", tools='" + tools + '\'' +
                ", imageUrls=" + imageUrls +
                ", width=" + width +
                ", height=" + height +
                ", stats=" + stats +
                ", publicity=" + publicity +
                ", ageLimit='" + ageLimit + '\'' +
                ", createdTime='" + createdTime + '\'' +
                ", reuploadedTime='" + reuploadedTime + '\'' +
                ", user=" + user +
                ", manga=" + manga +
                ", liked=" + liked +
                ", favoriteId='" + favoriteId + '\'' +
                ", pageCount=" + pageCount +
                ", bookStyle='" + bookStyle + '\'' +
                ", type='" + type + '\'' +
                ", metadata='" + metadata + '\'' +
                ", contentType='" + contentType + '\'' +
                ", sanityLevel='" + sanityLevel + '\'' +
                '}';
    }
}
