package examples;

import com.julienviet.rxjava.serviceflow.Flow;
import com.julienviet.rxjava.serviceflow.ServiceFlow;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.ext.web.Router;

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
    flow.withCircuitBreaker().withDiscovery(null).fromRoute(router.get("/foo/bar"), httpFlow -> {
      httpFlow.rxHttpGet(new JsonObject()).subscribe(req -> {
        req.addQueryParam("foo", httpFlow.pathParam("foo")).rxSend().subscribe(resp -> {

        });
      });
    });

    // Nested
    flow.fromRoute(router.get("/foo/bar"), httpFlow -> {
      httpFlow.rxHttpGet(new JsonObject()).subscribe(req -> {
        req.addQueryParam("foo", "bar").rxSend().subscribe(resp -> {
          //
        }, err -> {
          httpFlow.sendMessage(new JsonObject(), "failed");
        });
      });
    });

    // Parallel
    flow.fromEventBus(eventBus.<String>consumer("foobar"), messageFlow -> {
      messageFlow.rxHttpGet(new JsonObject()).subscribe(req -> {
        req.addQueryParam("foo", messageFlow.body()).rxSend().subscribe(resp -> {
          //
        });
      });
      messageFlow.sendMessage(new JsonObject(), messageFlow.body());
    });

  }
}
