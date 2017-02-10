package com.julienviet.serviceflow.impl;

import io.vertx.core.Future;
import io.vertx.servicediscovery.ServiceReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class LookupResult {

  final Future<Void> breaker;
  final ServiceReference ref;

  LookupResult(Future<Void> breaker, ServiceReference ref) {
    this.breaker = breaker;
    this.ref = ref;
  }
}
