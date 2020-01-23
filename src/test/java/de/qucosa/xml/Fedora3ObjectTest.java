package de.qucosa.xml;

import de.qucosa.camel.utils.DocumentXmlUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test read fedora 3 object xml.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Fedora3ObjectTest {

    @Test
    public void readObjectXml() {
        Document document = DocumentXmlUtils.document(getClass().getResourceAsStream("/fedora/qucosa:12164.xml"), true);
        assertNotNull(document);
        assertNotNull(document.getDocumentElement());
        assertTrue(document.getElementsByTagName("objectProfile").getLength() > 0);
        assertTrue(document.getElementsByTagName("objOwnerId").getLength() > 0);
        assertTrue(document.getElementsByTagName("objState").getLength() > 0);
    }
}
