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

package de.qucosa.camel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.qucosa.camel.model.Tenant;
import org.apache.camel.Exchange;
import org.apache.camel.json.simple.JsonObject;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.List;

public class AppendFedoraObjectInfo implements AggregationStrategy {
    private final XPath xPath =  XPathFactory.newInstance().newXPath();
    List<Tenant> tenants;

    public AppendFedoraObjectInfo(List<Tenant> tenants) {
        this.tenants = tenants;
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("obj", "http://www.fedora.info/definitions/1/0/access/");
        xPath.setNamespaceContext(namespaceContext);
    }

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        String originalJsonBody = original.getIn().getBody(String.class);
        Document fedoraObjectInformationResponse = resource.getIn().getBody(Document.class);

        String fedoraTenantName = null;
        String fedoraObjectState = null;

        try {
            fedoraTenantName = xPath.compile("//obj:objectProfile/obj:objOwnerId/text()")
                    .evaluate(fedoraObjectInformationResponse, XPathConstants.STRING).toString();
            fedoraObjectState = xPath.compile("//obj:objectProfile/obj:objState/text()")
                    .evaluate(fedoraObjectInformationResponse, XPathConstants.STRING).toString();
        } catch (XPathExpressionException e) {
            System.out.println("error getting tenant/objOwnerId for object.");
        }

        Tenant tenant = null;

        for (Tenant obj : tenants) {

            if (obj.getName().equals(fedoraTenantName)) {
                tenant = obj;
            }
        }

        if (tenant == null) {
            throw new RuntimeException("Cannot found fedora tenant " + fedoraTenantName + " in tenatns config.");
        }

        if (tenant.getSmall() == null || tenant.getSmall().isEmpty() ||
            tenant.getLarge() == null || tenant.getLarge().isEmpty()) {
            throw new RuntimeException("Small or large name definition for tenant " + fedoraTenantName + " failed.");
        }

        if (fedoraObjectState == null) {
            throw new RuntimeException("Fedora Object state missing.");
        }

        // append tenant (urlset-name)
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonInfo = mapper.readTree(originalJsonBody);
            ObjectNode node = (ObjectNode) jsonInfo;

            node.put("tenant_urlset", tenant.getSmall());
            node.put("tenant_url", tenant.getLarge());
            node.put("objectState", fedoraObjectState);

            original.getIn().setBody(node.toString(), JsonObject.class);
        } catch (IOException e) {
            System.out.println("Problem reading json-info from exchange-body.");
        }

        return original;
    }
}
