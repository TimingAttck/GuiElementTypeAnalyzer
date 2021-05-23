import GuiElementTypeInferring.GuiElementInferredDataType;
import Layout.*;
import ResourceTracker.ResourceTrackerConfig;
import org.xmlpull.v1.XmlPullParserException;
import soot.PackManager;
import soot.Transform;

import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, XmlPullParserException {

        // Get apk and android jar path from arguments
        String apkPath = args[0];
        String androidJarPath = args[1];

        GuiElementAnalysis guiElementAnalysis = new GuiElementAnalysis(apkPath, androidJarPath);

        // Set the resource tracker config to only track variables of the package name
        // i.e., only from the app code but not libraries
        ResourceTrackerConfig resourceTrackerConfig = new ResourceTrackerConfig();
        resourceTrackerConfig.setTrackOnlyPackageName(guiElementAnalysis.getProcessManifest().getPackageName());

        // Set the android attributes that we are interested in
        final String[] attributesToParse = {"id", "text", "label", "hint", "lines", "minlines", "maxlines", "capitalize", "autoText", "editable", "linkClickable", "textIsSelectable", "textLocale", "editable", "autoText", "inputFilter", "autoLink", "inputMethod", "inputType", "password", "numeric", "digits", "phoneNumber"};
        LayoutFileAnalyzerConfig layoutFileAnalyzerConfig = new LayoutFileAnalyzerConfig();
        layoutFileAnalyzerConfig.setAttributesToParse(Arrays.asList(attributesToParse));

        // finally run the analysis
        guiElementAnalysis.run(resourceTrackerConfig, layoutFileAnalyzerConfig);

        // Optionally: write the analysis result to a file
        guiElementAnalysis.writeResultToJSONToFile("./out/out.json");

    }

}