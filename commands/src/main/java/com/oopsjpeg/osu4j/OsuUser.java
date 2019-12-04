package com.oopsjpeg.osu4j;

import com.google.gson.JsonObject;
import com.neovisionaries.i18n.CountryCode;
import com.oopsjpeg.osu4j.abstractbackend.LazilyLoaded;
import com.oopsjpeg.osu4j.backend.*;
import com.oopsjpeg.osu4j.util.Utility;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OsuUser extends OsuElement {
	private final int userID;
	private final String username;
	private final int count300;
	private final int count100;
	private final int count50;
	private final int playcount;
	private final long rankedScore;
	private final long totalScore;
	private final int ppRank;
	private final float level;
	private final float ppRaw;
	private final float accuracy;
	private final int countRankSS;
	private final int countRankSSH;
	private final int countRankS;
	private final int countRankSH;
	private final int countRankA;
	private final CountryCode country;
	private final int ppCountryRank;
	private final List<Event> events;
	private long totalSecondsPlayed;

	private final GameMode mode;

	private int topScoresSize = -1;
	private LazilyLoaded<List<OsuScore>> topScores;
	private int recentScoresSize = -1;
	private LazilyLoaded<List<OsuScore>> recentScores;

	public OsuUser(Osu api, JsonObject obj, GameMode mode) {
		super(api);
		userID = obj.get("user_id").getAsInt();
		username = obj.get("username").getAsString();
		count300 = obj.get("count300").getAsInt();
		count100 = obj.get("count100").getAsInt();
		count50 = obj.get("count50").getAsInt();
		playcount = obj.get("playcount").getAsInt();
		rankedScore = obj.get("ranked_score").getAsLong();
		totalScore = obj.get("total_score").getAsLong();
		ppRank = obj.get("pp_rank").getAsInt();
		level = obj.get("level").getAsFloat();
		ppRaw = obj.get("pp_raw").getAsFloat();
		accuracy = obj.get("accuracy").getAsFloat();
		countRankSS = obj.get("count_rank_ss").getAsInt();
		countRankSSH = obj.get("count_rank_ssh").getAsInt();
		countRankS = obj.get("count_rank_s").getAsInt();
		countRankSH = obj.get("count_rank_sh").getAsInt();
		countRankA = obj.get("count_rank_a").getAsInt();
		country = CountryCode.getByCodeIgnoreCase(obj.get("country").getAsString());
		ppCountryRank = obj.get("pp_country_rank").getAsInt();
		totalSecondsPlayed = obj.get("total_seconds_played").getAsInt();
		events = new ArrayList<>();
		obj.getAsJsonArray("events").forEach(e -> events.add(new Event((JsonObject) e)));

		this.mode = mode;
	}

	public OsuUser(OsuUser other) {
		super(other);
		this.userID = other.userID;
		this.username = other.username;
		this.count300 = other.count300;
		this.count100 = other.count100;
		this.count50 = other.count50;
		this.playcount = other.playcount;
		this.rankedScore = other.rankedScore;
		this.totalScore = other.totalScore;
		this.ppRank = other.ppRank;
		this.level = other.level;
		this.ppRaw = other.ppRaw;
		this.accuracy = other.accuracy;
		this.countRankSSH = other.countRankSSH;
		this.countRankSS = other.countRankSS;
		this.countRankSH = other.countRankSH;
		this.countRankS = other.countRankS;
		this.countRankA = other.countRankA;
		this.country = other.country;
		this.ppCountryRank = other.ppCountryRank;
		this.events = other.events;
		this.totalSecondsPlayed = other.totalSecondsPlayed;

		this.topScoresSize = other.topScoresSize;
		this.topScores = other.topScores;
		this.recentScoresSize = other.recentScoresSize;
		this.recentScores = other.recentScores;

		this.mode = other.mode;
	}

	public LazilyLoaded<List<OsuScore>> getTopScores(int limit) {
		if (limit <= topScoresSize) {
			return topScores.map(list -> list.subList(0, limit));
		}
		topScoresSize = limit;
		return topScores = getAPI().userBests
				.getAsQuery(new EndpointUserBests.ArgumentsBuilder(userID).setMode(mode).setLimit(limit).build()).asLazilyLoaded();
	}

	public LazilyLoaded<List<OsuScore>> withRecentScores(int limit) {
		if (limit <= recentScoresSize) {
			return recentScores.map(list -> list.subList(0, limit));
		}
		recentScoresSize = limit;
		return recentScores = getAPI().userRecents
				.getAsQuery(new EndpointUserRecents.ArgumentsBuilder(userID).setMode(mode).setLimit(limit).build()).asLazilyLoaded();
	}

	public int getID() {
		return userID;
	}

	public String getUsername() {
		return username;
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

	public int getPlayCount() {
		return playcount;
	}

	public long getRankedScore() {
		return rankedScore;
	}

	public long getTotalScore() {
		return totalScore;
	}

	public int getRank() {
		return ppRank;
	}

	public float getLevel() {
		return level;
	}

	public int getPP() {
		return Math.round(ppRaw);
	}

	public float getPPRaw() {
		return ppRaw;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public int getCountRankSSH() {
		return countRankSSH;
	}

	public int getCountRankSS() {
		return countRankSS;
	}

	public int getCountRankSH() {
		return countRankSH;
	}

	public int getCountRankS() {
		return countRankS;
	}

	public int getCountRankA() {
		return countRankA;
	}

	public CountryCode getCountry() {
		return country;
	}

	public int getCountryRank() {
		return ppCountryRank;
	}

	public List<Event> getEvents() {
		return events;
	}

	public GameMode getMode() {
		return mode;
	}

	public URL getURL() throws MalformedURLException {
		return new URL(Osu.BASE_URL + "/u/" + userID);
	}

	@Override
	public String toString() {
		return username;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof OsuUser && ((OsuUser) obj).getID() == userID;
	}

	public long getTotalSecondsPlayed() {
		return totalSecondsPlayed;
	}

	public class Event extends OsuElement {
		private final String displayHTML;
		private final int beatmapID;
		private final LazilyLoaded<OsuBeatmap> beatmap;
		private final int beatmapSetID;
		private final LazilyLoaded<OsuBeatmapSet> beatmapSet;
		private final ZonedDateTime date;
		private final int epicFactor;

		public Event(JsonObject obj) {
			super(OsuUser.this.getAPI());
			displayHTML = obj.get("display_html").getAsString();
			beatmapID = obj.get("beatmap_id").isJsonNull() ? 0 : obj.get("beatmap_id").getAsInt();
			beatmap = getAPI().beatmaps.getAsQuery(new EndpointBeatmaps.ArgumentsBuilder()
					.setBeatmapID(beatmapID).build())
					.asLazilyLoaded().map(list -> list.stream().findFirst().orElse(null));
			beatmapSetID = obj.get("beatmapset_id").isJsonNull() ? 0 : obj.get("beatmapset_id").getAsInt();
			beatmapSet = getAPI().beatmapSets.getAsQuery(new EndpointBeatmapSet.Arguments(beatmapSetID))
					.asLazilyLoaded();
			date = obj.get("date").isJsonNull() ? null : Utility.parseDate(obj.get("date").getAsString());
            epicFactor = obj.get("epicfactor").getAsInt();
		}

		public Event(Event other) {
			super(other);
			this.displayHTML = other.displayHTML;
			this.beatmapID = other.beatmapID;
			this.beatmap = other.beatmap;
			this.beatmapSetID = other.beatmapSetID;
			this.beatmapSet = other.beatmapSet;
			this.date = other.date;
			this.epicFactor = other.epicFactor;
		}

		public OsuUser getUser() {
			return OsuUser.this;
		}

		public String getDisplayHTML() {
			return displayHTML;
		}

		public int getBeatmapID() {
			return beatmapID;
		}

		public LazilyLoaded<OsuBeatmap> getBeatmap() {
			return beatmap;
		}

		public int getBeatmapSetID() {
			return beatmapSetID;
		}

		public LazilyLoaded<OsuBeatmapSet> getBeatmapSet() {
			return beatmapSet;
		}

		public ZonedDateTime getDate() {
			return date;
		}

		public int getEpicFactor() {
			return epicFactor;
		}
	}
}
