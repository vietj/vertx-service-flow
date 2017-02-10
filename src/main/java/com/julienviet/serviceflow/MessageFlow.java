package com.julienviet.serviceflow;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.eventbus.Message;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface MessageFlow<T> extends ServiceFlow, Message<T> {
}
