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

package pl.fratik.commands.narzedzia;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.fratik.core.Globals;
import pl.fratik.core.Ustawienia;
import pl.fratik.core.command.CommandType;
import pl.fratik.core.command.NewCommand;
import pl.fratik.core.command.NewCommandContext;
import pl.fratik.core.util.UserUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DegradCommand extends NewCommand {

    private final ShardManager shardManager;
    private final Logger logger;

    public DegradCommand(ShardManager shardManager) {
        this.shardManager = shardManager;
        logger = LoggerFactory.getLogger(getClass());
        name = "degrad";
        type = CommandType.SUPPORT_SERVER;
        permissions = DefaultMemberPermissions.DISABLED;
        usage = "<gadmin:user>";
    }

    @Override
    public void execute(@NotNull NewCommandContext context) {
        if (!UserUtil.isZga(context.getSender(), shardManager)) {
            context.replyEphemeral(context.getTranslated("generic.no.permissions"));
            return;
        }
        User gadmin = context.getArguments().get("gadmin").getAsUser();
        if (!Globals.inFratikDev) throw new IllegalStateException("bot nie na FDev");
        @SuppressWarnings("ConstantConditions") // sprawdzamy to wyżej
        Member czlonek = shardManager.getGuildById(Ustawienia.instance.botGuild).getMember(gadmin);
        if (czlonek == null) {
            context.replyEphemeral(context.getTranslated("degrad.no.member"));
            return;
        }
        if (!UserUtil.isGadm(czlonek, shardManager)) {
            context.replyEphemeral(context.getTranslated("degrad.no.role"));
            return;
        }
        if (context.getSender().getId().equals(czlonek.getUser().getId())) {
            context.replyEphemeral(context.getTranslated("degrad.selfdegrad"));
            return;
        }
        InteractionHook hook = context.defer(false);
        byte[] zdjecie;
        try {
            zdjecie = IOUtils.toByteArray(getClass().getResourceAsStream("/image_degrad.jpg"));
        } catch (NullPointerException | IOException e) {
            logger.warn("Zdjęcie z degradem nie znalezione!");
            zdjecie = null;
        }
        WebhookMessageCreateAction<Message> maction = hook
                .sendMessage(context.getTranslated("degrad.inprogress", UserUtil.formatDiscrim(czlonek)));
        if (zdjecie != null) maction = maction.addFiles(FileUpload.fromData(zdjecie, "degrad.jpg"));
        maction.queue(msg -> czlonek.getGuild()
                .removeRoleFromMember(czlonek, Objects.requireNonNull(czlonek.getGuild().getRoleById(Ustawienia.instance.gadmRole)))
                .queueAfter(5, TimeUnit.SECONDS, success -> msg
                        .editMessage(context.getTranslated("degrad.success", UserUtil.formatDiscrim(czlonek)))
                        .queue()));

    }
}
