/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.springframework.security.saml.provider.identity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.security.saml.SamlMetadataCache;
import org.springframework.security.saml.SamlProviderNotFoundException;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.provider.AbstractHostedProvider;
import org.springframework.security.saml.provider.identity.config.LocalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.Saml2Object;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.saml2.authentication.AudienceRestriction;
import org.springframework.security.saml.saml2.authentication.AuthenticationRequest;
import org.springframework.security.saml.saml2.authentication.AuthenticationStatement;
import org.springframework.security.saml.saml2.authentication.Conditions;
import org.springframework.security.saml.saml2.authentication.Issuer;
import org.springframework.security.saml.saml2.authentication.LogoutRequest;
import org.springframework.security.saml.saml2.authentication.NameIdPrincipal;
import org.springframework.security.saml.saml2.authentication.Response;
import org.springframework.security.saml.saml2.authentication.Status;
import org.springframework.security.saml.saml2.authentication.StatusCode;
import org.springframework.security.saml.saml2.authentication.Subject;
import org.springframework.security.saml.saml2.authentication.SubjectConfirmation;
import org.springframework.security.saml.saml2.authentication.SubjectConfirmationData;
import org.springframework.security.saml.saml2.authentication.SubjectConfirmationMethod;
import org.springframework.security.saml.saml2.metadata.Binding;
import org.springframework.security.saml.saml2.metadata.Endpoint;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;

import org.joda.time.DateTime;

import static java.util.Arrays.asList;
import static org.springframework.security.saml.saml2.metadata.Binding.POST;

public class HostedIdentityProvider extends AbstractHostedProvider<
	LocalIdentityProviderConfiguration,
	IdentityProviderMetadata,
	ServiceProviderMetadata>
	implements IdentityProvider {

	public HostedIdentityProvider(LocalIdentityProviderConfiguration configuration,
								  IdentityProviderMetadata metadata,
								  SamlTransformer transformer,
								  SamlValidator validator,
								  SamlMetadataCache cache) {
		super(configuration, metadata, transformer, validator, cache);
	}

	@Override
	public ServiceProviderMetadata getRemoteProvider(Saml2Object saml2Object) {
		throw new IllegalArgumentException("Unable to resolve class:"+saml2Object.getClass().getName());
	}

	public ServiceProviderMetadata getRemoteProvider(AuthenticationRequest request) {
		String issuer = request.getIssuer() != null ?
			request.getIssuer().getValue() :
			null;
		return getRemoteProvider(issuer);
	}

	public ServiceProviderMetadata getRemoteProvider(LogoutRequest request) {
		String issuer = request.getIssuer() != null ?
			request.getIssuer().getValue() :
			null;
		return getRemoteProvider(issuer);
	}

	public ServiceProviderMetadata getRemoteProvider(Assertion localAssertion) {
		if (localAssertion == null || localAssertion.getSubject() == null) {
			throw new SamlProviderNotFoundException("Assertion must not be null");
		}
		Subject subject = localAssertion.getSubject();
		NameIdPrincipal principal = subject.getPrincipal();

		String spNameQualifier = principal != null ?
			principal.getSpNameQualifier() :
			null;

		return getRemoteProvider(spNameQualifier);
	}

	@Override
	public Assertion assertion(ServiceProviderMetadata sp, String principal, NameId principalFormat) {
		return assertion(sp, null, principal, principalFormat);
	}

	@Override
	public Assertion assertion(ServiceProviderMetadata sp,
							   AuthenticationRequest request,
							   String principal,
							   NameId principalFormat) {
		long now = getClock().millis();
		return new Assertion()
			.setSigningKey(getMetadata().getSigningKey(), getMetadata().getAlgorithm(), getMetadata().getDigest())
			.setVersion("2.0")
			.setIssueInstant(new DateTime(now))
			.setId(UUID.randomUUID().toString())
			.setIssuer(getMetadata().getEntityId())
			.setSubject(
				new Subject()
					.setPrincipal(
						new NameIdPrincipal()
							.setValue(principal)
							.setFormat(principalFormat)
							.setNameQualifier(sp.getEntityAlias())
							.setSpNameQualifier(sp.getEntityId())
					)
					.addConfirmation(
						new SubjectConfirmation()
							.setMethod(SubjectConfirmationMethod.BEARER)
							.setConfirmationData(
								new SubjectConfirmationData()
									.setInResponseTo(request != null ? request.getId() : null)
									//we don't set NotBefore. Gets rejected.
									//.setNotBefore(new DateTime(now - NOT_BEFORE))
									.setNotOnOrAfter(new DateTime(now + getConfiguration().getNotOnOrAfter()))
									.setRecipient(
										request != null ?
											request.getAssertionConsumerService().getLocation() :
											getACSFromSp(sp).getLocation()
									)
							)
					)


			)
			.setConditions(
				new Conditions()
					.setNotBefore(new DateTime(now - getConfiguration().getNotBefore()))
					.setNotOnOrAfter(new DateTime(now + getConfiguration().getNotOnOrAfter()))
					.addCriteria(
						new AudienceRestriction()
							.addAudience(sp.getEntityId())

					)
			)
			.addAuthenticationStatement(
				new AuthenticationStatement()
					.setAuthInstant(new DateTime(now))
					.setSessionIndex(UUID.randomUUID().toString())
					.setSessionNotOnOrAfter(new DateTime(now + getConfiguration().getSessionNotOnOrAfter()))

			);
	}

	@Override
	public Response response(Assertion assertion, ServiceProviderMetadata recipient) {
		return response(null, assertion, recipient);
	}

	@Override
	public Response response(AuthenticationRequest authn, Assertion assertion, ServiceProviderMetadata recipient) {
		Response result = new Response()
			.setAssertions(asList(assertion))
			.setId(UUID.randomUUID().toString())
			.setInResponseTo(authn != null ? authn.getId() : null)
			.setStatus(new Status().setCode(StatusCode.UNKNOWN_STATUS))
			.setIssuer(new Issuer().setValue(getMetadata().getEntityId()))
			.setSigningKey(getMetadata().getSigningKey(), getMetadata().getAlgorithm(), getMetadata().getDigest())
			.setIssueInstant(new DateTime())
			.setStatus(new Status().setCode(StatusCode.SUCCESS))
			.setVersion("2.0");
		Endpoint acs = (authn != null ? authn.getAssertionConsumerService() : null);
		if (acs == null) {
			acs = getPreferredACS(recipient, asList(POST));
		}
		if (acs != null) {
			result.setDestination(acs.getLocation());
		}
		return result;
	}

	public Endpoint getPreferredACS(ServiceProviderMetadata recipient,
									List<Binding> preferred) {

		List<Endpoint> eps = recipient.getServiceProvider().getAssertionConsumerService();

		if (eps == null || eps.isEmpty()) {
			return null;
		}
		Endpoint result = null;
		for (Endpoint e : eps) {
			if (e.isDefault() && preferred.contains(e.getBinding())) {
				result = e;
				break;
			}
		}
		for (Endpoint e : (result == null ? eps : Collections.<Endpoint>emptyList())) {
			if (e.isDefault()) {
				result = e;
				break;
			}
		}
		for (Endpoint e : (result == null ? eps : Collections.<Endpoint>emptyList())) {
			if (preferred.contains(e.getBinding())) {
				result = e;
				break;
			}
		}
		if (result == null ) {
			result = getACSFromSp(recipient);
		}
		return result;
	}
}
