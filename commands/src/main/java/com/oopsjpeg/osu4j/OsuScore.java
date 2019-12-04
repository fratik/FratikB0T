package com.oopsjpeg.osu4j;

import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.abstractbackend.LazilyLoaded;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.util.Utility;

import java.time.ZonedDateTime;

public class OsuScore extends OsuElement {
	private final int beatmapID;
	private final long scoreID;
	private final LazilyLoaded<OsuBeatmap> beatmap;
	private final int score;
	private final int maxcombo;
	private final int count300;
	private final int count100;
	private final int count50;
	private final int countmiss;
	private final int countkatu;
	private final int countgeki;
	private final boolean perfect;
	private final GameMod[] enabledMods;
	private final int userID;
	private final LazilyLoaded<OsuUser> user;
	private final ZonedDateTime date;
	private final String rank;
	private final float pp;
	private final int replayAvailable;

	public OsuScore(Osu api, JsonObject obj) {
		super(api);
		beatmapID = obj.has("beatmap_id") ? obj.get("beatmap_id").getAsInt() : 0;
		scoreID = obj.has("score_id") ? obj.get("score_id").getAsLong() : 0;
		score = obj.get("score").getAsInt();
		maxcombo = obj.get("maxcombo").getAsInt();
		count300 = obj.get("count300").getAsInt();
		count100 = obj.get("count100").getAsInt();
		count50 = obj.get("count50").getAsInt();
		countmiss = obj.get("countmiss").getAsInt();
		countkatu = obj.get("countkatu").getAsInt();
		countgeki = obj.get("countgeki").getAsInt();
		perfect = obj.get("perfect").getAsInt() == 1;
		enabledMods = GameMod.get(obj.get("enabled_mods").getAsLong());
		userID = obj.get("user_id").getAsInt();
		date = Utility.parseDate(obj.get("date").getAsString());
		rank = obj.get("rank").getAsString();
		pp = obj.has("pp") ? obj.get("pp").getAsFloat() : 0;
		replayAvailable = obj.has("replay_available") ? obj.get("replay_available").getAsInt() : 0;

		beatmap = getAPI().beatmaps.getAsQuery(new EndpointBeatmaps.ArgumentsBuilder()
				.setBeatmapID(beatmapID).build())
				.asLazilyLoaded().map(list -> list.stream().findFirst().orElse(null));

		user = getAPI().users.getAsQuery(new EndpointUsers.ArgumentsBuilder(userID).build())
				.asLazilyLoaded();
	}

	public OsuScore(OsuScore other) {
		super(other);
		this.beatmapID = other.beatmapID;
		this.scoreID = other.scoreID;
		this.beatmap = other.beatmap;
		this.score = other.score;
		this.maxcombo = other.maxcombo;
		this.count300 = other.count300;
		this.count100 = other.count100;
		this.count50 = other.count50;
		this.countmiss = other.countmiss;
		this.countkatu = other.countkatu;
		this.countgeki = other.countgeki;
		this.perfect = other.perfect;
		this.enabledMods = other.enabledMods;
		this.userID = other.userID;
		this.user = other.user;
		this.date = other.date;
		this.rank = other.rank;
		this.pp = other.pp;
		this.replayAvailable = other.replayAvailable;
	}

	public int getBeatmapID() {
		return beatmapID;
	}

	public LazilyLoaded<OsuBeatmap> getBeatmap() {
		return beatmap;
	}

	public int getScore() {
		return score;
	}

	public int getMaxCombo() {
		return maxcombo;
	}

	public int getHit300() {
		return count300;
	}

	public int getHit100() {
		return count100;
	}

	public int getHit50() {
		return count50;
	}

	public int getTotalHits() {
		return count300 + count100 + count50;
	}

	public int getMisses() {
		return countmiss;
	}

	public int getKatus() {
		return countkatu;
	}

	public int getGekis() {
		return countgeki;
	}

	public boolean isPerfect() {
		return perfect;
	}

	public GameMod[] getEnabledMods() {
		return enabledMods;
	}

	public int getUserID() {
		return userID;
	}

	public LazilyLoaded<OsuUser> getUser() {
		return user;
	}

	public ZonedDateTime getDate() {
		return date;
	}

	public String getRank() {
		return rank;
	}

	public float getPp() {
		return pp;
	}

	public long getScoreID() {
		return scoreID;
	}

	public boolean isReplayAvailable() {
		return replayAvailable == 1;
	}
}
