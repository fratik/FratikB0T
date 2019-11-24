package com.oopsjpeg.osu4j.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.abstractbackend.ArgumentBuilder;
import com.oopsjpeg.osu4j.abstractbackend.Endpoint;
import com.oopsjpeg.osu4j.backend.EndpointBeatmaps.Arguments;
import com.oopsjpeg.osu4j.backend.Osu.APIAccess;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import com.oopsjpeg.osu4j.util.Utility;

import java.time.ZonedDateTime;
import java.util.*;

public class EndpointBeatmaps implements Endpoint<Arguments, List<OsuBeatmap>> {
	private static final String API_ENDPOINT = "/api/get_beatmaps";
	private final APIAccess api;

	public EndpointBeatmaps(APIAccess api) {
		this.api = api;
	}

	@Override
	public List<OsuBeatmap> query(Arguments arguments) throws OsuAPIException {
		JsonArray beatmapsJson = api.getRESTfulArray(API_ENDPOINT, arguments.asURLArguments());
		List<OsuBeatmap> resultSet = new ArrayList<>();
		for (int i = 0; i < beatmapsJson.size(); ++i) {
			JsonObject beatmapJson = beatmapsJson.get(i).getAsJsonObject();
			resultSet.add(new OsuBeatmap(api.getAPI(), beatmapJson));
		}
		return resultSet;
	}

	public static class Arguments {
		private Map<String, String> arguments;

		public Arguments(ArgumentsBuilder builder) {
			Objects.requireNonNull(builder);
			Map<String, String> arguments = new HashMap<>();
			builder.sinceDate.ifPresent(date -> arguments.put("since", Utility.toMySqlString(date)));
			builder.beatmapsetID.ifPresent(id -> arguments.put("s", Integer.toString(id)));
			builder.beatmapID.ifPresent(id -> arguments.put("b", Integer.toString(id)));
			builder.user.ifPresent(user -> {
				arguments.put("u", user.getUserID());
				arguments.put("type", user.isTextualID() ? "string" : "id");
			});
			builder.mode.ifPresent(mode -> arguments.put("m", Integer.toString(mode.getID())));
			builder.includeConverted.ifPresent(inc -> arguments.put("a", inc ? "1" : "0"));
			builder.beatmapHash.ifPresent(hash -> arguments.put("h", hash));
			builder.limit.ifPresent(l -> arguments.put("limit", Integer.toString(l)));
			this.arguments = Collections.unmodifiableMap(arguments);
		}

		private Map<String, String> asURLArguments() {
			return arguments;
		}
	}

	public static class ArgumentsBuilder implements ArgumentBuilder<Arguments> {
		private Optional<ZonedDateTime> sinceDate = Optional.empty();
		private OptionalInt beatmapsetID = OptionalInt.empty();
		private OptionalInt beatmapID = OptionalInt.empty();
		private Optional<UserInfo> user = Optional.empty();
		private Optional<GameMode> mode = Optional.empty();
		private Optional<Boolean> includeConverted = Optional.empty();
		private Optional<String> beatmapHash = Optional.empty();
		private OptionalInt limit = OptionalInt.empty();

		public ArgumentsBuilder setSince(ZonedDateTime date) {
			this.sinceDate = Optional.of(date);
			return this;
		}

		public ArgumentsBuilder unsetSince() {
			this.sinceDate = Optional.empty();
			return this;
		}

		public ArgumentsBuilder setSetID(int setID) {
			this.beatmapsetID = OptionalInt.of(setID);
			return this;
		}

		public ArgumentsBuilder unsetSetID() {
			this.beatmapsetID = OptionalInt.empty();
			return this;
		}

		public ArgumentsBuilder setBeatmapID(int setID) {
			this.beatmapID = OptionalInt.of(setID);
			return this;
		}

		public ArgumentsBuilder unsetBeatmapID() {
			this.beatmapID = OptionalInt.empty();
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

		public ArgumentsBuilder setIncludeConverted(boolean include) {
			this.includeConverted = Optional.of(include);
			return this;
		}

		public ArgumentsBuilder unsetIncludeConverted() {
			this.includeConverted = Optional.empty();
			return this;
		}

		public ArgumentsBuilder setBeatmapHash(String hash) {
			this.beatmapHash = Optional.of(hash);
			return this;
		}

		public ArgumentsBuilder unsetBeatmapHash() {
			this.beatmapHash = Optional.empty();
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
		public Arguments build() throws IllegalStateException {
			return new Arguments(this);
		}
	}
}
