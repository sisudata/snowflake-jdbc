/*
 * Copyright (c) 2012-2021 Snowflake Computing Inc. All rights reserved.
 */

package net.snowflake.client.core;

import com.amazonaws.Protocol;
import com.google.common.base.Strings;
import java.io.Serializable;

/**
 * This class defines all non-static parameters needed to create an HttpClient object. It is used as
 * the key for the static hashmap of reusable http clients.
 */
public class HttpClientSettingsKey implements Serializable {

  private OCSPMode ocspMode;
  private boolean useProxy = false;
  private String proxyHost = "";
  private int proxyPort = 0;
  private String nonProxyHosts = "";
  private String proxyUser = "";
  private String proxyPassword = "";
  private String proxyProtocol = "http";
  private String userAgentSuffix = "";

  public HttpClientSettingsKey(
      OCSPMode mode,
      boolean useProxy,
      String host,
      int port,
      String nonProxyHosts,
      String user,
      String password,
      String scheme,
      String userPrefix) {
    this.useProxy = useProxy;
    this.ocspMode = mode != null ? mode : OCSPMode.FAIL_OPEN;
    this.proxyHost = !Strings.isNullOrEmpty(host) ? host.trim() : "";
    this.proxyPort = port;
    this.nonProxyHosts = !Strings.isNullOrEmpty(nonProxyHosts) ? nonProxyHosts.trim() : "";
    this.proxyUser = !Strings.isNullOrEmpty(user) ? user.trim() : "";
    this.proxyPassword = !Strings.isNullOrEmpty(password) ? password.trim() : "";
    this.proxyProtocol = !Strings.isNullOrEmpty(scheme) ? scheme.trim() : "http";
    this.userAgentSuffix = userPrefix;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof HttpClientSettingsKey) {
      HttpClientSettingsKey comparisonKey = (HttpClientSettingsKey) obj;
      if (comparisonKey.ocspMode.getValue() == this.ocspMode.getValue()) {
        if (!comparisonKey.useProxy && !this.useProxy) {
          return true;
        } else if (comparisonKey.proxyHost.equalsIgnoreCase(this.proxyHost)
            && comparisonKey.proxyPort == this.proxyPort
            && comparisonKey.proxyUser.equalsIgnoreCase(this.proxyUser)
            && comparisonKey.proxyPassword.equalsIgnoreCase(this.proxyPassword)
            && comparisonKey.proxyProtocol.equalsIgnoreCase(this.proxyProtocol)) {
          // update nonProxyHost if changed
          if (!this.nonProxyHosts.equalsIgnoreCase(comparisonKey.nonProxyHosts)) {
            comparisonKey.nonProxyHosts = this.nonProxyHosts;
          }
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.ocspMode.getValue()
        + (this.proxyHost
                + this.proxyPort
                + this.proxyUser
                + this.proxyPassword
                + this.proxyProtocol)
            .hashCode();
  }

  public OCSPMode getOcspMode() {
    return this.ocspMode;
  }

  public boolean usesProxy() {
    return this.useProxy;
  }

  public String getProxyHost() {
    return this.proxyHost;
  }

  public int getProxyPort() {
    return this.proxyPort;
  }

  public String getProxyUser() {
    return this.proxyUser;
  }

  public String getUserAgentSuffix() {
    return this.userAgentSuffix;
  }

  /** Be careful of using this! Should only be called when password is later masked. */
  String getProxyPassword() {
    return this.proxyPassword;
  }

  public String getNonProxyHosts() {
    return this.nonProxyHosts;
  }

  public Protocol getProxyProtocol() {
    return this.proxyProtocol.equalsIgnoreCase("https") ? Protocol.HTTPS : Protocol.HTTP;
  }

  public static class Builder {
    private OCSPMode mode;
    private String host;
    private int port;
    private String nonProxyHosts;
    private String user;
    private String password;
    private String scheme;
    private String userAgentSuffix;
    private boolean useProxy = false;

    public Builder setMode(OCSPMode mode) {
      this.mode = mode;
      return this;
    }

    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public Builder setProxy() {
      this.useProxy = true;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setNonProxyHosts(String nonProxyHosts) {
      this.nonProxyHosts = nonProxyHosts;
      return this;
    }

    public Builder setUser(String user) {
      this.user = user;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder setScheme(String scheme) {
      this.scheme = scheme;
      return this;
    }

    public Builder setUserAgentSuffix(String userAgentSuffix) {
      this.userAgentSuffix = userAgentSuffix;
      return this;
    }

    public HttpClientSettingsKey createHttpClientSettingsKey() {
      return new HttpClientSettingsKey(
          mode, useProxy, host, port, nonProxyHosts, user, password, scheme, userAgentSuffix);
    }
  }
}
