package Layout;

/**
 * The LayoutFile class encapsulates and abstracts information about a Layout File
 */
public class LayoutFile {

    private String name;
    private String fullName;
    private GuiElement root;

    /**
     * @param name The file name of the layout file (ex: activity_main.xml)
     * @param fullName The full file name (including the path) of the layout file (ex: res/layout/activity_main.xml)
     * @param root A instance of the root Gui Element of the layout file
     */
    public LayoutFile(String name, String fullName, GuiElement root) {
        this.name = name;
        this.fullName = fullName;
        this.root = root;
    }

    public LayoutFile() { }

    public void setName(String name) {
        this.name = name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRoot(GuiElement root) {
        this.root = root;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public GuiElement getRoot() {
        return root;
    }

    @Override
    public String toString() {
        return "LayoutFile{" +
                "name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", root=" + root +
                '}';
    }

}
