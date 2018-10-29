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

package com.example.camel.sitemapmodel;

import java.util.Objects;

public class Urlset {
    private String uri;
    private String loc;
    private String lastmod;

//    @JsonManagedReference
//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    private List<Url> urlList;

    public Urlset(String uri) {
        this.uri = uri;
    }

    public Urlset() { super(); }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

//    public List<Url> getUrlList() {
//        return urlList;
//    }
//
//    public void addUrl(Url url) {
//        this.urlList.add(url);
//    }
//
//    public void removeUrl(Url url) {
//        this.urlList.remove(url);
//    }
//
//    public void setUrlList(List<Url> urlList) {
//        this.urlList = urlList;
//    }

    public String getLastmod() {
        return lastmod;
    }

    public void setLastmod(String lastmod) {
        this.lastmod = lastmod;
    }

    public String getLoc() {
        return loc;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Urlset urlset = (Urlset) o;
        return Objects.equals(uri, urlset.uri);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uri);
    }
}
