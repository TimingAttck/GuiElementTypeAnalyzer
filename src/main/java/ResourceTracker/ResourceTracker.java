package ResourceTracker;

import ResourceTracker.resources.VariableReference;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.Sources;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import java.util.*;

public class ResourceTracker extends BodyTransformer {

    private final ResourceTrackerConfig resourceTrackerConfig;
    private final CallGraph callGraph;
    // private final List<String> findViewSignatures = new ArrayList<String>(Arrays.asList("<androidx.appcompat.app.AppCompatActivity: android.view.View findViewById(int)>","<android.app.Activity: android.view.View findViewById(int)>", "<android.app.View: android.view.View findViewById(int)>", "<android.view.View: android.view.View findViewById(int)>"));
    public Map<Integer, Set<VariableReference>> viewsAndTrackers = new HashMap<>();

    public ResourceTracker(CallGraph callgraph, ResourceTrackerConfig resourceTrackerConfig) {
        this.callGraph = callgraph;
        this.resourceTrackerConfig = resourceTrackerConfig;
    }

    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        findViewByIdCallsAndResolveReferencedResourceId(body);
    }

    private List<VariableReference> findPropagationsOfLocalThatReferencesView(Local local, Unit unit, ExceptionalUnitGraph unitGraph) {
        List<VariableReference> variableReferences = new ArrayList<>();

        // We check which local or field or field ref
        // further down uses the View reference
        for (Unit localUnit: unitGraph.getSuccsOf(unit)) {

            // A findViewById if it is considered a 'source' always assigns the result to variable
            if (localUnit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) localUnit;
                Value valueLeft = assignStmt.getLeftOp();
                Value valueRight = assignStmt.getRightOp();

                if (assignStmt.getRightOp() instanceof JCastExpr) {
                    JCastExpr jCastExpr = (JCastExpr) assignStmt.getRightOp();
                    valueRight = jCastExpr.getOp();
                }

                if (valueRight instanceof Local) {
                    Local assignedLocal = (Local) valueRight;
                    if (assignedLocal == local) {
                        if (valueLeft instanceof Local) {
                            variableReferences.add(new VariableReference((Local) valueLeft, null, null, unitGraph.getBody().getMethod(), unitGraph.getBody().getMethod().getDeclaringClass()));

                            // Recursive call to find references to this local
                            variableReferences.addAll(findPropagationsOfLocalThatReferencesView((Local) valueLeft, localUnit, unitGraph));
                        } else if (valueLeft instanceof SootField) {
                            variableReferences.add(new VariableReference(null, (SootField) valueLeft, null, unitGraph.getBody().getMethod(), unitGraph.getBody().getMethod().getDeclaringClass()));

                        } else if (valueLeft instanceof FieldRef) {
                            variableReferences.add(new VariableReference(null, null, (FieldRef) valueLeft, unitGraph.getBody().getMethod(), unitGraph.getBody().getMethod().getDeclaringClass()));
                        }
                    }
                }

            }
        }
        return variableReferences;
    }

    private void findViewByIdCallsAndResolveReferencedResourceId(Body body) {

        Object[] units = body.getUnits().toArray();
        ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(body);

        for (Object unitObject : units) {
            Unit unit = (Unit) unitObject;

            // We are only interested in assignment statements that make
            // a call to findViewById
            if (unit instanceof AssignStmt && ((AssignStmt) unit).containsInvokeExpr()) {
                AssignStmt assignStmt = (AssignStmt) unit;
                Value variable = assignStmt.getLeftOp();
                String signature = assignStmt.getInvokeExpr().getMethod().getSignature();
                InvokeExpr invokeExpr = (InvokeExpr) assignStmt.getRightOp();
                SootMethod containingMethod = body.getMethod();
                SootClass containingClass = body.getMethod().getDeclaringClass();

                // check if the current method is a findViewById call
                if (!(signature.contains("findViewById") || signature.contains("requireViewById")))
                    continue;

                // The resource id is always the first parameter to the findViewById method call
                Value resourceArgument = invokeExpr.getArg(0);

                // In the most trivial case the argument passed in is a constant
                if (resourceArgument instanceof IntConstant) {
                    int resourceId = ((IntConstant) invokeExpr.getArg(0)).value;
                    //if (findViewSignatures.contains(signature)) {
                        addToViewsAndTrackers(resourceId, variable, containingMethod, containingClass);
                    //}

                    // Once we have found the resource id and the variable holding it
                    // then we must track which other variables use this reference inside of this method
                    addAllPropagationsOfLocalToList(resourceId, variable, unit, unitGraph, containingClass);

                } else if (resourceArgument instanceof Local) {

                    // In the second case the resource id used in the call to findViewById
                    // is stored in a local of the method
                    Local local = (Local) resourceArgument;
                    //if (findViewSignatures.contains(invokeExpr.getMethod().getSignature())) {

                        // Check if the local that holds the resource id
                        // was assigned a value somewhere in this method
                        int resourceId = findMostRecentLocalReassignmentInSourceMethod(assignStmt, local, unitGraph, new HashSet<Stmt>());
                        addToViewsAndTrackers(resourceId, variable, containingMethod, containingClass);

                        // Once we have found the resource id and the variable holding it
                        // then we must track which other variables use this reference inside of this method
                        addAllPropagationsOfLocalToList(resourceId, variable, unit, unitGraph, containingClass);

                        // If no value was assigned inside of this method then the local must be a parameter
                        // Try to resolve the resource id passed as an argument to the containing method
                        // of the findVieById call
                        if (!(resourceId > 0)) {
                            List<Local> parameterLocals = containingMethod.getActiveBody().getParameterLocals();
                            if (parameterLocals.contains(local)) {
                                int positionOfLocal = parameterLocals.indexOf(local);

                                // For each method that might have possibly called this method with a argument passed for the id
                                // if that method has a resource id assignment that was able to be resolved, then add it to the list
                                // We do not know who the caller is at runtime, thus we must add all
                                List<SootMethod> sootMethods = getPossibleCallers(body.getMethod());
                                for (SootMethod sootMethod: sootMethods) {
                                    for (int resId : findIDInCallSiteMethod(sootMethod, body.getMethod(), positionOfLocal)) {
                                        addToViewsAndTrackers(resId, variable, containingMethod, containingClass);
                                        // Once we have found the resource id and the variable holding it
                                        // then we must track which other variables use this reference inside of this method
                                        addAllPropagationsOfLocalToList(resId, variable, unit, unitGraph, containingClass);
                                    }
                                }
                            }
                        }
                    // }
               }
            }
        }
    }

    private void addAllPropagationsOfLocalToList(int resId, Value variable, Unit unit, ExceptionalUnitGraph unitGraph, SootClass containingClass) {
        if (resId > 0) {
            if (variable instanceof Local) {
                for (VariableReference variableReference : findPropagationsOfLocalThatReferencesView((Local) variable, unit, unitGraph)) {
                    addToViewsAndTrackers(resId, variableReference, containingClass);
                }
            }
        }
    }

    private List<Integer> findIDInCallSiteMethod(SootMethod method, SootMethod parent, int localPositionInCall) {

        List<Integer> resourceIds = new ArrayList<>();

        for (Unit unit: method.getActiveBody().getUnits()) {

            if (unit instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) unit;

                if (assignStmt.getRightOp() instanceof InvokeStmt) {
                    unit = (Unit) assignStmt.getRightOp();
                }
            }

            // First we have to find the call made to the method
            // who's argument list contains the resource id
            if (unit instanceof InvokeStmt) {
                InvokeStmt invokeStmt = (InvokeStmt) unit;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
                SootMethod invokedMethod = invokeExpr.getMethod();

                // Invoke found that corresponds to the method to which the resource id is passed
                if (parent != invokedMethod)
                    continue;

                // Retrieve the value of the resource id passed into the method call
                Value resIdArg = invokeExpr.getArg(localPositionInCall);

                if (resIdArg instanceof IntConstant) {

                    // In the most trivial case it is a constant assignment
                    int resourceId = ((IntConstant) resIdArg).value;
                    resourceIds.add(resourceId);

                } else if (resIdArg instanceof Local) {

                    // In the second case the id of the resource comes from
                    // a local inside of this method
                    Local local = (Local) resIdArg;

                    ExceptionalUnitGraph unitGraph = new ExceptionalUnitGraph(method.getActiveBody());

                    // Check if it is a local assignment inside the method body
                    int resID = findMostRecentLocalReassignmentInSourceMethod(invokeStmt, local, unitGraph, new HashSet<Stmt>());

                    // If it is not a local assignment inside the method body
                    // then we need to test if its value comes from a parameter (recursion)
                    if (!(resID > 0)) {
                        if (method.getActiveBody().getParameterLocals().contains(local)) {

                            int positionOfLocal = method.getActiveBody().getParameterLocals().indexOf(local);
                            List<SootMethod> sootMethods = getPossibleCallers(method);

                            // Recursive call: find the local inside the calling method which
                            // passed the resource id via a parameter
                            for (SootMethod sootMethod: sootMethods) {
                                resourceIds.addAll(findIDInCallSiteMethod(sootMethod, method, positionOfLocal));
                            }
                        }
                    }
                }
            }
        }

        return resourceIds;
    }

    private int findMostRecentLocalReassignmentInSourceMethod(Stmt stmt, Local local, ExceptionalUnitGraph unitGraph, Set<Stmt> visitedPreds) {

        if (visitedPreds.contains(stmt))
            return -1;

        visitedPreds.add(stmt);

        // An assignment to the local that holds the resource id
        if (stmt instanceof AssignStmt) {
            AssignStmt assignStmt = (AssignStmt) stmt;

            if (assignStmt.getLeftOp() == local) {

                if (assignStmt.getRightOp() instanceof IntConstant) {

                    // A constant value is used
                    // i.e. R.id.someResource
                    return ((IntConstant) assignStmt.getRightOp()).value;

                } else if (assignStmt.getRightOp() instanceof Local) {

                    // Check if it is a local assignment inside the method body
                    return findMostRecentLocalReassignmentInSourceMethod(assignStmt, local, unitGraph, new HashSet<Stmt>());

                } else if (assignStmt.getRightOp() instanceof FieldRef) {

                    // The id that was used in the findViewById call
                    // was stored in a member field of the class
                    // resolve the id if the field tag indicates a constant value
                    SootField assignedField = ((FieldRef) assignStmt.getRightOp()).getField();

                    for (Tag tag : assignedField.getTags()) {
                        if (tag instanceof IntegerConstantValueTag) {
                            return ((IntegerConstantValueTag) tag).getIntValue();
                        }
                    }

                    // if it has not yet returned than the resource id was dynamically assigned to to this field
                    // As such the value is not constant, lets look for the last assignment of this field

                    return -3;
                    // return findMostRecentIDAssignmentToClassField(assignedField.getDeclaringClass(), unitGraph.getBody().getMethod(), assignedField);

                } else if (assignStmt.getRightOp() instanceof InvokeExpr) {
                    InvokeExpr inv = (InvokeExpr) assignStmt.getRightOp();

                    // TODO: resolve the id returned via calls to getIdentifier

                    if (inv.getMethod().getName().equals("getIdentifier"))
                        return -2;
                }

            }
        }

        // Loop over all the predecessor of the statement
        // the first assignment that is not null is the most recent assignment
        // to the local of interest and thus contains the resource id
        for (Unit pred : unitGraph.getPredsOf(stmt)) {
            if (pred instanceof Stmt) {

                int lastAssignment = findMostRecentLocalReassignmentInSourceMethod((Stmt) pred, local, unitGraph, visitedPreds);
                if (lastAssignment > 0)
                    return lastAssignment;
            }
        }

        // No resource id was able to be resolved
        return -1;

    }

    private List<SootMethod> getPossibleCallers(SootMethod target) {
        CallGraph cg = Scene.v().getCallGraph();
        Sources sources = new Sources(cg.edgesInto(target));
        List<SootMethod> possibleCallers = new ArrayList<>();
        while (sources.hasNext()) {
            SootMethod method = (SootMethod)sources.next();
            possibleCallers.add(method);
        }
        return possibleCallers;
    }

    private void addToViewsAndTrackers(int resourceId, Value value, SootMethod containingMethod, SootClass containingClass) {

        if (!resourceTrackerConfig.trackOnlyPackageName.isEmpty()) {
            if (!containingClass.getPackageName().contains(resourceTrackerConfig.trackOnlyPackageName)) {
                return;
            }
        }

        if (viewsAndTrackers.containsKey(resourceId)) {
            Set<VariableReference> vars = viewsAndTrackers.get(resourceId);
            if (value instanceof Local) {
                vars.add(new VariableReference((Local) value, null, null, containingMethod, containingClass));
            } else if (value instanceof SootField) {
                vars.add(new VariableReference(null, (SootField) value, null, containingMethod, containingClass));
            } else if (value instanceof FieldRef) {
                vars.add(new VariableReference(null, null, (FieldRef) value, containingMethod, containingClass));
            }
        } else {
            if (value instanceof Local) {
                viewsAndTrackers.put(resourceId, new HashSet<VariableReference>(Collections.singleton(new VariableReference((Local) value, null, null, containingMethod, containingClass))));
            } else if (value instanceof SootField) {
                viewsAndTrackers.put(resourceId, new HashSet<VariableReference>(Collections.singleton(new VariableReference(null, (SootField) value, null, containingMethod, containingClass))));
            } else if (value instanceof FieldRef) {
                viewsAndTrackers.put(resourceId, new HashSet<VariableReference>(Collections.singleton(new VariableReference(null, null, (FieldRef) value, containingMethod, containingClass))));
            }
        }
    }

    private void addToViewsAndTrackers(int resourceId, VariableReference variableReference, SootClass containingClass) {

        if (!resourceTrackerConfig.trackOnlyPackageName.isEmpty()) {
            if (!containingClass.getPackageName().contains(resourceTrackerConfig.trackOnlyPackageName)) {
                return;
            }
        }

        if (viewsAndTrackers.containsKey(resourceId)) {
            Set<VariableReference> variableReferences = viewsAndTrackers.get(resourceId);
            variableReferences.add(variableReference);
            viewsAndTrackers.put(resourceId, variableReferences);
        } else {
            viewsAndTrackers.put(resourceId, new HashSet<VariableReference>(Collections.singleton(variableReference)));
        }
    }

    private int findMostRecentIDAssignmentToClassField(SootClass sootClass, SootMethod methodWhereTheCallWithTheFieldIsMade, SootField field) {

        int lastNonNullIDAssignment = -1;

        // If the field is final (and not static in addition) then the assignment must have
        // have happened inside the constructor of the class
        if (Modifier.isFinal(field.getModifiers())) {

            // Look inside the constructor of the method
            for (SootMethod sootMethod: sootClass.getMethods()) {
                if (!sootMethod.isConstructor())
                    continue;

                for (Unit unit: sootMethod.getActiveBody().getUnits()) {
                    if (unit instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) unit;
                        if (assignStmt.getLeftOp() instanceof FieldRef) {
                            SootField assignedField = ((FieldRef) assignStmt.getLeftOp()).getField();
                            System.out.println(field == assignedField);

                        }
                    }
                }

            }
            // Due to the -w option static final fields should be inlined and the value
            // would have been found inside of the method findMostRecentIDAssignment
        } else {

            // For non final fields look at the method where the call was made with the field
            for (Unit unit: methodWhereTheCallWithTheFieldIsMade.getActiveBody().getUnits()) {
                if (unit instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) unit;

                    if (assignStmt.getLeftOp() instanceof FieldRef) {
                        SootField assignedField = ((FieldRef) assignStmt.getLeftOp()).getField();
                        System.out.println(field);
                        System.out.println(assignedField);
                        System.out.println(field == assignedField);
                    }
                }
            }

            // If we have not found an assigned to the field in the method where the call was made
            // then we need to look at the methods within the same class
            for (Iterator<Edge> it = callGraph.edgesInto(methodWhereTheCallWithTheFieldIsMade); it.hasNext(); ) {
                Edge edge = it.next();

                SootMethod sootMethod = edge.src();

                if (sootClass != sootMethod.getDeclaringClass())
                    continue;

                for (Unit unit: sootMethod.getActiveBody().getUnits()) {
                    if (unit instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) unit;
                        if (assignStmt.getLeftOp() instanceof FieldRef) {
                            SootField assignedField = ((FieldRef) assignStmt.getLeftOp()).getField();
                            System.out.println(field);
                            System.out.println(assignedField);
                            System.out.println(field == assignedField);
                        }
                    }
                }

            }

            // Last we would have to look at the possibility that the resource id was assigned from
            // outside the class
            if (Modifier.isPublic(field.getModifiers())) {
                for (Iterator<Edge> it = callGraph.edgesInto(methodWhereTheCallWithTheFieldIsMade); it.hasNext(); ) {
                    Edge edge = it.next();

                    SootMethod sootMethod = edge.src();

                    if (sootClass == sootMethod.getDeclaringClass())
                        continue;

                    for (Unit unit: sootMethod.getActiveBody().getUnits()) {

                        if (unit instanceof AssignStmt) {
                            AssignStmt assignStmt = (AssignStmt) unit;
                            if (assignStmt.getLeftOp() instanceof FieldRef) {
                                SootField assignedField = ((FieldRef) assignStmt.getLeftOp()).getField();
                                System.out.println(field == assignedField);
                            }
                        }
                    }

                }
            }

            // TODO: Resolve the static case
            // TODO: Resolve the protected case, i.e. field assignments from subclasses

        }

        System.out.println("Final: "+Modifier.isFinal(field.getModifiers()));
        System.out.println("Static: "+Modifier.isStatic(field.getModifiers()));
        System.out.println("Protected: "+Modifier.isProtected(field.getModifiers()));
        System.out.println("Public: "+Modifier.isPublic(field.getModifiers()));
        System.out.println("Private: "+Modifier.isPrivate(field.getModifiers()));
        return 0;
    }

    private void findVariablesUsingViewFieldReference(SootField field) {
        // TODO: implement
    }

    private int resolveGetIdentifierResourceID() {
        // TODO: Implement class to resolve id assignments that are made because of the call getIdentifier
        return 0;
    }

    public Map<Integer, Set<VariableReference>> getViewsAndTrackers() {
        return this.viewsAndTrackers;
    }

}
