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

package org.springframework.security.saml.provider.service.config;

import org.springframework.security.saml.provider.config.ExternalProviderConfiguration;
import org.springframework.security.saml.saml2.metadata.NameId;

public class ExternalIdentityProviderConfiguration extends
	ExternalProviderConfiguration<ExternalIdentityProviderConfiguration> {

	private NameId nameId;
	private int assertionConsumerServiceIndex;

	public NameId getNameId() {
		return nameId;
	}

	public ExternalIdentityProviderConfiguration setNameId(NameId nameId) {
		this.nameId = nameId;
		return this;
	}

	public int getAssertionConsumerServiceIndex() {
		return assertionConsumerServiceIndex;
	}

	public ExternalIdentityProviderConfiguration setAssertionConsumerServiceIndex(int assertionConsumerServiceIndex) {
		this.assertionConsumerServiceIndex = assertionConsumerServiceIndex;
		return this;
	}
}