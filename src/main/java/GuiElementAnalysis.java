import Layout.GuiElement;
import Layout.LayoutFile;
import Layout.LayoutFileAnalyzerConfig;
import Layout.LayoutFilesAnalysis;

import ResourceParser.ResourceResolver;
import ResourceTracker.ResourceTracker;
import ResourceTracker.ResourceTrackerConfig;
import ResourceTracker.resources.VariableReference;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A facade that acts as the centralized access point to run the GuiElementAnalysis
 * and access its api methods
 */
public class GuiElementAnalysis implements GuiElementAnalysisContract {

    private ResourceTracker resourceTracker;
    private LayoutFilesAnalysis layoutFilesAnalysis;
    private Set<LayoutFile> analyzedLayoutFiles;
    private InfoflowAndroidConfiguration infoflowAndroidConfiguration;
    public SetupApplication setupApplication;
    private ProcessManifest processManifest;
    private CallGraph callGraph;
    private ResourceTrackerConfig resourceTrackerConfig;
    private LayoutFileAnalyzerConfig layoutFileAnalyzerConfig;
    private final String apkPath;
    private final String androidJarPath;

    /**
     * @param apkPath The path to the Android apk file to analyze
     * @param androidJarPath The path to the android jar file
     * @throws IOException
     * @throws XmlPullParserException
     */
    public GuiElementAnalysis(String apkPath, String androidJarPath) throws IOException, XmlPullParserException {
        this.analyzedLayoutFiles = new HashSet<>();

        this.apkPath = apkPath;
        this.androidJarPath = androidJarPath;
        setUpApplication(apkPath, androidJarPath);

        // Instantiate the resource finder
        ResourceResolver.getInstance(infoflowAndroidConfiguration);
    }

    /**
     * Runs the Gui Element Analysis
     *
     * @param resourceTrackerConfig an instance of a ResourceTrackerConfig that dictates variables of which Package Name to track
     *                              Leave the trackOnlyPackageName field empty, if all variables (also from libraries) should
     *                              be tracked
     * @param layoutFileAnalyzerConfig an instance of a LayoutFileAnalyzerConfig that dictates which Attributes of a Gui Element to parse
     * @throws IOException
     */
    @Override
    public void run(ResourceTrackerConfig resourceTrackerConfig, LayoutFileAnalyzerConfig layoutFileAnalyzerConfig) throws IOException {
        this.resourceTrackerConfig = resourceTrackerConfig;
        this.layoutFileAnalyzerConfig = layoutFileAnalyzerConfig;
        this.resourceTracker = new ResourceTracker(callGraph, resourceTrackerConfig);

        PackManager.v().getPack("jtp").add(new Transform("jtp.ResourceTrackerTransform", resourceTracker));
        PackManager.v().runPacks();
        PackManager.v().writeOutput();

        this.layoutFilesAnalysis = new LayoutFilesAnalysis(new File(apkPath),this.resourceTracker.viewsAndTrackers);
        this.analyzedLayoutFiles = layoutFilesAnalysis.analyzeLayoutFiles(layoutFileAnalyzerConfig);

    }

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @throws IOException
     */
    @Override
    public void writeResultToJSONToFile(String path) throws IOException {
        this.layoutFilesAnalysis.writeJSONToFile(path);
    }

    /**
     * Writes a JSON file that summarizes the result of the whole Analysis to disk.
     * This can be used as input to some other analysis or program.
     *
     * @param path The path to which the JSON File of the Analysis result should be written to disk.
     * @param layoutFileName The name of the layout file, for which the analysis result should be written to disk.
     * @throws IOException
     */
    @Override
    public void writeResultToJSONToFile(String path, String layoutFileName) throws IOException {
        this.layoutFilesAnalysis.writeJSONToFile(path, layoutFileName);
    }

    /**
     * This method returns all variables that reference the given resource id
     *
     * @param resourceId The resource id that the variable should reference
     * @return A set of all variables that reference the given resource id
     */
    @Override
    public Set<VariableReference> getVariablesReferencingResourceId(int resourceId) {
        return layoutFilesAnalysis.getVariablesReferencingResourceId(resourceId);
    }

    /**
     * This method returns all variables that reference the given guiElement
     *
     * @param guiElement The guiElement that the variable should reference
     * @return A set of all variables that reference the given guiElement
     */
    @Override
    public Set<VariableReference> getVariablesReferencingGuiElement(GuiElement guiElement) {
        return layoutFilesAnalysis.getVariablesReferencingGuiElement(guiElement);
    }

    /**
     * This method returns all Gui Elements that reference the given variableReference
     *
     * @param variableReference The variableReference that the guiElement should link to
     * @return A set of all Gui Elements that reference the given variableReference
     */
    @Override
    public Set<GuiElement> getGuiElementsFromVariableReference(VariableReference variableReference) {
        return layoutFilesAnalysis.getGuiElementsReferencedByThisVariable(variableReference);
    }

