/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.cli;

import com.facebook.presto.client.ClientSession;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import io.airlift.airline.Option;
import io.airlift.units.Duration;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

import static com.facebook.presto.client.KerberosUtil.defaultCredentialCachePath;
import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;

public class ClientOptions
{
    @Option(name = "--server", title = "server", description = "Presto server location (default: localhost:8080)")
    public String server = "localhost:8080";

    @Option(name = "--enable-authentication", title = "enable authentication", description = "Enable client authentication")
    public boolean authenticationEnabled;

    @Option(name = "--krb5-remote-service-name", title = "krb5 remote service name", description = "Remote peer's kerberos service name")
    public String krb5RemoteServiceName;

    @Option(name = "--krb5-config-path", title = "krb5 config path", description = "Kerberos config file path (default: /etc/krb5.conf)")
    public String krb5ConfigPath = "/etc/krb5.conf";

    @Option(name = "--krb5-keytab-path", title = "krb5 keytab path", description = "Kerberos key table path (default: /etc/krb5.keytab)")
    public String krb5KeytabPath = "/etc/krb5.keytab";

    @Option(name = "--krb5-credential-cache-path", title = "krb5 credential cache path", description = "Kerberos credential cache path")
    public String krb5CredentialCachePath = defaultCredentialCachePath().orElse(null);

    @Option(name = "--krb5-principal", title = "krb5 principal", description = "Kerberos principal to be used")
    public String krb5Principal;

    @Option(name = "--krb5-disable-remote-service-hostname-canonicalization", title = "krb5 disable remote service hostname canonicalization", description = "Disable service hostname canonicalization using the DNS reverse lookup")
    public boolean krb5DisableRemoteServiceHostnameCanonicalization;

    @Option(name = "--keystore-path", title = "keystore path", description = "Keystore path")
    public String keystorePath;

    @Option(name = "--keystore-password", title = "keystore password", description = "Keystore password")
    public String keystorePassword;

    @Option(name = "--truststore-path", title = "truststore path", description = "Truststore path")
    public String truststorePath;

    @Option(name = "--truststore-password", title = "truststore password", description = "Truststore password")
    public String truststorePassword;

    @Option(name = "--user", title = "user", description = "Username")
    public String user = System.getProperty("user.name");

    @Option(name = "--password", title = "password", description = "Prompt for password")
    public boolean password;

    @Option(name = "--source", title = "source", description = "Name of source making query")
    public String source = "presto-cli";

    @Option(name = "--catalog", title = "catalog", description = "Default catalog")
    public String catalog;

    @Option(name = "--schema", title = "schema", description = "Default schema")
    public String schema;

    @Option(name = {"-f", "--file"}, title = "file", description = "Execute statements from file and exit")
    public String file;

    @Option(name = "--debug", title = "debug", description = "Enable debug information")
    public boolean debug;

    @Option(name = "--log-levels-file", title = "log levels file", description = "Configure log levels for debugging using this file")
    public String logLevelsFile;

    @Option(name = "--execute", title = "execute", description = "Execute specified statements and exit")
    public String execute;

    @Option(name = "--output-format", title = "output-format", description = "Output format for batch mode [ALIGNED, VERTICAL, CSV, TSV, CSV_HEADER, TSV_HEADER, NULL] (default: CSV)")
    public OutputFormat outputFormat = OutputFormat.CSV;

    @Option(name = "--session", title = "session", description = "Session property (property can be used multiple times; format is key=value; use 'SHOW SESSION' to see available properties)")
    public final List<ClientSessionProperty> sessionProperties = new ArrayList<>();

    @Option(name = "--socks-proxy", title = "socks-proxy", description = "SOCKS proxy to use for server connections")
    public HostAndPort socksProxy;

    @Option(name = "--http-proxy", title = "http-proxy", description = "HTTP proxy to use for server connections")
    public HostAndPort httpProxy;

    @Option(name = "--client-request-timeout", title = "client request timeout", description = "Client request timeout (default: 2m)")
    public Duration clientRequestTimeout = new Duration(2, MINUTES);

