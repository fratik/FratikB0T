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

package pl.fratik.fratikcoiny.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.MemberDao;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.EventWaiter;

import java.awt.*;
import java.util.Objects;

@SuppressWarnings("squid:S1192")
public abstract class SklepSharedCommand extends NewCommand {

    protected final GuildDao guildDao;
    protected final MemberDao memberDao;
    protected final EventWaiter eventWaiter;
    protected final ShardManager shardManager;
    protected final EventBus eventBus;

    public SklepSharedCommand(GuildDao guildDao, MemberDao memberDao, EventWaiter eventWaiter, ShardManager shardManager, EventBus eventBus) {
        this.guildDao = guildDao;
        this.memberDao = memberDao;
        this.eventWaiter = eventWaiter;
        this.shardManager = shardManager;
        this.eventBus = eventBus;
    }

    @SuppressWarnings("squid:S00107")
    protected EmbedBuilder generateEmbed(Typ typ, Role rola, String opis, long cena, long hajs, boolean hasRole, Tlumaczenia tlumaczenia, Language l) {
        EmbedBuilder eb = new EmbedBuilder();
        switch (typ) {
            case KUPOWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.kup"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                if (hajs < cena) {
                    eb.setDescription(tlumaczenia.get(l, "sklep.embed.zamalokasy"));
                    eb.setColor(Color.RED);
                    return eb;
                }
                if (hasRole) {
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.hasrole"), null);
                    eb.setColor(Color.RED);
                    return eb;
                }
                eb.setColor(Color.GREEN);
                return eb;
            case SPRZEDAZ:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.sprzedaj"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.sprzedaj.cena"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                if (!hasRole) {
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.hasrole.nie"), null);
                    eb.setColor(Color.RED);
                    return eb;
                }
                eb.setColor(Color.GREEN);
                return eb;
            case INFO:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.info", rola.getName()));
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.info.posiadanie"), hasRole ?
                        Objects.requireNonNull(shardManager.getEmojiById(Ustawienia.instance.emotki.greenTick)).getAsMention() :
                        Objects.requireNonNull(shardManager.getEmojiById(Ustawienia.instance.emotki.redTick)).getAsMention(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.hajs"), String.valueOf(hajs), true);
                eb.setColor(rola.getColor());
                return eb;
            case DODAWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.dodaj"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                if (hasRole) { //hasRole == nie ma permow w tym przypadku
                    eb.setColor(Color.RED);
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.dodaj.niemozna"), null);
                }
                eb.setColor(Color.GREEN);
                return eb;
            case USUWANIE:
                eb.setTitle(tlumaczenia.get(l, "sklep.embed.usun"));
                eb.addField(tlumaczenia.get(l, "sklep.embed.nazwa"), rola.getName(), false);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.kupna"), String.valueOf(cena), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.cena.sprzedazy"), String.valueOf((int) Math.floor((double) cena / 2)), true);
                eb.addField(tlumaczenia.get(l, "sklep.embed.opis"),
                        opis == null || opis.isEmpty() ? tlumaczenia.get(l, "sklep.embed.opis.pusty") : opis,
                        false);
                if (hasRole) { //hasRole == nie ma permow w tym przypadku
                    eb.setColor(Color.RED);
                    eb.setFooter(tlumaczenia.get(l, "sklep.embed.usun.niemozna"), null);
                }
                eb.setColor(Color.GREEN);
                return eb;
        }
        throw new IllegalStateException("typ nieprawidłowy");
    }

    protected enum Typ {
        KUPOWANIE, SPRZEDAZ, INFO, DODAWANIE, USUWANIE
    }

}
