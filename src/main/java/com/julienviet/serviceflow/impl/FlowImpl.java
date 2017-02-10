package com.julienviet.serviceflow.impl;

import com.julienviet.serviceflow.Flow;
import com.julienviet.serviceflow.HttpFlow;
import com.julienviet.serviceflow.MessageFlow;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowImpl implements Flow {

  final Vertx vertx;
  CircuitBreakerOptions circuitBreakerOptions = new CircuitBreakerOptions();
  ServiceDiscovery discovery;
  final ConcurrentMap<String, CircuitBreaker> breakerMap = new ConcurrentHashMap<>();
  final Function<String, CircuitBreaker> createBreaker = this::createBreaker;

  public FlowImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public CircuitBreaker breaker(String name) {
    return breakerMap.get(name);
  }

  @Override
  public Flow withBreaker(CircuitBreakerOptions options) {
    circuitBreakerOptions = options;
    return this;
  }

  @Override
  public Flow withDiscovery(ServiceDiscovery sd) {
    discovery = sd;
    return this;
  }

  @Override
  public Flow route(Route route, Handler<HttpFlow> handler) {
    route.handler(ctx -> {
      handler.handle(new HttpFlowImpl(this, ctx));
    });
    return this;
  }

  @Override
  public <T> Flow from(MessageConsumer<T> apply, Handler<MessageFlow<T>> handler) {
    return null;
  }

  private CircuitBreaker createBreaker(String breakerName) {
    return CircuitBreaker.create(breakerName, vertx, circuitBreakerOptions);
  }

  void getServiceRef(JsonObject filter, String breakerName, Handler<AsyncResult<LookupResult>> handler) {
    if (breakerName != null) {
      CircuitBreaker breaker = breakerMap.computeIfAbsent(breakerName, createBreaker);
      AtomicBoolean resolved = new AtomicBoolean();
      Future<Void> f = breaker.execute(fut -> getServiceRef(filter, ar -> {
        resolved.set(true);
        if (ar.succeeded()) {
          handler.handle(Future.succeededFuture(new LookupResult(fut, ar.result())));
        } else {
          fut.fail(ar.cause());
          handler.handle(Future.failedFuture(ar.cause()));
        }
      }));
      f.setHandler(ar -> {
        if (resolved.compareAndSet(false, true)) {
          handler.handle(ar.map(null));
        }
      });
    } else {
      getServiceRef(filter, ar -> {
        if (ar.succeeded()) {
          handler.handle(Future.succeededFuture(new LookupResult(null, ar.result())));
        } else {
          handler.handle(Future.failedFuture(ar.cause()));
        }
      });
    }
  }

  private void getServiceRef(JsonObject filter, Handler<AsyncResult<ServiceReference>> handler) {
    discovery.getRecord(filter, ar -> {
      if (ar.succeeded() && ar.result() != null) {
        ServiceReference ref = discovery.<HttpClient>getReference(ar.result());
        handler.handle(Future.succeededFuture(ref));
      } else {
        handler.handle(Future.failedFuture(ar.cause()));
      }
    });
  }
}
