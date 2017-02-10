package com.julienviet.serviceflow;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen(concrete = false)
public interface ServiceFlow {

  void httpRequest(JsonObject filter, HttpMethod method, String requestURI, Handler<AsyncResult<HttpRequest<Buffer>>> handler);

  void sendMessage(JsonObject filter, Object msg);

  <T> void getServiceProxy(JsonObject filter, Class<T> clazz, Handler<AsyncResult<T>> handler);

}
