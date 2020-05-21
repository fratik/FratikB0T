package com.oopsjpeg.osu4j.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuUser;
import com.oopsjpeg.osu4j.abstractbackend.ArgumentBuilder;
import com.oopsjpeg.osu4j.abstractbackend.Endpoint;
import com.oopsjpeg.osu4j.backend.EndpointUsers.Arguments;
import com.oopsjpeg.osu4j.backend.Osu.APIAccess;
import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.*;

public class EndpointUsers implements Endpoint<Arguments, OsuUser> {
	private static final String API_ENDPOINT = "/api/get_user";
	private final APIAccess api;

	public EndpointUsers(APIAccess api) {
		this.api = api;
	}

	@Override
	public OsuUser query(Arguments arguments) throws OsuAPIException {
		JsonArray beatmapUser = api.getRESTfulArray(API_ENDPOINT, arguments.asURLArguments());
		if (beatmapUser.size() == 0) return null;
		JsonObject userObject = beatmapUser.get(0).getAsJsonObject();
		return new OsuUser(api.getAPI(), userObject, arguments.getMode());
	}

	public static class Arguments {
		private Map<String, String> arguments;
		private GameMode mode;

		public Arguments(ArgumentsBuilder builder) {
			Objects.requireNonNull(builder);
			Map<String, String> arguments = new HashMap<>();
			arguments.put("u", builder.user.getUserID());
			arguments.put("type", builder.user.isTextualID() ? "string" : "id");
			builder.mode.ifPresent(mode -> arguments.put("m", Integer.toString(mode.getID())));
			builder.eventDays.ifPresent(l -> arguments.put("event_days", Integer.toString(l)));
			this.arguments = Collections.unmodifiableMap(arguments);
			this.mode = builder.mode.orElse(GameMode.STANDARD);
		}

		private GameMode getMode() {
			return mode;
		}

		private Map<String, String> asURLArguments() {
			return arguments;
		}
	}

	public static class ArgumentsBuilder implements ArgumentBuilder<Arguments> {
		private UserInfo user;
		private Optional<GameMode> mode = Optional.empty();
		private OptionalInt eventDays = OptionalInt.empty();

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

		public ArgumentsBuilder setEventDays(int limit) {
			this.eventDays = OptionalInt.of(limit);
			return this;
		}

		public ArgumentsBuilder unsetEventDays() {
			this.eventDays = OptionalInt.empty();
			return this;
		}

		@Override
		public Arguments build() {
			return new Arguments(this);
		}
	}
}
