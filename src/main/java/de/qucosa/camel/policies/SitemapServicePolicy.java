package de.qucosa.camel.policies;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.apache.http.conn.HttpHostConnectException;

import static de.qucosa.camel.config.RouteIds.SITEMAP_SERVICE_OBSERVER_ID;

public class SitemapServicePolicy extends WebServicePolicyAbstract {

    @Override
    public void onExchangeBegin(Route route, Exchange exchange) {
        super.onExchangeBegin(route, exchange);
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {
        super.onExchangeDone(route, exchange);
        CamelContext context = exchange.getContext();

        if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) instanceof HttpHostConnectException) {

            if (context.getRouteStatus(SITEMAP_SERVICE_OBSERVER_ID).isStopped()) {
                context.createFluentProducerTemplate()
                        .to("controlbus:route?action=start&routeId=" + SITEMAP_SERVICE_OBSERVER_ID).send();
            }

            stopRoutes(context);
        } else {

            if (context.getRouteStatus(SITEMAP_SERVICE_OBSERVER_ID).isStarted()) {
                context.createFluentProducerTemplate()
                        .to("controlbus:route?action=stop&routeId=" + SITEMAP_SERVICE_OBSERVER_ID).send();
            }

            startRoutes(context);
        }
    }
}
