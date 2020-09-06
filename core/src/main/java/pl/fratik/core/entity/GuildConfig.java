/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.tlumaczenia.Language;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Table("guilds")
@GIndex({"id"})
@Data
@AllArgsConstructor
public class GuildConfig implements DatabaseEntity {

    public GuildConfig(String guildId) {
        this.guildId = guildId;
    }

    public GuildConfig(String guildId, Language language, List<String> prefixes) {
        this.guildId = guildId;
        this.language = language;
        this.prefixes = prefixes;
    }

    @PrimaryKey
    @ConfigField(dontDisplayInSettings = true)
    private final String guildId;
    @ConfigField(dontDisplayInSettings = true)
    private Language language = Language.POLISH;
    private List<String> prefixes = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private String adminRole = "";
    private Boolean antiswear = false;
    private Boolean autoban = false;
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private List<String> autorole = new ArrayList<>();
    private Boolean autoroleZa1szaWiadomosc = false;
    private List<String> customAntiSwearWords = new ArrayList<>();
    private Integer dlugoscTymczasowegoBanaZaWarny = 3;
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private String fullLogs = "";
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private String kanalAdministracji = "";
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> kanalyGdzieAntiInviteNieDziala = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private String modLog = "";
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private String modRole = "";
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> swearchannels = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> nolvlchannelchange = new ArrayList<>();
    private Boolean warnAdminLubModTraciRange = true;
    private Integer warnyNaBan = 15;
    private Integer warnyNaKick = 5;
    private Integer warnyNaTymczasowegoBana = 10;
    private Integer maxRoliDoSamododania = 0;
    @Deprecated
    private Boolean wymagajWeryfikacjiDwuetapowej = false;
    private Boolean wysylajDmOKickachLubBanach = true;
    @ConfigField(dontDisplayInSettings = true)
    private Boolean zapamietajRole = false;
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private String djRole = "";
    private Boolean tylkoDjWGoreMozeDodawacPiosenki = false;
    @ConfigField(holdsEntity = ConfigField.Entities.COMMAND)
    private List<String> disabledCommands = new ArrayList<>();
    private String kanalMuzyczny = "";
    private Boolean punktyWlaczone = true;
    private String lvlupMessagesCustomChannel = null;
    private Boolean ukryjZgsr = false;
    private Boolean antiInvite = true;
    private Boolean antiRaid = false;
    private Boolean antiRaidExtreme = false;
    private Integer antiRaidCzulosc = 50;
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> kanalyGdzieAntiRaidNieDziala = new ArrayList<>();
    private Map<Integer, String> roleZaPoziomy = new HashMap<>();
    private String wyciszony;
    private String timezone = "default";
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private List<String> uzytkownicyMogaNadacSobieTeRange = new ArrayList<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, Long> roleDoKupienia = new HashMap<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, String> roleDoKupieniaOpisy = new HashMap<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, String> powitania = new HashMap<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, String> pozegnania = new HashMap<>();
    private Boolean wysylajOgloszenia = false;
    @ConfigField(dontDisplayInSettings = true)
    private String liczekKanal = "";
    private Map<String, Webhook> webhooki = new HashMap<>();
    private String lvlUpMessage;
    private Boolean resetujOstrzezeniaPrzyBanie = true;
    private Map<String, PermLevel> cmdPermLevelOverrides = new HashMap<>();
    private Boolean lvlUpNotify = true;
    private Boolean deleteSwearMessage = false;

    // TODO: 09/04/2020 można to zrobić dla każdego Boolean'a, ale to już kwestia kosmetyki kodu chyba
    public boolean isResetujOstrzezeniaPrzyBanie() {
        return resetujOstrzezeniaPrzyBanie != null && resetujOstrzezeniaPrzyBanie;
    }

    public boolean isLvlUpNotify() {
        return lvlUpNotify == null || lvlUpNotify;
    }

    public boolean isDeleteSwearMessage() {
        return deleteSwearMessage != null && deleteSwearMessage;
    }

    @Transient
    @JsonIgnore
    @Override
    public String getTableName() {
        return "guilds";
    }

    @Data
    @AllArgsConstructor
    public static class Webhook {
        private final String id;
        private final String token;
    }
}
