/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.security.saml.configuration;

import java.util.List;

import org.springframework.security.saml.saml2.key.KeyData;

public abstract class ExternalProviderConfiguration<T extends ExternalProviderConfiguration> {
	private final String alias;
	private final String metadata;
	private final String linktext;
	private final boolean skipSslValidation;
	private final boolean metadataTrustCheck;
	private final List<KeyData> verificationKeys;

	ExternalProviderConfiguration(String alias,
								  String metadata,
								  String linktext,
								  boolean skipSslValidation,
								  boolean metadataTrustCheck,
								  List<KeyData> verificationKeys) {
		this.alias = alias;
		this.metadata = metadata;
		this.linktext = linktext;
		this.skipSslValidation = skipSslValidation;
		this.metadataTrustCheck = metadataTrustCheck;
		this.verificationKeys = verificationKeys;
	}

	public String getAlias() {
		return alias;
	}

	public String getMetadata() {
		return metadata;
	}

	public String getLinktext() {
		return linktext;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public boolean isMetadataTrustCheck() {
		return metadataTrustCheck;
	}

	public List<KeyData> getVerificationKeys() {
		return verificationKeys;
	}
}
