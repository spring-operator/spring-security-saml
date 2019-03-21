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

package org.springframework.security.saml.samples;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.configuration.ExternalIdentityProviderConfiguration;
import org.springframework.security.saml.saml2.key.KeyData;
import org.springframework.security.saml.saml2.key.KeyType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.springframework.security.config.annotation.web.configurers.SamlServiceProviderConfigurer.saml2Login;
import static org.springframework.security.saml.saml2.metadata.NameId.EMAIL;
import static org.springframework.security.saml.saml2.metadata.NameId.PERSISTENT;
import static org.springframework.security.saml.saml2.metadata.NameId.UNSPECIFIED;
import static org.springframework.security.saml.serviceprovider.web.configuration.SingletonServiceProviderConfigurationResolver.fromConfiguration;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		String pathPrefix = "/saml/sp";
		http
			.authorizeRequests()
				.mvcMatchers("/**").authenticated()
				.and()
			.apply(
				saml2Login()
					.configurationResolver(
						fromConfiguration(
							config -> config
								.pathPrefix(pathPrefix)
								.entityId("spring.security.saml.sp.id")
								.alias("boot-sample-sp")
								.signMetadata(true)
								.signRequests(true)
								.wantAssertionsSigned(true)
								.singleLogoutEnabled(true)
								.nameIds(asList(UNSPECIFIED, EMAIL, PERSISTENT))
								.keys(
									asList(
										new KeyData(
											"sp-signing-key",
											privateKey,
											certificate,
											"sppassword",
											KeyType.SIGNING
										)
									)
								)
								.providers(
									asList(
										new ExternalIdentityProviderConfiguration(
											"simplesamlphp",
											"http://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
											"Simple SAML PHP IDP (Java Config)",
											true,
											false,
											UNSPECIFIED,
											0,
											emptyList()
										)
									)
								)
						)
					)
			);
	}

	String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
		"Proc-Type: 4,ENCRYPTED\n" +
		"DEK-Info: DES-EDE3-CBC,7C8510E4CED17A9F\n" +
		"\n" +
		"SRYezKuY+AgM+gdiklVDBQ1ljeCFKnW3c5BM9sEyEOfkQm0zZx6fLr0afup0ToE4\n" +
		"iJGLxKw8swAnUAIjYda9wxqIEBb9mILyuRPevyfzmio2lE9KnARDEYRBqbwD9Lpd\n" +
		"vwZKNGHHJbZAgcUNfhXiYakmx0cUyp8HeO3Vqa/0XMiI/HAdlJ/ruYeT4e2DSrz9\n" +
		"ORZA2S5OvNpRQeCVf26l6ODKXnkDL0t5fDVY4lAhaiyhZtoT0sADlPIERBw73kHm\n" +
		"fGCTniY9qT0DT+R5Rqukk42mN2ij/cAr+kdV5colBi1fuN6d9gawCiH4zSb3LzHQ\n" +
		"9ccSlz6iQV1Ty2cRuTkB3zWC6Oy4q0BRlXnVRFOnOfYJztO6c2hD3Q9NxkDAbcgR\n" +
		"YWJWHpd0/HI8GyBpOG7hAS1l6aoleH30QCDOo7N2rFrTAaPC6g84oZOFSqkqvx4R\n" +
		"KTbWRwgJsqVxM6GqV6H9x1LNn2CpBizdGnp8VvnIiYcEvItMJbT1C1yeIUPoDDU2\n" +
		"Ct0Jofw/dquXStHWftPFjpIqB+5Ou//HQ2VNzjbyThNWVGtjnEKwSiHacQLS1sB3\n" +
		"iqFtSN/VCpdOcRujEBba+x5vlc8XCV1qr6x1PbvfPZVjyFdSM6JQidr0uEeDGDW3\n" +
		"TuYC1YgURN8zh0QF2lJIMX3xgbhr8HHNXv60ulcjeqYmna6VCS8AKJQgRTr4DGWt\n" +
		"Afv9BFV943Yp3nHwPC7nYC4FvMxOn4qW4KrHRJl57zcY6VDL4J030CfmvLjqUbuT\n" +
		"LYiQp/YgFlmoE4bcGuCiaRfUJZCwooPK2dQMoIvMZeVl9ExUGdXVMg==\n" +
		"-----END RSA PRIVATE KEY-----";
	String certificate = "-----BEGIN CERTIFICATE-----\n" +
		"MIICgTCCAeoCCQCuVzyqFgMSyDANBgkqhkiG9w0BAQsFADCBhDELMAkGA1UEBhMC\n" +
		"VVMxEzARBgNVBAgMCldhc2hpbmd0b24xEjAQBgNVBAcMCVZhbmNvdXZlcjEdMBsG\n" +
		"A1UECgwUU3ByaW5nIFNlY3VyaXR5IFNBTUwxCzAJBgNVBAsMAnNwMSAwHgYDVQQD\n" +
		"DBdzcC5zcHJpbmcuc2VjdXJpdHkuc2FtbDAeFw0xODA1MTQxNDMwNDRaFw0yODA1\n" +
		"MTExNDMwNDRaMIGEMQswCQYDVQQGEwJVUzETMBEGA1UECAwKV2FzaGluZ3RvbjES\n" +
		"MBAGA1UEBwwJVmFuY291dmVyMR0wGwYDVQQKDBRTcHJpbmcgU2VjdXJpdHkgU0FN\n" +
		"TDELMAkGA1UECwwCc3AxIDAeBgNVBAMMF3NwLnNwcmluZy5zZWN1cml0eS5zYW1s\n" +
		"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDRu7/EI0BlNzMEBFVAcbx+lLos\n" +
		"vzIWU+01dGTY8gBdhMQNYKZ92lMceo2CuVJ66cUURPym3i7nGGzoSnAxAre+0YIM\n" +
		"+U0razrWtAUE735bkcqELZkOTZLelaoOztmWqRbe5OuEmpewH7cx+kNgcVjdctOG\n" +
		"y3Q6x+I4qakY/9qhBQIDAQABMA0GCSqGSIb3DQEBCwUAA4GBAAeViTvHOyQopWEi\n" +
		"XOfI2Z9eukwrSknDwq/zscR0YxwwqDBMt/QdAODfSwAfnciiYLkmEjlozWRtOeN+\n" +
		"qK7UFgP1bRl5qksrYX5S0z2iGJh0GvonLUt3e20Ssfl5tTEDDnAEUMLfBkyaxEHD\n" +
		"RZ/nbTJ7VTeZOSyRoVn5XHhpuJ0B\n" +
		"-----END CERTIFICATE-----";


}

