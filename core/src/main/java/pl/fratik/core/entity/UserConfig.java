/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
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

package pl.fratik.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.fratik.core.tlumaczenia.Language;

import java.util.ArrayList;
import java.util.List;

@Table("users")
@GIndex({"id", "punkty"})
@Data
@AllArgsConstructor
public class UserConfig implements DatabaseEntity {
    public UserConfig() {}

    public UserConfig(String id) {
        this.id = id;
    }

    @PrimaryKey
    @ConfigField(dontDisplayInSettings = true)
    private String id = "";
    @ConfigField(dontDisplayInSettings = true)
    private Language language = Language.DEFAULT;
    @ConfigField(dontDisplayInSettings = true)
    private long reputation = 0L;
    @ConfigField(dontDisplayInSettings = true)
    private long voted = 0L;
    private boolean privWlaczone = true;
    private boolean privBlacklist = false;
    @ConfigField(holdsEntity = ConfigField.Entities.USER)
    private List<String> privIgnored = new ArrayList<>();
    private boolean boomWlaczone = true;
    private boolean lvlupMessages = true;
    private boolean lvlUpOnDM = false;
    private String location = null;
    @ConfigField(holdsEntity = ConfigField.Entities.EMOJI)
    private String reakcja = "436919889207361536";
    @ConfigField(holdsEntity = ConfigField.Entities.EMOJI)
    private String reakcjaBlad = "436919889232658442";
    @ConfigField(dontDisplayInSettings = true)
    @Deprecated
    private String twoFactorKey = "";
    @ConfigField(dontDisplayInSettings = true)
    @Deprecated
    private List<String> twoFactorBackupCodes = new ArrayList<>();
    @ConfigField(dontDisplayInSettings = true)
    @Deprecated
    private Long twoFactorTimeLocked = null;
    @ConfigField(dontDisplayInSettings = true)
    private String timezone = "default";
    private Boolean cytujFbot = false;
    private Boolean ytWarningSeen = false;

    public boolean isCytujFbot() {
        return cytujFbot != null && cytujFbot;
    }

    public boolean isYtWarningSeen() {
        return ytWarningSeen != null && ytWarningSeen;
    }

    @Override
    @JsonIgnore
    public String getTableName() {
        return "users";
    }

}