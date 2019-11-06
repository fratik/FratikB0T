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

package net.hypixel.api.reply;

import com.google.gson.JsonObject;
import net.hypixel.api.util.Banner;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class GuildReply extends AbstractReply {
    private Guild guild;

    public Guild getGuild() {
        return guild;
    }

    @Override
    public String toString() {
        return "GuildReply{" +
                "guild=" + guild +
                "} " + super.toString();
    }

    public static class Guild {
        private String _id;

        private String name;
        private String description;
        private String tag;
        private String tagColor;
        private Boolean publiclyListed;
        private Banner banner;
        private List<Member> members;
        private int coins;
        private int coinsEver;
        private ZonedDateTime created;
        private Boolean joinable;
        private long exp;
        private int memberSizeLevel;
        private int bankSizeLevel;
        private Boolean canTag;
        private Boolean canParty;
        private Boolean canMotd;
        private JsonObject achievements;

        public String get_id() {
            return _id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTag() {
            return tag;
        }

        public String getTagColor() {
            return tagColor;
        }

        public Boolean getPubliclyListed() {
            return publiclyListed;
        }

        public Banner getBanner() {
            return banner;
        }

        public List<Member> getMembers() {
            return members;
        }

        public int getCoins() {
            return coins;
        }

        public int getCoinsEver() {
            return coinsEver;
        }

        public ZonedDateTime getCreated() {
            return created;
        }

        public Boolean getJoinable() {
            return joinable;
        }

        public long getExp() {
            return exp;
        }

        public int getMemberSizeLevel() {
            return memberSizeLevel;
        }

        public int getBankSizeLevel() {
            return bankSizeLevel;
        }

        public Boolean getCanTag() {
            return canTag;
        }

        public Boolean getCanParty() {
            return canParty;
        }

        public Boolean getCanMotd() {
            return canMotd;
        }

        @Override
        public String toString() {
            return "Guild{" +
                    "_id='" + _id + '\'' +
                    ", name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", tag='" + tag + '\'' +
                    ", tagColor='" + tagColor + '\'' +
                    ", publiclyListed=" + publiclyListed +
                    ", banner=" + banner +
                    ", members=" + members +
                    ", coins=" + coins +
                    ", coinsEver=" + coinsEver +
                    ", created=" + created +
                    ", joinable=" + joinable +
                    ", exp=" + exp +
                    ", memberSizeLevel=" + memberSizeLevel +
                    ", bankSizeLevel=" + bankSizeLevel +
                    ", canTag=" + canTag +
                    ", canParty=" + canParty +
                    ", canMotd=" + canMotd +
                    ", achievements=" + achievements +
                    '}';
        }

        public JsonObject getAchievements() {
            return achievements;
        }

        public enum GuildRank {
            GUILDMASTER, OFFICER, MEMBER
        }

        static class Member {
            private UUID uuid;
            private GuildRank rank;
            private ZonedDateTime joined;

            public UUID getUuid() {
                return uuid;
            }

            public GuildRank getRank() {
                return rank;
            }

            public ZonedDateTime getJoined() {
                return joined;
            }

            @Override
            public String toString() {
                return "Member{" +
                        "uuid=" + uuid +
                        ", rank=" + rank +
                        ", joined=" + joined +
                        '}';
            }
        }
    }
}
