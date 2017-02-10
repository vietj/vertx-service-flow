package com.julienviet.serviceflow.impl;

import com.julienviet.serviceflow.Flow;
import com.julienviet.serviceflow.HttpFlow;
import com.julienviet.serviceflow.MessageFlow;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.Route;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowImpl implements Flow {

  private final Vertx vertx;
  private boolean circuitBreaker;
  private ServiceDiscovery discovery;

  public FlowImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Flow withCircuitBreaker() {
    circuitBreaker = true;
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
      handler.handle(new HttpFlowImpl(ctx, discovery));
    });
    return this;
  }

  @Override
  public <T> Flow from(MessageConsumer<T> apply, Handler<MessageFlow<T>> handler) {
    return null;
  }
}
