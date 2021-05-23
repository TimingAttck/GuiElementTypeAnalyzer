package Layout;

import ResourceParser.ResourceResolver;
import soot.jimple.infoflow.android.axml.AXmlAttribute;

import java.io.IOException;

/**
 * A class that abstracts and encapsulates all the necessary information
 * of a Gui Element attribute.
 *
 * @param <T> The type of the attribute value (Ex: REFERENCE)
 */
public class Attribute<T> {

    private final String name;
    private final T value;
    private final AttributeType type;
    private final String localizedStringValue;

    /**
     * @param name The name/tag name of the XML attribute (Ex: text)
     * @param value The value that this attribute holds
     * @param type The type of the attribute value (Ex: REFERENCE)
     * @throws IOException
     */
    public Attribute(String name, T value, AttributeType type) throws IOException {
        this.name = name;
        this.value = value;
        this.type = type;

        // Resolve attributes that might hold a reference to a localized string
        // example: text and label
        if (type == AttributeType.REFERENCE)
            this.localizedStringValue = ResourceResolver.getInstance(null).resolveStringFromLocalizationResourceID((Integer) this.value);
        else
            this.localizedStringValue = null;
    }

    /**
     * Constructs an instance of a Attribute from a AXmlAttribute.
     *
     * @param aXmlAttribute A AXmlAttribute instance (received from AXmlReader, ...)
     * @param <L> The type of the Attribute / Attribute value
     * @return an instance of a Attribute
     * @throws IOException
     */
    public static <L> Attribute<L> from (AXmlAttribute<L> aXmlAttribute) throws IOException {

        String name = aXmlAttribute.getName();
        L value = aXmlAttribute.getValue();
        AttributeType attributeType = AttributeTypeConverter.from(aXmlAttribute.getAttributeType());

        return new Attribute<L>(name, value, attributeType);

    }

    public String getLocalizedStringValue() {
        return localizedStringValue;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public AttributeType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Attribute{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", type=" + type +
                ", localizedStringValue='" + localizedStringValue + '\'' +
                '}';
    }

}
