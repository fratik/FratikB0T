/*
 * Copyright (C) 2019-2022 FratikB0T Contributors
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

package pl.fratik.tags.commands;

import com.google.common.eventbus.EventBus;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.command.SubCommand;
import pl.fratik.core.command.SubCommandGroup;
import pl.fratik.core.util.ClassicEmbedPaginator;
import pl.fratik.core.util.EventWaiter;
import pl.fratik.core.util.UserUtil;
import pl.fratik.tags.TagsManager;
import pl.fratik.tags.entity.Tag;
import pl.fratik.tags.entity.Tags;
import pl.fratik.tags.entity.TagsDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static pl.fratik.tags.Module.MAX_TAG_NAME_LENGTH;

public class TagCommand extends NewCommand {
    private final TagsDao tagsDao;
    private final EventWaiter eventWaiter;
    private final EventBus eventBus;
    private final TagsManager tagsManager;

    public TagCommand(TagsDao tagsDao, EventWaiter eventWaiter, EventBus eventBus, TagsManager tagsManager) {
        this.tagsDao = tagsDao;
        this.eventWaiter = eventWaiter;
        this.eventBus = eventBus;
        this.tagsManager = tagsManager;
        name = "tag";
        permissions = DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER);
    }

    @SubCommand(name = "utworz", usage = "<nazwa:string> <tresc:string>")
    public void create(NewCommandContext context) {
        String tagName = context.getArguments().get("nazwa").getAsString().toLowerCase();
        String content = context.getArguments().get("tresc").getAsString();
        if (!checkName(context, tagName)) return;
        Tags tags = tagsDao.get(context.getGuild().getId());
        if (tags.getTagi().stream().anyMatch(t -> t.getName().equalsIgnoreCase(tagName))) {
            context.replyEphemeral(context.getTranslated("createtag.exists"));
            return;
        }
        context.defer(false);
        tags.getTagi().add(new Tag(tagName, context.getSender().getId(), content));
        tagsDao.save(tags);
        try {
            tagsManager.syncGuild(tags, context.getGuild());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("createtag.sync.fail"));
            return;
        }
        context.sendMessage(context.getTranslated("createtag.success"));
    }

    private boolean checkName(NewCommandContext context, String tagName) {
        if (tagName.length() > MAX_TAG_NAME_LENGTH) {
            context.replyEphemeral(context.getTranslated("createtag.too.long"));
            return false;
        }
        if (!Checks.ALPHANUMERIC_WITH_DASH.matcher(tagName).matches()) {
            context.replyEphemeral(context.getTranslated("createtag.invalid"));
            return false;
        }
        return true;
    }

    @SubCommand(name = "usun", usage = "<nazwa:string>")
    public void delete(NewCommandContext context) {
        String tagName = context.getArguments().get("nazwa").getAsString().toLowerCase();
        Tags tags = tagsDao.get(context.getGuild().getId());
        if (tags.getTagi().stream().noneMatch(t -> t.getName().equalsIgnoreCase(tagName))) {
            context.reply(context.getTranslated("deletetag.doesnt.exist"));
            return;
        }
        context.defer(false);
        Tag tag = tags.getTagi().stream().filter(t -> t.getName().equalsIgnoreCase(tagName)).findFirst()
                .orElseThrow(IllegalStateException::new);
        tags.getTagi().remove(tag);
        tagsDao.save(tags);
        try {
            tagsManager.syncGuild(tags, context.getGuild());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("deletetag.sync.fail"));
            return;
        }
        context.sendMessage(context.getTranslated("deletetag.success"));
    }

    @SubCommand(name = "lista")
    public void list(NewCommandContext context) {
        InteractionHook hook = context.defer(false);
        Tags tags = tagsDao.get(context.getGuild().getId());
        List<EmbedBuilder> pages = new ArrayList<>();
        List<Tag> valid = new ArrayList<>();
        List<Tag> invalid = new ArrayList<>();
        tags.getTagi().stream().sorted(Comparator.comparing(t -> t.getName().toLowerCase())).forEachOrdered(tag -> {
            if (valid.size() >= 100 || tag.getName().length() > 32 || !Checks.ALPHANUMERIC_WITH_DASH.matcher(tag.getName()).matches())
                invalid.add(tag);
            else valid.add(tag);
        });
        List<String> validStrings = new ArrayList<>();
        List<String> invalidStrings = new ArrayList<>();
        generateString(valid, validStrings);
        generateString(invalid, invalidStrings);
        for (int i = 0; i < Math.max(validStrings.size(), invalidStrings.size()); i++) {
            String validStr;
            String invalidStr;
            if (validStrings.size() > i) validStr = validStrings.get(i);
            else validStr = null;
            if (invalidStrings.size() > i) invalidStr = invalidStrings.get(i);
            else invalidStr = null;
            pages.add(renderEmbed(validStr, invalidStr, context, i == 0 && !invalidStrings.isEmpty()));
        }
        if (pages.isEmpty()) {
            context.sendMessage(context.getTranslated("listtag.no.tags"));
            return;
        }
        if (pages.size() == 1) {
            context.sendMessage(pages.get(0).build());
            return;
        }
        new ClassicEmbedPaginator(eventWaiter, pages, context.getSender(), context.getLanguage(),
                context.getTlumaczenia(), eventBus).create(hook);
    }

    private void generateString(List<Tag> tags, List<String> strings) {
        StringBuilder builder = new StringBuilder("`");
        for (Tag tag : tags) {
            if (builder.length() + tag.getName().length() + 4 > MessageEmbed.VALUE_MAX_LENGTH) {
                strings.add(builder.substring(0, builder.length() - 4) + "`");
                builder.setLength(0);
                builder.append("`");
            }
            builder.append(tag.getName().toLowerCase()).append("`, `");
        }
        if (builder.length() > 4) strings.add(builder.substring(0, builder.length() - 4) + "`");
    }

    @SubCommandGroup(name = "edytuj")
    @SubCommand(name = "nazwa", usage = "<stara_nazwa:string> <nowa_nazwa:string>")
    public void editName(NewCommandContext context) {
        String staraNazwa = context.getArguments().get("stara_nazwa").getAsString();
        String nowaNazwa = context.getArguments().get("nowa_nazwa").getAsString().toLowerCase();
        if (!checkName(context, nowaNazwa)) return;
        context.defer(false);
        Tags tags = tagsDao.get(context.getGuild().getId());
        Tag tag = tags.getTagi().stream().filter(t -> t.getName().equalsIgnoreCase(staraNazwa)).findAny().orElse(null);
        if (tag == null) {
            context.sendMessage(context.getTranslated("deletetag.doesnt.exist"));
            return;
        }
        tags.getTagi().remove(tag);
        tags.getTagi().add(new Tag(nowaNazwa, context.getSender().getId(), tag.getContent()));
        tagsDao.save(tags);
        try {
            tagsManager.syncGuild(tags, context.getGuild());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("createtag.sync.fail"));
            return;
        }
        context.sendMessage(context.getTranslated("tag.edit.name.success"));
    }

    @SubCommandGroup(name = "edytuj")
    @SubCommand(name = "tresc", usage = "<nazwa:string> <tresc:string>")
    public void editContent(NewCommandContext context) {
        String nazwa = context.getArguments().get("nazwa").getAsString().toLowerCase();
        String content = context.getArguments().get("tresc").getAsString();
        context.defer(false);
        Tags tags = tagsDao.get(context.getGuild().getId());
        Tag tag = tags.getTagi().stream().filter(t -> t.getName().equalsIgnoreCase(nazwa)).findAny().orElse(null);
        if (tag == null) {
            context.sendMessage(context.getTranslated("deletetag.doesnt.exist"));
            return;
        }
        tags.getTagi().remove(tag);
        tags.getTagi().add(new Tag(tag.getName(), context.getSender().getId(), content));
        tagsDao.save(tags);
        try {
            tagsManager.syncGuild(tags, context.getGuild());
        } catch (Exception e) {
            context.sendMessage(context.getTranslated("createtag.sync.fail"));
            return;
        }
        context.sendMessage(context.getTranslated("tag.edit.content.success"));
    }

    @NotNull
    private EmbedBuilder renderEmbed(String valid, String invalid, NewCommandContext ctx, boolean hasInvalid) {
        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(ctx.getTranslated("listtag.embed.header"))
                .setColor(ctx.getMember() == null || ctx.getMember().getColor() == null ?
                        UserUtil.getPrimColor(ctx.getSender()) :
                        ctx.getMember().getColor());
        if (valid != null) builder.addField(ctx.getTranslated("listtag.embed.valid"), valid, false);
        if (invalid != null) builder.addField(ctx.getTranslated("listtag.embed.invalid"), invalid, false);
        if (hasInvalid) builder.addField(ctx.getTranslated("listtag.embed.invalid.notice.title"),
                ctx.getTranslated("listtag.embed.invalid.notice.description"), false);
        return builder;
    }
}
