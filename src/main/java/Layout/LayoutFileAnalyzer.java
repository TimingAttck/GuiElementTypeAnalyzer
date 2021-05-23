package Layout;

import ResourceTracker.resources.VariableReference;
import soot.jimple.infoflow.android.axml.AXmlDocument;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The LayoutFileAnalyzer class analyses a Layout File.
 * It recursively traverses the hierarchy of the Layout file
 * parses the file and each contained Gui element and Attribute
 * and links references made on the Gui Element in the Java Code
 * to the respective Gui Element.
 */
public class LayoutFileAnalyzer {

    private LayoutFile layoutFile;
    private final byte[] xmlFile;
    private final LayoutFileAnalyzerConfig layoutFileAnalyzerConfig;
    private GuiElementConfig guiElementConfig;

    private Map<Integer, Set<VariableReference>> resourceIdToVariablesMap;
    private Map<VariableReference, Set<Integer>> variablesToResourceIdMap;

    private Map<GuiElement, Set<VariableReference>> guiElementsToVariablesMap;
    private Map<VariableReference, Set<GuiElement>> variablesToGuiElementsMap;

    private Set<GuiElement> guiElements;

    /**
     * @param xmlFile The byte array of the raw XML file
     * @param path The full path of the layout file inside the apk
     * @param layoutFileAnalyzerConfig A LayoutFileAnalyzerConfig instance that dictates which Attributes of a Gui Element to parse
     * @param resourceIdToVariablesMap The Resource Id to Variables Map returned by the Resource Tracker analysis
     */
    public LayoutFileAnalyzer(byte[] xmlFile, String path, LayoutFileAnalyzerConfig layoutFileAnalyzerConfig, Map<Integer, Set<VariableReference>> resourceIdToVariablesMap) {
        this.layoutFile = new LayoutFile();
        File f = new File(path);
        this.layoutFile.setName(f.getName());
        this.layoutFile.setFullName(path);
        this.xmlFile = xmlFile;
        this.layoutFileAnalyzerConfig = layoutFileAnalyzerConfig;
        guiElementConfig = new GuiElementConfig();
        guiElementConfig.setAttributesToParse(layoutFileAnalyzerConfig.attributesToParse);
        guiElementConfig.setFilterAttributes(!layoutFileAnalyzerConfig.attributesToParse.isEmpty());
        this.resourceIdToVariablesMap = resourceIdToVariablesMap;
        this.guiElementsToVariablesMap = new HashMap<>();
        this.variablesToResourceIdMap = new HashMap<>();
        this.variablesToGuiElementsMap = new HashMap<>();
        this.guiElements = new HashSet<>();
    }

    /**
     * Starts the analysis of the Layout file.
     * It traverses the Hierarchy of the Layout file
     * parses all AXmlNodes and AXmlAttributes and add links from
     * a Gui Element to those variables that use the particular
     * Gui Elements in the code
     *
     * @throws IOException
     */
    public void analyzeLayoutFile() throws IOException {

        AXML20Parser axml20Parser = new AXML20Parser();
        axml20Parser.parseFile(xmlFile);
        AXmlDocument aXmlDocument = axml20Parser.getDocument();

        // Recursively go over the axml document
        AXmlNode root = aXmlDocument.getRootNode();

        GuiElementConfig guiElementConfig = new GuiElementConfig();
        guiElementConfig.setAttributesToParse(layoutFileAnalyzerConfig.attributesToParse);
        guiElementConfig.setFilterAttributes(!layoutFileAnalyzerConfig.attributesToParse.isEmpty());

        // This from call is recursive and converts all AXmlNodes and AXmlAttributes
        // into GuiElements and Attributes respectively
        this.layoutFile.setRoot(GuiElement.from(root, guiElementConfig));

        // Traverse the Hierarchy and add links to those variables
        // that use the particular Gui Elements
        linkAllVariablesThatAreHoldingAReferenceToThisElement(this.layoutFile.getRoot());

        for (Map.Entry<Integer, Set<VariableReference>> entry: resourceIdToVariablesMap.entrySet()) {

            for (VariableReference variableReference: entry.getValue()) {
                if (variablesToResourceIdMap.containsKey(variableReference)) {
                    Set<Integer> list = variablesToResourceIdMap.get(variableReference);
                    list.add(entry.getKey());
                    variablesToResourceIdMap.put(variableReference, list);
                } else {
                    variablesToResourceIdMap.put(variableReference, new HashSet<Integer>(Collections.singletonList(entry.getKey())));
                }
            }

        }

        for (Map.Entry<GuiElement, Set<VariableReference>> entry: guiElementsToVariablesMap.entrySet()) {

            for (VariableReference variableReference: entry.getValue()) {
                if (variablesToGuiElementsMap.containsKey(variableReference)) {
                    Set<GuiElement> list = variablesToGuiElementsMap.get(variableReference);
                    list.add(entry.getKey());
                    variablesToGuiElementsMap.put(variableReference, list);
                } else {
                    variablesToGuiElementsMap.put(variableReference, new HashSet<GuiElement>(Collections.singletonList(entry.getKey())));
                }
            }

        }

    }

    /**
     * A method that recursively traverses the hierarchy of the Layout file and links variables to the GuiElements which
     * reference said GuiElement in the code
     *
     * @param node A root/child node of the Layout File
     */
    private void linkAllVariablesThatAreHoldingAReferenceToThisElement(GuiElement node) {

        this.guiElements.add(node);

        for (GuiElement guiElement : node.getChildren()) {

            guiElementsToVariablesMap.put(guiElement,guiElement.getLocalOrFieldsThatUseTheGuiElement());

            if (guiElement.getId() == null) {
                // traverse his children
                linkAllVariablesThatAreHoldingAReferenceToThisElement(guiElement);
                continue;
            }

            for (Map.Entry<Integer, Set<VariableReference>> entry: resourceIdToVariablesMap.entrySet()) {
                if (guiElement.getId().equals(entry.getKey()))
                    guiElement.addVariablesReferencingGuiElement(entry.getValue());
            }

            // Recursive call
            linkAllVariablesThatAreHoldingAReferenceToThisElement(guiElement);

        }

    }

    public Set<GuiElement> getGuiElements() {
        return guiElements;
    }

    public Set<VariableReference> getVariablesReferencingResourceId(int resourceId) {
        return resourceIdToVariablesMap.get(resourceId);
    }

    public Set<VariableReference> getVariablesReferencingGuiElement(GuiElement guiElement) {
        return guiElementsToVariablesMap.get(guiElement);
    }

    public Map<Integer, Set<VariableReference>> getResourceIdToVariablesMap() {
        return resourceIdToVariablesMap;
    }

    public Map<VariableReference, Set<GuiElement>> getVariablesToGuiElementsMap() {
        return variablesToGuiElementsMap;
    }

    public Map<VariableReference, Set<Integer>> getVariablesToResourceIdMap() {
        return variablesToResourceIdMap;
    }

    public Map<GuiElement, Set<VariableReference>> getGuiElementsToVariablesMap() {
        return guiElementsToVariablesMap;
    }

    public Map<VariableReference, Set<GuiElement>> variablesToGuiElementsMap() {
        return variablesToGuiElementsMap;
    }

    public Map<VariableReference, Set<Integer>> variablesToResourceIdMap() {
        return variablesToResourceIdMap;
    }

    public LayoutFile getLayoutFile() {
        return layoutFile;
    }

}