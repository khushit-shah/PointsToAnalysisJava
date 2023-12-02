import soot.RefType;
import soot.SootMethod;
import soot.Value;
import soot.jimple.*;

import java.util.*;

public class ApproximateCallStringLatticeElement implements LatticeElement {

    public static final int K = 2;
    HashMap<ArrayDeque<String>, PointsToLatticeElement> state;

    ApproximateCallStringLatticeElement() {
        state = new HashMap<>();
    }

    ApproximateCallStringLatticeElement(HashMap<ArrayDeque<String>, PointsToLatticeElement> state) {
        this.state = state;
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        ApproximateCallStringLatticeElement other = (ApproximateCallStringLatticeElement) r;
        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = new HashMap<>();


        for (ArrayDeque<String> key : state.keySet()) { // for all elements in this state, just put them as it is.
            newState.put(key, state.get(key));
        }

        for (ArrayDeque<String> key : other.state.keySet()) {
            if (newState.containsKey(key)) {
                newState.put(key, newState.get(key).join_op(other.state.get(key)));
            } else {
                newState.put(key, other.state.get(key));
            }
        }

        return new ApproximateCallStringLatticeElement(newState);
    }

    @Override
    public boolean equals(LatticeElement r) {
        if (!(r instanceof ApproximateCallStringLatticeElement)) return false;

        return this.state.equals(((ApproximateCallStringLatticeElement) r).state);
    }

    @Override
    public LatticeElement tf_identity_fn() {
        return new ApproximateCallStringLatticeElement(clone(this.state));
    }

    @Override
    public LatticeElement tf_assign_stmt(ProgramPoint pt) {
        Stmt st = pt.stmt;

        if(st.containsInvokeExpr()) return tf_invoke_stmt(pt);

        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = clone(this.state);

        newState.replaceAll((k, v) -> (PointsToLatticeElement) v.tf_assign_stmt(pt));

        return new ApproximateCallStringLatticeElement(newState);
    }

    @Override
    public LatticeElement tf_if_stmt(ProgramPoint pt, boolean b) {
        Stmt st = pt.stmt;

        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = clone(this.state);

        newState.replaceAll((k, v) -> (PointsToLatticeElement) v.tf_if_stmt(pt, b));

        return new ApproximateCallStringLatticeElement(newState);
    }

