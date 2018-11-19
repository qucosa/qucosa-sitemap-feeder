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

package com.example.camel.camelprocessors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.json.simple.JsonObject;

import javax.xml.parsers.DocumentBuilderFactory;

public class UrlFormatProcessor implements Processor {
    public UrlFormatProcessor() {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String uriTemplate = "##protocol##://##tenant##.qucosa.de/id/##pid##";
        String originalJsonString = exchange.getIn().getBody(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(originalJsonString);
        String tenant = jsonNode.get("tenant").asText();
        String encodedpid = jsonNode.get("encodedpid").asText();

        // convert ...-date to w3c-datetime for sitemap-entry
        String modifiedDate = jsonNode.get("modifiedDate").asText();

        uriTemplate = uriTemplate.replace("##protocol##", "https")
                .replace("##tenant##", tenant)
                .replace("##pid##", encodedpid);

        ObjectNode urlJsonFormat = mapper.createObjectNode();
        urlJsonFormat.put("loc", uriTemplate);
        urlJsonFormat.put("lastmod", modifiedDate);

        exchange.getIn().setBody(urlJsonFormat.toString(), JsonObject.class);
    }
}
