package org.example.drumboiler.fmu;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads {@code modelDescription.xml} from a Functional Mock-up Unit (FMU) archive and extracts ScalarVariables.
 */
public class FmuParser {

    private static final String MODEL_DESCRIPTION_FILE = "modelDescription.xml";

    /**
     * Reads the FMU zip archive, parses {@code modelDescription.xml}, and returns all ScalarVariables.
     *
     * @param fmuPath location of the FMU archive
     * @return variables declared in the FMU
     * @throws IOException if the archive cannot be read
     */
    public List<FmuVariable> parseModelDescription(Path fmuPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(fmuPath.toFile())) {
            ZipEntry modelDescription = zipFile.getEntry(MODEL_DESCRIPTION_FILE);
            if (modelDescription == null) {
                throw new IOException("FMU archive does not contain " + MODEL_DESCRIPTION_FILE);
            }
            try (InputStream stream = zipFile.getInputStream(modelDescription);
                 Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Document document = buildDocument(reader);
                return extractScalarVariables(document);
            } catch (ParserConfigurationException e) {
                throw new IOException("Could not configure XML parser", e);
            } catch (SAXException e) {
                throw new IOException("Malformed modelDescription.xml", e);
            }
        }
    }

    private Document buildDocument(Reader reader) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(reader));
    }

    private List<FmuVariable> extractScalarVariables(Document document) {
        List<FmuVariable> variables = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("ScalarVariable");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            String name = element.getAttribute("name");
            String valueReference = element.getAttribute("valueReference");
            String causality = element.getAttribute("causality");
            String variability = element.getAttribute("variability");
            String type = null;
            String unit = null;

            NodeList children = element.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element childElement = (Element) child;
                type = childElement.getTagName();
                String declaredType = attributeOrNull(childElement, "declaredType");
                String unitAttr = attributeOrNull(childElement, "unit");
                if (unitAttr != null) {
                    unit = unitAttr;
                } else if (declaredType != null) {
                    unit = declaredType;
                }
                break;
            }

            variables.add(new FmuVariable(
                    name,
                    valueReference,
                    type == null ? "Real" : type,
                    unit,
                    causality,
                    variability
            ));
        }
        return variables;
    }

    private String attributeOrNull(Element element, String name) {
        if (!element.hasAttribute(name)) {
            return null;
        }
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? null : value;
    }
}
