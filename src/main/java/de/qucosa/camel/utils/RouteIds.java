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

package de.qucosa.camel.utils;

public class RouteIds {
    public static final String ACTIVEMQ_ROUTE = "amq_update_event_route";
    public static final String KAFKA_BULK_INSERT_ROUTE = "kafka_bulk_insert_route";
    public static final String KAFKA_BULK_DELETE_ROUTE = "kafka_bulk_delete_route";
    public static final String SITEMAP_FEEDER_ROUTE = "sitemap_feeder";
    public static final String APPEND_FEDORA_OBJ_INFO = "append_fedora_object_info";
    public static final String HTTP_INGEST_ID = "ingest";
    public static final String HTTP_ADD_DATASTREAM_ID = "add_datastream";
    public static final String HTTP_PURGE_OBJECT_ID = "purge_object";
    public static final String HTTP_MODIFY_OBJECT_ID = "modify_object";
}
