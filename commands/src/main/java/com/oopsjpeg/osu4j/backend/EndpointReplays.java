package com.oopsjpeg.osu4j.backend;

import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.GameMode;
import com.oopsjpeg.osu4j.OsuReplay;
import com.oopsjpeg.osu4j.abstractbackend.ArgumentBuilder;
import com.oopsjpeg.osu4j.abstractbackend.Endpoint;
import com.oopsjpeg.osu4j.backend.EndpointReplays.Arguments;
import com.oopsjpeg.osu4j.backend.Osu.APIAccess;
import com.oopsjpeg.osu4j.exception.OsuAPIException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EndpointReplays implements Endpoint<Arguments, OsuReplay> {
	private static final String API_ENDPOINT = "/api/get_replay";
	private final APIAccess api;

	public EndpointReplays(APIAccess api) {
		this.api = api;
	}

	@Override
	public OsuReplay query(Arguments arguments) throws OsuAPIException {
		JsonObject replayJson = api.getRESTfulObject(API_ENDPOINT, arguments.asURLArguments());
		return new OsuReplay(api.getAPI(), replayJson);
	}

	public static class Arguments {
		private Map<String, String> arguments;

		public Arguments(ArgumentsBuilder builder) {
			Objects.requireNonNull(builder);
			Map<String, String> arguments = new HashMap<>();
			arguments.put("m", Integer.toString(builder.mode.getID()));
			arguments.put("b", Integer.toString(builder.beatmapID));
			arguments.put("u", Integer.toString(builder.userID));
			this.arguments = Collections.unmodifiableMap(arguments);
		}

		private Map<String, String> asURLArguments() {
			return arguments;
		}
	}

	public static class ArgumentsBuilder implements ArgumentBuilder<Arguments> {
		private GameMode mode;
		private int beatmapID;
		private int userID;

		public ArgumentsBuilder(GameMode mode, int beatmapID, int userID) {
			setMode(mode).setBeatmapID(beatmapID).setUserID(userID);
		}

		public ArgumentsBuilder setMode(GameMode mode) {
			this.mode = Objects.requireNonNull(mode);
			return this;
		}

		public ArgumentsBuilder setBeatmapID(int mapID) {
			this.beatmapID = mapID;
			return this;
		}

		public ArgumentsBuilder setUserID(int userID) {
			this.userID = userID;
			return this;
		}

		@Override
		public Arguments build() {
			return new Arguments(this);
		}
	}
}
