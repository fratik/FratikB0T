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
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.entity.GuildConfig;
import pl.fratik.core.entity.GuildDao;
import pl.fratik.core.entity.Kara;
import pl.fratik.core.event.ScheduleEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.moderation.entity.*;
import pl.fratik.moderation.utils.ModLogBuilder;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        Guild guild = guildBanEvent.getGuild();
        if (knownCases.get(guild) == null ||
                knownCases.get(guild).stream()
                        .noneMatch(c -> c.getUserId().equals(guildBanEvent.getUser().getId()) && c.getType() == Kara.BAN)) {
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            Case aCase = new CaseBuilder().setUser(guildBanEvent.getUser()).setGuild(guild)
                    .setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.BAN).createCase();
            User odpowiedzialny = null;
            String powod = null;
            try {
                List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.BAN).complete();
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
            setAndSend(guildConfig, caseRow, mode, mlogchan, aCase, odpowiedzialny, powod, guild, true);
        } else {
            Optional<Case> oCase = knownCases.get(guild).stream()
                    .filter(c -> c.getUserId().equals(guildBanEvent.getUser().getId()) && c.getType() == Kara.BAN)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            checkSendActionAndSave(guild, aCase, guildConfig, caseRow, mode, mlogchan, true);
            List<Case> zabijciemnie = knownCases.get(guild);
            zabijciemnie.remove(aCase);
            knownCases.put(guild, zabijciemnie);
        }
    }

    @Subscribe
    public void onGuildUnban(GuildUnbanEvent guildUnbanEvent) {
        Guild guild = guildUnbanEvent.getGuild();
        if (knownCases.get(guild) == null ||
                knownCases.get(guild).stream()
                        .noneMatch(c -> c.getUserId().equals(guildUnbanEvent.getUser().getId()) && c.getType() == Kara.UNBAN)) {
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            TemporalAccessor timestamp = Instant.now();
            boolean isCase = false;
            for (Case banCase : caseRow.getCases()) {
                if (banCase.getType() != Kara.BAN || !banCase.isValid() || !banCase.getUserId()
                        .equals(guildUnbanEvent.getUser().getId())) continue;
                isCase = true;
                invalidateCase(guildConfig, timestamp, mode, mlogchan, banCase, guild);
            }
            if (!isCase) return;
            int caseId = Case.getNextCaseId(caseRow);
            Case aCase = new CaseBuilder().setUser(guildUnbanEvent.getUser()).setGuild(guild)
                    .setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.UNBAN).createCase();
            User odpowiedzialny = null;
            String powod = null;
            try {
                List<AuditLogEntry> entries = guild.retrieveAuditLogs().type(ActionType.UNBAN).complete();
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
            setAndSend(guildConfig, caseRow, mode, mlogchan, aCase, odpowiedzialny, powod, guild, false);
        } else {
            Optional<Case> oCase = knownCases.get(guild).stream()
                    .filter(c -> c.getUserId().equals(guildUnbanEvent.getUser().getId()) && c.getType() == Kara.UNBAN)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            Case aCase = oCase.get();
            GuildConfig guildConfig = guildDao.get(guild);
            CaseRow caseRow = casesDao.get(guild);
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            boolean isCase = false;
            for (Case banCase : caseRow.getCases()) {
                if (banCase.getType() != Kara.BAN || !banCase.isValid() || !banCase.getUserId()
                        .equals(guildUnbanEvent.getUser().getId())) continue;
                isCase = true;
                invalidateCase(guildConfig, aCase.getValidTo(), mode, mlogchan, banCase, guild);
            }
            if (!isCase) return;
            if (mode == ModLogMode.MODLOG) {
                saveToModLog(aCase, guildConfig, caseRow, mlogchan, guild);
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
    public void onMemberRemove(GuildMemberRemoveEvent e) {
        OffsetDateTime now = OffsetDateTime.now();
        Guild guild = e.getGuild();
        User user = e.getUser();
        if (!guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) return;

        guild.retrieveAuditLogs().type(ActionType.KICK).delay(2, TimeUnit.SECONDS).queue(entries -> {
            for (AuditLogEntry entry : entries) {
                if (!entry.getTimeCreated().isAfter(now.minusSeconds(30))) continue;
                if (entry.getTargetIdLong() == user.getIdLong()) {
                    if (knownCases.get(guild) == null || knownCases.get(guild).stream()
                            .noneMatch(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.KICK)) {
                        GuildConfig guildConfig = guildDao.get(guild);
                        CaseRow caseRow = casesDao.get(guild);
                        ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
                        ModLogMode mode = modeResolver.getMode();
                        TextChannel mlogchan = modeResolver.getMlogchan();
                        int caseId = Case.getNextCaseId(caseRow);
                        TemporalAccessor timestamp = Instant.now();
                        Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId)
                                .setTimestamp(timestamp).setMessageId(null).setKara(Kara.KICK).createCase();
                        User odpowiedzialny = entry.getUser();
                        String powod = entry.getReason();
                        Member srember = guild.getMember(user);
                        setAndSend(guildConfig, caseRow, mode, mlogchan, aCase, odpowiedzialny, powod, guild, true);
                    } else {
                        Optional<Case> oCase = knownCases.get(guild).stream()
                                .filter(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.KICK)
                                .findFirst();
                        if (!oCase.isPresent()) throw NOCASEEXC;
                        if (!oCase.get().getFlagi().contains(Case.Flaga.SILENT))
                            sendAction(oCase.get(), guild, guildDao.get(guild));
                        saveKnownCase(guild, oCase.get());
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
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.MUTE).createCase();
            if (mode == ModLogMode.MODLOG) {
                saveToModLog(aCase, guildConfig, caseRow, mlogchan, guild);
            } else {
                caseRow.getCases().add(aCase);
                casesDao.save(caseRow);
            }
        } else {
            Optional<Case> oCase = knownCases.get(guild).stream()
                    .filter(c -> c.getUserId().equals(user.getId()) && c.getType() == Kara.MUTE)
                    .findFirst();
            if (!oCase.isPresent()) throw NOCASEEXC;
            saveKnownCase(guild, oCase.get());
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
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            int caseId = Case.getNextCaseId(caseRow);
            TemporalAccessor timestamp = Instant.now();
            List<Case> cases = caseRow.getCases();
            Case aCase = new CaseBuilder().setUser(user).setGuild(guild).setCaseId(caseId).setTimestamp(timestamp).setMessageId(null).setKara(Kara.UNMUTE).createCase();
            invalidateOldMutes(guildMemberRoleRemoveEvent, guildConfig, caseRow, cases, timestamp, mode, mlogchan);
            if (mode == ModLogMode.MODLOG) {
                saveToModLog(aCase, guildConfig, caseRow, mlogchan, guild);
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
            ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
            ModLogMode mode = modeResolver.getMode();
            TextChannel mlogchan = modeResolver.getMlogchan();
            invalidateOldMutes(guildMemberRoleRemoveEvent, guildConfig, caseRow, cases, teraz, mode, mlogchan);
            poddajeSieZTymiNazwami(guild, aCase, guildConfig, caseRow, mode, mlogchan);
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
                MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager, gc.getLanguage(), managerKomend, true);
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
            MessageEmbed embed = ModLogBuilder.generate(bCase, guild, shardManager, gc.getLanguage(), managerKomend, true);
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
            MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager, tlumaczenia.getLanguage(guild), managerKomend, true);
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
            Member member;
            try {
                member = guild.retrieveMemberById(akcja.getCase().getUserId()).complete();
            } catch (ErrorResponseException er) {
                if (er.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER)
                    member = null;
                else throw er;
            }
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

    private void setAndSend(GuildConfig guildConfig, CaseRow caseRow, ModLogMode mode, TextChannel mlogchan, Case aCase, User odpowiedzialny, String powod, Guild guild, boolean send) {
        if (odpowiedzialny != null) aCase.setIssuerId(odpowiedzialny);
        if (powod != null) aCase.setReason(powod);
        checkSendActionAndSave(guild, aCase, guildConfig, caseRow, mode, mlogchan, send);
    }

    private void saveToModLog(Case aCase, GuildConfig guildConfig, CaseRow caseRow, TextChannel mlogchan, Guild guild) {
        if (aCase.getFlagi().contains(Case.Flaga.SILENT)) {
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
            return;
        }
        MessageEmbed embed = ModLogBuilder.generate(aCase, guild, shardManager,
                guildConfig.getLanguage(), managerKomend, true);
        mlogchan.sendMessage(embed).queue(message -> {
            aCase.setMessageId(message.getId());
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        });
    }

    private void invalidateCase(GuildConfig gc, TemporalAccessor validTo, ModLogMode mode, TextChannel mlogchan, Case c, Guild guild) {
        c.setValidTo(validTo, true);
        c.setValid(false);
        MessageEmbed embed = ModLogBuilder.generate(c, guild,
                shardManager, gc.getLanguage(), managerKomend, true);
        if (mode == ModLogMode.MODLOG) {
            try {
                Message msg = mlogchan.retrieveMessageById(c.getMessageId()).complete();
                if (msg == null) throw new IllegalStateException();
                msg.editMessage(embed).override(true).complete();
            } catch (Exception ignored) {
                /*lul*/
            }
        }
    }

    private void invalidateOldMutes(GuildMemberRoleRemoveEvent guildMemberRoleRemoveEvent, GuildConfig guildConfig, CaseRow caseRow, List<Case> cases, TemporalAccessor teraz, ModLogMode mode, TextChannel mlogchan) {
        for (Case muteCase : cases) {
            if (muteCase.getType() != Kara.MUTE || !muteCase.isValid() || !muteCase.getUserId()
                    .equals(guildMemberRoleRemoveEvent.getMember().getUser().getId())) continue;
            invalidateCase(guildConfig, teraz, mode, mlogchan, muteCase, guildMemberRoleRemoveEvent.getGuild());
        }
        caseRow.setCases(cases);
    }

    private void saveKnownCase(Guild guild, Case aCase) {
        GuildConfig guildConfig = guildDao.get(guild);
        CaseRow caseRow = casesDao.get(guild);
        ModeResolver modeResolver = new ModeResolver(guildConfig).invoke();
        ModLogMode mode = modeResolver.getMode();
        TextChannel mlogchan = modeResolver.getMlogchan();
        poddajeSieZTymiNazwami(guild, aCase, guildConfig, caseRow, mode, mlogchan);
    }

    private void poddajeSieZTymiNazwami(Guild guild, Case aCase, GuildConfig guildConfig, CaseRow caseRow, ModLogMode mode, TextChannel mlogchan) {
        if (mode == ModLogMode.MODLOG) {
            saveToModLog(aCase, guildConfig, caseRow, mlogchan, guild);
        } else {
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        }
        List<Case> zabijciemnie = knownCases.get(guild);
        zabijciemnie.remove(aCase);
        knownCases.put(guild, zabijciemnie);
    }

    private void sendAction(Case caze, Guild guild, GuildConfig gc) {
        if (gc.getWysylajDmOKickachLubBanach() == null) {
            gc.setWysylajDmOKickachLubBanach(true); // nie chce mi sie kombinować z guilDao.save
        }
        if (caze.getFlagi().contains(Case.Flaga.SILENT) || !gc.getWysylajDmOKickachLubBanach()) return;
        MessageEmbed embed = ModLogBuilder.generate(caze, guild, shardManager,
                gc.getLanguage(), managerKomend, true);

        User u = shardManager.getUserById(caze.getUserId());
        if (u == null) return; // użytkownik nie znaleziony, można śmiało ignorować
        try {
            Language lang = tlumaczenia.getLanguage(u);
            u.openPrivateChannel()
                    .flatMap(c -> c.sendMessage(tlumaczenia.get(lang, "modlog.dm.msg", guild.getName()))
                            .embed(embed)).complete();
        } catch (Exception ignored) { }
    }

    private void checkSendActionAndSave(Guild guild, Case aCase, GuildConfig guildConfig, CaseRow caseRow, ModLogMode mode, TextChannel mlogchan, boolean send) {
        if (!aCase.getFlagi().contains(Case.Flaga.SILENT) && send) sendAction(aCase, guild, guildConfig);
        if (mode == ModLogMode.MODLOG) {
            saveToModLog(aCase, guildConfig, caseRow, mlogchan, guild);
        } else {
            caseRow.getCases().add(aCase);
            casesDao.save(caseRow);
        }
    }

    public enum ModLogMode {
        MODLOG,
        DATABASE;
    }
    @Getter
    private class ModeResolver {

        private final GuildConfig gc;
        private ModLogMode mode;
        private TextChannel mlogchan;

        public ModeResolver(GuildConfig gc) {
            this.gc = gc;
        }

        public ModeResolver invoke() {
            String mlogchanStr = gc.getModLog();
            if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
            mlogchan = shardManager.getTextChannelById(mlogchanStr);
            if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                    Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                mode = ModLogMode.DATABASE;
            else mode = ModLogMode.MODLOG;
            return this;
        }
    }
}