    @Override
    public LatticeElement transfer(ProgramPoint pt, boolean isConditional, boolean conditionTaken, int edgeIndex) {
        Stmt st = pt.stmt;
        System.out.println(st);
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assign_stmt(pt));
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
                setResult(tf_if_stmt(pt, conditionTaken));
            }

            @Override
            public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
                setResult(tf_return_void_stmt(pt, edgeIndex));
            }

            @Override
            public void caseRetStmt(RetStmt stmt) {
                setResult(tf_return_stmt(pt, edgeIndex));
            }

            @Override
            public void caseInvokeStmt(InvokeStmt stmt) {
                if (stmt.getInvokeExpr() instanceof SpecialInvokeExpr) {
                    setResult(tf_identity_fn());
                    return;
                }

                setResult(tf_invoke_stmt(pt));
            }

            // do we need to implement throw statement???
            @Override
            public void caseIdentityStmt(IdentityStmt stmt) {
                setResult(tf_identity_stmt(pt));
            }

            @Override
            public void defaultCase(Object obj) {
                // for any other type of statement apply the identity function.
                setResult(tf_identity_fn());
            }
        };
        st.apply(stmtSwitch);
        return stmtSwitch.getResult();
    }

    private LatticeElement tf_return_void_stmt(ProgramPoint pt, int edgeIndex) {
        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = new HashMap<>();

        String correctCallString = AnalysisInfo.points.get(pt.successors.get(edgeIndex)).correctEndCallString;
        for (Map.Entry<ArrayDeque<String>, PointsToLatticeElement> entry : state.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }

            if (!entry.getKey().getLast().equals(correctCallString)) {
                continue;
            }

            // now, the end call string, is correct.
            if (entry.getKey().size() < K) { // just remove the call edge from end, don't add anything to the beginning.
                ArrayDeque<String> newKey = new ArrayDeque<>(entry.getKey());
                newKey.removeLast();
                newState.put(newKey, entry.getValue());
                continue;
            }

            // the no. of callstrings are exactly = K.
            ArrayDeque<String> newKey = new ArrayDeque<>(entry.getKey());
            newKey.removeLast();

            String methodName = newKey.getFirst().substring(0, newKey.getFirst().lastIndexOf(".in"));
            ArrayList<String> possiblePrefixCallstrings = AnalysisInfo.possiblePrefixes.get(methodName);

            if (possiblePrefixCallstrings.isEmpty()) {
                newState.put(newKey, entry.getValue());
            } else {
                for (String prefix : possiblePrefixCallstrings) {
                    ArrayDeque<String> newKeyWithGivenPrefix = new ArrayDeque<>(newKey);
                    newKeyWithGivenPrefix.addFirst(prefix);
                    newState.put(newKeyWithGivenPrefix, entry.getValue());
                }
            }
        }

        return new ApproximateCallStringLatticeElement(newState);
    }

    private LatticeElement tf_return_stmt(ProgramPoint pt, int edgeIndex) {
        Stmt st = pt.stmt;
        ReturnStmt returnStmt = (ReturnStmt) st;

        if (!(returnStmt.getOp().getType() instanceof RefType)) return tf_return_void_stmt(pt, edgeIndex);

        ProgramPoint programStartPoint = AnalysisInfo.points.get(AnalysisInfo.methodStart.get(pt.method));

        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = new HashMap<>();

        String correctCallString = AnalysisInfo.points.get(pt.successors.get(edgeIndex)).correctEndCallString;
        for (Map.Entry<ArrayDeque<String>, PointsToLatticeElement> entry : state.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }

            if (!entry.getKey().getLast().equals(correctCallString)) {
                continue;
            }

            // now, the end call string, is correct.
            if (entry.getKey().size() < K) { // just remove the call edge from end, don't add anything to the beginning.
                ArrayDeque<String> newKey = new ArrayDeque<>(entry.getKey());
                newKey.removeLast();
                newState.put(newKey, entry.getValue());
                continue;
            }

            // the no. of callstrings are exactly = K.
            ArrayDeque<String> newKey = new ArrayDeque<>(entry.getKey());
            newKey.removeLast();

            String methodName = newKey.getFirst().substring(0, newKey.getFirst().lastIndexOf(".in"));
            ArrayList<String> possiblePrefixCallstrings = AnalysisInfo.possiblePrefixes.get(methodName);

            if (possiblePrefixCallstrings.isEmpty()) {
                newState.put(newKey, entry.getValue());
            } else {
                for (String prefix : possiblePrefixCallstrings) {
                    ArrayDeque<String> newKeyWithGivenPrefix = new ArrayDeque<>(newKey);
                    newKeyWithGivenPrefix.addFirst(prefix);
                    newState.put(newKeyWithGivenPrefix, entry.getValue());
                }
            }
        }

        return new ApproximateCallStringLatticeElement(newState);
    }

    private LatticeElement tf_identity_stmt(ProgramPoint pt) {
        Stmt st = pt.stmt;
        IdentityStmt identityStmt = (IdentityStmt) st;
        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = new HashMap<>();

        Value left = identityStmt.getLeftOp();
        Value right = identityStmt.getRightOp();

        String leftStr = Helper.getSimplifiedVarName(left, pt.method);

        if (!(right instanceof ParameterRef) || !(right.getType() instanceof RefType)) return tf_identity_fn();

        int parameterIndex = ((ParameterRef) right).getIndex();

        ProgramPoint methodStartPoint = AnalysisInfo.points.get(AnalysisInfo.methodStart.get(pt.method));

        for (Map.Entry<ArrayDeque<String>, PointsToLatticeElement> entry : state.entrySet()) {
            System.out.println(st);
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            System.out.println(state);


            PointsToLatticeElement argsLatticeElement = methodStartPoint.callStringStates.getOrDefault(entry.getKey(), new PointsToLatticeElement());
            ArrayList<String> argsList = methodStartPoint.callStringArgList.getOrDefault(entry.getKey(), new ArrayList<>());

            if (argsList.size() <= parameterIndex) {
                break;
            }

            PointsToLatticeElement curLatticeElement = entry.getValue() == null ? new PointsToLatticeElement() : entry.getValue().clone();
            HashSet<String> toBeAdded = new HashSet<>(argsLatticeElement.state.get(argsList.get(parameterIndex)));

            if (curLatticeElement.state.containsKey(leftStr)) {
                HashSet<String> curSet = curLatticeElement.state.get(leftStr);
                curSet.addAll(toBeAdded);
            } else {
                curLatticeElement.state.put(leftStr, toBeAdded);
            }


            newState.put(entry.getKey(), curLatticeElement);
        }

        return new ApproximateCallStringLatticeElement(newState);
    }

    private LatticeElement tf_invoke_stmt(ProgramPoint pt) {
        Stmt st = pt.stmt;
        System.out.println(st);
        // push the current method to the call strings and pop the strings which go beyond K.
        HashMap<ArrayDeque<String>, PointsToLatticeElement> newState = new HashMap<>();
        System.out.println(state);
        for (Map.Entry<ArrayDeque<String>, PointsToLatticeElement> entry : state.entrySet()) {
            ArrayDeque<String> newKey = new ArrayDeque<>(entry.getKey());
            newKey.addLast(pt.method + ".in" + String.format("%02d", pt.indexInPoints));
            if (newKey.size() > K) {
                newKey.removeFirst();
            }
            newState.put(newKey, entry.getValue().clone()); // set the underlying state to empty.
            // as in the new function, variable values will be different.
            // but we need to set the parameter values, for each of the call strings, in the
            // called function.
            // just give clone of whole PointsToElement state to the called function and the list of variable.
            SootMethod calledMethod = st.getInvokeExpr().getMethod();

            int calledMethodStartIndex = AnalysisInfo.methodStart.get(calledMethod);

            ProgramPoint calledMethodStartProgramPoint = AnalysisInfo.points.get(calledMethodStartIndex);

            calledMethodStartProgramPoint.callStringStates.put(newKey, entry.getValue());

            List<Value> arguments = st.getInvokeExpr().getArgs();
            ArrayList<String> strArguments = new ArrayList<>();
            for (int i = 0; i < arguments.size(); i++) {
                if (!Helper.is_val_valid(arguments.get(i))) {
                    strArguments.add("");
                } else {
                    strArguments.add(Helper.getSimplifiedVarName(arguments.get(i), pt.method));
                }
            }
            calledMethodStartProgramPoint.callStringArgList.put(newKey, strArguments);
        }
        System.out.println(newState);
        return new ApproximateCallStringLatticeElement(newState);
    }

    private HashMap<ArrayDeque<String>, PointsToLatticeElement> clone(HashMap<ArrayDeque<String>, PointsToLatticeElement> x) {
        HashMap<ArrayDeque<String>, PointsToLatticeElement> cloned = new HashMap<>();
        if (x == null) return null;
        for (Map.Entry<ArrayDeque<String>, PointsToLatticeElement> entry : x.entrySet()) {
            cloned.put(new ArrayDeque<>(entry.getKey()), entry.getValue() != null ? entry.getValue().clone() : null);
        }

        return cloned;
    }

    @Override
    public String toString() {
        return this.state.toString();
    }
}
