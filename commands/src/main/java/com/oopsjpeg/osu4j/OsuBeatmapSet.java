package com.oopsjpeg.osu4j;

import com.oopsjpeg.osu4j.backend.Osu;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class OsuBeatmapSet extends OsuBeatmap {
	private final List<OsuBeatmap> beatmaps;

	public OsuBeatmapSet(List<OsuBeatmap> beatmaps) {
		super(beatmaps.get(0));
		this.beatmaps = beatmaps;
	}

	public OsuBeatmapSet(OsuBeatmapSet other) {
		super(other);
		this.beatmaps = other.beatmaps;
	}

	public List<OsuBeatmap> getBeatmaps() {
		return beatmaps;
	}

	@Override
	public URL getURL() throws MalformedURLException {
		return new URL(Osu.BASE_URL + "/s/" + getBeatmapSetID());
	}

	@Override
	public String toString() {
		return getArtist() + " - " + getTitle();
	}
}
