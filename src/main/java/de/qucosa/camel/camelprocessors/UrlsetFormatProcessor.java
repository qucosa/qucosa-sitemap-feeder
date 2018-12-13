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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.json.simple.JsonObject;

import java.io.IOException;

public class UrlsetFormatProcessor implements Processor {

    public UrlsetFormatProcessor() { }

    @Override
    public void process(Exchange exchange) throws IOException {
        String originalJsonString = exchange.getIn().getBody(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(originalJsonString);
        String tenant = jsonNode.get("tenant").asText();

        ObjectNode urlsetJsonFormat = mapper.createObjectNode();
        urlsetJsonFormat.put("uri", tenant);

        exchange.getIn().setBody(urlsetJsonFormat.toString(), JsonObject.class);
    }
}
