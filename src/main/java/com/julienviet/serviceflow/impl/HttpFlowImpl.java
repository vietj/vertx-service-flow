package com.julienviet.serviceflow.impl;

import com.julienviet.serviceflow.HttpFlow;
import io.vertx.codegen.annotations.CacheReturn;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.LanguageHeader;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HttpFlowImpl extends ServiceFlowBase implements HttpFlow {

  private final RoutingContext context;

  HttpFlowImpl(RoutingContext context, ServiceDiscovery discovery) {
    super(discovery);
    this.context = context;
  }

  @Override
  @CacheReturn
  public HttpServerRequest request() {
    return context.request();
  }

  @Override
  @CacheReturn
  public HttpServerResponse response() {
    return context.response();
  }

  @Override
  public void next() {
    context.next();
  }

  @Override
  public void fail(int statusCode) {
    context.fail(statusCode);
  }

  @Override
  public void fail(Throwable throwable) {
    context.fail(throwable);
  }

  @Override
  @Fluent
  public HttpFlow put(String key, Object obj) {
    context.put(key, obj);
    return this;
  }

  @Override
  public <T> T get(String key) {
    return context.get(key);
  }

  @Override
  public <T> T remove(String key) {
    return context.remove(key);
  }

  @Override
  @GenIgnore
  public Map<String, Object> data() {
    return context.data();
  }

  @Override
  @CacheReturn
  public Vertx vertx() {
    return context.vertx();
  }

  @Override
  public String mountPoint() {
    return context.mountPoint();
  }

  @Override
  public Route currentRoute() {
    return context.currentRoute();
  }

  @Override
  public String normalisedPath() {
    return context.normalisedPath();
  }

  @Override
  public Cookie getCookie(String name) {
    return context.getCookie(name);
  }

  @Override
  @Fluent
  public HttpFlow addCookie(Cookie cookie) {
    context.addCookie(cookie);
    return this;
  }

  @Override
  public Cookie removeCookie(String name) {
    return context.removeCookie(name);
  }

  @Override
  public int cookieCount() {
    return context.cookieCount();
  }

  @Override
  public Set<Cookie> cookies() {
    return context.cookies();
  }

  @Override
  public String getBodyAsString() {
    return context.getBodyAsString();
  }

  @Override
  public String getBodyAsString(String encoding) {
    return context.getBodyAsString(encoding);
  }

  @Override
  public JsonObject getBodyAsJson() {
    return context.getBodyAsJson();
  }

  @Override
  public JsonArray getBodyAsJsonArray() {
    return context.getBodyAsJsonArray();
  }

  @Override
  public Buffer getBody() {
    return context.getBody();
  }

  @Override
  public Set<FileUpload> fileUploads() {
    return context.fileUploads();
  }

  @Override
  public Session session() {
    return context.session();
  }

  @Override
  public User user() {
    return context.user();
  }

  @Override
  @CacheReturn
  public Throwable failure() {
    return context.failure();
  }

  @Override
  @CacheReturn
  public int statusCode() {
    return context.statusCode();
  }

  @Override
  public String getAcceptableContentType() {
    return context.getAcceptableContentType();
  }

  @Override
  @CacheReturn
  public ParsedHeaderValues parsedHeaders() {
    return context.parsedHeaders();
  }

  @Override
  public int addHeadersEndHandler(Handler<Void> handler) {
    return context.addHeadersEndHandler(handler);
  }

  @Override
  public boolean removeHeadersEndHandler(int handlerID) {
    return context.removeHeadersEndHandler(handlerID);
  }

  @Override
  public int addBodyEndHandler(Handler<Void> handler) {
    return context.addBodyEndHandler(handler);
  }

  @Override
  public boolean removeBodyEndHandler(int handlerID) {
    return context.removeBodyEndHandler(handlerID);
  }

  @Override
  public boolean failed() {
    return context.failed();
  }

  @Override
  public void setBody(Buffer body) {
    context.setBody(body);
  }

  @Override
  public void setSession(Session session) {
    context.setSession(session);
  }

  @Override
  public void setUser(User user) {
    context.setUser(user);
  }

  @Override
  public void clearUser() {
    context.clearUser();
  }

  @Override
  public void setAcceptableContentType(String contentType) {
    context.setAcceptableContentType(contentType);
  }

  @Override
  public void reroute(String path) {
    context.reroute(path);
  }

  @Override
  public void reroute(HttpMethod method, String path) {
    context.reroute(method, path);
  }

  @Override
  @Deprecated
  @CacheReturn
  public List<Locale> acceptableLocales() {
    return context.acceptableLocales();
  }

  @Override
  @CacheReturn
  public List<LanguageHeader> acceptableLanguages() {
    return context.acceptableLanguages();
  }

  @Override
  @CacheReturn
  @Deprecated
  public Locale preferredLocale() {
    return context.preferredLocale();
  }

  @Override
  @CacheReturn
  public LanguageHeader preferredLanguage() {
    return context.preferredLanguage();
  }

  @Override
  public Map<String, String> pathParams() {
    return context.pathParams();
  }

  @Override
  public String pathParam(String name) {
    return context.pathParam(name);
  }
}
