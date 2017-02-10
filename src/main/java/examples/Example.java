package examples;

import com.julienviet.serviceflow.Flow;
import com.julienviet.serviceflow.ServiceFlow;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Example {

  private static Router buildRouter() {
    throw new UnsupportedOperationException();
  }

  private static EventBus buildEventBus() {
    throw new UnsupportedOperationException();
  }

  private static ServiceFlow flow() {
    throw new UnsupportedOperationException();
  }

  private static ServiceDiscovery buildDiscovery() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();
    Router router = buildRouter();
    EventBus eventBus = buildEventBus();
    ServiceDiscovery discovery = buildDiscovery();

    Flow flow = Flow
      .flow(vertx)
      .withDiscovery(discovery);

    // Simple
    flow.route(router.get("/foo/bar"), httpFlow -> {
      httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ar -> {
        if (ar.succeeded()) {
          ar.result().addQueryParam("foo", httpFlow.pathParam("foo")).send(ar2 -> {
            //
          });
        } else {
          // Use fallback
        }
      });
    });

    // Nested
    flow.route(router.get("/foo/bar"), httpFlow -> {
      httpFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ar -> {
        if (ar.succeeded()) {
          ar.result().addQueryParam("foo", "bar").send(ar2 -> {
            if (ar.succeeded()) {
              //
            } else {
              httpFlow.sendMessage(new JsonObject().put("name", "some-eventbus-service"), "failed");
            }
          });
        }
      });
    });

    // Parallel
    flow.from(eventBus.<String>consumer("foobar"), messageFlow -> {
      messageFlow.httpRequest(new JsonObject().put("name", "hello-service"), HttpMethod.GET, "/", ar -> {
        if (ar.succeeded()) {
          ar.result().addQueryParam("foo", messageFlow.body()).send(ar2 -> {
            //
          });
        }
      });
      messageFlow.sendMessage(new JsonObject().put("name", "some-eventbus-service"), messageFlow.body());
    });

  }
}
