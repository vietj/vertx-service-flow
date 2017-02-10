package com.julienviet.serviceflow;

import com.julienviet.serviceflow.impl.FlowImpl;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.ext.web.Route;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface Flow {

  static Flow flow(Vertx vertx) {
    return new FlowImpl(vertx);
  }

  @Fluent
  Flow withCircuitBreaker();

  @Fluent
  Flow withDiscovery(ServiceDiscovery discovery);

  @Fluent
  Flow route(Route route, Handler<HttpFlow> handler);

  @Fluent
  <T> Flow from(MessageConsumer<T> apply, Handler<MessageFlow<T>> handler);


}
