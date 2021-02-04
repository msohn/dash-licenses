/*************************************************************************
 * Copyright (c) 2021 The Eclipse Foundation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

import org.eclipse.dash.licenses.context.IContext;

public class HttpClientService implements IHttpClientService {

	private IContext context;

	public HttpClientService(IContext context) {
		this.context = context;
	}

	@Override
	public int request(URI uri, String contentType, String payload, Consumer<String> handler) {
		try {
			Duration timeout = Duration.ofSeconds(context.getSettings().getTimeout());
			HttpRequest request = HttpRequest.newBuilder(uri).header("Content-Type", contentType)
					.POST(BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).timeout(timeout).build();

			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
			HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				handler.accept(response.body());
			}
			return response.statusCode();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}