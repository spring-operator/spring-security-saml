/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.security.saml.serviceprovider.web.html;

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.saml.SamlException;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

public class StandaloneHtmlWriter {

	private HeaderWriter cacheHeaderWriter = new CacheControlHeadersWriter();

	public StandaloneHtmlWriter() {
	}

	public void processHtmlBody(HttpServletRequest request,
								HttpServletResponse response,
								AbstractHtmlContent content) {
		getCacheHeaderWriter().writeHeaders(request, response);
		response.setContentType(TEXT_HTML_VALUE);
		response.setCharacterEncoding(UTF_8.name());
		StringWriter out = new StringWriter();
		try {
			response.getWriter().write(content.getHtml());
		} catch (IOException e) {
			throw new SamlException(e);
		}
	}

	public HeaderWriter getCacheHeaderWriter() {
		return cacheHeaderWriter;
	}

	public StandaloneHtmlWriter setCacheHeaderWriter(HeaderWriter cacheHeaderWriter) {
		this.cacheHeaderWriter = cacheHeaderWriter;
		return this;
	}
}
