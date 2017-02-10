package com.julienviet.serviceflow;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.impl.DiscoveryImpl;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class FlowTest {

  private Record record = HttpEndpoint.createRecord("hello-service", "localhost", 8081, "/foo");
  private WebClient client;
  private Vertx vertx;
  private ServiceDiscovery discovery;
  private HttpServer backend;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    discovery = new DiscoveryImpl(vertx, new ServiceDiscoveryOptions());
  }

  @After
  public void tearDown(TestContext ctx) {
    vertx.close(ctx.asyncAssertSuccess());
  }

  private void publishBackendBlocking(TestContext ctx) {
    Async async = ctx.async();
    publishBackend(ctx, v -> async.complete());
    async.awaitSuccess(10000);
  }

  private void publishBackend(TestContext ctx, Handler<AsyncResult<Void>> completionHandler) {
    discovery.publish(record, ctx.asyncAssertSuccess(v -> completionHandler.handle(null)));
  }

  private void startBackendBlocking(TestContext ctx) {
    if (backend != null) {
      ctx.fail("Backend already started");
    }
    backend = startServerBlocking(ctx, 8081, req -> req.response().end("Hello World"));
  }

  private void startBackend(TestContext ctx, Handler<Void> completionHandler) {
    if (backend != null) {
      ctx.fail("Backend already started");
    }
    startServer(ctx, 8081, req -> req.response().end("Hello World"), result -> {
      backend = result;
      completionHandler.handle(null);
    });
  }

  private HttpServer startServerBlocking(TestContext ctx, int port, Handler<HttpServerRequest> requestHandler) {
    Async async = ctx.async();
    HttpServer server = vertx.createHttpServer()
      .requestHandler(requestHandler)
      .listen(port, ctx.asyncAssertSuccess(v -> async.complete()));
    async.awaitSuccess(10000);
    return server;
  }

  private void startServer(TestContext ctx, int port, Handler<HttpServerRequest> requestHandler, Handler<HttpServer> completionHandler) {
    HttpServer server = vertx.createHttpServer().requestHandler(requestHandler);
    server.listen(port, ctx.asyncAssertSuccess(completionHandler));
  }

  @Test
  public void testHttpToHttp(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
    Flow flow = Flow.flow(vertx)
      .withDiscovery(discovery);
    flow.route(router.get("/foo"), httpFlow -> {
      httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertSuccess(req -> {
        req.send(ctx.asyncAssertSuccess(resp -> {
          ctx.assertEquals(200, resp.statusCode());
          httpFlow.response().end(resp.body());
          async.complete();
        }));
      }));
    });
    Async listenAsync = ctx.async();
    server.listen(8080, ctx.asyncAssertSuccess(v -> listenAsync.complete()));
    listenAsync.awaitSuccess(10000);
    startBackendBlocking(ctx);
    publishBackendBlocking(ctx);
    client
      .get(8080, "localhost", "/foo")
      .send(ctx.asyncAssertSuccess(resp -> {
        ctx.assertEquals(200, resp.statusCode());
        ctx.assertEquals("Hello World", resp.bodyAsString());
      }));
  }

  @Test
  public void testCircuitBreakerOpen(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
    Flow flow = Flow.flow(vertx)
      .withDiscovery(discovery);
    flow.route(router.get("/foo"), httpFlow -> {
      AtomicInteger count = new AtomicInteger(6);
      doRec(6, fut -> {
        httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertFailure(v1 -> {
          CircuitBreakerState state = flow.breaker("hello-service").state();
          int val = count.decrementAndGet();
          if (val == 0) {
            ctx.assertEquals(CircuitBreakerState.OPEN, state);
            httpFlow.response().end("failed");
            async.complete();
          } else if (val == 1) {
            ctx.assertEquals(CircuitBreakerState.OPEN, state);
          } else {
            ctx.assertEquals(CircuitBreakerState.CLOSED, state);
          }
          fut.complete();
        }));
      });
    });
    Async listenAsync = ctx.async();
    server.listen(8080, ctx.asyncAssertSuccess(v -> listenAsync.complete()));
    listenAsync.awaitSuccess(10000);
    client
      .get(8080, "localhost", "/foo")
      .send(ctx.asyncAssertSuccess(resp -> {
        ctx.assertEquals(200, resp.statusCode());
        ctx.assertEquals("failed", resp.bodyAsString());
      }));
  }

  private void doRec(int num, Handler<Future<Void>> handler) {
    if (num > 0) {
      vertx.runOnContext(v -> {
        Future<Void> fut = Future.future();
        fut.setHandler(ar -> {
          doRec(num -1, handler);
        });
        handler.handle(fut);
      });
    }
  }

  @Test
  public void testCircuitBreakerLookupFail(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
    Flow flow = Flow.flow(vertx)
      .withDiscovery(discovery)
      .withBreaker(new CircuitBreakerOptions());
    flow.route(router.get("/foo"), httpFlow -> {
      httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertFailure(v1 -> {
        publishBackend(ctx, v2 -> {
          httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertSuccess(req -> {
            req.send(ctx.asyncAssertSuccess(resp -> {
              ctx.assertEquals(200, resp.statusCode());
              httpFlow.response().end(resp.body());
              async.complete();
            }));
          }));
        });
      }));
    });
    Async listenAsync = ctx.async();
    server.listen(8080, ctx.asyncAssertSuccess(v -> listenAsync.complete()));
    listenAsync.awaitSuccess(10000);
    startBackendBlocking(ctx);
    client
      .get(8080, "localhost", "/foo")
      .send(ctx.asyncAssertSuccess(resp -> {
        ctx.assertEquals(200, resp.statusCode());
        ctx.assertEquals("Hello World", resp.bodyAsString());
      }));
  }

  @Test
  public void testCircuitBreakerServerFail(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
    Flow flow = Flow.flow(vertx)
      .withDiscovery(discovery)
      .withBreaker(new CircuitBreakerOptions());
    flow.route(router.get("/foo"), httpFlow -> {
      AtomicInteger num = new AtomicInteger(5);
      for (int i = 0;i < 5;i++) {
        httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertSuccess(req1 -> {
          req1.send(ctx.asyncAssertFailure(v1 -> {
            if (num.decrementAndGet() == 0) {
              CircuitBreaker breaker = flow.breaker("hello-service");
              ctx.assertNotNull(breaker);
              ctx.assertEquals(CircuitBreakerState.OPEN, breaker.state());
              breaker.reset();
              startBackend(ctx, v2 -> {
                httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertSuccess(req2 -> {
                  req2.send(ctx.asyncAssertSuccess(resp -> {
                    ctx.assertEquals(200, resp.statusCode());
                    httpFlow.response().end(resp.body());
                    async.complete();
                  }));
                }));
              });
            }
          }));
        }));
      }
    });
    Async listenAsync = ctx.async();
    server.listen(8080, ctx.asyncAssertSuccess(v -> listenAsync.complete()));
    listenAsync.awaitSuccess(10000);
    publishBackendBlocking(ctx);
    client
      .get(8080, "localhost", "/foo")
      .send(ctx.asyncAssertSuccess(resp -> {
        ctx.assertEquals(200, resp.statusCode());
        ctx.assertEquals("Hello World", resp.bodyAsString());
      }));
  }
}
