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

package pl.fratik.moderation.listeners;

import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.event.ScheduleEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.*;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public class ModLogListener {

    private final GuildDao guildDao;
    private final ShardManager shardManager;
    private final CasesDao casesDao;
    @Setter private static Tlumaczenia tlumaczenia;
    @Setter private static ManagerKomend managerKomend;

    @Getter private static final HashMap<Guild, List<Case>> knownCases = new HashMap<>();
    @Getter private final List<String> ignoredMutes = new ArrayList<>();
    private static final IllegalStateException NOCASEEXC = new IllegalStateException("Nie ma case'a");

    public ModLogListener(GuildDao guildDao, ShardManager shardManager, CasesDao casesDao) {
        this.guildDao = guildDao;
        this.shardManager = shardManager;
        this.casesDao = casesDao;
    }

    @Subscribe
    public void onGuildBan(GuildBanEvent guildBanEvent) {
        if (knownCases.get(guildBanEvent.getGuild()) == null ||
                knownCases.get(guildBanEvent.getGuild()).stream()
                        .noneMatch(c -> c.getUserId().equals(guildBanEvent.getUser().getId()) && c.getType() == Kara.BAN)) {
            GuildConfig guildConfig = guildDao.get(guildBanEvent.getGuild());
            CaseRow caseRow = casesDao.get(guildBanEvent.getGuild());
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                            Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            Case aCase = new CaseBuilder().setUser(guildBanEvent.getUser()).setGuild(guildBanEvent.getGuild())
                    .setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.BAN).createCase();
            User odpowiedzialny = null;
            String powod = null;
            try {
                List<AuditLogEntry> entries = guildBanEvent.getGuild().retrieveAuditLogs().type(ActionType.BAN).complete();
                for (AuditLogEntry e : entries) {
                    if (e.getTimeCreated().isAfter(OffsetDateTime.now().minusSeconds(15)) &&
                            e.getTargetIdLong() == guildBanEvent.getUser().getIdLong()) {
                        odpowiedzialny = e.getUser();
                        powod = e.getReason();
                        break;
                    }
                }
            } catch (Exception e) {
                // nie mamy permów i guess
            }
            setAndSend(guildConfig, caseRow, mode, mlogchan, aCase, odpowiedzialny, powod, guildBanEvent.getGuild());
        } else {
            Optional<Case> oCase = knownCases.get(guildBanEvent.getGuild()).stream()
                    .filter(c -> c.getUserId().equals(guildBanEvent.getUser().getId()) && (c.getType() == Kara.BAN ||
                            c.getType() == Kara.TIMEDBAN))
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            GuildConfig guildConfig = guildDao.get(guildBanEvent.getGuild());
            CaseRow caseRow = casesDao.get(guildBanEvent.getGuild());
            ModLogMode mode;
            TextChannel mlogchan = null;
            if (guildConfig.getModLog() != null && !guildConfig.getModLog().isEmpty())
                mlogchan = shardManager.getTextChannelById(guildConfig.getModLog());
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guildBanEvent.getGuild(), shardManager,
                        guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
            List<Case> zabijciemnie = knownCases.get(guildBanEvent.getGuild());
            zabijciemnie.remove(aCase);
            knownCases.put(guildBanEvent.getGuild(), zabijciemnie);
        }
    }

    @Subscribe
    public void onGuildUnban(GuildUnbanEvent guildUnbanEvent) {
        if (knownCases.get(guildUnbanEvent.getGuild()) == null ||
                knownCases.get(guildUnbanEvent.getGuild()).stream()
                        .noneMatch(c -> c.getUserId().equals(guildUnbanEvent.getUser().getId()) && c.getType() == Kara.UNBAN)) {
            GuildConfig guildConfig = guildDao.get(guildUnbanEvent.getGuild());
            CaseRow caseRow = casesDao.get(guildUnbanEvent.getGuild());
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            TemporalAccessor timestamp = Instant.now();
            boolean isCase = false;
            for (Case banCase : caseRow.getCases()) {
                if (banCase.getType() != Kara.BAN || !banCase.isValid()) continue;
                isCase = true;
                banCase.setValidTo(timestamp, true);
                banCase.setValid(false);
                MessageEmbed embed = ModLogBuilder.generate(banCase, guildUnbanEvent.getGuild(),
                        shardManager, guildConfig.getLanguage(), managerKomend);
                if (mode == ModLogMode.MODLOG) {
                    try {
                        Message msg = mlogchan.retrieveMessageById(banCase.getMessageId()).complete();
                        if (msg == null) throw new IllegalStateException();
                        msg.editMessage(embed).override(true).complete();
                    } catch (Exception ignored) {
                        /*lul*/
                    }
                }
            }
            if (!isCase) return;
            int caseId = Case.getNextCaseId(caseRow);
            Case aCase = new CaseBuilder().setUser(guildUnbanEvent.getUser()).setGuild(guildUnbanEvent.getGuild())
                    .setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.UNBAN).createCase();
            User odpowiedzialny = null;
            String powod = null;
            try {
                List<AuditLogEntry> entries = guildUnbanEvent.getGuild().retrieveAuditLogs().type(ActionType.UNBAN).complete();
                for (AuditLogEntry e : entries) {
                    if (e.getTimeCreated().isAfter(OffsetDateTime.now().minusSeconds(15)) &&
                            e.getTargetIdLong() == guildUnbanEvent.getUser().getIdLong()) {
                        odpowiedzialny = e.getUser();
                        powod = e.getReason();
                        break;
                    }
                }
            } catch (Exception e) {
                // nie mamy permów i guess
            }
            setAndSend(guildConfig, caseRow, mode, mlogchan, aCase, odpowiedzialny, powod, guildUnbanEvent.getGuild());
        } else {
            Optional<Case> oCase = knownCases.get(guildUnbanEvent.getGuild()).stream()
                    .filter(c -> c.getUserId().equals(guildUnbanEvent.getUser().getId()) && c.getType() == Kara.UNBAN)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            GuildConfig guildConfig = guildDao.get(guildUnbanEvent.getGuild());
            CaseRow caseRow = casesDao.get(guildUnbanEvent.getGuild());
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            boolean isCase = false;
            for (Case banCase : caseRow.getCases()) {
                if (banCase.getType() != Kara.BAN || !banCase.isValid()) continue;
                isCase = true;
                banCase.setValidTo(aCase.getValidTo(), true);
                banCase.setValid(false);
                MessageEmbed embed = ModLogBuilder.generate(banCase, guildUnbanEvent.getGuild(),
                        shardManager, guildConfig.getLanguage(), managerKomend);
                if (mode == ModLogMode.MODLOG) {
                    try {
                        Message msg = mlogchan.retrieveMessageById(banCase.getMessageId()).complete();
                        if (msg == null) throw new IllegalStateException();
                        msg.editMessage(embed).override(true).complete();
                    } catch (Exception ignored) {
                        /*lul*/
                    }
                }
            }
            if (!isCase) return;
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guildUnbanEvent.getGuild(), shardManager,
                        guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
            List<Case> zabijciemnie = knownCases.get(guildUnbanEvent.getGuild());
            zabijciemnie.remove(aCase);
            knownCases.put(guildUnbanEvent.getGuild(), zabijciemnie);
        }
    }

    private void setAndSend(GuildConfig guildConfig, CaseRow caseRow, ModLogMode mode, TextChannel mlogchan, Case aCase, User odpowiedzialny, String powod, Guild guild) {
        if (odpowiedzialny != null) aCase.setIssuerId(odpowiedzialny);
        if (powod != null) aCase.setReason(powod);
        if (mode == ModLogMode.MODLOG) {
            MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                    guildConfig.getLanguage(), managerKomend);
            mlogchan.sendMessage(embed).queue(message -> {
                aCase.setMessageId(message.getId());
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            });
        } else {
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        }
    }

    @Subscribe
    public void onMemberRemove(GuildMemberLeaveEvent guildMemberLeaveEvent) {
        Guild guild = guildMemberLeaveEvent.getGuild();
        User user = guildMemberLeaveEvent.getUser();
        if (!guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) return;

        guild.retrieveAuditLogs().type(ActionType.KICK).queue(entries -> {
            for (AuditLogEntry entry : entries) {
                if (!entry.getTimeCreated().isAfter(OffsetDateTime.now().minusSeconds(15))) continue;
                if (entry.getTargetIdLong() == user.getIdLong()) {
                    if (knownCases.get(guild) == null || knownCases.get(guild).stream()
                            .noneMatch(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.KICK)) {
                        GuildConfig guildConfig = guildDao.get(guild);
                        CaseRow caseRow = casesDao.get(guild);
                        ModLogMode mode;
                        String mlogchanStr = guildConfig.getModLog();
                        if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
                        TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
                        if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                             mode = ModLogMode.DATABASE;
                        else mode = ModLogMode.MODLOG;
                        int caseId = Case.getNextCaseId(caseRow);
                        TemporalAccessor timestamp = Instant.now();
                        Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId)
                                .setTimestamp(timestamp).setMessageId(null).setKara(Kara.KICK).createCase();
                        User odpowiedzialny = entry.getUser();
                        String powod = entry.getReason();
                        if (odpowiedzialny != null) aCase.setIssuerId(odpowiedzialny);
                        if (powod != null) aCase.setReason(powod);
                        if (mode == ModLogMode.MODLOG) {
                            MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                                    guildConfig.getLanguage(), managerKomend);
                            mlogchan.sendMessage(embed).queue(message -> {
                                aCase.setMessageId(message.getId());
                                caseRow.getCases().add(aCase);
                                casesDao.save(caseRow);
                            });
                        } else {
                            caseRow.getCases().add(aCase);
                            casesDao.save(caseRow);
                        }
                    } else {
                        Optional<Case> oCase = knownCases.get(guild).stream()
                                .filter(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.KICK)
                                .findFirst();
                        if (!oCase.isPresent()) throw NOCASEEXC;
                        Case aCase = oCase.get();
                        GuildConfig guildConfig = guildDao.get(guild);
                        CaseRow caseRow = casesDao.get(guild);
                        ModLogMode mode;
                        String mlogchanStr = guildConfig.getModLog();
                        if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
                        TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
                        if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                             mode = ModLogMode.DATABASE;
                        else mode = ModLogMode.MODLOG;
                        if (mode == ModLogMode.MODLOG) {
                            MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                                    guildConfig.getLanguage(), managerKomend);
                            mlogchan.sendMessage(embed).queue(message -> {
                                aCase.setMessageId(message.getId());
                                caseRow.getCases().add(aCase);
                                casesDao.save(caseRow);
                            });
                        } else {
                            caseRow.getCases().add(aCase);
                            casesDao.save(caseRow);
                        }
                        List<Case> zabijciemnie = knownCases.get(guild);
                        zabijciemnie.remove(aCase);
                        knownCases.put(guild, zabijciemnie);
                    }
                    break;
                }
            }}, err -> {}
        );
    }

    @Subscribe
    public void onMemberRoleAdd(GuildMemberRoleAddEvent guildMemberRoleAddEvent) {
        if (ignoredMutes.contains(guildMemberRoleAddEvent.getUser().getId() + guildMemberRoleAddEvent.getGuild().getId())) {
            ignoredMutes.remove(guildMemberRoleAddEvent.getUser().getId() + guildMemberRoleAddEvent.getGuild().getId());
            return;
        }
        Guild guild = guildMemberRoleAddEvent.getGuild();
        GuildConfig gc = guildDao.get(guild);
        User user = guildMemberRoleAddEvent.getUser();
        Role rola;
        try {
            rola = guild.getRoleById(gc.getWyciszony());
        } catch (Exception ignored) {
            rola = null;
        }
        if (rola == null) return;
        if (!guildMemberRoleAddEvent.getRoles().contains(rola)) return;
        if (knownCases.get(guild) == null || knownCases.get(guild).stream()
                .noneMatch(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.MUTE)) {
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.MUTE).createCase();
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager, guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
        } else {
            Optional<Case> oCase = knownCases.get(guild).stream()
                    .filter(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.MUTE)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                        guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
            List<Case> zabijciemnie = knownCases.get(guild);
            zabijciemnie.remove(aCase);
            knownCases.put(guild, zabijciemnie);
        }
    }

    @Subscribe
    public void onMemberRoleRemove(GuildMemberRoleRemoveEvent guildMemberRoleRemoveEvent) {
        Guild guild = guildMemberRoleRemoveEvent.getGuild();
        GuildConfig guildConfig = guildDao.get(guild);
        CaseRow caseRow = casesDao.get(guild);
        User user = guildMemberRoleRemoveEvent.getUser();
        Role rola;
        try {
            rola = guild.getRoleById(guildConfig.getWyciszony());
        } catch (Exception ignored) {
            rola = null;
        }
        if (rola == null) return;
        if (!guildMemberRoleRemoveEvent.getRoles().contains(rola)) return;
        if (knownCases.get(guild) == null || knownCases.get(guild).stream()
                .noneMatch(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.UNMUTE)) {
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            List<Case> cases = caseRow.getCases();
            Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.UNMUTE).createCase();
            for (Case muteCase : cases) {
                if (muteCase.getType() != Kara.MUTE || !muteCase.isValid()) continue;
                muteCase.setValidTo(timestamp, true);
                muteCase.setValid(false);
                MessageEmbed embed = ModLogBuilder.generate(muteCase, guildMemberRoleRemoveEvent.getGuild(),
                        shardManager, guildConfig.getLanguage(), managerKomend);
                if (mode == ModLogMode.MODLOG) {
                    try {
                        Message msg = mlogchan.retrieveMessageById(muteCase.getMessageId()).complete();
                        if (msg == null) throw new IllegalStateException();
                        msg.editMessage(embed).override(true).complete();
                    } catch (Exception ignored) {
                        /*lul*/
                    }
                }
            }
            caseRow.setCases(cases);
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                        guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
        } else {
            Optional<Case> oCase = knownCases.get(guild).stream()
                    .filter(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.UNMUTE)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            List<Case> cases = caseRow.getCases();
            TemporalAccessor teraz = Instant.now();
            ModLogMode mode;
            String mlogchanStr = guildConfig.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            for (Case muteCase : cases) {
                if (muteCase.getType() != Kara.MUTE || !muteCase.isValid()) continue;
                muteCase.setValidTo(teraz, true);
                muteCase.setValid(false);
                MessageEmbed embed = ModLogBuilder.generate(muteCase, guildMemberRoleRemoveEvent.getGuild(),
                        shardManager, guildConfig.getLanguage(), managerKomend);
                if (mode == ModLogMode.MODLOG) {
                    try {
                        Message msg = mlogchan.retrieveMessageById(muteCase.getMessageId()).complete();
                        if (msg == null) throw new IllegalStateException();
                        msg.editMessage(embed).override(true).complete();
                    } catch (Exception ignored) {
                        /*lul*/
                    }
                }
            }
            caseRow.setCases(cases);
            if (mode == ModLogMode.MODLOG) {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                        guildConfig.getLanguage(), managerKomend);
                mlogchan.sendMessage(embed).queue(message -> {
                    aCase.setMessageId(message.getId());
                    caseRow.getCases().add(aCase);
                    casesDao.save(caseRow);
                });
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
            List<Case> zabijciemnie = knownCases.get(guild);
            zabijciemnie.remove(aCase);
            knownCases.put(guild, zabijciemnie);
        }
    }

    @Subscribe
    public void onRoleDelete(RoleDeleteEvent roleDeleteEvent) {
        Guild guild = roleDeleteEvent.getGuild();
        GuildConfig gc = guildDao.get(guild);
        CaseRow caseRow = casesDao.get(guild);
        if (!roleDeleteEvent.getRole().getId().equals(gc.getWyciszony())) return;
        TemporalAccessor ts = Instant.now();
        boolean perms = false;
        String mlogchanStr = gc.getModLog();
        if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
        TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
        if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                Permission.MESSAGE_HISTORY, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
            perms = true;
        List<Integer> muty = new ArrayList<>();
        for (Case aCase : caseRow.getCases()) {
            if (!(aCase.isValid() && aCase.getType() == Kara.MUTE)) continue;
            muty.add(aCase.getCaseId());
            aCase.setValid(false);
            aCase.setValidTo(ts, true);
            try {
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager, gc.getLanguage(), managerKomend);
                if (perms && mlogchan != null) {
                    Message msg = mlogchan.retrieveMessageById(aCase.getMessageId()).complete();
                    if (msg == null) throw new IllegalStateException();
                    msg.editMessage(embed).override(true).complete();
                }
            } catch (Exception ignored) {
                /*lul*/
            }
        }
        List<Case> tmpCases = new ArrayList<>();
        int lastCid = -1;
        for (Integer caseId : muty) {
            Case aCase = Case.getCaseById(caseId, caseRow.getCases());
            if (aCase == null || aCase.getType() != Kara.MUTE) continue;
            if (lastCid == -1) lastCid = Case.getNextCaseId(guild) - 1;
            Case bCase = new CaseBuilder().setUser(aCase.getUserId()).setGuild(aCase.getGuildId()).setCaseId(lastCid + 1)
                    .setTimestamp(ts).setMessageId(null).setKara(Kara.UNMUTE).createCase();
            MessageEmbed embed = ModLogBuilder.generate(bCase, guild, shardManager, gc.getLanguage(), managerKomend);
            try {
                if (perms && mlogchan != null) mlogchan.sendMessage(embed).complete();
            } catch (Exception ignored) {/*lul*/}
            tmpCases.add(bCase);
            lastCid++;
        }
        for (Case a : tmpCases) caseRow.getCases().add(a);
        casesDao.save(caseRow);
    }

    @Subscribe
    public void onChannelCreate(TextChannelCreateEvent textChannelCreateEvent) {
        if (!textChannelCreateEvent.getGuild().getSelfMember().hasPermission(textChannelCreateEvent.getChannel(),
                Permission.MANAGE_PERMISSIONS)) return;
        Role rola;
        try {
            rola = textChannelCreateEvent.getGuild().getRoleById(guildDao.get(textChannelCreateEvent.getGuild()).getWyciszony());
        } catch (Exception ignored) {
            rola = null;
        }
        if (rola == null) return;
        textChannelCreateEvent.getChannel().upsertPermissionOverride(rola).deny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
    }

    @Subscribe
    public void onScheduleEvent(ScheduleEvent e) {
        if (!(e.getContent() instanceof AutoAkcja)) return;
        AutoAkcja akcja = (AutoAkcja) e.getContent();
        Guild guild = shardManager.getGuildById(akcja.getGuildId());
        if (guild == null) return;
        Kara adw = akcja.getAkcjaDoWykonania();
        Case aCase = new CaseBuilder().setUser(akcja.getCase().getUserId()).setGuild(akcja.getCase().getGuildId())
                .setCaseId(Case.getNextCaseId(guild)).setTimestamp(Instant.now()).setMessageId(null).setKara(adw)
                .createCase();
        aCase.setIssuerId(Objects.requireNonNull(shardManager.getShardById(0)).getSelfUser());
        String reason = tlumaczenia.get(tlumaczenia.getLanguage(guild), "modlog.timed.reason",
                tlumaczenia.get(tlumaczenia.getLanguage(guild), "modlog." +
                        adw.name().toLowerCase()).toLowerCase());
        aCase.setReason(reason);
        if (adw == Kara.UNBAN) {
            List<Case> caseList = knownCases.getOrDefault(guild, new ArrayList<>());
            caseList.add(aCase);
            guild.unban(akcja.getCase().getUserId()).reason(reason).queue(null, t -> {
                caseList.remove(aCase);
                knownCases.put(guild, caseList);
            });
            knownCases.put(guild, caseList);
            return;
        }
        if (adw == Kara.UNWARN) {
            MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager, tlumaczenia.getLanguage(guild), managerKomend);
            GuildConfig gc = guildDao.get(guild);
            String mlogchanStr;
            if (gc.getModLog() == null || gc.getModLog().isEmpty()) mlogchanStr = "0";
            else mlogchanStr = gc.getModLog();
            TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null) {
                CaseRow caseRow = casesDao.get(guild);
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
                return;
            }
            mlogchan.sendMessage(embed).queue(message -> {
                CaseRow caseRow = casesDao.get(guild);
                caseRow.getCases().add(aCase);
                aCase.setMessageId(message.getId());
                casesDao.save(caseRow);
            });
            return;
        }
        if (adw == Kara.UNMUTE) {
            Role muteRole = guild.getRoleById(guildDao.get(guild).getWyciszony());
            if (muteRole == null) return;
            List<Case> caseList = knownCases.getOrDefault(guild, new ArrayList<>());
            caseList.add(aCase);
            Member member = guild.getMemberById(akcja.getCase().getUserId());
            if (member == null) return;
            guild.removeRoleFromMember(member, muteRole)
                    .reason(reason).queue(null, t -> {
                List<Case> caseList2 = knownCases.getOrDefault(guild, new ArrayList<>());
                caseList2.remove(aCase);
                knownCases.put(guild, caseList2);
            });
            knownCases.put(guild, caseList);
        }
    }

    public enum ModLogMode {
        MODLOG,
        DATABASE
    }
}
