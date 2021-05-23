package Layout;

import java.util.List;

/**
 * The LayoutFileAnalyzerConfig class, it dictates which Attributes of a Gui Element to parse
 */
public class LayoutFileAnalyzerConfig {

    public List<String> attributesToParse;

    public LayoutFileAnalyzerConfig() {};

    public void setAttributesToParse(List<String> attributesToParse) {
        this.attributesToParse = attributesToParse;
    }

}