package com.oopsjpeg.osu4j.backend;

import com.oopsjpeg.osu4j.OsuBeatmap;
import com.oopsjpeg.osu4j.OsuBeatmapSet;
import com.oopsjpeg.osu4j.abstractbackend.MappingEndpoint;
import com.oopsjpeg.osu4j.backend.EndpointBeatmapSet.Arguments;

import java.util.List;

public class EndpointBeatmapSet extends MappingEndpoint<Arguments, OsuBeatmapSet> {
	public EndpointBeatmapSet(EndpointBeatmaps beatmaps) {
		super(beatmaps, EndpointBeatmapSet::transformArguments, EndpointBeatmapSet::transformResult);
	}

	private static EndpointBeatmaps.Arguments transformArguments(Arguments arguments) {
		return arguments.actualArguments;
	}

	private static OsuBeatmapSet transformResult(List<OsuBeatmap> beatmaps) {
		return new OsuBeatmapSet(beatmaps);
	}

	public static class Arguments {
		private EndpointBeatmaps.Arguments actualArguments;

		public Arguments(int setID) {
			this.actualArguments = new EndpointBeatmaps.ArgumentsBuilder().setBeatmapID(setID).build();
		}
	}

}
