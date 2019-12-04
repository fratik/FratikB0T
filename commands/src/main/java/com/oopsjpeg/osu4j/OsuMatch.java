package com.oopsjpeg.osu4j;

import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.abstractbackend.LazilyLoaded;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.util.Utility;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class OsuMatch extends OsuElement {
	private final int matchID;
	private final String name;
	private final ZonedDateTime startTime;
	// private final ZonedDateTime endTime; Not yet implemented!
	private final List<Game> games;

	public OsuMatch(Osu api, JsonObject obj) {
		super(api);
		JsonObject match = obj.getAsJsonObject("match");
		matchID = match.get("match_id").getAsInt();
		name = match.get("name").getAsString();
		startTime = Utility.parseDate(match.get("start_time").getAsString());
		// endDate = match.get("end_time").getAsString();

		this.games = new ArrayList<>();
		obj.getAsJsonArray("games").forEach(e -> this.games.add(new Game(e.getAsJsonObject())));
	}

	public OsuMatch(OsuMatch other) {
		super(other);
		this.matchID = other.matchID;
		this.name = other.name;
		this.startTime = other.startTime;
		// this.endTime = other.endTime;
		this.games = other.games;
	}

	public int getID() {
		return matchID;
	}

	public String getName() {
		return name;
	}

	public ZonedDateTime getStartTime() {
		return startTime;
	}

	//public ZonedDateTime getEndDate() {
	//	return endDate;
	//}

	public List<Game> getGames() {
		return games;
	}

	public enum ScoringType {
		SCORE(0, "Score"),
		ACCURACY(1, "Accuracy"),
		COMBO(2, "Combo"),
		SCORE_V2(3, "Score v2");

		private final int id;
		private final String name;

		ScoringType(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public static ScoringType fromID(int id) {
			for (ScoringType s : values())
				if (s.id == id) return s;
			return null;
		}

		public int getID() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public enum TeamType {
		HEAD_TO_HEAD(0, "Head-to-Head"),
		TAG_COOP(1, "Tag Co-op"),
		TEAM_VS(2, "Team Versus"),
		TAG_TEAM_VS(3, "Tag Team Versus");

		private final int id;
		private final String name;

		TeamType(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public static TeamType fromID(int id) {
			for (TeamType t : values())
				if (t.id == id) return t;
			return null;
		}

		public int getID() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public class Game extends OsuElement {
		private final int gameID;
		private final int beatmapID;
		private final LazilyLoaded<OsuBeatmap> beatmap;
		private final GameMode playMode;
		private final int matchType;
		private final ScoringType scoringType;
		private final TeamType teamType;
		private final GameMod[] mods;
		private final List<Score> scores;
		private ZonedDateTime startTime;
		private ZonedDateTime endTime;

		public Game(JsonObject obj) {
			super(OsuMatch.this.getAPI());
			gameID = obj.get("game_id").getAsInt();
			startTime = Utility.parseDate(obj.get("start_time").getAsString());
			endTime = Utility.parseDate(obj.get("end_time").getAsString());
			beatmapID = obj.get("beatmap_id").getAsInt();
			playMode = GameMode.fromID(obj.get("play_mode").getAsInt());
			matchType = obj.get("match_type").getAsInt();
			scoringType = ScoringType.fromID(obj.get("scoring_type").getAsInt());
			teamType = TeamType.fromID(obj.get("team_type").getAsInt());
			mods = GameMod.get(obj.get("mods").getAsLong());
			scores = new ArrayList<>();
			obj.getAsJsonArray("scores").forEach(e -> this.scores.add(new Score(e.getAsJsonObject())));

			beatmap = getAPI().beatmaps.getAsQuery(new EndpointBeatmaps.ArgumentsBuilder()
					.setBeatmapID(beatmapID).build())
					.asLazilyLoaded().map(list -> list.get(0));
		}

		public Game(Game other) {
			super(other);
			this.gameID = other.gameID;
			this.startTime = other.startTime;
			this.endTime = other.endTime;
			this.beatmapID = other.beatmapID;
			this.beatmap = other.beatmap;
			this.playMode = other.playMode;
			this.matchType = other.matchType;
			this.scoringType = other.scoringType;
			this.teamType = other.teamType;
			this.mods = other.mods;
			this.scores = other.scores;
		}

		public int getGameID() {
			return gameID;
		}

		public ZonedDateTime getStartTime() {
			return startTime;
		}

		public ZonedDateTime getEndTime() {
			return endTime;
		}

		public int getBeatmapID() {
			return beatmapID;
		}

		public LazilyLoaded<OsuBeatmap> getBeatmap() {
			return beatmap;
		}

		public GameMode getPlayMode() {
			return playMode;
		}

		public int getMatchType() {
			return matchType;
		}

		public ScoringType getScoringType() {
			return scoringType;
		}

		public TeamType getTeamType() {
			return teamType;
		}

		public GameMod[] getMods() {
			return mods;
		}

		public List<Score> getScores() {
			return scores;
		}

		public class Score extends OsuElement {
			private final int slot;
			private final int team;
			private final int userID;
			private final LazilyLoaded<OsuUser> user;
			private final int score;
			// private final int rank; Not used!
			private final int count50;
			private final int count100;
			private final int count300;
			private final int countmiss;
			private final int countgeki;
			private final int countkatu;
			private final boolean perfect;
			private final boolean pass;

			public Score(JsonObject obj) {
				super(Game.this.getAPI());
				slot = obj.get("slot").getAsInt();
				team = obj.get("team").getAsInt();
				userID = obj.get("user_id").getAsInt();
				score = obj.get("score").getAsInt();
				count50 = obj.get("count50").getAsInt();
				count100 = obj.get("count100").getAsInt();
				count300 = obj.get("count300").getAsInt();
				countmiss = obj.get("countmiss").getAsInt();
				countgeki = obj.get("countgeki").getAsInt();
				countkatu = obj.get("countkatu").getAsInt();
				perfect = obj.get("perfect").getAsInt() == 1;
				pass = obj.get("pass").getAsInt() == 1;

				user = getAPI().users.getAsQuery(new EndpointUsers.ArgumentsBuilder(userID)
						.setMode(playMode).build())
						.asLazilyLoaded();
			}

			public Score(Score other) {
				super(other);
				this.slot = other.slot;
				this.team = other.team;
				this.userID = other.userID;
				this.user = other.user;
				this.score = other.score;
				this.count50 = other.count50;
				this.count100 = other.count100;
				this.count300 = other.count300;
				this.countmiss = other.countmiss;
				this.countgeki = other.countgeki;
				this.countkatu = other.countkatu;
				this.perfect = other.perfect;
				this.pass = other.pass;
			}
		}
	}

}
