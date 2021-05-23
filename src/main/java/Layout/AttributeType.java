package Layout;

import soot.jimple.infoflow.android.axml.AXmlTypes;

/**
 * The AttributeType Enum denotes different types of a value that a Attribute holds
 */
public enum AttributeType {

    BOOLEAN,
    REFERENCE,
    INT,
    STRING,
    FIRST_INT,
    UNKNOWN

}

/**
 * The AttributeTypeConverter class encapsulates a type converter from the
 * AXml attribute value type to a AttributeType enum type
 */
class AttributeTypeConverter {

    /**
     * Constructs an instance of a AttributeType from a AXml attribute value type
     *
     * @param aXmlType The integer that indicates the AXml attribute value type
     * @return an instance of a AttributeType
     */
    static AttributeType from (int aXmlType) {

        AttributeType attributeType;

        switch (aXmlType) {
            case AXmlTypes.TYPE_REFERENCE:
                attributeType = AttributeType.REFERENCE;
                break;
            case AXmlTypes.TYPE_STRING:
                attributeType = AttributeType.STRING;
                break;
            case AXmlTypes.TYPE_INT_BOOLEAN:
                attributeType = AttributeType.BOOLEAN;
                break;
            case AXmlTypes.TYPE_INT_HEX:
                attributeType = AttributeType.INT;
                break;
            case AXmlTypes.TYPE_FIRST_INT:
                attributeType = AttributeType.FIRST_INT;
                break;
            default:
                attributeType = AttributeType.UNKNOWN;
        }

        return attributeType;

    }

}
