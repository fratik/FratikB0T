package com.oopsjpeg.osu4j.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuScore;
import com.oopsjpeg.osu4j.abstractbackend.ArgumentBuilder;
import com.oopsjpeg.osu4j.abstractbackend.Endpoint;
import com.oopsjpeg.osu4j.backend.EndpointUserRecents.Arguments;
import com.oopsjpeg.osu4j.backend.Osu.APIAccess;
import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.*;

public class EndpointUserRecents implements Endpoint<Arguments, List<OsuScore>> {
	private static final String API_ENDPOINT = "/api/get_user_recent";
	private final APIAccess api;

	public EndpointUserRecents(APIAccess api) {
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
			arguments.put("u", builder.user.getUserID());
			arguments.put("type", builder.user.isTextualID() ? "string" : "id");
			builder.mode.ifPresent(mode -> arguments.put("m", Integer.toString(mode.getID())));
			builder.limit.ifPresent(l -> arguments.put("limit", Integer.toString(l)));
			this.arguments = Collections.unmodifiableMap(arguments);
		}

		private Map<String, String> asURLArguments() {
			return arguments;
		}
	}

	public static class ArgumentsBuilder implements ArgumentBuilder<Arguments> {
		private UserInfo user;
		private Optional<GameMode> mode = Optional.empty();
		private OptionalInt limit = OptionalInt.empty();

		public ArgumentsBuilder(String userName) {
			setUserName(userName);
		}

		public ArgumentsBuilder(int userID) {
			setUserID(userID);
		}

		public ArgumentsBuilder setUserName(String userName) {
			this.user = UserInfo.create(userName);
			return this;
		}

		public ArgumentsBuilder setUserID(int userID) {
			this.user = UserInfo.create(userID);
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
