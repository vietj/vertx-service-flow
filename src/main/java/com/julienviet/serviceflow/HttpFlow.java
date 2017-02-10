package com.julienviet.serviceflow;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface HttpFlow extends ServiceFlow, RoutingContext {

  @Override
  HttpFlow put(String key, Object obj);

  @Override
  HttpFlow addCookie(Cookie cookie);

}
