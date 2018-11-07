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

package com.example.camel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.json.simple.JsonObject;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class MetsToUrlsetProcessor implements Processor {
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public MetsToUrlsetProcessor() { }

    @Override
    public void process(Exchange exchange) throws Exception {
        Document metsDoc = exchange.getIn().getBody(Document.class);
        XPath xPath = xPathFactory.newXPath();
        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("mets", "http://www.loc.gov/METS/");
        xPath.setNamespaceContext(namespaceContext);
        // get tenant name
        XPathExpression tenantExpr = xPath.compile("//mets:mets/mets:metsHdr/mets:agent/mets:name");
        String tenantName = tenantExpr.evaluate(metsDoc, XPathConstants.STRING).toString();

        exchange.setProperty("tenant", tenantName);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode uriNode = mapper.createObjectNode();
        uriNode.put("uri", tenantName);

        exchange.getIn().setBody(uriNode.toString(), JsonObject.class);
    }
}
