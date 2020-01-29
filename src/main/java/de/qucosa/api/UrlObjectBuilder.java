package de.qucosa.api;

import de.qucosa.camel.model.Tenant;
import de.qucosa.camel.model.Url;
import de.qucosa.camel.utils.DateTimeConverter;
import de.qucosa.camel.utils.DocumentXmlUtils;
import de.qucosa.events.FedoraUpdateEvent;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

public class UrlObjectBuilder {

    private final List<Tenant> tenants;

    private final Document document;

    private final FedoraUpdateEvent event;

    private final XPath xPath = DocumentXmlUtils.xpath(Collections.singletonMap("obj", "http://www.fedora.info/definitions/1/0/access/"));

    public UrlObjectBuilder(FedoraUpdateEvent event, Document document, List<Tenant> tenants) {
        this.document = document;
        this.event = event;
        this.tenants = tenants;
    }

    public Url sitemapUrlObject() throws UnsupportedEncodingException {
        Url url = new Url();

        try {
//            String fedoraTenantName = xPath.compile("//obj:objectProfile/obj:objOwnerId/text()")
//                    .evaluate(document, XPathConstants.STRING).toString();
            String fedoraTenantName = xPath.compile("//objectProfile/objOwnerId/text()")
                    .evaluate(document, XPathConstants.STRING).toString();
            Tenant tenant = tenant(fedoraTenantName);

            url.setUrlSetUri(tenant.getSmall());
            url.setLoc("https://" + tenant.getSmall() + "/id/" + URLEncoder.encode(event.getIdentifier(), "UTF-8"));
            url.setLastmod(DateTimeConverter.getCurrentW3cDatetime());
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Object ownner ID is not exist in document.", e);
        }

        return url;
    }

    public String objectState() {
        try {
            return xPath.compile("//objectProfile/objState/text()").evaluate(document, XPathConstants.STRING).toString();
//            return xPath.compile("//obj:objectProfile/obj:objState/text()").evaluate(document, XPathConstants.STRING).toString();
        } catch (XPathExpressionException e) {
            throw new RuntimeException("Object state is not exist in document.", e);
        }
    }

    private Tenant tenant(String fedoraTenantName) {
        Tenant tenant = new Tenant();

        for (Tenant obj : tenants) {

            if (obj.getName().equals(fedoraTenantName)) {
                tenant = obj;
            }
        }

        if (tenant.getSmall() == null || tenant.getHost() == null) {
            throw new RuntimeException("Tenant " + fedoraTenantName + " has failed properties.");
        }

        if (tenant.getSmall().isEmpty() || tenant.getHost().isEmpty()) {
            throw new RuntimeException("Tenant object " + fedoraTenantName + " has empty property values.");
        }

        return tenant;
    }
}
