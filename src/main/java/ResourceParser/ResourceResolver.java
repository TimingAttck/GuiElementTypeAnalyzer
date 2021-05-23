package ResourceParser;

import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.IOException;

/**
 * The ResourceResolver class tries to resolve String resource references
 * such as ‘@string/email_address_label’ into their
 * constant value ‘Enter your email here’.
 */
public class ResourceResolver {

    private static ResourceResolver instance;
    private final ARSCFileParser arscFileParser;

    private ResourceResolver(InfoflowAndroidConfiguration infoflowAndroidConfiguration) throws IOException {
        this.arscFileParser = new ARSCFileParser();
        this.arscFileParser.parse(infoflowAndroidConfiguration.getAnalysisFileConfig().getTargetAPKFile());
    }

    /**
     * A recursive method that tries to resolve a constant string value from
     * a localized String resource id reference
     *
     * @param id the resource id of the Localized String to resolve
     * @return
     */
    public String resolveStringFromLocalizationResourceID(int id) {
        ARSCFileParser.AbstractResource abstractResource = arscFileParser.findResource(id);

        if (abstractResource instanceof ARSCFileParser.ReferenceResource) {
            return resolveStringFromLocalizationResourceID(((ARSCFileParser.ReferenceResource) abstractResource).getReferenceID());
        }

        if (abstractResource instanceof ARSCFileParser.StringResource) {
           return ((ARSCFileParser.StringResource) abstractResource).getValue();
        }

        return null;

    }

    /**
     * Returns a singleton instance of the Resource Resolver
     *
     * @param infoflowAndroidConfiguration
     * @return The single instance of the ResourceResolver
     * @throws IOException
     */
    public static ResourceResolver getInstance(InfoflowAndroidConfiguration infoflowAndroidConfiguration) throws IOException {
        if (instance == null) {
            instance = new ResourceResolver(infoflowAndroidConfiguration);
        }
        return instance;
    }

}