package Layout;

import java.util.ArrayList;
import java.util.List;

/**
 * The GuiElementConfig class, it dictates which Attributes to parse
 */
public class GuiElementConfig {

    public boolean filterAttributes;
    public List<String> keepList = new ArrayList<>();

    /**
     * @param keepList The list of attributes to parse from the Gui Element
     */
    public void setAttributesToParse(List<String> keepList) {
        this.filterAttributes = true;
        this.keepList = keepList;
    }

    public void setFilterAttributes(boolean filterAttributes) {
        this.filterAttributes = filterAttributes;
    }

    public boolean isFilterAttributes() {
        return filterAttributes;
    }

    public List<String> getKeepList() {
        return keepList;
    }

}
