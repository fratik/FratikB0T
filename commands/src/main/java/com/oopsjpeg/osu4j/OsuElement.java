package com.oopsjpeg.osu4j;

import com.oopsjpeg.osu4j.backend.Osu;

public abstract class OsuElement {
	private final Osu api;

	public OsuElement(Osu api) {
		this.api = api;
	}

	public OsuElement(OsuElement other) {
		this.api = other.api;
	}

	public Osu getAPI() {
		return api;
	}
}
