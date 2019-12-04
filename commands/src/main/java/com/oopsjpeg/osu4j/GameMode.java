package com.oopsjpeg.osu4j;

public enum GameMode {
	STANDARD(0, "osu!"),
	TAIKO(1, "osu!taiko"),
	CATCH_THE_BEAT(2, "osu!catch"),
	MANIA(3, "osu!mania");

	private final int id;
	private final String name;

	GameMode(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static GameMode fromID(int id) {
		for (GameMode m : values())
			if (m.id == id) return m;
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
