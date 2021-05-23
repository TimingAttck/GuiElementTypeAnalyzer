package Layout.serializables;

import Layout.LayoutFile;

/**
 * The SerializableLayoutFile is a simpler abstraction of the Layout File class
 * used when serializing, construction and writing the JSON to file.
 */
public class SerializableLayoutFile {

    private String name;
    private String fullName;
    private SerializableGuiElement root;

    /**
     * @param name The file name of the layout file (ex: activity_main.xml)
     * @param fullName The full file name (including the path) of the layout file (ex: res/layout/activity_main.xml)
     * @param root A serializable instance of the root Gui Element of the layout file
     */
    public SerializableLayoutFile(String name, String fullName, SerializableGuiElement root) {
        this.name = name;
        this.fullName = fullName;
        this.root = root;
    }

    /**
     * Constructs an instance of a SerializableLayoutFile from a LayoutFile.
     *
     * @param layoutFile A LayoutFile instance
     * @return an instance of a SerializableLayoutFile
     */
    public static SerializableLayoutFile from (LayoutFile layoutFile) {

        String name = layoutFile.getName();
        String fullName = layoutFile.getFullName();
        SerializableGuiElement root = SerializableGuiElement.from(layoutFile.getRoot());

        return new SerializableLayoutFile(name, fullName, root);

    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public SerializableGuiElement getRoot() {
        return root;
    }

}
