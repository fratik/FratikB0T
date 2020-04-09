package com.oopsjpeg.osu4j;

public enum ApprovalState {
	LOVED(4, "Loved"),
	QUALIFIED(3, "Qualified"),
	APPROVED(2, "Approved"),
	RANKED(1, "Ranked"),
	PENDING(0, "Pending"),
	WIP(-1, "Work in Progress"),
	GRAVEYARD(-2, "Graveyard");

	private final int id;
	private final String name;

	ApprovalState(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static ApprovalState fromID(int id) {
		for (ApprovalState a : values())
			if (a.id == id) return a;
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
