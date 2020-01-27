package de.qucosa.camel;

import de.qucosa.camel.routebuilders.SitemapFeederRoutes;
import org.apache.camel.main.Main;

public class CamelApplication {

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        if (args.length > 0) {
            main.setPropertyPlaceholderLocations("classpath:" + args[0]);
        } else {
            main.setPropertyPlaceholderLocations("classpath:application-dev.properties");
        }

        main.addRouteBuilder(new SitemapFeederRoutes());
        main.run();
    }
}
