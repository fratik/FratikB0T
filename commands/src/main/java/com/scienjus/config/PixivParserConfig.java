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

package com.scienjus.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author Scienjus
 * @date 2015/8/11.
 */
public class PixivParserConfig {

    /**
     * 不指定数量
     * the number means doesn't use limit
     */
    public static final int NO_LIMIT = -1;

    /**
     * 字符编码
     * charset
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    /**
     * 登陆地址
     * login url
     */
    public static final String LOGIN_URL = "https://oauth.secure.pixiv.net/auth/token";

    /**
     * 作品详情地址
     * illust detail url
     */
    public static final String ILLUST_DETAIL_URL = "https://public-api.secure.pixiv.net/v1/works/{illustId}.json";

    /**
     * 作者详情地址
     * author detail url
     */
    public static final String AUTHOR_DETAIL_URL = "https://public-api.secure.pixiv.net/v1/users/{authorId}/works.json";

    /**
     * 排行榜地址
     * rank url
     */
    public static final String RANK_URL = "https://public-api.secure.pixiv.net/v1/ranking/all";

    /**
     * 搜索地址
     * search url
     */
    public static final String SEARCH_URL = "https://public-api.secure.pixiv.net/v1/search/works.json";

    /**
     * 起始页的页码
     * start page number
     */
    public static final int START_PAGE = 1;

    /**
     * 无下一页的页码
     * the page number when do not have next page
     */
    public static final int NO_NEXT_PAGE = -1;

}
