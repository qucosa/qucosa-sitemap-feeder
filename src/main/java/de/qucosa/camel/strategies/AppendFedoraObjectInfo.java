/*
 * Copyright (C) 2016 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.camel.strategies;

import de.qucosa.api.EventTypeExtractor;
import de.qucosa.api.UrlObjectBuilder;
import de.qucosa.camel.model.Tenant;
import de.qucosa.events.FedoraUpdateEvent;
import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.w3c.dom.Document;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class AppendFedoraObjectInfo implements AggregationStrategy {

    private List<Tenant> tenants;

    public AppendFedoraObjectInfo(List<Tenant> tenants) {
        this.tenants = tenants;
    }

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        FedoraUpdateEvent event = original.getIn().getBody(FedoraUpdateEvent.class);

        UrlObjectBuilder urlObjectBuilder = new UrlObjectBuilder(
                event,
                resource.getIn().getBody(Document.class),
                tenants
        );

        original.setProperty("objectState", urlObjectBuilder.objectState());
        original.setProperty("eventType", EventTypeExtractor.extract(event.getEventType()));

        try {
            original.getIn().setBody(urlObjectBuilder.sitemapUrlObject());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL encoded error.", e);
        }

        return original;
    }
}
