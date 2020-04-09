package com.oopsjpeg.osu4j;

public enum Language {
	ANY(0, "Any"),
	OTHER(1, "Other"),
	ENGLISH(2, "English"),
	JAPANESE(3, "Japanese"),
	CHINESE(4, "Chinese"),
	INSTRUMENTAL(5, "Instrumental"),
	KOREAN(6, "Korean"),
	FRENCH(7, "French"),
	GERMAN(8, "German"),
	SWEDISH(9, "Swedish"),
	SPANISH(10, "Spanish"),
	ITALIAN(11, "Italian");

	private final int id;
	private final String name;

	Language(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static Language fromID(int id) {
		for (Language l : values())
			if (l.id == id) return l;
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
