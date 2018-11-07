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
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.net.URLEncoder;

public class MetsToUrlProcessor implements Processor {
    private final XPathFactory xPathFactory = XPathFactory.newInstance();

    public MetsToUrlProcessor() {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String uriTemplate = "##protocol##://##tenant##.qucosa.de/id/##pid##";

        Document metsDoc = exchange.getIn().getBody(Document.class);
        XPath xPath = XPathFactory.newInstance().newXPath();
        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("mets", "http://www.loc.gov/METS/");
        xPath.setNamespaceContext(namespaceContext);
        // get tenant name
        XPathExpression tenantExpr = xPath.compile("//mets:mets/mets:metsHdr/mets:agent/mets:name");
        String tenantName = tenantExpr.evaluate(metsDoc, XPathConstants.STRING).toString();

        exchange.setProperty("tenant", tenantName);
        String pid = exchange.getProperty("pid", String.class);
        String encodedpid = URLEncoder.encode(pid, "UTF-8");

        uriTemplate = uriTemplate.replace("##protocol##", "http")
                .replace("##tenant##", tenantName)
                .replace("##pid##", encodedpid);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode uriNode = mapper.createObjectNode();
        uriNode.put("loc", uriTemplate);
//        uriNode.put("lastmod", lastmod);
        exchange.setProperty("encodedpid", encodedpid);

        exchange.getIn().setBody(uriNode.toString(), String.class);
    }
}
