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

import de.qucosa.camel.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.json.simple.JsonObject;

import java.net.URLEncoder;

public class SetupJsonForBulkInsert implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectInfoAsJson = mapper.createObjectNode();

        String pid = exchange.getIn().getBody(String.class);
        String encodedpid = URLEncoder.encode(pid, "UTF-8");

        objectInfoAsJson.put("pid", pid);
        objectInfoAsJson.put("encodedpid", encodedpid);
        objectInfoAsJson.put("method", "ingest");
        objectInfoAsJson.put("modifiedDate", Utils.getCurrentW3cDatetime());

        exchange.getIn().setBody(objectInfoAsJson.toString(), JsonObject.class);
    }
}
