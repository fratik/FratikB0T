/*
 * Copyright (C) 2020 FratikB0T Contributors
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

package pl.fratik.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.fratik.core.command.Command;
import pl.fratik.core.command.CommandContext;
import pl.fratik.core.util.NetworkUtil;
import pl.fratik.core.util.UserUtil;

import java.util.Iterator;

@SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfStringBuilder"})
public class CommitCommand extends Command {

    private static String REPO = "fratik/FratikB0T";

    public CommitCommand() {
        name = "commit";
        aliases = new String[] {"git", "github"};
        cooldown = 10;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getPrimColor(context.getSender()));
        eb.setFooter("", context.getGuild().getJDA().getSelfUser().getAvatarUrl());

        try {
            JSONObject commits = NetworkUtil.getJson(String.format("https://api.github.com/repos/%s/commits", REPO));
            if (commits == null) throw new Exception();

            JSONObject commitInfo = NetworkUtil.getJson(commits.getString("url"));
            if (commitInfo == null) throw new Exception();

            JSONObject parents = commitInfo.getJSONObject("parents");
            eb.addField(context.getTranslated("commit.lastcommit"), context.getTranslated("generic.click",
                    parents.getString("html_url")), false);

            JSONObject commit = commitInfo.getJSONObject("commit");
            eb.addField(context.getTranslated("commit.author"),
                    commit.getJSONObject("author").getString("name"), false);

            eb.addField(context.getTranslated("commit.message"), commit.getString("message"), false);

            JSONObject stats = commitInfo.getJSONObject("stats");
            eb.addField(context.getTranslated("commit.statistic"), context.getTranslated("commit.stats", stats.getInt("additions"),
                    stats.getInt("deletions")), false);

            eb.setThumbnail(commitInfo.getJSONObject("committer").getString("avatar_url"));

            StringBuilder sb = new StringBuilder();
            sb.append(context.getTranslated("commit.edit")).append("```");
            JSONObject files = commitInfo.getJSONObject("files");

            Iterator<String> key = files.keys();
            while (key.hasNext()) {
                String next = key.next();
                if (next.equals("filename")) {
                    String value = files.getString(next);
                    String[] split = value.split("/");
                    value = split[split.length - 1];
                    sb.append(value).append("\n");
                }
            }
            sb.append("```");
            if (sb.toString().length() < MessageEmbed.TEXT_MAX_LENGTH) eb.setDescription(eb.toString());

        } catch (Exception e) {
            context.send(context.getTranslated("commit.error"));
            return false;
        }

        context.send(eb.build());
        return true;
    }

}
