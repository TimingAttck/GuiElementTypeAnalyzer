package Layout;

import Layout.serializables.SerializableLayoutFile;
import ResourceTracker.resources.VariableReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import soot.Local;
import soot.SootField;
import soot.jimple.FieldRef;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.*;

/**
 * The LayoutFilesAnalysis class, encapsulates the Analysis of a Layout File in Android.
 * It retrieves all layout files from the apk, parses the Layout File using Soot/axml,
 * retrieves all the attributes of interest, resolves resources such as
 * ‘@string/email_address_label’ and finally links all the variables, found
 * by ResourceTracker to the respective Gui Elements.
 */
public class LayoutFilesAnalysis {

    private final File apk;
    private final LayoutFileFinder layoutFileFinder;
    private Set<LayoutFile> layoutFileSet;
    private final Map<String, LayoutFile> layoutFileMap;
    private final Map<Integer, Set<VariableReference>> referencesToVariablesMap;
    private final Map<GuiElement, Set<VariableReference>> guiElementsToVariablesMap;
    private final Map<VariableReference, Set<GuiElement>> variablesToGuiElementsMap;
    private final Map<VariableReference, Set<Integer>> variablesToResourceIdMap;
    private final Set<GuiElement> guiElements;

    /**
     * @param apk The Android apk file
     * @param referencesToVariablesMap The Resource Id to Variables Map returned by the Resource Tracker analysis
     * @throws IOException
     */
    public LayoutFilesAnalysis(File apk, Map<Integer, Set<VariableReference>> referencesToVariablesMap) throws IOException {
        this.apk = apk;
        this.layoutFileFinder = new LayoutFileFinder(apk);
        this.layoutFileSet = new HashSet<>();
        this.layoutFileMap = new HashMap<>();
        this.referencesToVariablesMap = referencesToVariablesMap;
        this.guiElementsToVariablesMap = new HashMap<>();
        this.variablesToResourceIdMap = new HashMap<>();
        this.variablesToGuiElementsMap = new HashMap<>();
        this.guiElements = new HashSet<>();
    }

    /**
     * Returns a JSON file that summarizes the result of the whole Analysis.
     * This can be used as input to some other analysis or program.
     *
     * @return A JSON string of the analysis result
     * @throws JsonProcessingException
     */
    public String getJSON() throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        Set<SerializableLayoutFile> serializableLayoutFileSet = new HashSet<>();

        for (LayoutFile layoutFile: layoutFileSet)
            serializableLayoutFileSet.add(SerializableLayoutFile.from(layoutFile));

