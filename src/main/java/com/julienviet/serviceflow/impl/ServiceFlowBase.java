package com.julienviet.serviceflow.impl;

import com.julienviet.serviceflow.ServiceFlow;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageProducer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.servicediscovery.types.EventBusService;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ServiceFlowBase implements ServiceFlow {

  private final FlowImpl flow;

  public ServiceFlowBase(FlowImpl flow) {
    this.flow = flow;
  }

  @Override
  public void httpRequest(JsonObject filter, HttpMethod method, String requestURI, Handler<AsyncResult<HttpRequest<Buffer>>> handler) {
    String breakerName = filter.getString("name");
    flow.getServiceRef(filter, breakerName, ar -> {
      LookupResult a = ar.result();
      if (ar.succeeded()) {
        WebClient client = a.ref.getAs(WebClient.class);
        HttpRequest<Buffer> proxy = new HttpRequest<Buffer>() {
          HttpRequest<Buffer> req = client.request(method, requestURI);
          @Override
          public HttpRequest<Buffer> method(HttpMethod value) {
            req.method(value);
            return this;
          }
          @Override
          public HttpRequest<Buffer> port(int value) {
            req.port(value);
            return this;
          }
          @Override
          public <U> HttpRequest<U> as(BodyCodec<U> responseCodec) {
            throw new UnsupportedOperationException("todo");
          }
          @Override
          public HttpRequest<Buffer> host(String value) {
            return this;
          }
          @Override
          public HttpRequest<Buffer> uri(String value) {
            return this;
          }
          @Override
          public HttpRequest<Buffer> putHeader(String name, String value) {
            return this;
          }
          @Override
          public MultiMap headers() {
            return req.headers();
          }
          @Override
          public HttpRequest<Buffer> timeout(long value) {
            req.timeout(value);
            return this;
          }
          @Override
          public HttpRequest<Buffer> addQueryParam(String paramName, String paramValue) {
            req.addQueryParam(paramName, paramValue);
            return this;
          }
          @Override
          public HttpRequest<Buffer> setQueryParam(String paramName, String paramValue) {
            req.setQueryParam(paramName, paramValue);
            return this;
          }
          @Override
          public MultiMap queryParams() {
            return req.queryParams();
          }
          @Override
          public HttpRequest<Buffer> copy() {
            req = req.copy();
            return this;
          }
          Handler<AsyncResult<HttpResponse<Buffer>>> wrap(Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            return ar -> {
              a.ref.release();
              if (a.breaker != null) {
                if (ar.succeeded()) {
                  a.breaker.complete();
                } else {
                  a.breaker.fail(ar.cause());
                }
              }
              handler.handle(ar);
            };
          }
          @Override
          public void sendStream(ReadStream<Buffer> body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.sendStream(body, wrap(handler));
          }
          @Override
          public void sendBuffer(Buffer body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.sendBuffer(body, wrap(handler));
          }
          @Override
          public void sendJsonObject(JsonObject body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.sendJsonObject(body, wrap(handler));
          }
          @Override
          public void sendJson(Object body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.sendJson(body, wrap(handler));
          }
          @Override
          public void sendForm(MultiMap body, Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.sendForm(body, wrap(handler));
          }
          @Override
          public void send(Handler<AsyncResult<HttpResponse<Buffer>>> handler) {
            req.send(wrap(handler));
          }
        };
        handler.handle(Future.succeededFuture(proxy));
      } else {

        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }

  @Override
  public void sendMessage(JsonObject filter, Object msg) {
    flow.getServiceRef(filter, filter.getString("name"), ar -> {
      if (ar.succeeded()) {
        LookupResult a = ar.result();
        MessageProducer producer = a.ref.getAs(MessageProducer.class);
        producer.send(msg);
      }
    });
  }

  @Override
  public <T> void getServiceProxy(JsonObject filter, Class<T> clazz, Handler<AsyncResult<T>> handler) {
    flow.getServiceRef(filter, filter.getString("name"), ar -> {
      if (ar.succeeded()) {
        EventBusService.getProxy(flow.discovery, clazz, ar2 -> {
          T t = ar2.result();
          handler.handle(Future.succeededFuture(t));

        });
      }
    });



  }
}
