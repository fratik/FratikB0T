package com.oopsjpeg.osu4j;

public enum Genre {
	ANY(0, "Any"),
	UNSPECIFIED(1, "Unspecified"),
	VIDEO_GAME(2, "Video Game"),
	ANIME(3, "Anime"),
	ROCK(4, "Rock"),
	POP(5, "Pop"),
	OTHER(6, "Other"),
	NOVELTY(7, "Novelty"),
	HIP_HOP(9, "Hip Hop"),
	ELECTRONIC(10, "Electronic");

	private final int id;
	private final String name;

	Genre(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static Genre fromID(int id) {
		for (Genre g : values())
			if (g.id == id) return g;
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
