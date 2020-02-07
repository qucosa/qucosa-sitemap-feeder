package de.qucosa.camel.policies;

import org.apache.camel.CamelContext;
import org.apache.camel.support.RoutePolicySupport;

import static de.qucosa.camel.config.RouteIds.FEDORA_3_OBJECTINFO_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_DELETE_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_BULK_INSERT_ID;
import static de.qucosa.camel.config.RouteIds.KAFKA_SITEMAP_CONSUMER_ID;

public class WebServicePolicyAbstract extends RoutePolicySupport {

    protected void stopRoutes(CamelContext context) {

        if (context.getRouteStatus(FEDORA_3_OBJECTINFO_ID).isStarted()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=stop&routeId=" + FEDORA_3_OBJECTINFO_ID).send();
        }

        if (context.getRouteStatus(KAFKA_SITEMAP_CONSUMER_ID).isStarted()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=stop&routeId=" + KAFKA_SITEMAP_CONSUMER_ID).send();
        }

        if (context.getRouteStatus(KAFKA_BULK_INSERT_ID).isStarted()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=stop&routeId=" + KAFKA_BULK_INSERT_ID).send();
        }

        if (context.getRouteStatus(KAFKA_BULK_DELETE_ID).isStarted()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=stop&routeId=" + KAFKA_BULK_DELETE_ID).send();
        }
    }

    protected void startRoutes(CamelContext context) {

        if (context.getRouteStatus(FEDORA_3_OBJECTINFO_ID).isStopped()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=start&routeId=" + FEDORA_3_OBJECTINFO_ID).send();
        }

        if (context.getRouteStatus(KAFKA_SITEMAP_CONSUMER_ID).isStopped()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=start&routeId=" + KAFKA_SITEMAP_CONSUMER_ID).send();
        }

        if (context.getRouteStatus(KAFKA_BULK_INSERT_ID).isStopped()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=start&routeId=" + KAFKA_BULK_INSERT_ID).send();
        }

        if (context.getRouteStatus(KAFKA_BULK_DELETE_ID).isStopped()) {
            context.createFluentProducerTemplate()
                    .to("controlbus:route?action=start&routeId=" + KAFKA_BULK_DELETE_ID).send();
        }
    }
}
