package com.oopsjpeg.osu4j;

import com.google.gson.JsonObject;
import com.oopsjpeg.osu4j.abstractbackend.LazilyLoaded;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet;
import com.oopsjpeg.osu4j.backend.EndpointUsers;
import com.oopsjpeg.osu4j.backend.Osu;
import com.oopsjpeg.osu4j.util.Utility;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;

public class OsuBeatmap extends OsuElement {
	private final ApprovalState approved;
	private final ZonedDateTime submitDate;
	private final ZonedDateTime approvedDate;
	private final ZonedDateTime lastUpdate;
	private final String artist;
	private final int beatmapID;
	private final int beatmapSetID;
	private final LazilyLoaded<OsuBeatmapSet> beatmapSet;
	private final float bpm;
	private final String creatorName;
	private final LazilyLoaded<OsuUser> creator;
	private final float difficultyrating;
	private final float diffSize;
	private final float diffOverall;
	private final float diffApproach;
	private final float diffDrain;
	private final int hitLength;
	private final String source;
	private final Genre genre;
	private final Language language;
	private final String title;
	private final int totalLength;
	private final String version;
	private final String fileMD5;
	private final GameMode mode;
	private final String[] tags;
	private final int favouriteCount;
	private final float rating;
	private final int playcount;
	private final int passcount;
	private final int maxCombo;

	public OsuBeatmap(Osu api, JsonObject obj) {
		super(api);
		approved = ApprovalState.fromID(obj.get("approved").getAsInt());
		submitDate = Utility.parseDate(obj.get("submit_date").getAsString());
		approvedDate = obj.get("approved_date").isJsonNull() ? null : Utility.parseDate(obj.get("approved_date").getAsString());
		lastUpdate = Utility.parseDate(obj.get("last_update").getAsString());
		artist = obj.get("artist").getAsString();
		beatmapID = obj.get("beatmap_id").getAsInt();
		beatmapSetID = obj.get("beatmapset_id").getAsInt();
		bpm = obj.get("bpm").getAsFloat();
		creatorName = obj.get("creator").getAsString();
		difficultyrating = obj.get("difficultyrating").getAsFloat();
		diffSize = obj.get("diff_size").getAsFloat();
		diffOverall = obj.get("diff_overall").getAsFloat();
		diffApproach = obj.get("diff_approach").getAsFloat();
		diffDrain = obj.get("diff_drain").getAsFloat();
		hitLength = obj.get("hit_length").getAsInt();
		source = obj.get("source").getAsString();
		genre = Genre.fromID(obj.get("genre_id").getAsInt());
		language = Language.fromID(obj.get("language_id").getAsInt());
		title = obj.get("title").getAsString();
		totalLength = obj.get("total_length").getAsInt();
		version = obj.get("version").getAsString();
		fileMD5 = obj.get("file_md5").getAsString();
		mode = GameMode.fromID(obj.get("mode").getAsInt());
		tags = obj.get("tags").getAsString().split(" ");
		favouriteCount = obj.get("favourite_count").getAsInt();
		rating = obj.get("rating").getAsFloat();
		playcount = obj.get("playcount").getAsInt();
		passcount = obj.get("passcount").getAsInt();
		maxCombo = obj.get("max_combo").getAsInt();

		beatmapSet = getAPI().beatmapSets.getAsQuery(new EndpointBeatmapSet.Arguments(beatmapSetID))
				.asLazilyLoaded();
		creator = getAPI().users.getAsQuery(new EndpointUsers.ArgumentsBuilder(creatorName).build())
				.asLazilyLoaded();
	}

	public OsuBeatmap(OsuBeatmap other) {
		super(other);
		this.approved = other.approved;
		this.submitDate = other.submitDate;
		this.approvedDate = other.approvedDate;
		this.lastUpdate = other.lastUpdate;
		this.artist = other.artist;
		this.beatmapID = other.beatmapID;
		this.beatmapSetID = other.beatmapSetID;
		this.beatmapSet = other.beatmapSet;
		this.bpm = other.bpm;
		this.creatorName = other.creatorName;
		this.creator = other.creator;
		this.difficultyrating = other.difficultyrating;
		this.diffSize = other.diffSize;
		this.diffOverall = other.diffOverall;
		this.diffApproach = other.diffApproach;
		this.diffDrain = other.diffDrain;
		this.hitLength = other.hitLength;
		this.source = other.source;
		this.genre = other.genre;
		this.language = other.language;
		this.title = other.title;
		this.totalLength = other.totalLength;
		this.version = other.version;
		this.fileMD5 = other.fileMD5;
		this.mode = other.mode;
		this.tags = other.tags;
		this.favouriteCount = other.favouriteCount;
		this.rating = other.rating;
		this.playcount = other.playcount;
		this.passcount = other.passcount;
		this.maxCombo = other.maxCombo;
	}

	public ApprovalState getApproved() {
		return approved;
	}

	public ZonedDateTime getSubmitDate() {
		return submitDate;
	}

	public ZonedDateTime getApprovedDate() {
		return approvedDate;
	}

	public ZonedDateTime getLastUpdate() {
		return lastUpdate;
	}

	public String getArtist() {
		return artist;
	}

	public int getID() {
		return beatmapID;
	}

	public int getBeatmapSetID() {
		return beatmapSetID;
	}

	public LazilyLoaded<OsuBeatmapSet> getBeatmapSet() {
		return beatmapSet;
	}

	public float getBPM() {
		return bpm;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public LazilyLoaded<OsuUser> getCreator() {
		return creator;
	}

	public float getDifficulty() {
		return difficultyrating;
	}

	public float getSize() {
		return diffSize;
	}

	public float getOverall() {
		return diffOverall;
	}

	public float getApproach() {
		return diffApproach;
	}

	public float getDrain() {
		return diffDrain;
	}

	public int getHitLength() {
		return hitLength;
	}

	public String getSource() {
		return source;
	}

	public Genre getGenre() {
		return genre;
	}

	public Language getLanguage() {
		return language;
	}

	public String getTitle() {
		return title;
	}

	public int getTotalLength() {
		return totalLength;
	}

	public String getVersion() {
		return version;
	}

	public String getFileMD5() {
		return fileMD5;
	}

	public GameMode getMode() {
		return mode;
	}

	public String[] getTags() {
		return tags;
	}

	public int getFavouriteCount() {
		return favouriteCount;
	}

	public int getFavoriteCount() {
		return favouriteCount;
	}

	public float getRating() {
		return rating;
	}

	public int getPlayCount() {
		return playcount;
	}

	public int getPassCount() {
		return passcount;
	}

	public int getMaxCombo() {
		return maxCombo;
	}

	public URL getURL() throws MalformedURLException {
		return new URL(Osu.BASE_URL + "/b/" + beatmapID);
	}

	@Override
	public String toString() {
		return artist + " - " + title + " [" + version + "]";
	}
}
