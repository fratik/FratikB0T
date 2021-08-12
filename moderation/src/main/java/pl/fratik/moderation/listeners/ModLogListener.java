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

package pl.fratik.moderation.listeners;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.role.GenericRoleEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.fratik.core.Globals;
import pl.fratik.core.cache.Cache;
import pl.fratik.core.cache.RedisCacheManager;
import pl.fratik.core.entity.*;
import pl.fratik.core.event.ScheduleEvent;
import pl.fratik.core.manager.ManagerKomend;
import pl.fratik.core.tlumaczenia.Tlumaczenia;
import pl.fratik.core.util.ExpiringHashMap;
import pl.fratik.core.util.ExpiringHashSet;
import pl.fratik.moderation.entity.AutoAkcja;
import pl.fratik.moderation.entity.Case;
import pl.fratik.moderation.entity.CaseDao;
import pl.fratik.moderation.events.NewCaseEvent;
import pl.fratik.moderation.events.UpdateCaseEvent;
import pl.fratik.moderation.utils.ModLogBuilder;
import pl.fratik.moderation.utils.WarnUtil;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Predicate;

public class ModLogListener {
    @Getter private final Map<String, Case> knownCases = new ExpiringHashMap<>(30, TimeUnit.SECONDS);
    private final LeaveHandler leaveHandler = new LeaveHandler();
    private final ShardManager shardManager;
    private final CaseDao caseDao;
    private final GuildDao guildDao;
    private final ScheduleDao scheduleDao;
    private final Tlumaczenia tlumaczenia;
    private final ManagerKomend managerKomend;
    private final Cache<GuildConfig> gcCache;
    private static final ThreadLocal<GuildConfig> localGc = new ThreadLocal<>();

    public ModLogListener(ShardManager shardManager, CaseDao caseDao, GuildDao guildDao, ScheduleDao scheduleDao, Tlumaczenia tlumaczenia, ManagerKomend managerKomend, RedisCacheManager rcm) {
        this.shardManager = shardManager;
        this.caseDao = caseDao;
        this.guildDao = guildDao;
        this.scheduleDao = scheduleDao;
        this.tlumaczenia = tlumaczenia;
        this.managerKomend = managerKomend;
        gcCache = rcm.new CacheRetriever<GuildConfig>() {}.getCache();
    }

    public static String generateKey(Member mem) {
        return generateKey(mem.getUser(), mem.getGuild());
    }

    public static String generateKey(User user, Guild guild) {
        return generateKey(user.getId(), guild.getId());
    }

    public static String generateKey(long uid, long gid) {
        return generateKey(Long.toUnsignedString(uid), Long.toUnsignedString(gid));
    }

    public static String generateKey(String uid, String gid) {
        return uid + gid;
    }

    public boolean checkPermissions(GenericGuildEvent e) {
        return checkPermissions(e.getGuild());
    }

    public boolean checkPermissions(GenericRoleEvent e) {
        return checkPermissions(e.getGuild());
    }

