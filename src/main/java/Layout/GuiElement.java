package Layout;

import GuiElementTypeInferring.GuiElementInferredDataType;
import GuiElementTypeInferring.GuiElementTypeInferring;
import ResourceParser.ResourceResolver;
import ResourceTracker.resources.VariableReference;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.AXmlTypes;

import java.io.IOException;
import java.util.*;

/**
 * A class that abstracts and encapsulates all the necessary information
 * and methods of a Gui Element
 */
public class GuiElement {

    private Integer id;
    private String label;
    private String tag;
    private String localizedLabel;
    private GuiElementInferredDataType inferredDataType;

    private final Set<GuiElement> children;
    private final Map<String, Attribute<?>> attributes;
    private final Set<VariableReference> localOrFieldsThatUseTheGuiElement;

    /**
     * @param tag The XML tag name of the Gui Element
     */
    public GuiElement(String tag) {
        this.tag = tag;
        this.children = new HashSet<>();
        this.attributes = new HashMap<>();
        this.localOrFieldsThatUseTheGuiElement = new HashSet<>();
    }

    /**
     * Constructs an instance of a GuiElement from a AXmlNode.
     *
     * @param aXmlNode A aXmlNode instance (received from AXmlReader, ...)
     * @param guiElementConfig A GuiElementConfig instance, it dictates which Attributes to parse
     * @return an instance of a GuiElement
     * @throws IOException
     */
    public static GuiElement from(AXmlNode aXmlNode, GuiElementConfig guiElementConfig) throws IOException {

        GuiElement guiElement = new GuiElement(aXmlNode.getTag());

        AXmlAttribute<?> id = aXmlNode.getAttribute("id");
        AXmlAttribute<?> label = aXmlNode.getAttribute("label");

        if (id != null && id.getType() == AXmlTypes.TYPE_INT_HEX)
            guiElement.setId((Integer) id.getValue());

        if (label != null && label.getType() == AXmlTypes.TYPE_STRING)
            guiElement.setLabel((String) label.getValue());

        for (Map.Entry<String, AXmlAttribute<?>> entry: aXmlNode.getAttributes().entrySet()) {

            if (!guiElementConfig.filterAttributes) {
                guiElement.addAXmlAttribute(entry.getValue());
                continue;
            }

            if (guiElementConfig.getKeepList().contains(entry.getKey()) || entry.getKey().equals("id") || entry.getKey().equals("label")
                    || entry.getKey().equals("autoLink") || entry.getKey().equals("inputType")) {
                guiElement.addAXmlAttribute(entry.getValue());
            }

        }

        guiElement.inferredDataType = new GuiElementTypeInferring(guiElement).getInferredDataHoldingType();

        for (AXmlNode child: aXmlNode.getChildren()) {
            guiElement.addChild(GuiElement.from(child, guiElementConfig));
        }

        return guiElement;
    }

    /**
     * Adds a AXml Attribute to this Gui Elements
     *
     * @param axmlAttribute An instance of a AXmlAttribute
     * @param <T> The type of the attribute value (Ex: REFERENCE)
     * @throws IOException
     */
    public <T> void addAXmlAttribute(AXmlAttribute<T> axmlAttribute) throws IOException {

        Attribute<T> attribute = Attribute.from(axmlAttribute);

        if (attribute.getName().contains("label") && attribute.getValue() == AttributeType.REFERENCE)
            this.localizedLabel = ResourceResolver.getInstance(null).resolveStringFromLocalizationResourceID((Integer) axmlAttribute.getValue());

        this.attributes.put(attribute.getName(), attribute);

    }

    /**
     * Retrieve an Attribute of the Gui Element (if it exists)
     *
     * @param name
     * @return
     */
    public Attribute<?> getAttribute(String name) {

        if (attributes.containsKey(name))
            return attributes.get(name);
        else
            return null;
    }

    public void addChild(GuiElement child) {
        this.children.add(child);
    }

    public void addVariablesReferencingGuiElement(Set<VariableReference> variableReference) {
        this.localOrFieldsThatUseTheGuiElement.addAll(variableReference);
    }

    public void addAXmlChild(AXmlNode child, GuiElementConfig guiElementConfig) throws IOException {
        this.children.add(GuiElement.from(child, guiElementConfig));
    }

    public GuiElementInferredDataType getInferredDataType() {
        return inferredDataType;
    }

    public String getTag() {
        return tag;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Set<GuiElement> getChildren() {
        return children;
    }

    public Map<String, Attribute<?>> getAttributes() {
        return attributes;
    }

    public Set<VariableReference> getLocalOrFieldsThatUseTheGuiElement() {
        return localOrFieldsThatUseTheGuiElement;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "GuiElement{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", tag='" + tag + '\'' +
                ", localizedLabel='" + localizedLabel + '\'' +
                ", inferredDataType=" + inferredDataType +
                ", children=" + children +
                ", attributes=" + attributes +
                ", localOrFieldsThatUseTheGuiElement=" + localOrFieldsThatUseTheGuiElement +
                '}';
    }

}
