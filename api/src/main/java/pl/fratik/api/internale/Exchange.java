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

package pl.fratik.api.internale;

@SuppressWarnings("all")
public class Exchange {
    private Exchange() {}

    public static interface BodyImpl extends
            ContentTypeSenders
            , JsonSender
            , JsonParser {};
    private static final BodyImpl BODY = new BodyImpl(){};
    public static BodyImpl body() {
        return BODY;
    }

    public static interface RedirectImpl extends RedirectSenders {};
    private static final RedirectImpl REDIRECT = new RedirectImpl(){};
    public static RedirectImpl redirect() {
        return REDIRECT;
    }

    public static interface QueryParamImpl extends QueryParams, WebsocketQueryParams {};
    private static final QueryParamImpl QUERYPARAMS = new QueryParamImpl(){};
    public static QueryParamImpl queryParams() {
        return QUERYPARAMS;
    }

    public static interface PathParamImpl extends PathParams {};
    private static final PathParamImpl PATHPARAMS = new PathParamImpl(){};
    public static PathParamImpl pathParams() {
        return PATHPARAMS;
    }

    public static interface UrlImpl extends Urls {};
    private static final UrlImpl URLS = new UrlImpl(){};
    public static UrlImpl urls() {
        return URLS;
    }

    public static interface HeaderImpl extends Headers {};
    private static final HeaderImpl HEADERS = new HeaderImpl(){};
    public static HeaderImpl headers() {
        return HEADERS;
    }

}
