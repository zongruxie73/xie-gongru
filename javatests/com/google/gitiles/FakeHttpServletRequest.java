// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gitiles;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.gitiles.TestGitilesUrls.URLS;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import org.eclipse.jgit.http.server.ServletUtils;
import org.eclipse.jgit.internal.storage.dfs.DfsRepository;

/** Simple fake implementation of {@link HttpServletRequest}. */
public class FakeHttpServletRequest implements HttpServletRequest {
  public static final String SERVLET_PATH = "/b";

  public static FakeHttpServletRequest newRequest() {
    return new FakeHttpServletRequest(URLS.getHostName(null), 80, "", SERVLET_PATH);
  }

  public static FakeHttpServletRequest newRequest(DfsRepository repo) {
    FakeHttpServletRequest req = newRequest();
    req.setAttribute(ServletUtils.ATTRIBUTE_REPOSITORY, repo);
    return req;
  }

  private final Map<String, Object> attributes;
  private final ListMultimap<String, String> headers;

  private ListMultimap<String, String> parameters;
  private String hostName;
  private int port;
  private String method;
  private String contextPath;
  private String servletPath;
  private String path;

  private FakeHttpServletRequest(
      String hostName, int port, String contextPath, String servletPath) {
    this.hostName = checkNotNull(hostName, "hostName");
    checkArgument(port > 0);
    this.port = port;
    this.method = "GET";
    this.contextPath = checkNotNull(contextPath, "contextPath");
    this.servletPath = checkNotNull(servletPath, "servletPath");
    attributes = Maps.newConcurrentMap();
    parameters = LinkedListMultimap.create();
    headers = LinkedListMultimap.create();
  }

  @Override
  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(attributes.keySet());
  }

  @Override
  public String getCharacterEncoding() {
    return UTF_8.name();
  }

  @Override
  public int getContentLength() {
    return -1;
  }

  @Override
  public long getContentLengthLong() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getLocalAddr() {
    return "1.2.3.4";
  }

  @Override
  public String getLocalName() {
    return hostName;
  }

  @Override
  public int getLocalPort() {
    return port;
  }

  @Override
  public ServletContext getServletContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncContext startAsync() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAsyncStarted() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAsyncSupported() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DispatcherType getDispatcherType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Locale getLocale() {
    return Locale.US;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return Collections.enumeration(Collections.singleton(Locale.US));
  }

  @Override
  public String getParameter(String name) {
    return Iterables.getFirst(parameters.get(name), null);
  }

  private static final String[] stringCollectionToArray(Collection<String> values) {
    return values.toArray(new String[0]);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return Collections.unmodifiableMap(
        Maps.transformValues(parameters.asMap(), c -> stringCollectionToArray(c)));
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(parameters.keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return stringCollectionToArray(parameters.get(name));
  }

  public void setQueryString(String qs) {
    ListMultimap<String, String> params = LinkedListMultimap.create();
    for (String entry : Splitter.on('&').split(qs)) {
      List<String> kv = Splitter.on('=').limit(2).splitToList(entry);
      try {
        params.put(
            URLDecoder.decode(kv.get(0), UTF_8.name()),
            kv.size() == 2 ? URLDecoder.decode(kv.get(1), UTF_8.name()) : "");
      } catch (UnsupportedEncodingException e) {
        throw new IllegalArgumentException(e);
      }
    }
    parameters = params;
  }

  @Override
  public String getProtocol() {
    return "HTTP/1.1";
  }

  @Override
  public BufferedReader getReader() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public String getRealPath(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getRemoteAddr() {
    return "5.6.7.8";
  }

  @Override
  public String getRemoteHost() {
    return "remotehost";
  }

  @Override
  public int getRemotePort() {
    return 1234;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getScheme() {
    return port == 443 ? "https" : "http";
  }

  @Override
  public String getServerName() {
    return hostName;
  }

  @Override
  public int getServerPort() {
    return port;
  }

  @Override
  public boolean isSecure() {
    return port == 443;
  }

  @Override
  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  @Override
  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getAuthType() {
    return null;
  }

  @Override
  public String getContextPath() {
    return contextPath;
  }

  @Override
  public Cookie[] getCookies() {
    return new Cookie[0];
  }

  @Override
  public long getDateHeader(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getHeader(String name) {
    return Iterables.getFirst(headers.get(name), null);
  }

  public boolean setHeader(String name, String value) {
    return headers.put(name, value);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(headers.keySet());
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return Collections.enumeration(headers.get(name));
  }

  @Override
  public int getIntHeader(String name) {
    return Integer.parseInt(getHeader(name));
  }

  @Override
  public String getMethod() {
    return method;
  }

  public void setMethod(String m) {
    method = m;
  }

  @Override
  public String getPathInfo() {
    return path;
  }

  public void setPathInfo(String path) {
    this.path = checkNotNull(path);
  }

  @Override
  public String getPathTranslated() {
    return path;
  }

  @Override
  public String getQueryString() {
    return GitilesView.paramsToString(parameters);
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public String getRequestURI() {
    String uri = contextPath + servletPath + path;
    if (!parameters.isEmpty()) {
      uri += "?" + GitilesView.paramsToString(parameters);
    }
    return uri;
  }

  @Override
  public StringBuffer getRequestURL() {
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @Override
  public String getServletPath() {
    return servletPath;
  }

  @Override
  public HttpSession getSession() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String changeSessionId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public HttpSession getSession(boolean create) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Principal getUserPrincipal() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Deprecated
  public boolean isRequestedSessionIdFromUrl() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void login(String username, String password) throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void logout() throws ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass)
      throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isUserInRole(String role) {
    throw new UnsupportedOperationException();
  }
}