    public enum OutputFormat
    {
        ALIGNED,
        VERTICAL,
        CSV,
        TSV,
        CSV_HEADER,
        TSV_HEADER,
        NULL
    }

    public ClientSession toClientSession()
    {
        return new ClientSession(
                parseServer(server),
                user,
                source,
                null, // client-supplied payload field not yet supported in CLI
                catalog,
                schema,
                TimeZone.getDefault().getID(),
                Locale.getDefault(),
                toProperties(sessionProperties),
                emptyMap(),
                null,
                debug,
                clientRequestTimeout);
    }

    public static URI parseServer(String server)
    {
        server = server.toLowerCase(ENGLISH);
        if (server.startsWith("http://") || server.startsWith("https://")) {
            return URI.create(server);
        }

        HostAndPort host = HostAndPort.fromString(server);
        try {
            return new URI("http", null, host.getHostText(), host.getPortOrDefault(80), null, null, null);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Map<String, String> toProperties(List<ClientSessionProperty> sessionProperties)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (ClientSessionProperty sessionProperty : sessionProperties) {
            String name = sessionProperty.getName();
            if (sessionProperty.getCatalog().isPresent()) {
                name = sessionProperty.getCatalog().get() + "." + name;
            }
            builder.put(name, sessionProperty.getValue());
        }
        return builder.build();
    }

    public static final class ClientSessionProperty
    {
        private static final Splitter NAME_VALUE_SPLITTER = Splitter.on('=').limit(2);
        private static final Splitter NAME_SPLITTER = Splitter.on('.');
        private final Optional<String> catalog;
        private final String name;
        private final String value;

        public ClientSessionProperty(String property)
        {
            List<String> nameValue = NAME_VALUE_SPLITTER.splitToList(property);
            checkArgument(nameValue.size() == 2, "Session property: %s", property);

            List<String> nameParts = NAME_SPLITTER.splitToList(nameValue.get(0));
            checkArgument(nameParts.size() == 1 || nameParts.size() == 2, "Invalid session property: %s", property);
            if (nameParts.size() == 1) {
                catalog = Optional.empty();
                name = nameParts.get(0);
            }
            else {
                catalog = Optional.of(nameParts.get(0));
                name = nameParts.get(1);
            }
            value = nameValue.get(1);

            verifyProperty(catalog, name, value);
        }

        public ClientSessionProperty(Optional<String> catalog, String name, String value)
        {
            this.catalog = requireNonNull(catalog, "catalog is null");
            this.name = requireNonNull(name, "name is null");
            this.value = requireNonNull(value, "value is null");

            verifyProperty(catalog, name, value);
        }

        private static void verifyProperty(Optional<String> catalog, String name, String value)
        {
            checkArgument(!catalog.isPresent() || !catalog.get().isEmpty(), "Invalid session property: %s.%s:%s", catalog, name, value);
            checkArgument(!name.isEmpty(), "Session property name is empty");

            CharsetEncoder charsetEncoder = US_ASCII.newEncoder();
            checkArgument(catalog.orElse("").indexOf('=') < 0, "Session property catalog must not contain '=': %s", name);
            checkArgument(charsetEncoder.canEncode(catalog.orElse("")), "Session property catalog is not US_ASCII: %s", name);
            checkArgument(name.indexOf('=') < 0, "Session property name must not contain '=': %s", name);
            checkArgument(charsetEncoder.canEncode(name), "Session property name is not US_ASCII: %s", name);
            checkArgument(charsetEncoder.canEncode(value), "Session property value is not US_ASCII: %s", value);
        }

        public Optional<String> getCatalog()
        {
            return catalog;
        }

        public String getName()
        {
            return name;
        }

        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return (catalog.isPresent() ? catalog.get() + '.' : "") + name + '=' + value;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(catalog, name, value);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ClientSessionProperty other = (ClientSessionProperty) obj;
            return Objects.equals(this.catalog, other.catalog) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.value, other.value);
        }
    }
}
