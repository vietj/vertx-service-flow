package com.julienviet.serviceflow;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@RunWith(VertxUnitRunner.class)
public class FlowTest {

  private WebClient client;
  private Vertx vertx;
  private ServiceDiscovery discovery;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx();
    client = WebClient.create(vertx);
    discovery = new DiscoveryImpl(vertx, new ServiceDiscoveryOptions());
    Record record = HttpEndpoint.createRecord("hello-service", "localhost", 8081, "/foo");
    discovery.publish(record, ctx.asyncAssertSuccess());
    vertx.createHttpServer().requestHandler(req -> {
      req.response().end("Hello World");
    }).listen(8081, ctx.asyncAssertSuccess());
  }

  @Test
  public void testHttpToHttp(TestContext ctx) throws Exception {
    Async async = ctx.async();
    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer().requestHandler(router::accept);
    Flow flow = Flow.flow(vertx).withDiscovery(discovery);
    flow.route(router.get("/foo"), httpFlow -> {
      httpFlow.request(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ctx.asyncAssertSuccess(req -> {
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
    client
      .get(8080, "localhost", "/foo")
      .send(ctx.asyncAssertSuccess(resp -> {
        ctx.assertEquals(200, resp.statusCode());
        ctx.assertEquals("Hello World", resp.bodyAsString());
      }));
  }
}
