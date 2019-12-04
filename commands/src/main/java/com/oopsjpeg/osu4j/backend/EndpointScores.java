package com.oopsjpeg.osu4j.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.GameMod;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.abstractbackend.ArgumentBuilder;
import com.oopsjpeg.osu4j.abstractbackend.Endpoint;
import com.oopsjpeg.osu4j.backend.EndpointScores.Arguments;
import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.*;

public class EndpointScores implements Endpoint<Arguments, List<OsuScore>> {
	private static final String API_ENDPOINT = "/api/get_scores";
	private final Osu.APIAccess api;

	public EndpointScores(Osu.APIAccess api) {
		this.api = api;
	}

	@Override
	public List<OsuScore> query(Arguments arguments) throws OsuAPIException {
		JsonArray scoresJson = api.getRESTfulArray(API_ENDPOINT, arguments.asURLArguments());
		List<OsuScore> resultSet = new ArrayList<>();
		for (int i = 0; i < scoresJson.size(); ++i) {
			JsonObject scoreJson = scoresJson.get(i).getAsJsonObject();
			resultSet.add(new OsuScore(api.getAPI(), scoreJson));
		}
		return resultSet;
	}

	public static class Arguments {
		private Map<String, String> arguments;

		public Arguments(ArgumentsBuilder builder) {
			Objects.requireNonNull(builder);
			Map<String, String> arguments = new HashMap<>();
			arguments.put("b", Integer.toString(builder.beatmapID));
			builder.user.ifPresent(user -> {
				arguments.put("u", user.getUserID());
				arguments.put("type", user.isTextualID() ? "string" : "id");
			});
			builder.mode.ifPresent(mode -> arguments.put("m", Integer.toString(mode.getID())));
			builder.mods.ifPresent(mods -> arguments.put("mods", Long.toString(mods.stream().mapToLong(GameMod::getBit).sum())));
			builder.limit.ifPresent(l -> arguments.put("limit", Integer.toString(l)));
			this.arguments = Collections.unmodifiableMap(arguments);
		}

		private Map<String, String> asURLArguments() {
			return arguments;
		}
	}

	public static class ArgumentsBuilder implements ArgumentBuilder<Arguments> {
		private int beatmapID;
		private Optional<UserInfo> user = Optional.empty();
		private Optional<GameMode> mode = Optional.empty();
		private Optional<EnumSet<GameMod>> mods = Optional.empty();
		private OptionalInt limit = OptionalInt.empty();

		public ArgumentsBuilder(int beatmapID) {
			setBeatmapID(beatmapID);
		}

		public ArgumentsBuilder setBeatmapID(int beatmapID) {
			this.beatmapID = beatmapID;
			return this;
		}

		public ArgumentsBuilder setUserName(String userName) {
			this.user = Optional.of(UserInfo.create(userName));
			return this;
		}

		public ArgumentsBuilder setUserID(int userID) {
			this.user = Optional.of(UserInfo.create(userID));
			return this;
		}

		public ArgumentsBuilder unsetUser() {
			this.user = Optional.empty();
			return this;
		}

		public ArgumentsBuilder setMode(GameMode mode) {
			this.mode = Optional.of(mode);
			return this;
		}

		public ArgumentsBuilder unsetMode() {
			this.mode = Optional.empty();
			return this;
		}

		public ArgumentsBuilder setMods(EnumSet<GameMod> mods) {
			this.mods = Optional.of(mods);
			return this;
		}

		public ArgumentsBuilder unsetMods() {
			this.mods = Optional.empty();
			return this;
		}

		public ArgumentsBuilder setLimit(int limit) {
			this.limit = OptionalInt.of(limit);
			return this;
		}

		public ArgumentsBuilder unsetLimit() {
			this.limit = OptionalInt.empty();
			return this;
		}

		@Override
		public Arguments build() {
			return new Arguments(this);
		}
	}
}