    /**
     * This method returns all resource ids that reference the given variableReference
     *
     * @param variableReference The variableReference that the resource links
     * @return A set of all Resource ids that reference the given variableReference
     */
    @Override
    public Set<Integer> getResourceIDsFromVariableReference(VariableReference variableReference) {
        return layoutFilesAnalysis.getResourceIDsReferencedByThisVariable(variableReference);
    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided SootField
     *
     * @param field The SootField that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided SootField
     */
    @Override
    public Set<GuiElement> getGuiElementsHeldByVariable(SootField field) {
        return layoutFilesAnalysis.getGuiElementsHeldByVariable(field);
    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided FieldRef
     *
     * @param fieldRef The FieldRef that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided FieldRef
     */
    @Override
    public Set<GuiElement> getGuiElementsHeldByVariable(FieldRef fieldRef) {
        return layoutFilesAnalysis.getGuiElementsHeldByVariable(fieldRef);
    }

    /**
     * Retrieves the Set of Gui Elements that might be referenced by the provided Local
     *
     * @param local The Local that references the Gui Element
     * @return the Set of Gui Elements that might be referenced by the provided Local
     */
    @Override
    public Set<GuiElement> getGuiElementsHeldByVariable(Local local) {
        return layoutFilesAnalysis.getGuiElementsHeldByVariable(local);
    }

    /**
     * Retrieves the map of all Variables that hold a reference to a resource id
     *
     * @return returns the map of all Variables that hold a reference to the resource id
     */
    @Override
    public Map<Integer, Set<VariableReference>> getReferencesToVariablesMap() {
        return layoutFilesAnalysis.getReferencesToVariablesMap();
    }

    /**
     * Retrieves the map of all Variables that hold a reference to a Gui Element
     *
     * @return returns the map of all Variables that hold a reference to a Gui Element
     */
    @Override
    public Map<GuiElement, Set<VariableReference>> getGuiElementsToVariablesMap() {
        return layoutFilesAnalysis.getGuiElementsToVariablesMap();
    }

    /**
     * Retrieves the map of all Gui Elements that are referenced by a Gui Element
     *
     * @return returns the map of all Gui Elements that are referenced by a Gui Element
     */
    @Override
    public Map<VariableReference, Set<GuiElement>> getVariablesToGuiElementsMap() {
        return layoutFilesAnalysis.getVariablesToGuiElementsMap();
    }

    /**
     * Retrieves the map of all Gui Elements that are referenced by a resource id
     *
     * @return returns the map of all Gui Elements that are referenced by a resource id
     */
    @Override
    public Map<VariableReference, Set<Integer>> getVariablesToResourceIdMap() {
        return layoutFilesAnalysis.getVariablesToResourceIdMap();
    }

    /**
     * Retrieve the analyzed Layout file.
     *
     * @param layoutFile The layout file name
     * @return returns the analyzed Layout file.
     */
    @Override
    public LayoutFile getLayoutFile(String layoutFile) {
        return null;
    }

    /**
     * Retrieves the layout files analysis instance
     *
     * @return returns the layout files analysis instance
     */
    @Override
    public LayoutFilesAnalysis getLayoutFilesAnalysis() {
        return layoutFilesAnalysis;
    }

    /**
     * Retrieves the resource tracker analysis instance
     *
     * @return returns the resource tracker instance
     */
    @Override
    public ResourceTracker getResourceTracker() {
        return resourceTracker;
    }

    /**
     * Retrieves the set of all analyzed Layout files
     *
     * @return the set of all analyzed Layout files
     */
    @Override
    public Set<LayoutFile> getLayoutFiles() {
        return layoutFilesAnalysis.getLayoutFileSet();
    }

    /**
     * Retrieves the map of all analyzed Layout files
     *
     * @return the map of all analyzed Layout files
     */
    @Override
    public Map<String, LayoutFile> getLayoutFileMap() {
        return layoutFilesAnalysis.getLayoutFileMap();
    }

    /**
     * Retrieves the set of all analyzed Gui elements
     *
     * @return the set of all analyzed Gui elements
     */
    @Override
    public Set<GuiElement> getGuiElements() {
        return layoutFilesAnalysis.getGuiElements();
    }

    private void setUpApplication(String apkPath, String androidJarPath) throws IOException, XmlPullParserException {

        InfoflowAndroidConfiguration infoflowAndroidConfiguration = new InfoflowAndroidConfiguration();
        infoflowAndroidConfiguration.getAnalysisFileConfig().setAndroidPlatformDir(androidJarPath);
        infoflowAndroidConfiguration.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        infoflowAndroidConfiguration.setEnableReflection(true);

        SetupApplication sa = new SetupApplication(infoflowAndroidConfiguration);
        sa.constructCallgraph();
        this.callGraph = Scene.v().getCallGraph();

        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_force_android_jar(androidJarPath);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_soot_classpath(androidJarPath);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_include_all(true);
        // Options.v().setPhaseOption("jb", "use-original-names:true"); // <- causes crashes :[
        Scene.v().loadNecessaryClasses();

        this.processManifest = new ProcessManifest(infoflowAndroidConfiguration.getAnalysisFileConfig().getTargetAPKFile());

        this.setupApplication = sa;
        this.infoflowAndroidConfiguration = infoflowAndroidConfiguration;

    }

    public InfoflowAndroidConfiguration getInfoflowAndroidConfiguration() {
        return infoflowAndroidConfiguration;
    }

    public ProcessManifest getProcessManifest() {
        return processManifest;
    }

    public String getApkPath() {
        return apkPath;
    }

}