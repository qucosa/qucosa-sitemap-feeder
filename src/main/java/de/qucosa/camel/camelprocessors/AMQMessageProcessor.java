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

package de.qucosa.camel.camelprocessors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.json.simple.JsonObject;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.net.URLEncoder;

public class AMQMessageProcessor implements Processor {
    private final XPath xPath =  XPathFactory.newInstance().newXPath();
    private final String pidXpath = "/atom:entry/atom:summary[@type='text']/text()";
    private final String dsidXpath = "/atom:entry/atom:category[@scheme='fedora-types:dsID']/@term";
    private final String methodXpath = "/atom:entry/atom:title[@type='text']/text()";
    private final String dateXpath = "/atom:entry/atom:updated/text()";

    public AMQMessageProcessor() {
        SimpleNamespaceContext namespaceContext = new SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("atom", "http://www.w3.org/2005/Atom");
        xPath.setNamespaceContext(namespaceContext);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Document amqMessageDoc = exchange.getIn().getBody(Document.class);

        String pid = xPath.compile(pidXpath)
                .evaluate(amqMessageDoc, XPathConstants.STRING).toString();
        String dsid = xPath.compile(dsidXpath)
                .evaluate(amqMessageDoc, XPathConstants.STRING).toString();
        String method = xPath.compile(methodXpath)
                .evaluate(amqMessageDoc, XPathConstants.STRING).toString();
        String modifiedDate = xPath.compile(dateXpath)
                .evaluate(amqMessageDoc, XPathConstants.STRING).toString();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode amqMessageInfoAsJson = mapper.createObjectNode();

        String encodedpid = URLEncoder.encode(pid, "UTF-8");

        amqMessageInfoAsJson.put("pid", pid);
        amqMessageInfoAsJson.put("encodedpid", encodedpid);
        amqMessageInfoAsJson.put("dsid", dsid);
        amqMessageInfoAsJson.put("method", method);
        amqMessageInfoAsJson.put("modifiedDate", modifiedDate);

        exchange.getIn().setBody(amqMessageInfoAsJson.toString(), JsonObject.class);
    }
}