        return ow.writeValueAsString(serializableLayoutFileSet);
    }

    /**
     * Returns a JSON file that summarizes the result of the whole Analysis.
     * This can be used as input to some other analysis or program.
     *
     * @param layoutFileName The name of the layout file, for which the analysis result should be retrieved.
     * @return A JSON string of the analysis result
     * @throws JsonProcessingException
     */
    public String getJSON(String layoutFileName) throws JsonProcessingException {
        if (!layoutFileMap.containsKey(layoutFileName))
            throw new NoSuchElementException();

        LayoutFile layoutFile = layoutFileMap.get(layoutFileName);
        SerializableLayoutFile serializableLayoutFile = SerializableLayoutFile.from(layoutFile);

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(serializableLayoutFile);
    }

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @throws IOException
     */
    public void writeJSONToFile(String path) throws IOException {
        // Get the file
        File file = new File(path);

        if (file.exists())
            throw new FileAlreadyExistsException("File already exists.");

        file.getParentFile().mkdirs();
        file.createNewFile();
        FileOutputStream stream = new FileOutputStream(file, false);
        stream.write(this.getJSON().getBytes(StandardCharsets.UTF_8));
        stream.flush();
        stream.close();
    }

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @param layoutFileName The name of the layout file, for which the analysis result should be written to disk.
     * @throws IOException
     */
    public void writeJSONToFile(String path, String layoutFileName) throws IOException {
        // Get the file
        File f = new File(path);

        if (f.exists())
            throw new FileAlreadyExistsException("File already exists.");
        if (!f.getParentFile().canWrite())
            throw new IOException("No write permissions in directory");

        FileWriter fileWriter = new FileWriter(path);
        fileWriter.write(getJSON(layoutFileName));
    }

    /**
     * This method an retrieves all layout files from the apk, parses the Layout File using Soot/axml,
     * retrieves all the attributes of interest, traverses the Hierarchy of the Layout file
     * parses all AXmlNodes and AXmlAttributes and add links from
     * a Gui Element to those variables that use the particular
     * Gui Elements in the code
     *
     * @param layoutFileAnalyzerConfig A LayoutFileAnalyzerConfig instance that dictates which Attributes of a Gui Element to parse
     * @return A set of the analyzed Layout Files
     * @throws IOException
     */
    public Set<LayoutFile> analyzeLayoutFiles(LayoutFileAnalyzerConfig layoutFileAnalyzerConfig) throws IOException {

        Set<LayoutFile> layoutFileSet = new HashSet<>();

        for (Map.Entry<String, byte[]> entry : layoutFileFinder.getAllLayoutFilesFromApk().entrySet()) {
            LayoutFileAnalyzer layoutFileAnalyzer = new LayoutFileAnalyzer(entry.getValue(), entry.getKey(), layoutFileAnalyzerConfig, referencesToVariablesMap);
            layoutFileAnalyzer.analyzeLayoutFile();
            layoutFileSet.add(layoutFileAnalyzer.getLayoutFile());
            layoutFileMap.put(layoutFileAnalyzer.getLayoutFile().getName(), layoutFileAnalyzer.getLayoutFile());
            this.guiElements.addAll(layoutFileAnalyzer.getGuiElements());

            for (Map.Entry<GuiElement, Set<VariableReference>> guiElementListEntry: layoutFileAnalyzer.getGuiElementsToVariablesMap().entrySet()) {
                if (guiElementsToVariablesMap.containsKey(guiElementListEntry.getKey())) {
                    Set<VariableReference> list = guiElementsToVariablesMap.get(guiElementListEntry.getKey());
                    list.addAll(guiElementListEntry.getValue());
                    guiElementsToVariablesMap.put(guiElementListEntry.getKey(), list);
                } else {
                    guiElementsToVariablesMap.put(guiElementListEntry.getKey(), guiElementListEntry.getValue());
                }
            }

            for (Map.Entry<VariableReference, Set<Integer>> variableReferenceListEntry: layoutFileAnalyzer.getVariablesToResourceIdMap().entrySet()) {
                    if (variablesToResourceIdMap.containsKey(variableReferenceListEntry.getKey())) {
                        Set<Integer> list = variablesToResourceIdMap.get(variableReferenceListEntry.getKey());
                        list.addAll(variableReferenceListEntry.getValue());
                        variablesToResourceIdMap.put(variableReferenceListEntry.getKey(), list);
                    } else {
                        variablesToResourceIdMap.put(variableReferenceListEntry.getKey(), new HashSet<Integer>(variableReferenceListEntry.getValue()));
                    }
            }

            for (Map.Entry<VariableReference, Set<GuiElement>> variableReferenceListEntry: layoutFileAnalyzer.getVariablesToGuiElementsMap().entrySet()) {
                if (variablesToGuiElementsMap.containsKey(variableReferenceListEntry.getKey())) {
                    Set<GuiElement> list = variablesToGuiElementsMap.get(variableReferenceListEntry.getKey());
                    list.addAll(variableReferenceListEntry.getValue());
                    variablesToGuiElementsMap.put(variableReferenceListEntry.getKey(), list);
                } else {
                    variablesToGuiElementsMap.put(variableReferenceListEntry.getKey(), new HashSet<GuiElement>(variableReferenceListEntry.getValue()));
                }
            }

        }

        this.layoutFileSet = layoutFileSet;
        return layoutFileSet;

    }

    /**
     * This method returns the Set of variables that reference this GUI element in the Java code.
     *
     * @param guiElement The GuiElement from which the Set of variables should be retrieved
     *                   that reference this GUI element in the Java code.
     * @return The set of variables that reference the Gui element
     */
    public Set<VariableReference> getVariablesHoldingReferenceToGuiElement(GuiElement guiElement) {

        if (guiElement.getId() == null)
            return new HashSet<>();
        else if (!referencesToVariablesMap.containsKey(guiElement.getId()))
            return new HashSet<>();
        else
            return referencesToVariablesMap.get(guiElement.getId());

    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided SootField
     *
     * @param field The SootField that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided SootField
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(SootField field) {

        Set<GuiElement> guiElements = new HashSet<>();

        for (Map.Entry<GuiElement, Set<VariableReference>> entry: guiElementsToVariablesMap.entrySet()) {
            for (VariableReference variableReference : entry.getValue()) {
                if (variableReference.field == field)
                    guiElements.add(entry.getKey());
            }
        }

        return guiElements;

    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided FieldRef
     *
     * @param fieldRef The FieldRef that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided FieldRef
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(FieldRef fieldRef) {

        Set<GuiElement> guiElements = new HashSet<>();

        for (Map.Entry<GuiElement, Set<VariableReference>> entry: guiElementsToVariablesMap.entrySet()) {
            for (VariableReference variableReference : entry.getValue()) {
                if (variableReference.sootFieldRef == fieldRef)
                    guiElements.add(entry.getKey());
            }
        }

        return guiElements;

    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided Local
     *
     * @param local The Local that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided Local
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(Local local) {

        Set<GuiElement> guiElements = new HashSet<>();

        for (Map.Entry<GuiElement, Set<VariableReference>> entry: guiElementsToVariablesMap.entrySet()) {
            for (VariableReference variableReference : entry.getValue()) {
                if (variableReference.local == local)
                    guiElements.add(entry.getKey());
            }
        }

        return guiElements;

    }

    public Map<Integer, Set<VariableReference>> getReferencesToVariablesMap() {
        return referencesToVariablesMap;
    }

    public Map<GuiElement, Set<VariableReference>> getGuiElementsToVariablesMap() {
        return guiElementsToVariablesMap;
    }

    public Map<VariableReference, Set<GuiElement>> getVariablesToGuiElementsMap() {
        return variablesToGuiElementsMap;
    }

    public Map<VariableReference, Set<Integer>> getVariablesToResourceIdMap() {
        return variablesToResourceIdMap;
    }

    public Set<GuiElement> getGuiElements() {
        return guiElements;
    }

    public Set<LayoutFile> getLayoutFileSet() {
        return layoutFileSet;
    }

    public Map<String, LayoutFile> getLayoutFileMap() {
        return layoutFileMap;
    }

    public Set<VariableReference> getVariablesReferencingResourceId(int resourceId) {
        return referencesToVariablesMap.get(resourceId);
    }

    public Set<VariableReference> getVariablesReferencingGuiElement(GuiElement guiElement) {
        return guiElementsToVariablesMap.get(guiElement);
    }

    public Set<GuiElement> getGuiElementsReferencedByThisVariable(VariableReference variableReference) {
        return variablesToGuiElementsMap.get(variableReference);
    }

    public Set<Integer> getResourceIDsReferencedByThisVariable(VariableReference variableReference) {
        return variablesToResourceIdMap.get(variableReference);
    }

    public File getApk() {
        return apk;
    }

}
