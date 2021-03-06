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

package de.qucosa.camel.config;

public class RouteIds {
    public static final String KAFKA_SITEMAP_CONSUMER_ID = "sitemap_consumer_id";
    public static final String SITEMAP_CONSUMER_APPEND_OBJ_INFO = "sitemap_consumer_append_obj_info";
    public static final String BULK_INSERT_APPEND_OBJ_INFO = "bulk_insert_append_obj_info";
    public static final String FEDORA_3_OBJECTINFO_ID = "fedora_3_object_info_id";
    public static final String SITEMAP_CREATE_URL_ID = "sitemap_create_url_id";
    public static final String SITEMAP_DELETE_URL_ID = "sitemap_delete_url_id";
    public static final String SITEMAP_CONSUMER_PUSH_TO_SERVICE = "sitemap_consumer_push_to_service";
    public static final String BULK_INSERT_PUSH_TO_SERVICE = "bulk_insert_push_to_service";
    public final static String KAFKA_BULK_INSERT_ID = "bulk_insert_consumer_id";
    public final static String KAFKA_BULK_DELETE_ID = "bulk_delete_consumer_id";
    public final static String BULK_DELETE_PUSH_TO_SERVICE = "bulk_delete_push_to_service";
    public final static String BULK_DELETE_APPEND_OBJ_INFO = "bulk_delete_append_obj_info";
    public static final String FEDORA_SERVICE_OBSERVER_ID = "fedora_service_observer_id";
    public static final String SITEMAP_SERVICE_OBSERVER_ID = "sitemap_service_observer_id";
}
