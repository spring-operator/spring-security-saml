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

package org.springframework.security.saml.provider;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.saml.SamlMetadataCache;
import org.springframework.security.saml.SamlTransformer;
import org.springframework.security.saml.SamlValidator;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.provider.service.HostedServiceProvider;
import org.springframework.security.saml.provider.service.ServiceProvider;
import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.Binding;
import org.springframework.security.saml.saml2.metadata.NameId;
import org.springframework.security.saml.saml2.metadata.ServiceProviderMetadata;
import org.springframework.security.saml.saml2.signature.AlgorithmMethod;
import org.springframework.security.saml.saml2.signature.DigestMethod;

import static java.util.Arrays.asList;
import static org.springframework.security.saml.saml2.metadata.Binding.REDIRECT;
import static org.springframework.util.StringUtils.hasText;

public class HostBasedSamlServiceProviderProvisioning
	extends AbstractHostbasedSamlProviderProvisioning
	implements SamlProviderProvisioning<ServiceProvider> {

	public HostBasedSamlServiceProviderProvisioning(SamlConfigurationRepository configuration,
													SamlTransformer transformer,
													SamlValidator validator,
													SamlMetadataCache cache) {
		super(configuration, transformer, validator, cache);
	}


	@Override
	public ServiceProvider getHostedProvider(HttpServletRequest request) {
		String basePath = getBasePath(request);
		LocalServiceProviderConfiguration configuration =
			getConfiguration().getServerConfiguration(request).getServiceProvider();

		List<SimpleKey> keys = new LinkedList<>();
		keys.add(configuration.getKeys().getActive());
		keys.addAll(configuration.getKeys().getStandBy());
		SimpleKey signingKey = configuration.isSignMetadata() ? configuration.getKeys().getActive() : null;

		String prefix = hasText(configuration.getPrefix()) ? configuration.getPrefix() : "saml/sp/";
		String aliasPath = getAliasPath(configuration);
		ServiceProviderMetadata metadata =
			serviceProviderMetadata(
				basePath,
				signingKey,
				keys,
				prefix,
				aliasPath,
				configuration.getDefaultSigningAlgorithm(),
				configuration.getDefaultDigest()
			);
		if (!configuration.getNameIds().isEmpty()) {
			metadata.getServiceProvider().setNameIds(configuration.getNameIds());
		}

		return new HostedServiceProvider(
			configuration,
			metadata,
			getTransformer(),
			getValidator(),
			getCache()
		);
	}

	private ServiceProviderMetadata serviceProviderMetadata(String baseUrl,
															SimpleKey signingKey,
															List<SimpleKey> keys,
															String prefix,
															String aliasPath,
															AlgorithmMethod signAlgorith,
															DigestMethod signDigest) {

		return new ServiceProviderMetadata()
			.setEntityId(baseUrl)
			.setId(UUID.randomUUID().toString())
			.setSigningKey(signingKey, signAlgorith, signDigest)
			.setProviders(
				asList(
					new org.springframework.security.saml.saml2.metadata.ServiceProvider()
						.setKeys(keys)
						.setWantAssertionsSigned(true)
						.setAuthnRequestsSigned(signingKey != null)
						.setAssertionConsumerService(
							asList(
								getEndpoint(baseUrl, prefix + "SSO/alias/" + aliasPath, Binding.POST, 0, true),
								getEndpoint(baseUrl, prefix + "SSO/alias/" + aliasPath, REDIRECT, 1, false)
							)
						)
						.setNameIds(asList(NameId.PERSISTENT, NameId.EMAIL))
						.setKeys(keys)
						.setSingleLogoutService(
							asList(
								getEndpoint(baseUrl, prefix + "logout/alias/" + aliasPath, REDIRECT, 0, true)
							)
						)
				)
			);

	}

}
