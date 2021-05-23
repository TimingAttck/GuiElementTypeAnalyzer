package ResourceTracker.resources;

import soot.Local;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.FieldRef;

/**
 * A class that abstracts a Variable Reference
 * that represents a variable that holds a reference to a Gui Element
 */
public class VariableReference {

    public Local local;
    public SootField field;
    public FieldRef sootFieldRef;
    public SootMethod assignedInsideMethod;
    public SootClass containingClass;

    /**
     * @param local The local that holds a reference to the Gui Element (if it is a local)
     * @param field The field that holds a reference to the Gui Element (if it is a local)
     * @param sootFieldRef The field ref that holds a reference to the Gui Element (if it is a local)
     * @param assignedInsideMethod The Method in which this variable is contained
     * @param containingClass The Class in which this variable is contained
     */
    public VariableReference(Local local, SootField field, FieldRef sootFieldRef, SootMethod assignedInsideMethod, SootClass containingClass) {
        this.local = local;
        this.field = field;
        this.sootFieldRef = sootFieldRef;
        this.containingClass = containingClass;
        this.assignedInsideMethod = assignedInsideMethod;
    }

    @Override
    public String toString() {
        return "VariableReference{" +
                "local=" + local +
                ", field=" + field +
                ", sootFieldRef=" + sootFieldRef +
                ", assignedInsideMethod=" + assignedInsideMethod +
                ", containingClass=" + containingClass +
                '}';
    }

}