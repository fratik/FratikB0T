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

package com.scienjus.param;

import com.scienjus.callback.WorkCallback;
import com.scienjus.config.PixivParserConfig;
import com.scienjus.filter.WorkFilter;

/**
 * @author Scienjus
 * @date 2015/12/15.
 */
public class ParserParam {

    private int limit = PixivParserConfig.NO_LIMIT;

    private WorkFilter filter;

    private WorkCallback callback;

    public ParserParam withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public ParserParam withFilter(WorkFilter filter) {
        this.filter = filter;
        return this;
    }

    public ParserParam withCallback(WorkCallback callback) {
        this.callback = callback;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public WorkFilter getFilter() {
        return filter;
    }

    public WorkCallback getCallback() {
        return callback;
    }
}
