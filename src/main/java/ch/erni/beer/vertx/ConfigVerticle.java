package ch.erni.beer.vertx;

import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

/**
 * Created by Michal Boska on 2. 12. 2014.
 */
public class ConfigVerticle extends Verticle {

    @Override
    public void start() {
        System.out.println("HTTP port: " + Configuration.getInteger("http.port", container).toString());

    }

}
