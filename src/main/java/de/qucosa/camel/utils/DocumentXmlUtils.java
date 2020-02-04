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

import org.apache.commons.io.IOUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DocumentXmlUtils {
    @SuppressWarnings("CanBeFinal") // initialization happens in static block
    private static DocumentBuilder db;

    static {
        try {
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            db = domFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static <T> Document document(T source, boolean namespaceAware) {
        Document document = null;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(namespaceAware);

        try {
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

            if (source == null) {
                document = documentBuilder.newDocument();
            } else if (source instanceof InputStream) {
                document = documentBuilder.parse((InputStream) source);
            } else if (source instanceof String) {
                document = documentBuilder.parse((String) source);
            } else if (source instanceof InputSource) {
                document = documentBuilder.parse((InputSource) source);
            } else if (source instanceof File) {
                document = documentBuilder.parse((File) source);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return document;
    }

    public static XPath xpath(Map<String, String> namespaces) {
        XPath xPath = XPathFactory.newInstance().newXPath();

        if (namespaces != null && !namespaces.isEmpty()) {
            xPath.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        }

        return xPath;
    }

    public static String resultXml(Document document) throws IOException {
        OutputFormat outputFormat = new OutputFormat(document);
        outputFormat.setOmitXMLDeclaration(true);
        StringWriter stringWriter = new StringWriter();
        XMLSerializer serialize = new XMLSerializer(stringWriter, outputFormat);
        serialize.serialize(document);
        return stringWriter.toString();
    }

    public static Element node(InputStream stream) throws ParserConfigurationException, IOException, SAXException {
        Element element;
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);

        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        Document document = documentBuilder.parse(stream);
        element = document.getDocumentElement();

        return element;
    }

    public static Element node(String input) throws IOException, SAXException, ParserConfigurationException {
        return node(IOUtils.toInputStream(input, Charset.defaultCharset()));
    }

    public static String documentToString(Document doc) throws Exception{
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    public static Document stringToDocument(String xmlString) throws IOException, SAXException {
        return db.parse(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8)));
    }
}

