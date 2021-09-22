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
import pl.fratik.core.command.PermLevel;
import pl.fratik.core.tlumaczenia.Language;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

    @ConfigField(holdsEntity = ConfigField.Entities.STRING)
    private List<String> prefixes = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private String adminRole = "";
    @ConfigField(required = true)
    private Boolean antiswear = false;
    @ConfigField(required = true)
    private Boolean antiLink = false;
    @ConfigField(required = true)
    private Boolean autoban = false;
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private List<String> autorole = new ArrayList<>();
    @ConfigField(required = true)
    private Boolean autoroleZa1szaWiadomosc = false;
    @ConfigField(holdsEntity = ConfigField.Entities.STRING)
    private List<String> customAntiSwearWords = new ArrayList<>();
    @ConfigField(required = true, max = 730)
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
    private List<String> linkchannels = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> nolvlchannelchange = new ArrayList<>();
    @ConfigField(required = true)
    private Boolean warnAdminLubModTraciRange = true;
    @ConfigField(required = true, max = 100)
    private Integer warnyNaBan = 15;
    @ConfigField(required = true, max = 100)
    private Integer warnyNaKick = 5;
    @ConfigField(required = true, max = 100)
    private Integer warnyNaTymczasowegoBana = 10;
    @ConfigField(required = true, max = 1000)
    private Integer maxRoliDoSamododania = 0;
    @Deprecated
    @ConfigField(dontDisplayInSettings = true)
    private Boolean wymagajWeryfikacjiDwuetapowej = false;
    @ConfigField(required = true)
    private Boolean wysylajDmOKickachLubBanach = true;
    @ConfigField(dontDisplayInSettings = true)
    private Boolean zapamietajRole = false;
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private String djRole = "";
    @ConfigField(required = true)
    private Boolean tylkoDjWGoreMozeDodawacPiosenki = false;
    @ConfigField(holdsEntity = ConfigField.Entities.COMMAND)
    private List<String> disabledCommands = new ArrayList<>();
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private String kanalMuzyczny = "";
    @ConfigField(required = true)
    private Boolean punktyWlaczone = true;
    private String lvlupMessagesCustomChannel = null;
    @ConfigField(required = true)
    private Boolean ukryjZgsr = false;
    @ConfigField(required = true)
    private Boolean antiInvite = true;
    @ConfigField(required = true)
    private Boolean antiRaid = false;
    @ConfigField(required = true)
    private Boolean antiRaidExtreme = false;
    @ConfigField(required = true, max = 100)
    private Integer antiRaidCzulosc = 50;
    @ConfigField(holdsEntity = ConfigField.Entities.CHANNEL)
    private List<String> kanalyGdzieAntiRaidNieDziala = new ArrayList<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<Integer, String> roleZaPoziomy = new HashMap<>();
    @ConfigField(dontDisplayInSettings = true)
    private String wyciszony;
    @ConfigField(dontDisplayInSettings = true)
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
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, Webhook> webhooki = new HashMap<>();
    @ConfigField(max = 1500)
    private String lvlUpMessage;
    @ConfigField(required = true)
    private Boolean resetujOstrzezeniaPrzyBanie = true;
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, PermLevel> cmdPermLevelOverrides = new HashMap<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<Integer, String> roleZaZaproszenia = new HashMap<>();
    @ConfigField(required = true)
    private Boolean lvlUpNotify = true;
    @ConfigField(required = true)
    private Boolean trackInvites = false;
    @ConfigField(required = true)
    private Boolean deleteSwearMessage = false;
    @ConfigField(required = true)
    private Boolean deleteLinkMessage = false;
    @ConfigField(required = true)
    private Boolean cytujFbot = false;
    @ConfigField(dontDisplayInSettings = true)
    private Boolean publikujReakcja = false;
    @ConfigField(required = true)
    private Boolean antiLinkMediaAllowed = true;
    @ConfigField(required = true)
    private Boolean antiLinkIgnoreAdmins = false;
    @ConfigField(holdsEntity = ConfigField.Entities.ROLE)
    private List<String> antiLinkIgnoreRoles = new ArrayList<>();
    @ConfigField(dontDisplayInSettings = true)
    private Map<String, Wyplata> wyplaty = new HashMap<>();

    // TODO: 09/04/2020 można to zrobić dla każdego Boolean'a, ale to już kwestia kosmetyki kodu chyba
    public boolean isResetujOstrzezeniaPrzyBanie() {
        return resetujOstrzezeniaPrzyBanie != null && resetujOstrzezeniaPrzyBanie;
    }

    public boolean isLvlUpNotify() {
        return lvlUpNotify == null || lvlUpNotify;
    }

    public boolean isTrackInvites() {
        return trackInvites != null && trackInvites;
    }

    public boolean isDeleteSwearMessage() {
        return deleteSwearMessage != null && deleteSwearMessage;
    }

    public boolean isCytujFbot() {
        return cytujFbot != null && cytujFbot;
    }

    public boolean isPublikujReakcja() {
        return publikujReakcja != null && publikujReakcja;
    }

    public Boolean isAntiLink() {
        return antiLink != null && antiLink;
    }

    public Boolean isAntiLinkMediaAllowed() {
        return antiLinkMediaAllowed == null || antiLinkMediaAllowed;
    }

    public Boolean isAntiLinkIgnoreAdmins() {
        return antiLinkIgnoreAdmins != null && antiLinkIgnoreAdmins;
    }

    public List<String> getLinkchannels() {
        if (linkchannels == null) linkchannels = new ArrayList<>();
        return linkchannels;
    }

    public boolean isDeleteLinkMessage() {
        return deleteLinkMessage != null && deleteLinkMessage;
    }

    public Map<String, Wyplata> getWyplaty() {
        if (wyplaty == null) wyplaty = new HashMap<>();
        return wyplaty;
    }

    public boolean isWysylajDmOKickachLubBanach() {
        return wysylajDmOKickachLubBanach == null || wysylajDmOKickachLubBanach;
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

    @Data
    @AllArgsConstructor
    public static class Wyplata {
        private final long kwota;
        private final int cooldown; // w minutach
    }

    @JsonIgnore
    public static Object getValue(Field f, GuildConfig guildConfig) {
        Object value;
        try {
            StringBuilder methodName = new StringBuilder("get");
            boolean first = true;
            for (char ch : f.getName().toCharArray()) {
                if (first) {
                    methodName.append(Character.toUpperCase(ch));
                    first = false;
                }
                else methodName.append(ch);
            }
            try {
                value = GuildConfig.class.getDeclaredMethod(methodName.toString()).invoke(guildConfig);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        } catch (NoSuchMethodException e) {
            StringBuilder methodName = new StringBuilder("is");
            boolean first = true;
            for (char ch : f.getName().toCharArray()) {
                if (first) {
                    methodName.append(Character.toUpperCase(ch));
                    first = false;
                }
                else methodName.append(ch);
            }
            try {
                value = GuildConfig.class.getDeclaredMethod(methodName.toString()).invoke(guildConfig);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
                throw new IllegalStateException(e1);
            }
        }
        return value;
    }

}