    public boolean checkPermissions(Guild g) {
        return g.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS, Permission.MANAGE_ROLES,
                Permission.BAN_MEMBERS, Permission.KICK_MEMBERS, Permission.MANAGE_SERVER);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMemberRemove(GuildMemberRemoveEvent e) {
        if (!checkPermissions(e)) return;
        User user = e.getUser();
        Guild guild = e.getGuild();
        String key = generateKey(user, guild);
        leaveHandler.memberLeft(key);
        Optional<AuditLogEntry> entry = findAuditLogEntry(guild, user.getIdLong(), ActionType.KICK);
        if (!entry.isPresent()) return;
        AuditLogEntry kickEntry = entry.get();
        Case kickCase = knownCases.get(key);
        if (kickCase == null) {
            TemporalAccessor timestamp = Instant.now();
            Long issuerId = null;
            String reason = null;
            User issuer = kickEntry.getUser();
            if (issuer != null && kickEntry.getReason() != null) {
                issuerId = issuer.getIdLong();
                reason = kickEntry.getReason();
                timestamp = kickEntry.getTimeCreated();
            }
            kickCase = new Case.Builder(guild, user, timestamp, Kara.KICK).setIssuerId(issuerId).setReason(reason, true).build();
        }
        saveCase(null, kickCase, true);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildBan(GuildBanEvent e) {
        if (!checkPermissions(e)) return;
        User user = e.getUser();
        Guild guild = e.getGuild();
        String key = generateKey(user, guild);
        boolean left = leaveHandler.wait(key);
        Case banCase = knownCases.get(key);
        if (banCase == null) {
            TemporalAccessor timestamp = Instant.now();
            Optional<AuditLogEntry> entry = findAuditLogEntry(e.getGuild(), user.getIdLong(), ActionType.BAN);
            Long issuerId = null;
            String reason = null;
            if (entry.isPresent()) {
                AuditLogEntry banEntry = entry.get();
                User issuer = banEntry.getUser();
                if (issuer != null && banEntry.getReason() != null) {
                    issuerId = issuer.getIdLong();
                    reason = banEntry.getReason();
                    timestamp = banEntry.getTimeCreated();
                }
            }
            banCase = new Case.Builder(guild, user, timestamp, Kara.BAN).setIssuerId(issuerId).setReason(reason, true).build();
        }
        saveCase(null, banCase, left);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onGuildUnban(GuildUnbanEvent e) {
        if (!checkPermissions(e)) return;
        User user = e.getUser();
        Guild guild = e.getGuild();
        String key = generateKey(user, guild);
        Case lastCase = null;
        List<Case> cases = caseDao.getCasesByMember(user, guild);
        Iterator<Case> iterator = cases.iterator();
        while (iterator.hasNext() && lastCase == null) {
            Case aCase = iterator.next();
            if (aCase.getType() == Kara.BAN) {
                if (!aCase.isValid()) break;
                lastCase = aCase;
            }
        }
        Case unbanCase = knownCases.get(key);
        if (unbanCase == null) {
            TemporalAccessor timestamp = Instant.now();
            Optional<AuditLogEntry> entry = findAuditLogEntry(e.getGuild(), user.getIdLong(), ActionType.UNBAN);
            Long issuerId = null;
            String reason = null;
            if (entry.isPresent()) {
                AuditLogEntry unbanEntry = entry.get();
                User issuer = unbanEntry.getUser();
                if (issuer != null && unbanEntry.getReason() != null) {
                    issuerId = issuer.getIdLong();
                    reason = unbanEntry.getReason();
                    timestamp = unbanEntry.getTimeCreated();
                }
            }
            unbanCase = new Case.Builder(guild, user, timestamp, Kara.UNBAN).setIssuerId(issuerId).setReason(reason, true).build();
        }
        if (lastCase != null) {
            lastCase.setValid(false);
            lastCase.setValidTo(unbanCase.getValidTo());
        }
        saveCase(lastCase, unbanCase, false);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onMemberUpdate(GuildMemberUpdateEvent e) {
        if (!checkPermissions(e)) return;
        Member mem = e.getMember();
        Guild guild = e.getGuild();
        String key = generateKey(mem);
        Role muteRole = getMuteRole(guild);
        if (muteRole == null) return;
        Case lastCase = null;
        List<Case> cases = caseDao.getCasesByMember(mem);
        Iterator<Case> iterator = cases.iterator();
        while (iterator.hasNext() && lastCase == null) {
            Case aCase = iterator.next();
            if (aCase.getType() == Kara.MUTE || aCase.getType() == Kara.UNMUTE) {
                if (!aCase.isValid()) break;
                lastCase = aCase;
            }
        }
        Case aCase = null;
        Optional<AuditLogEntry> entry = Optional.empty();
        TemporalAccessor timestamp = Instant.now();
        Kara type = null;
        Case knownCase = knownCases.get(key);
        if (mem.getRoles().contains(muteRole)) { // MUTE
            if (lastCase != null && lastCase.getType() != Kara.UNMUTE) return;
            //zapisz mute'a jeżeli ostatnią karą jest unmute lub żadnej nie ma - aka jeżeli nie ma żadnego ważnego mute'a
            if (knownCase != null && knownCase.getType() == Kara.MUTE) aCase = knownCase;
            else {
                entry = findAuditLogEntry(guild, mem.getIdLong(), ActionType.MEMBER_ROLE_UPDATE, l -> {
                    AuditLogChange changeByKey = l.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD);
                    if (changeByKey == null) return false;
                    List<String> newValue = changeByKey.getNewValue();
                    return newValue != null && newValue.contains(muteRole.getId());
                });
                type = Kara.MUTE;
            }
        } else { // UNMUTE
            if (lastCase == null || lastCase.getType() != Kara.MUTE) return;
            //zapisz unmute'a tylko jeżeli ostatnią karą jest mute - aka jeżeli jest ważny mute
            if (knownCase != null && knownCase.getType() == Kara.UNMUTE) aCase = knownCase;
            else {
                entry = findAuditLogEntry(guild, mem.getIdLong(), ActionType.MEMBER_ROLE_UPDATE, l -> {
                    AuditLogChange changeByKey = l.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE);
                    if (changeByKey == null) return false;
                    List<String> newValue = changeByKey.getNewValue();
                    return newValue != null && newValue.contains(muteRole.getId());
                });
                type = Kara.UNMUTE;
            }
        }
        if (aCase == null) {
            Long issuerId = null;
            String reason = null;
            if (entry.isPresent()) {
                AuditLogEntry logEntry = entry.get();
                User issuer = logEntry.getUser();
                if (issuer != null) {
                    issuerId = issuer.getIdLong();
                    reason = logEntry.getReason();
                    timestamp = logEntry.getTimeCreated();
                }
            }
            aCase = new Case.Builder(mem, timestamp, type).setIssuerId(issuerId).setReason(reason, true).build();
        }
        if (lastCase != null) {
            lastCase.setValid(false);
            lastCase.setValidTo(aCase.getTimestamp());
        }
        saveCase(lastCase, aCase, false);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onRoleDelete(RoleDeleteEvent e) {
        Instant now = Instant.now();
        if (!checkPermissions(e)) return;
        Guild guild = e.getGuild();
        if (!e.getRole().getId().equals(getGuildConfig(guild.getIdLong()).getWyciszony())) return;
        AuditLogEntry entry = findAuditLogEntry(guild, e.getRole().getIdLong(), ActionType.ROLE_DELETE).orElse(null);
        for (Case aCase : caseDao.getCasesByGuild(guild.getId())) {
            if (aCase.getType() != Kara.MUTE || aCase.isValid()) continue;
            aCase.setValid(false);
            aCase.setValidTo(now);
            Case.Builder bCase = new Case.Builder(aCase.getGuildId(), aCase.getUserId(), now, Kara.UNMUTE);
            if (entry != null && entry.getUser() != null) bCase
                    .setIssuerId(entry.getUser().getIdLong())
                    .setReasonKey("modlog.mute.role.self.deleted");
            else bCase
                    .setIssuerId(Globals.clientId)
                    .setReasonKey("modlog.mute.role.deleted");
            saveCase(aCase, bCase.build(), false);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onNewCase(NewCaseEvent e) {
        Guild g = shardManager.getGuildById(e.getCase().getGuildId());
        if (g == null || !checkPermissions(g)) return;
        Case aCase = e.getCase();
        User user;
        try {
            user = shardManager.retrieveUserById(aCase.getUserId()).complete();
        } catch (Exception ex) {
            return;
        }
        if (!aCase.getFlagi().contains(Case.Flaga.SILENT)) {
            caseDao.lock(aCase);
            try {
                sendCaseMessage(aCase, resolveModLogChannel(getGuildConfig(aCase.getGuildId())));
                if (e.isSendDm() && getGuildConfig(aCase.getGuildId()).isWysylajDmOKickachLubBanach())
                    sendDm(aCase, user, g);
            } finally {
                caseDao.unlock(aCase);
            }
        }
        if (aCase.getValidTo() != null && aCase.getIssuerId() != null) {
            scheduleDao.save(scheduleDao.createNew(Instant.from(aCase.getValidTo()).toEpochMilli(),
                    Long.toUnsignedString(aCase.getIssuerId()), Akcja.EVENT, new AutoAkcja(aCase.getCaseNumber(),
                            aCase.getType().opposite(), Long.toUnsignedString(aCase.getGuildId()))));
        }
        if (e.getCase().getType() == Kara.WARN || e.getCase().getType() == Kara.UNWARN)
            WarnUtil.takeAction(this, guildDao, caseDao, g.retrieveMember(user).complete(), e.getChannel(),
                    e.getLanguage(), tlumaczenia, managerKomend);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onSchedule(ScheduleEvent e) {
        if (!(e.getContent() instanceof AutoAkcja)) return;
        AutoAkcja cnt = (AutoAkcja) e.getContent();
        Guild g = shardManager.getGuildById(cnt.getGuildId());
        if (g == null) return;
        Case aCase = caseDao.getLocked(CaseDao.getId(cnt.getGuildId(), cnt.getCaseId()));
        if (aCase == null) return; //?
        try {
            Kara adw = cnt.getAkcjaDoWykonania();
            Case newCase = new Case.Builder(aCase.getGuildId(), aCase.getUserId(), aCase.getValidTo(), adw)
                    .setIssuerId(Globals.clientId).setReasonKey("modlog.timed.reason." + adw.name().toLowerCase()).build();
            if (adw == Kara.UNWARN) {
                aCase.setValid(false);
                aCase.setValidTo(Instant.now());
                caseDao.createNew(aCase, newCase, false);
            }
            else if (adw == Kara.UNMUTE || adw == Kara.UNBAN)
                getKnownCases().put(ModLogListener.generateKey(aCase.getUserId(), aCase.getGuildId()), newCase);
            if (adw == Kara.UNBAN) {
                try {
                    g.unban(User.fromId(aCase.getUserId())).complete();
                } catch (Exception ignored) {
                    // nie udało się, ignoruj
                    return;
                }
            } else if (adw == Kara.UNMUTE) {
                try {
                    g.removeRoleFromMember(aCase.getUserId(), getMuteRole(g)).complete();
                } catch (Exception ignored) {
                    // nie udało się, ignoruj
                    return;
                }
            }
        } finally {
            caseDao.unlock(aCase);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void onUpdateCase(UpdateCaseEvent e) {
        if (e.isInternalChange()) return;
        Guild g = shardManager.getGuildById(e.getCase().getGuildId());
        if (g == null || !checkPermissions(g)) return;
        Case aCase = e.getCase();
        updateCaseMessage(aCase, resolveModLogChannel(getGuildConfig(aCase.getGuildId())));
        updateDm(aCase, shardManager.retrieveUserById(aCase.getUserId()).onErrorMap(ex -> null).complete(), g);
    }

    private void saveCase(Case toModify, Case aCase, boolean sendDm) {
        caseDao.createNew(toModify, aCase, sendDm);
    }

    private Case sendCaseMessage(Case aCase, TextChannel mlogchan) {
        if (mlogchan == null) return aCase;
        Message toSend = ModLogBuilder.generate(aCase, mlogchan.getGuild(), shardManager,
                tlumaczenia.getLanguage(mlogchan.getGuild()), managerKomend, true, false, false);
        try {
            Message msg = mlogchan.sendMessage(toSend).complete();
            aCase.setMessageId(msg.getIdLong());
            caseDao.save(aCase, true);
        } catch (Exception ignored) {
            // ignoruj, zapisz tylko w db
        }
        return aCase;
    }

    private Case updateCaseMessage(Case aCase, TextChannel mlogchan) {
        if (mlogchan == null || aCase.getMessageId() == null) return aCase;
        Message toSend = ModLogBuilder.generate(aCase, mlogchan.getGuild(), shardManager,
                tlumaczenia.getLanguage(mlogchan.getGuild()), managerKomend, true, false, false);
        try {
            mlogchan.retrieveMessageById(aCase.getMessageId())
                    .flatMap(m -> m.editMessage(toSend).override(true)).complete();
        } catch (Exception e) {
            if (!aCase.isNeedsUpdate()) return sendCaseMessage(aCase, mlogchan);
        }
        return aCase;
    }

    private Case sendDm(Case aCase, User user, Guild g) {
        if (aCase.getIssuerId() == null) return aCase;
        Message toSend = ModLogBuilder.generate(aCase, g, shardManager, tlumaczenia.getLanguage(user),
                null, true, false, true);
        try {
            Message msg = user.openPrivateChannel().flatMap(chan -> chan.sendMessage(toSend)).complete();
            aCase.setDmMsgId(msg.getIdLong());
            caseDao.save(aCase, true);
        } catch (Exception ignored) {
            // jeżeli nie uda się wysłać DMa, ignoruj
        }
        return aCase;
    }

    private Case updateDm(Case aCase, User user, Guild g) {
        if (aCase.getIssuerId() == null || aCase.getDmMsgId() == null || user == null) return aCase;
        Message toSend = ModLogBuilder.generate(aCase, g, shardManager, tlumaczenia.getLanguage(user),
                null, true, false, true);
        try {
            user.openPrivateChannel().flatMap(chan -> chan.retrieveMessageById(aCase.getDmMsgId()))
                    .flatMap(m -> m.editMessage(toSend).override(true)).complete();
        } catch (Exception ignored) {
            // ignoruj
        }
        return aCase;
    }

    private GuildConfig getGuildConfig(long id) {
        GuildConfig savedGc = localGc.get();
        if (savedGc != null && savedGc.getGuildId().equals(Long.toUnsignedString(id))) return savedGc;
        else localGc.remove();
        GuildConfig gc = gcCache.get(Long.toUnsignedString(id), guildDao::get);
        localGc.set(gc);
        return gc;
    }

    private static class LeaveHandler {
        private final Set<String> recentlyLeft = new ExpiringHashSet<>(30, TimeUnit.SECONDS);
        private final Map<String, Thread> waiting = new ExpiringHashMap<>(30, TimeUnit.SECONDS);
        private final Object lock = new Object();

        private void memberLeft(String id) {
            synchronized (lock) {
                recentlyLeft.add(id);
            }
            notify(id);
        }

        private void notify(String id) {
            Thread thread;
            synchronized (lock) {
                thread = waiting.remove(id);
                if (thread == null) return;
            }
            LockSupport.unpark(thread);
        }

        private boolean wait(String id) {
            synchronized (lock) {
                if (recentlyLeft.remove(id)) return true;
                waiting.put(id, Thread.currentThread());
            }
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
            synchronized (lock) {
                waiting.remove(id);
                return recentlyLeft.remove(id);
            }
        }
    }

    public TextChannel resolveModLogChannel(GuildConfig gc) {
        String mlogchanStr = gc.getModLog();
        if (mlogchanStr == null || mlogchanStr.equals("")) mlogchanStr = "0";
        TextChannel mlogchan = shardManager.getTextChannelById(mlogchanStr);
        if (mlogchan == null || !mlogchan.getGuild().getSelfMember().hasPermission(mlogchan,
                Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
            return null;
        else return mlogchan;
    }

    private Optional<AuditLogEntry> findAuditLogEntry(Guild g, Long id, ActionType type) {
        return findAuditLogEntry(g, id, type, l -> true);
    }

    private Optional<AuditLogEntry> findAuditLogEntry(Guild g, Long id, ActionType type, Predicate<AuditLogEntry> extraFilter) {
        return g.retrieveAuditLogs().type(type).complete()
                .stream().filter(l -> l.getTimeCreated().isAfter(OffsetDateTime.now().minusSeconds(15)) &&
                (id == null || l.getTargetIdLong() == id) && extraFilter.test(l)).findFirst();
    }

    public Role getMuteRole(Guild guild) {
        String wyciszony = getGuildConfig(guild.getIdLong()).getWyciszony();
        if (wyciszony == null || wyciszony.isEmpty()) return null;
        return guild.getRoleById(wyciszony);
    }
}
