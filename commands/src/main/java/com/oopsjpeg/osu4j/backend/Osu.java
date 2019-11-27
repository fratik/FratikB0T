package com.oopsjpeg.osu4j.backend;

import com.google.gson.*;
import com.oopsjpeg.osu4j.exception.MalformedRequestException;
import com.oopsjpeg.osu4j.exception.OsuAPIException;
import com.oopsjpeg.osu4j.exception.OsuRateLimitException;
import com.oopsjpeg.osu4j.util.Utility;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

/**
 * Encapsules the raw API, managing requests per minute, etc..
 *
 * @author WorldSEnder
 */
public class Osu {
	public static final Gson GSON = new GsonBuilder().create();
	public static final String BASE_URL = "https://osu.ppy.sh";
	private static final String API_PARAMETER = "k";
	private static final int READ_TIMEOUT = 10000;
	private static final int DEFAULT_LIMIT = 120;
	private static Map<OsuToken, Osu> apiPerToken = Collections.synchronizedMap(new HashMap<>());
	private OsuToken token;
	private APIAccess access = new APIAccess();
	public final EndpointBeatmaps beatmaps = new EndpointBeatmaps(access);
	public final EndpointBeatmapSet beatmapSets = new EndpointBeatmapSet(beatmaps);
	public final EndpointMatches matches = new EndpointMatches(access);
	public final EndpointReplays replays = new EndpointReplays(access);
	public final EndpointScores scores = new EndpointScores(access);
	public final EndpointUserBests userBests = new EndpointUserBests(access);
	public final EndpointUserRecents userRecents = new EndpointUserRecents(access);
	public final EndpointUsers users = new EndpointUsers(access);
	private RateLimiter limiter = new RateLimiter(DEFAULT_LIMIT);

	private Osu(OsuToken token) {
		this.token = token;
	}

	public static Osu getAPI(String token) {
		return getAPI(new OsuToken(token));
	}

	public static Osu getAPI(OsuToken token) {
		return apiPerToken.computeIfAbsent(token, Osu::new);
	}

	private JsonElement makeApiRequest(String baseURL, Map<String, String> arguments) throws OsuAPIException {
		Objects.requireNonNull(baseURL);

		waitForFreeTicket();
		arguments = validateArguments(arguments);
		URL requestURL = buildURL(baseURL, arguments);
		URLConnection connection = openConnection(requestURL);
		if (!(connection instanceof HttpURLConnection)) {
			throw new MalformedRequestException("url has to support http protocol");
		}
		HttpURLConnection httpConnection = (HttpURLConnection) connection;
		try {
			httpConnection.setRequestMethod("GET");
		} catch (ProtocolException e) {
			throw new MalformedRequestException(e);
		}
		httpConnection.setReadTimeout(READ_TIMEOUT);
		try (InputStreamReader isr = new InputStreamReader(httpConnection.getInputStream())) {
			checkResponseStatus(httpConnection.getResponseCode());
			return new JsonParser().parse(isr);
		} catch (IOException ioe) {
			throw new OsuAPIException(ioe);
		}
	}

	private URLConnection openConnection(URL requestURL) throws OsuAPIException {
		try {
			return requestURL.openConnection();
		} catch (IOException e) {
			throw new MalformedRequestException(e);
		}
	}

	private URL buildURL(String endpointURL, Map<String, String> arguments) throws MalformedRequestException {
		StringBuilder fullURL = new StringBuilder(BASE_URL);
		fullURL.append(endpointURL);
		if (!arguments.isEmpty()) {
			fullURL.append("?");
			for (Iterator<Map.Entry<String, String>> argumentIt = arguments.entrySet().iterator(); argumentIt
					.hasNext(); ) {
				Map.Entry<String, String> argument = argumentIt.next();
				fullURL.append(urlEncodeKey(argument.getKey())).append("=")
						.append(urlEncodeArgument(argument.getValue()));
				if (argumentIt.hasNext()) {
					fullURL.append("&");
				}
			}
		}
		try {
			return new URL(fullURL.toString());
		} catch (MalformedURLException murle) {
			throw new MalformedRequestException(murle);
		}
	}

	private void waitForFreeTicket() throws OsuRateLimitException {
		limiter.getOrWaitForTicket();
	}

	private String urlEncodeArgument(String value) {
		return Utility.urlEncode(value);
	}

	private String urlEncodeKey(String key) {
		return Utility.urlEncode(key);
	}

	private void checkResponseStatus(int responseCode) throws OsuAPIException {
		if (responseCode >= 400 && responseCode < 500) {
			throw new OsuAPIException("Invalid request, is your API key correct?");
		}
		if (responseCode != HttpURLConnection.HTTP_OK) {
			throw new OsuAPIException("Unexpected response code: " + responseCode);
		}
	}

	private Map<String, String> validateArguments(Map<String, String> arguments) throws MalformedRequestException {
		Map<String, String> allArguments = arguments == null ? new HashMap<>(1) : new HashMap<>(arguments);
		if (allArguments.containsKey(API_PARAMETER)) {
			throw new MalformedRequestException("Can't override the api key parameter");
		}
		allArguments.put(API_PARAMETER, token.getTokenRaw());
		return allArguments;
	}

	public final class APIAccess {
		public Osu getAPI() {
			return Osu.this;
		}

		public JsonArray getRESTfulArray(String baseUrl, Map<String, String> arguments) throws OsuAPIException {
			return Osu.this.makeApiRequest(baseUrl, arguments).getAsJsonArray();
		}

		public JsonObject getRESTfulObject(String baseUrl, Map<String, String> arguments) throws OsuAPIException {
			JsonObject object = Osu.this.makeApiRequest(baseUrl, arguments).getAsJsonObject();
			if (object.has("error"))
				throw new OsuAPIException("Error returned by " + baseUrl + "[" + arguments + "]: " + object.get("error"));
			return object;
		}
	}

}
