package com.oopsjpeg.osu4j.abstractbackend;

import java.net.URL;

public interface URLEndpoint<A, R> extends Endpoint<A, R> {

	URL resolveToURL(A arguments);

}
