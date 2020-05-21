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

package pl.fratik.moderation.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.CommandCategory;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.entity.Uzycie;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseRow;
import pl.fratik.moderation.entity.CasesDao;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AkcjeCommand extends ModerationCommand {

    private final CasesDao casesDao;
    private final ShardManager shardManager;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final ManagerKomend managerKomend;

    public AkcjeCommand(CasesDao casesDao, ShardManager shardManager, EventWaiter eventWaiter, EventBus eventBus, ManagerKomend managerKomend) {
        this.casesDao = casesDao;
        this.shardManager = shardManager;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.managerKomend = managerKomend;
        name = "akcje";
        aliases = new String[] {"administracyjne", "adm", "listawarnow", "listakickow", "listabanow", "ostrzezenia", "kicki", "bany", "ilewarnów"};
        permissions.add(Permission.MESSAGE_MANAGE);
        permissions.add(Permission.MESSAGE_EMBED_LINKS);
        permissions.add(Permission.MESSAGE_HISTORY);
        permissions.add(Permission.MESSAGE_ADD_REACTION);
        category = CommandCategory.MODERATION;
        uzycieDelim = " ";
        uzycie = new Uzycie("czlonek", "user");
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        Message m = context.send(context.getTranslated("generic.loading"));
        User tmpUser = null;
        Object[] args = context.getArgs();
        if (args.length > 0 && args[0] != null) tmpUser = (User) args[0];
        if (tmpUser == null) tmpUser = context.getSender();
        CaseRow caseRow = casesDao.get(context.getGuild());
        User user = tmpUser;
        List<Case> mcases = caseRow.getCases().stream()
                .filter(c -> c.getUserId().equals(user.getId())).collect(Collectors.toList());
        List<Case> warnCases = mcases.stream().filter(c -> c.getType() == Kara.WARN).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType() == Kara.UNWARN).collect(Collectors.toList());
        long warnow = 0;
        long unwarnow = 0;
        for (Case c : warnCases) {
            if (c.getIleRazy() == null) warnow++;
            else warnow += c.getIleRazy();
        }
        for (Case c : unwarnCases) {
            if (c.getIleRazy() == null) unwarnow++;
            else unwarnow += c.getIleRazy();
        }
        long kickow = mcases.stream().filter(c -> c.getType() == Kara.KICK).count();
        long banow = mcases.stream().filter(c -> c.getType() == Kara.BAN).count();
        long mutow = mcases.stream().filter(c -> c.getType() == Kara.MUTE).count();
        long unmutow = mcases.stream().filter(c -> c.getType() == Kara.UNMUTE).count();
        ArrayList<EmbedBuilder> strony = new ArrayList<>();
        strony.add(context.getBaseEmbed(UserUtil.formatDiscrim(user), user.getEffectiveAvatarUrl()
                .replace(".webp", ".png"))
                .addField(context.getTranslated("akcje.embed.warns"),
                        String.format(context.getTranslated("akcje.embed.warns.content"),
                                (warnow - unwarnow), warnow, unwarnow),
                        true)
                .addField(context.getTranslated("akcje.embed.kicks"), String.valueOf(kickow), true)
                .addField(context.getTranslated("akcje.embed.bans"), String.valueOf(banow), true)
                .addField(context.getTranslated("akcje.embed.mutes"),
                        String.format(context.getTranslated("akcje.embed.mutes.content"),
                                (mutow - unmutow), mutow, unmutow),
                        true)
                .setDescription(context.getTranslated("akcje.embed.description")).setFooter("%s/%s", null));
        for (Case aCase : caseRow.getCases().stream().filter(c -> c.getUserId().equals(user.getId())).collect(Collectors.toList())) {
            EmbedBuilder eb = new EmbedBuilder(ModLogBuilder.generate(aCase, context.getGuild(), shardManager, context.getLanguage(), managerKomend, false));
            eb.setFooter(Objects.requireNonNull(eb.build().getFooter()).getText() + " (%s/%s)", null);
            strony.add(eb);
        }
        new ClassicEmbedPaginator(eventWaiter, strony, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(m);
        return true;
    }

    @SubCommand(name = "admin", aliases = {"adm"})
    public boolean adminMode(@NotNull CommandContext context) {
        Message m = context.send(context.getTranslated("generic.loading"));
        User tmpUser = null;
        Object[] args = context.getArgs();
        if (args.length > 0 && args[0] != null) tmpUser = (User) args[0];
        if (tmpUser == null) tmpUser = context.getSender();
        CaseRow caseRow = casesDao.get(context.getGuild());
        User user = tmpUser;
        List<Case> mcases = caseRow.getCases().stream()
                .filter(c -> c.getUserId().equals(user.getId())).collect(Collectors.toList());
        List<Case> warnCases = mcases.stream().filter(c -> c.getType() == Kara.WARN).collect(Collectors.toList());
        List<Case> unwarnCases = mcases.stream().filter(c -> c.getType() == Kara.UNWARN).collect(Collectors.toList());
        long warnow = 0;
        long unwarnow = 0;
        for (Case c : warnCases) {
            if (c.getIleRazy() == null) warnow++;
            else warnow += c.getIleRazy();
        }
        for (Case c : unwarnCases) {
            if (c.getIleRazy() == null) unwarnow++;
            else unwarnow += c.getIleRazy();
        }
        long kickow = mcases.stream().filter(c -> c.getType() == Kara.KICK).count();
        long banow = mcases.stream().filter(c -> c.getType() == Kara.BAN).count();
        long mutow = mcases.stream().filter(c -> c.getType() == Kara.MUTE).count();
        long unmutow = mcases.stream().filter(c -> c.getType() == Kara.UNMUTE).count();
        ArrayList<EmbedBuilder> strony = new ArrayList<>();
        strony.add(context.getBaseEmbed(UserUtil.formatDiscrim(user), user.getEffectiveAvatarUrl()
                .replace(".webp", ".png"))
                .addField(context.getTranslated("akcje.admin.embed.warns"),
                        String.format(context.getTranslated("akcje.admin.embed.warns.content"),
                                (warnow - unwarnow), warnow, unwarnow),
                        true)
                .addField(context.getTranslated("akcje.admin.embed.kicks"), String.valueOf(kickow), true)
                .addField(context.getTranslated("akcje.admin.embed.bans"), String.valueOf(banow), true)
                .addField(context.getTranslated("akcje.admin.embed.mutes"), String.valueOf(mutow), true)
                .addField(context.getTranslated("akcje.admin.embed.unmutes"), String.valueOf(unmutow), true)
                .setDescription(context.getTranslated("akcje.admin.embed.description")).setFooter("%s/%s", null));
        for (Case aCase : caseRow.getCases().stream().filter(c -> Objects.equals(c.getIssuerId(), user.getId())).collect(Collectors.toList())) {
            EmbedBuilder eb = new EmbedBuilder(ModLogBuilder.generate(aCase, context.getGuild(), shardManager, context.getLanguage(), managerKomend, false));
            eb.setFooter(Objects.requireNonNull(eb.build().getFooter()).getText() + " (%s/%s)", null);
            strony.add(eb);
        }
        new ClassicEmbedPaginator(eventWaiter, strony, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).setCustomFooter(true).create(m);
        return true;
    }
}
