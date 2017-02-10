package examples;

import com.julienviet.rxjava.serviceflow.Flow;
import com.julienviet.rxjava.serviceflow.ServiceFlow;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.codec.BodyCodec;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class RxExample {

  private static Router buildRouter() {
    throw new UnsupportedOperationException();
  }

  private static EventBus buildEventBus() {
    throw new UnsupportedOperationException();
  }

  private static ServiceFlow flow() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();
    Router router = buildRouter();
    EventBus eventBus = buildEventBus();

    Flow flow = Flow.flow(vertx);

    // Simple
    flow.withDiscovery(null).route(router.get("/foo/bar"), httpFlow -> {
      httpFlow.rxHttpRequest(new JsonObject(), HttpMethod.GET, "/")
        .subscribe(req -> {
        req.addQueryParam("foo", httpFlow.pathParam("foo")).rxSend().subscribe(resp -> {

        });
      });
    });

    // Simple with fallback
    flow.withDiscovery(null).route(router.get("/foo/bar"), httpFlow -> {
      httpFlow.rxHttpRequest(new JsonObject(), HttpMethod.GET, "/")
        .flatMap(req -> req.addQueryParam("foo", httpFlow.pathParam("foo")).as(BodyCodec.string()).rxSend())
        .map(HttpResponse::body)
        .onErrorReturn(err -> "use the fallback").subscribe(s -> {
        System.out.println("result is " + s);
      });
    });

    // Nested
    flow.route(router.get("/foo/bar"), httpFlow -> {
      httpFlow.rxHttpRequest(new JsonObject(), HttpMethod.GET, "/").subscribe(req -> {
        req.addQueryParam("foo", "bar").rxSend().subscribe(resp -> {
          //
        }, err -> {
          httpFlow.sendMessage(new JsonObject(), "failed");
        });
      });
    });

    // Parallel
    flow.from(eventBus.<String>consumer("foobar"), messageFlow -> {
      messageFlow.rxHttpRequest(new JsonObject(), HttpMethod.GET, "/").subscribe(req -> {
        req.addQueryParam("foo", messageFlow.body()).rxSend().subscribe(resp -> {
          //
        });
      });
      messageFlow.sendMessage(new JsonObject(), messageFlow.body());
    });
  }
}
