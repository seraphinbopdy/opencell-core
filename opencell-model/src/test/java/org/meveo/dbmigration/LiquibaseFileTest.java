package org.meveo.dbmigration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LiquibaseFileTest {

    /**
     * Validate:</br>
     * </br>
     * - There are no duplicate tags in liquibase files</br>
     * - All current/structure.xml changesets are present in rebuild/structure.xml file</br>
     * - Changesets from current/structure.xml do not contain content in rebuild/structure.xml</br>
     * - Changesets from current/structure.xml are not present in rebuild/data.xml</br>
     * - Changesets from current/structure.xml are not present in rebuild/data-scripts.xml</br>
     * - Changesets from current/structure.xml are not present in rebuild/data-reports.xml</br>
     * - Empty changesets from rebuild/structure.xml are not present in current/structure.xml</br>
     * 
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    //@Test
    public void verifyLiquibaseChangesets() throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);

        DocumentBuilder builder = factory.newDocumentBuilder();

        // Load and validate that there are no duplicate tags in liquibase files
        // Changeset ID + author + dbms = Is tag empty
        Map<String, Boolean> rebuildStructureChangeSets = parseAndValidateLiquibaseFileForDuplicates(builder, "db_resources/changelog/rebuild/structure.xml", true);
        Map<String, Boolean> rebuildDataChangeSets = parseAndValidateLiquibaseFileForDuplicates(builder, "db_resources/changelog/rebuild/data.xml", false);
        Map<String, Boolean> rebuildScriptsChangeSets = parseAndValidateLiquibaseFileForDuplicates(builder, "db_resources/changelog/rebuild/data-scripts.xml", false);
        Map<String, Boolean> rebuildReportsChangeSets = parseAndValidateLiquibaseFileForDuplicates(builder, "db_resources/changelog/rebuild/data-reports.xml", false);
        Map<String, Boolean> currentStructureChangeSets = parseAndValidateLiquibaseFileForDuplicates(builder, "db_resources/changelog/current/structure.xml", false);

        // Verify that all current/structure.xml changesets are present in rebuild/structure.xml file
        List<String> missingCurrentChangesets = new ArrayList<String>();
        List<String> notEmpyCurrentChangesets = new ArrayList<String>();
        for (String changeSetId : currentStructureChangeSets.keySet()) {

            Boolean isRebuildChangesetEmpty = rebuildStructureChangeSets.get(changeSetId);

            if (isRebuildChangesetEmpty == null) {
                missingCurrentChangesets.add(changeSetId);
            } else if (!isRebuildChangesetEmpty) {
                notEmpyCurrentChangesets.add(changeSetId);
            }
        }

        for (String changesetId : missingCurrentChangesets) {
            if (changesetId.endsWith("..")) {
                System.out
                    .println("<changeSet id=\"" + changesetId.substring(0, changesetId.indexOf("..")) + "\" author=\"" + changesetId.substring(changesetId.indexOf("..") + 2, changesetId.lastIndexOf("..")) + "\"/>");
            } else {
                System.out.println("<changeSet id=\"" + changesetId.substring(0, changesetId.indexOf("..")) + "\" author=\"" + changesetId.substring(changesetId.indexOf("..") + 2, changesetId.lastIndexOf(".."))
                        + "\" dbms=\"" + changesetId.substring(changesetId.lastIndexOf("..") + 2) + "\"/>");
            }
        }

        assertTrue("The following changesets from current/structure.xml are missing in rebuild/structure.xml: " + missingCurrentChangesets.toString(), missingCurrentChangesets.isEmpty());
        assertTrue("The following changesets from current/structure.xml contain content in rebuild/structure.xml: " + notEmpyCurrentChangesets.toString(), notEmpyCurrentChangesets.isEmpty());

        // Verify that no current/structure.xml changesets are present in rebuild/data.xml, rebuild/data-reports.xml nor rebuild/data-scripts.xml file
        List<String> presentChangesetsInData = new ArrayList<String>();
        List<String> presentChangesetsInScripts = new ArrayList<String>();
        List<String> presentChangesetsInReports = new ArrayList<String>();
        for (String changeSetId : currentStructureChangeSets.keySet()) {

            if (rebuildDataChangeSets.get(changeSetId) != null) {
                presentChangesetsInData.add(changeSetId);
            }
            if (rebuildScriptsChangeSets.get(changeSetId) != null) {
                presentChangesetsInScripts.add(changeSetId);
            }
            if (rebuildReportsChangeSets.get(changeSetId) != null) {
                presentChangesetsInReports.add(changeSetId);
            }
        }

        assertTrue("The following changesets from current/structure.xml are present in rebuild/data.xml: " + presentChangesetsInData.toString(), presentChangesetsInData.isEmpty());
        assertTrue("The following changesets from current/structure.xml are present in rebuild/data-scripts.xml: " + presentChangesetsInScripts.toString(), presentChangesetsInScripts.isEmpty());
        assertTrue("The following changesets from current/structure.xml are present in rebuild/data-reports.xml: " + presentChangesetsInReports.toString(), presentChangesetsInReports.isEmpty());

        // Verify that all empty changesets in rebuild file are present in current structure file
        List<String> emptyRebuildChangesets = rebuildStructureChangeSets.entrySet().stream().filter(entry -> entry.getValue() == true).map(entry -> entry.getKey()).collect(Collectors.toList());

        List<String> missingChangesetsInCurrent = new ArrayList<String>();
        for (String changeSetId : emptyRebuildChangesets) {

            if (currentStructureChangeSets.get(changeSetId) == null) {
                missingChangesetsInCurrent.add(changeSetId);
            }
        }

        assertTrue("The following empty changesets from rebuild/structure.xml are not present in current/structure.xml: " + missingChangesetsInCurrent.toString(), missingChangesetsInCurrent.isEmpty());

    }

    /**
     * Parse and validate a liquibase file
     * 
     * @param builder DOM builder
     * @param filename File to examine
     * @param allowEmpty Are empty changesets allowed
     * @return A map of changesets with Changeset ID + author + dbms as a key and Is tag empty as a value
     * @throws SAXException File parsing exception
     * @throws IOException File parsing exception
     */
    private Map<String, Boolean> parseAndValidateLiquibaseFileForDuplicates(DocumentBuilder builder, String filename, boolean allowEmpty) throws SAXException, IOException {

        Map<String, Boolean> changeSets = new HashMap<>();
        List<String> duplicteChangeSets = new ArrayList<String>();

        Document rebuildStructureXmlDom = builder.parse(new File(Thread.currentThread().getContextClassLoader().getResource(filename).getPath()));

        NodeList nodeList = rebuildStructureXmlDom.getFirstChild().getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {

            Node node = nodeList.item(i);
            if (!"changeSet".equals(node.getNodeName())) {
                continue;
            }

            String changeSetId = node.getAttributes().getNamedItem("id").getNodeValue() + ".." + node.getAttributes().getNamedItem("author").getNodeValue() + ".."
                    + (node.getAttributes().getNamedItem("dbms") != null ? node.getAttributes().getNamedItem("dbms").getNodeValue() : "");

            boolean hasChildren = node.getChildNodes().getLength() > 1 || (node.hasChildNodes() && node.getFirstChild().getNodeType() != Node.TEXT_NODE);

            Boolean oldValue = changeSets.put(changeSetId, !hasChildren);

            if (oldValue != null) {
                duplicteChangeSets.add(changeSetId);
            }

        }

        assertTrue(filename + " contain the folowing duplicate changeset ids: " + duplicteChangeSets.toString(), duplicteChangeSets.isEmpty());

        if (!allowEmpty) {

            List<String> emptyChangesets = changeSets.entrySet().stream().filter(entry -> entry.getValue() == true).map(entry -> entry.getKey()).collect(Collectors.toList());
            assertTrue(filename + " should not contain empty changesets. Empty changeset ids: " + emptyChangesets.toString(), emptyChangesets.isEmpty());
        }

        return changeSets;
    }
}