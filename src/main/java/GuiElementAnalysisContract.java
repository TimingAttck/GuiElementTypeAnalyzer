import Layout.GuiElement;
import Layout.LayoutFile;
import Layout.LayoutFileAnalyzerConfig;
import Layout.LayoutFilesAnalysis;
import ResourceTracker.ResourceTracker;
import ResourceTracker.ResourceTrackerConfig;
import ResourceTracker.resources.VariableReference;
import soot.Local;
import soot.SootField;
import soot.jimple.FieldRef;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * A facade contract that acts as the centralized access point to run the GuiElementAnalysis
 * and access its api methods
 */
public interface GuiElementAnalysisContract {

    /**
     * Runs the Gui Element Analysis
     *
     * @param resourceTrackerConfig an instance of a ResourceTrackerConfig that dictates variables of which Package Name to track
     *                              Leave the trackOnlyPackageName field empty, if all variables (also from libraries) should
     *                              be tracked
     * @param layoutFileAnalyzerConfig an instance of a LayoutFileAnalyzerConfig that dictates which Attributes of a Gui Element to parse
     * @throws IOException
     */
    public void run(ResourceTrackerConfig resourceTrackerConfig, LayoutFileAnalyzerConfig layoutFileAnalyzerConfig) throws IOException;

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @throws IOException
     */
    public void writeResultToJSONToFile(String path) throws IOException;

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @param layoutFileName The name of the layout file, for which the analysis result should be written to disk.
     * @throws IOException
     */
    public void writeResultToJSONToFile(String path, String layoutFileName) throws IOException;

    /**
     * This method returns all variables that reference the given resource id
     *
     * @param resourceId The resource id that the variable should reference
     * @return A set of all variables that reference the given resource id
     */
    public Set<VariableReference> getVariablesReferencingResourceId(int resourceId);

    /**
     * This method returns all variables that reference the given guiElement
     *
     * @param guiElement The guiElement that the variable should reference
     * @return A set of all variables that reference the given guiElement
     */
    public Set<VariableReference> getVariablesReferencingGuiElement(GuiElement guiElement);

    /**
     * This method returns all Gui Elements that reference the given variableReference
     *
     * @param variableReference The variableReference that the guiElement should link to
     * @return A set of all Gui Elements that reference the given variableReference
     */
    public Set<GuiElement> getGuiElementsFromVariableReference(VariableReference variableReference);

    /**
     * This method returns all resource ids that reference the given variableReference
     *
     * @param variableReference The variableReference that the resource links
     * @return A set of all Resource ids that reference the given variableReference
     */
    public Set<Integer> getResourceIDsFromVariableReference(VariableReference variableReference);

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided SootField
     *
     * @param field The SootField that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided SootField
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(SootField field);

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided FieldRef
     *
     * @param fieldRef The FieldRef that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided FieldRef
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(FieldRef fieldRef);

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided Local
     *
     * @param local The Local that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided Local
     */
    public Set<GuiElement> getGuiElementsHeldByVariable(Local local);

    /**
     * Retrieves the map of all Variables that hold a reference to a resource id
     *
     * @return returns the map of all Variables that hold a reference to the resource id
     */
    public Map<Integer, Set<VariableReference>> getReferencesToVariablesMap();

    /**
     * Retrieves the map of all Variables that hold a reference to a Gui Element
     *
     * @return returns the map of all Variables that hold a reference to a Gui Element
     */
    public Map<GuiElement, Set<VariableReference>> getGuiElementsToVariablesMap();

    /**
     * Retrieves the map of all Gui Elements that are referenced by a Gui Element
     *
     * @return returns the map of all Gui Elements that are referenced by a Gui Element
     */
    public Map<VariableReference, Set<GuiElement>> getVariablesToGuiElementsMap();

    /**
     * Retrieves the map of all Gui Elements that are referenced by a resource id
     *
     * @return returns the map of all Gui Elements that are referenced by a resource id
     */
    public Map<VariableReference, Set<Integer>> getVariablesToResourceIdMap();

    /**
     * Retrieves the resource tracker analysis instance
     *
     * @return returns the resource tracker instance
     */
    public ResourceTracker getResourceTracker();

    /**
     * Retrieves the layout files analysis instance
     *
     * @return returns the layout files analysis instance
     */
    public LayoutFilesAnalysis getLayoutFilesAnalysis();

    /**
     * Retrieve the analyzed Layout file.
     *
     * @param layoutFile The layout file name
     * @return returns the analyzed Layout file.
     */
    public LayoutFile getLayoutFile(String layoutFile);

    /**
     * Retrieves the set of all analyzed Layout files
     *
     * @return the set of all analyzed Layout files
     */
    public Set<LayoutFile> getLayoutFiles();

    /**
     * Retrieves the map of all analyzed Layout files
     *
     * @return the map of all analyzed Layout files
     */
    public Map<String, LayoutFile> getLayoutFileMap();

    /**
     * Retrieves the set of all analyzed Gui elements
     *
     * @return the set of all analyzed Gui elements
     */
    public Set<GuiElement> getGuiElements();

}
