package Layout.serializables;

import GuiElementTypeInferring.GuiElementInferredDataType;
import Layout.Attribute;
import Layout.GuiElement;
import ResourceTracker.resources.VariableReference;

import java.util.*;

/**
 * The SerializableGuiElement is a simpler abstraction of the Gui Element class
 * used when serializing, construction and writing the JSON to file.
 */
public class SerializableGuiElement {

    private Integer id;
    private String label;
    private String tag;
    private GuiElementInferredDataType inferredDataType;

    private Set<SerializableGuiElement> children;
    private Map<String, Attribute<?>> attributes;
    private List<SerializableVariableReference> localOrFieldsThatUseTheGuiElement;

    /**
     * @param tag The XML tag name of the Gui Element
     */
    public SerializableGuiElement(String tag) {
        this.tag = tag;
        this.children = new HashSet<>();
        this.attributes = new HashMap<>();
        this.localOrFieldsThatUseTheGuiElement = new ArrayList<>();
    }

    /**
     * Constructs an instance of a SerializableGuiElement from a Gui Element.
     *
     * @param guiElement A Gui Element instance
     * @return an instance of a SerializableGuiElement
     */
    public static SerializableGuiElement from(GuiElement guiElement) {

        SerializableGuiElement serializeableGuiElement = new SerializableGuiElement(guiElement.getTag());

        serializeableGuiElement.id = guiElement.getId();
        serializeableGuiElement.label = guiElement.getLabel();
        serializeableGuiElement.attributes = guiElement.getAttributes();
        serializeableGuiElement.inferredDataType = guiElement.getInferredDataType();

        for (VariableReference variableReference : guiElement.getLocalOrFieldsThatUseTheGuiElement())
            serializeableGuiElement.localOrFieldsThatUseTheGuiElement.add(SerializableVariableReference.from(variableReference));

        for (GuiElement child : guiElement.getChildren())
            serializeableGuiElement.children.add(SerializableGuiElement.from(child));

        return serializeableGuiElement;

    }

    public GuiElementInferredDataType getInferredDataType() {
        return inferredDataType;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getTag() {
        return tag;
    }

    public Set<SerializableGuiElement> getChildren() {
        return children;
    }

    public Map<String, Attribute<?>> getAttributes() {
        return attributes;
    }

    public List<SerializableVariableReference> getLocalOrFieldsThatUseTheGuiElement() {
        return localOrFieldsThatUseTheGuiElement;
    }

}
