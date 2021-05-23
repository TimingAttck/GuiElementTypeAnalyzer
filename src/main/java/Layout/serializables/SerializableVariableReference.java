package Layout.serializables;

import ResourceTracker.resources.VariableReference;

/**
 * The SerializableVariableReference is a simpler abstraction of the Variable Reference class
 * used when serializing, construction and writing the JSON to file.
 */
public class SerializableVariableReference {

    private String containingClass;
    private String containingMethod;
    private String fieldRef;
    private String field;
    private String local;

    /**
     * Constructs an instance of a SerializableVariableReference from a VariableReference.
     *
     * @param variableReference A VariableReference instance
     * @return an instance of a SerializableVariableReference
     */
    public static SerializableVariableReference from(VariableReference variableReference) {

        SerializableVariableReference serializableVariableReference = new SerializableVariableReference();

        serializableVariableReference.containingClass = variableReference.containingClass.getName();
        serializableVariableReference.containingMethod = variableReference.assignedInsideMethod.getSignature();

        if (variableReference.sootFieldRef != null)
            serializableVariableReference.fieldRef = variableReference.sootFieldRef.toString();

        if (variableReference.field != null)
            serializableVariableReference.field = variableReference.field.toString();

        if (variableReference.local != null)
            serializableVariableReference.local = variableReference.local.toString();

        return serializableVariableReference;

    }

    public String getContainingClass() {
        return containingClass;
    }

    public String getContainingMethod() {
        return containingMethod;
    }

    public String getFieldRef() {
        return fieldRef;
    }

    public String getField() {
        return field;
    }

    public String getLocal() {
        return local;
    }

}
