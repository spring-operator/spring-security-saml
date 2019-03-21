/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.security.saml.serviceprovider.web.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.saml.SamlException;
import org.springframework.security.saml.SamlProviderNotFoundException;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.configuration.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.provider.HostedServiceProvider;
import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml.saml2.authentication.Issuer;
import org.springframework.security.saml.saml2.authentication.NameIdPolicy;
import org.springframework.security.saml.saml2.metadata.Binding;
import org.springframework.security.saml.saml2.metadata.BindingType;
import org.springframework.security.saml.saml2.metadata.Endpoint;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml.serviceprovider.web.html.HtmlWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import org.joda.time.DateTime;

import static org.springframework.util.StringUtils.hasText;

public class AuthenticationRequestFilter extends OncePerRequestFilter implements SamlFilter<HostedServiceProvider> {

	private final SamlTransformer transformer;
	private final AntPathRequestMatcher matcher;
	private final HtmlWriter template;
	private Clock clock = Clock.systemUTC();
	private String postTemplate = "/templates/saml2-post-binding.vm";

	public AuthenticationRequestFilter(AntPathRequestMatcher matcher,
									   SamlTransformer transformer,
									   HtmlWriter template) {
		this.template = template;
		this.matcher = matcher;
		this.transformer = transformer;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		if (matcher.matches(request)) {
			try {
				HostedServiceProvider provider = getProvider(request);
				Assert.notNull(provider, "Each request must resolve into a hosted SAML provider");
				Map.Entry<ExternalIdentityProviderConfiguration, IdentityProviderMetadata> entity =
					getIdentityProvider(request, provider);
				ExternalIdentityProviderConfiguration idpConfig = entity.getKey();
				IdentityProviderMetadata idp = entity.getValue();
				ServiceProviderMetadata localSp = provider.getMetadata();
				AuthenticationRequest authn = getAuthenticationRequest(
					localSp,
					idp,
					entity.getKey().getAssertionConsumerServiceIndex(),
					idpConfig.getNameId()
				);
				sendAuthenticationRequest(authn, authn.getDestination(), request, response);
			} catch (SamlException x) {
				displayError(request, response, x.getMessage());
			}
		}
		else {
			filterChain.doFilter(request, response);
		}
	}

	/*
	 * UAA would want to override this as the entities are referred to by alias rather
	 * than ID
	 */
	protected Map.Entry<ExternalIdentityProviderConfiguration, IdentityProviderMetadata> getIdentityProvider(
		HttpServletRequest request,
		HostedServiceProvider sp
	) {
		Map.Entry<ExternalIdentityProviderConfiguration, IdentityProviderMetadata> result = null;
		String idp = request.getParameter("idp");
		if (hasText(idp)) {
			result = sp.getRemoteProviders().entrySet().stream()
				.filter(p -> idp.equals(p.getValue().getEntityId()))
				.findFirst()
				.orElse(null);
		}
		else if (sp.getRemoteProviders().size() == 1) { //we only have one, consider it the default
			result = sp.getRemoteProviders()
				.entrySet()
				.stream()
				.findFirst()
				.orElse(null);
		}
		if (result == null) {
			throw new SamlProviderNotFoundException("Unable to identify a configured identity provider.");
		}
		return result;
	}

	protected AuthenticationRequest getAuthenticationRequest(ServiceProviderMetadata sp,
															 IdentityProviderMetadata idp,
															 int preferredEndpointIndex,
															 NameId requestedNameId) {
		Endpoint endpoint = getPreferredEndpoint(
			idp.getIdentityProvider().getSingleSignOnService(),
			BindingType.REDIRECT,
			preferredEndpointIndex
		);
		AuthenticationRequest request = new AuthenticationRequest()
			// Some service providers will not accept first character if 0..9
			// Azure AD IdP for example.
			.setId("_" + UUID.randomUUID().toString().substring(1))
			.setIssueInstant(new DateTime(getClock().millis()))
			.setForceAuth(Boolean.FALSE)
			.setPassive(Boolean.FALSE)
			.setBinding(endpoint.getBinding())
			.setAssertionConsumerService(
				getPreferredEndpoint(
					sp.getServiceProvider().getAssertionConsumerService(),
					null,
					-1
				)
			)
			.setIssuer(new Issuer().setValue(sp.getEntityId()))
			.setDestination(endpoint);
		if (sp.getServiceProvider().isAuthnRequestsSigned()) {
			request.setSigningKey(sp.getSigningKey(), sp.getAlgorithm(), sp.getDigest());
		}
		if (requestedNameId != null) {
			request.setNameIdPolicy(new NameIdPolicy(
				requestedNameId,
				sp.getEntityId(),
				true
			));
		}
		else if (idp.getDefaultNameId() != null) {
			request.setNameIdPolicy(new NameIdPolicy(
				idp.getDefaultNameId(),
				sp.getEntityId(),
				true
			));
		}
		else if (idp.getIdentityProvider().getNameIds().size() > 0) {
			request.setNameIdPolicy(new NameIdPolicy(
				idp.getIdentityProvider().getNameIds().get(0),
				sp.getEntityId(),
				true
			));
		}
		return request;
	}

	protected void sendAuthenticationRequest(AuthenticationRequest authn,
											 Endpoint destination,
											 HttpServletRequest request,
											 HttpServletResponse response) throws IOException {
		String relayState = request.getParameter("RelayState");
		if (destination.getBinding().equals(Binding.REDIRECT)) {
			String encoded = transformer.samlEncode(transformer.toXml(authn), true);
			UriComponentsBuilder url = UriComponentsBuilder.fromUriString(destination.getLocation());
			url.queryParam("SAMLRequest", UriUtils.encode(encoded, StandardCharsets.UTF_8.name()));
			if (hasText(relayState)) {
				url.queryParam("RelayState", UriUtils.encode(relayState, StandardCharsets.UTF_8.name()));
			}
			String redirect = url.build(true).toUriString();
			response.sendRedirect(redirect);
		}
		else if (destination.getBinding().equals(Binding.POST)) {
			String encoded = transformer.samlEncode(transformer.toXml(authn), false);
			Map<String, Object> model = new HashMap<>();
			model.put("action", destination.getLocation());
			model.put("SAMLRequest", encoded);
			if (hasText(relayState)) {
				model.put("RelayState", relayState);
			}
			template.processHtmlBody(
				request,
				response,
				getPostTemplate(),
				model
			);
		}
		else {
			displayError(request, response, "Unsupported binding:" + destination.getBinding().toString());
		}
	}

	private void displayError(HttpServletRequest request,
							  HttpServletResponse response,
							  String message) {
		template.processHtmlBody(
			request,
			response,
			template.getErrorTemplate(),
			Collections.singletonMap("message", message)
		);
	}

	protected Clock getClock() {
		return clock;
	}

	public AuthenticationRequestFilter setClock(Clock clock) {
		this.clock = clock;
		return this;
	}

	protected String getPostTemplate() {
		return postTemplate;
	}

	public AuthenticationRequestFilter setPostTemplate(String postTemplate) {
		this.postTemplate = postTemplate;
		return this;
	}
}
