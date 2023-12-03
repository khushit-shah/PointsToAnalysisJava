import soot.RefType;
import soot.Value;
import soot.jimple.*;

import java.util.*;

public class InterProcPointsToLatticeElement implements LatticeElement {

    public static final int K = 2;

    // for each call string store the state, the parameter passes to the function.
    HashMap<LinkedList<String>, PointsToLatticeElement> state = new HashMap<>();
    HashMap<LinkedList<String>, ArrayList<HashSet<String>>> parameters = new HashMap<>();

    InterProcPointsToLatticeElement() {

    }

    InterProcPointsToLatticeElement(HashMap<LinkedList<String>, PointsToLatticeElement> state, HashMap<LinkedList<String>, ArrayList<HashSet<String>>> parameters) {
        this.state = state;
        this.parameters = parameters;
    }

    @Override
    public LatticeElement join_op(LatticeElement r) {
        // join the state and parameters.
        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParameters = new HashMap<>();

        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : this.state.entrySet()) {
            newState.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
        }

        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : ((InterProcPointsToLatticeElement) r).state.entrySet()) {
            if (newState.containsKey(entry.getKey())) {
                newState.put(cloneX(entry.getKey()), newState.get(entry.getKey()).join_op(entry.getValue()));
            } else {
                newState.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
            }
        }


        for (Map.Entry<LinkedList<String>, ArrayList<HashSet<String>>> entry : this.parameters.entrySet()) {
            newParameters.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
        }

        for (Map.Entry<LinkedList<String>, ArrayList<HashSet<String>>> entry : ((InterProcPointsToLatticeElement) r).parameters.entrySet()) {
            if (newParameters.containsKey(entry.getKey())) {
                ArrayList<HashSet<String>> cur = cloneX(newParameters.get(entry.getKey()));
                for (int i = 0; i < entry.getValue().size(); i++) {
                    if (cur.size() <= i) {
                        cur.add(cloneX(entry.getValue().get(i)));
                    } else {
                        HashSet<String> curSet = cloneX(cur.get(i)); // union of values.
                        curSet.addAll(entry.getValue().get(i));
                        cur.set(i, curSet);
                    }
                }
            } else {
                newParameters.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
            }
        }

        return new InterProcPointsToLatticeElement(newState, newParameters);
    }



    @Override
    public boolean equals(LatticeElement r) {
        // if both the HashMap and Parameter is equal then both are equal.
        InterProcPointsToLatticeElement other = (InterProcPointsToLatticeElement) r;

        if (!(this.state.equals(other.state))) return false;

        return (this.parameters.equals(other.parameters));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InterProcPointsToLatticeElement)) return false;
        return equals((InterProcPointsToLatticeElement) o);
    }

    @Override
    public LatticeElement tf_identity_fn() {
        return new InterProcPointsToLatticeElement(cloneX(this.state), cloneX(this.parameters, 1));
    }


    @Override
    public LatticeElement tf_assign_stmt(ProgramPoint pt, int edgeIndex) {
        Stmt st = pt.stmt;
        if (st.containsInvokeExpr()) {
            if (edgeIndex == pt.callingEdge)
                return tf_invoke_stmt(pt, edgeIndex);
            else {
                return tf_identity_fn();
            }
        }

        HashMap<LinkedList<String>, PointsToLatticeElement> newState = cloneX(this.state);

        newState.replaceAll((k, v) -> (PointsToLatticeElement) v.tf_assign_stmt(pt, edgeIndex));

        return new InterProcPointsToLatticeElement(newState, cloneX(this.parameters, 1));
    }

    @Override
    public LatticeElement tf_if_stmt(ProgramPoint pt, boolean b) {
        HashMap<LinkedList<String>, PointsToLatticeElement> newState = cloneX(this.state);

        newState.replaceAll((k, v) -> (PointsToLatticeElement) v.tf_if_stmt(pt, b));

        return new InterProcPointsToLatticeElement(newState, cloneX(this.parameters, 1));
    }

    public LatticeElement tf_identity_stmt(ProgramPoint pt) {
        Stmt st = pt.stmt;
        IdentityStmt identityStmt = (IdentityStmt) st;

        Value leftOp = identityStmt.getLeftOp();
        Value rightOp = identityStmt.getRightOp();

        System.out.println("here");

        // not the type of prarmaterRef.
        if (!(rightOp instanceof ParameterRef)) return tf_identity_fn();

        ParameterRef param = (ParameterRef) rightOp;

        if (!(param.getType() instanceof RefType)) return tf_identity_fn();

        int paramIndex = param.getIndex();

        String lhsStr = Helper.getSimplifiedVarName(leftOp);

        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParams = cloneX(this.parameters, 1);

        System.out.println("here");
        System.out.println(lhsStr);
        System.out.println(this.parameters);

        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : this.state.entrySet()) {
            ArrayList<HashSet<String>> paramList = this.parameters.getOrDefault(entry.getKey(), new ArrayList<>());

            System.out.println(entry.getKey());
            System.out.println(paramList);

            LinkedList<String> newKey = cloneX(entry.getKey());
            PointsToLatticeElement newVal = entry.getValue().clone();

            System.out.println(newVal);
            if (paramList.size() <= paramIndex) {
                newState.put(newKey, new PointsToLatticeElement()); // empty set, this should not happen.
                continue;
            }

            newVal = newVal.put(lhsStr, cloneX(paramList.get(paramIndex)));

            System.out.println(newVal);

            newState.put(newKey, newVal);
        }

        return new InterProcPointsToLatticeElement(newState, newParams);
    }


    public LatticeElement tf_invoke_stmt(ProgramPoint pt, int edgeIndex) {
        Stmt st = pt.stmt;

        InvokeExpr invokeCall = st.getInvokeExpr();

        if (edgeIndex != pt.callingEdge) return tf_identity_fn();

        // only process the static invokes.
        if (!(invokeCall instanceof StaticInvokeExpr)) return tf_identity_fn();

        String callStringToAppend = Helper.getCallString(pt);

        List<Value> args = invokeCall.getArgs();

        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParams = new HashMap<>();


        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : this.state.entrySet()) {
            LinkedList<String> newCallString = cloneX(entry.getKey());

            newCallString.addLast(callStringToAppend);

            if (newCallString.size() > K) {
                newCallString.removeFirst();
            }

            // from the entry, remove all local variable except of the once's starting with "new".
            PointsToLatticeElement newValue = entry.getValue().removeLocalVar();

            newState.put(newCallString, newValue);

            System.out.println("here");
            System.out.println(args);
            // add the param values.
            ArrayList<HashSet<String>> paramList = new ArrayList<>();
            for (Value v : args) {
                if (!(v.getType() instanceof RefType)) {
                    paramList.add(new HashSet<>());
                } else {
                    String argName = Helper.getSimplifiedVarName(v);
                    System.out.println(argName);
                    paramList.add(entry.getValue().getFactOf(argName));
                }
            }
            if(newParams.containsKey(newCallString)) {
                ArrayList<HashSet<String>> cur = newParams.get(newCallString);
                for(int i = 0;  i < cur.size(); i ++) {
                    HashSet<String> x = cur.get(i);
                    x.addAll(paramList.get(i));
                    cur.set(i, x);
                }
                newParams.put(cloneX(newCallString), cur);
            } else {
                newParams.put(cloneX(newCallString), paramList);
            }
        }


        return new InterProcPointsToLatticeElement(newState, newParams);
    }

    public LatticeElement tf_ret_void_stmt(ProgramPoint pt, int edgeIndex) {
        Stmt st = pt.stmt;
//        ReturnVoidStmt retStmt = (ReturnVoidStmt) st;

        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParams = new HashMap<>(); // send the empty params.

        String callEdge = AnalysisInfo.points.get(pt.successors.get(edgeIndex)).callEdge;

        // in the returned state all the new.. values must be there
        // all the values from the state before the return must be there.
        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : this.state.entrySet()) {
            LinkedList<String> newKey = cloneX(entry.getKey());

            if (newKey.isEmpty()) {
                // return on empty callstring is not possible.
                continue; // don't add anything to the new state.
            }

            if (!newKey.getLast().equals(callEdge)) {
                // the ret edge does not match the callEdge.
                continue; // don't add anything to the new state.
            }


            // get the possible pre callEdge of the first callEdge.
//            ProgramPoint retProgramPoint = Helper.pointFromCallString(callEdge);

//            InterProcPointsToLatticeElement wholePrevState = (InterProcPointsToLatticeElement) retProgramPoint.state;
            // first method that is called.

            newKey.removeLast();

            ArrayList<String> possiblePrevCallEdge;

            if (newKey.isEmpty()) {
                possiblePrevCallEdge = new ArrayList<>();
            } else {
                possiblePrevCallEdge = AnalysisInfo.possiblePrevCallEdge.getOrDefault(newKey.getFirst().substring(0, newKey.getFirst().lastIndexOf(".in")), new ArrayList<>());
            }

            if (possiblePrevCallEdge.isEmpty()) {
                LinkedList<String> newCallString = cloneX(newKey);

                // get the state before call.
//                PointsToLatticeElement prevState = wholePrevState.state.getOrDefault(newCallString, new PointsToLatticeElement());

                // JOIN the prev state with the global vars of the current state.
                PointsToLatticeElement newVal = entry.getValue().removeLocalVar();

                newState.put(newCallString, newVal);
            } else {
                for (String preCallEdge : possiblePrevCallEdge) {
                    LinkedList<String> newCallString = cloneX(newKey);

                    newCallString.addFirst(preCallEdge);

                    // get the state before call.
//                    PointsToLatticeElement prevState = wholePrevState.state.getOrDefault(newCallString, new PointsToLatticeElement());

                    // JOIN the prev state with the global vars of the current state.
                    PointsToLatticeElement newVal = entry.getValue().removeLocalVar();

                    newState.put(newCallString, newVal);
                }
            }
        }

        return new InterProcPointsToLatticeElement(newState, newParams);
    }

    public LatticeElement tf_ret_stmt(ProgramPoint pt, int edgeIndex) {
        //TODO: Assuming return r0;
        // Assuming return r0.f; will not be present.
        // CHECK.

        Stmt st = pt.stmt;
        ReturnStmt retStmt = (ReturnStmt) st;

        if (!(retStmt.getOp().getType() instanceof RefType)) return tf_ret_void_stmt(pt, edgeIndex);

        // r0
        String retVal = Helper.getSimplifiedVarName(retStmt.getOp());

        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParams = new HashMap<>(); // send the empty params.

        String callEdge = AnalysisInfo.points.get(pt.successors.get(edgeIndex)).callEdge;

        // get the calling program point.
        ProgramPoint retProgramPoint = Helper.pointFromCallString(callEdge);
        // TODO: Assuming only assignment statement can contain the call.
        // Assuming call is always the type of r1 = foo();
        // Or, Assuming call is always the type of r1.f = foo();

        if(!(retProgramPoint.stmt instanceof AssignStmt)) {
            return  tf_identity_fn();
        }

        AssignStmt callStmt = (AssignStmt) retProgramPoint.stmt;
        String lhsStr = Helper.getSimplifiedVarName(callStmt.getLeftOp()); //r1
        // in the returned state all the new.. values must be there
        // all the values from the state before the return must be there.
        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : this.state.entrySet()) {
            LinkedList<String> newKey = cloneX(entry.getKey());

            if (newKey.isEmpty()) {
                // return on empty callstring is not possible.
                continue; // don't add anything to the new state.
            }

            if (!newKey.getLast().equals(callEdge)) {
                // the ret edge does not match the callEdge.
                continue; // don't add anything to the new state.
            }



            newKey.removeLast();

            ArrayList<String> possiblePrevCallEdge;

            if (newKey.isEmpty()) {
                possiblePrevCallEdge = new ArrayList<>();
            } else {
                possiblePrevCallEdge = AnalysisInfo.possiblePrevCallEdge.getOrDefault(newKey.getFirst().substring(0, newKey.getFirst().lastIndexOf(".in")), new ArrayList<>());
            }

            if (possiblePrevCallEdge.isEmpty()) {
                LinkedList<String> newCallString = cloneX(newKey);

                // get the state before call.
//                PointsToLatticeElement prevState = wholePrevState.state.getOrDefault(newCallString, new PointsToLatticeElement());

                // JOIN the prev state with the global vars of the current state.
                PointsToLatticeElement newVal = entry.getValue().removeLocalVar();

                newVal = newVal.assign(lhsStr, entry.getValue().getFactOf(retVal)); // add the fact to the newVal.

                newState.put(newCallString, newVal);
            } else {
                for (String preCallEdge : possiblePrevCallEdge) {
                    LinkedList<String> newCallString = cloneX(newKey);

                    newCallString.addFirst(preCallEdge);

                    // get the state before call.
//                    PointsToLatticeElement prevState = wholePrevState.state.getOrDefault(newCallString, new PointsToLatticeElement());

                    // JOIN the prev state with the global vars of the current state.
                    PointsToLatticeElement newVal = entry.getValue().removeLocalVar();

                    newVal = newVal.assign(lhsStr, entry.getValue().getFactOf(retVal)); // add the fact to the newVal.

                    newState.put(newCallString, newVal);
                }
            }
        }

        return new InterProcPointsToLatticeElement(newState, newParams);
    }

    @Override
    public LatticeElement transfer(ProgramPoint pt, boolean isConditional, boolean conditionTaken, int edgeIndex) {
        Stmt stmt = pt.stmt;

        System.out.println("State:");
        System.out.println(this.state);
        System.out.println("Param:");
        System.out.println(this.parameters);
        System.out.println("Statement:");
        System.out.println(stmt);
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assign_stmt(pt, edgeIndex));
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
                setResult(tf_if_stmt(pt, conditionTaken));
            }

            @Override
            public void caseInvokeStmt(InvokeStmt stmt) {
                setResult(tf_invoke_stmt(pt, edgeIndex));
            }

            @Override
            public void caseReturnStmt(ReturnStmt stmt) {
                setResult(tf_ret_stmt(pt, edgeIndex));
            }

            @Override
            public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
                setResult(tf_ret_void_stmt(pt, edgeIndex));
            }

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
        stmt.apply(stmtSwitch);

        System.out.println("New State:");
        System.out.println(((InterProcPointsToLatticeElement) stmtSwitch.getResult()).state);
        System.out.println("New Param:");
        System.out.println(((InterProcPointsToLatticeElement) stmtSwitch.getResult()).parameters);

        System.out.println("===========================================================================================");
        return stmtSwitch.getResult();
    }


    private PointsToLatticeElement cloneX(PointsToLatticeElement element) {
        return element.clone();
    }

    private ArrayList<HashSet<String>> cloneX(ArrayList<HashSet<String>> list) {
        ArrayList<HashSet<String>> newlist = new ArrayList<>();
        for (HashSet<String> x : list) {
            newlist.add(cloneX(x));
        }
        return newlist;
    }

    private HashSet<String> cloneX(HashSet<String> set) {
        return new HashSet<>(set);
    }

    private LinkedList<String> cloneX(LinkedList<String> list) {
        return new LinkedList<>(list);
    }


    private HashMap<LinkedList<String>, ArrayList<HashSet<String>>> cloneX(HashMap<LinkedList<String>, ArrayList<HashSet<String>>> parameters, int x) {
        HashMap<LinkedList<String>, ArrayList<HashSet<String>>> newParams = new HashMap<>();
        for (Map.Entry<LinkedList<String>, ArrayList<HashSet<String>>> entry : parameters.entrySet()) {
            newParams.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
        }
        return newParams;
    }

    private HashMap<LinkedList<String>, PointsToLatticeElement> cloneX(HashMap<LinkedList<String>, PointsToLatticeElement> state) {
        HashMap<LinkedList<String>, PointsToLatticeElement> newState = new HashMap<>();
        for (Map.Entry<LinkedList<String>, PointsToLatticeElement> entry : state.entrySet()) {
            newState.put(cloneX(entry.getKey()), cloneX(entry.getValue()));
        }
        return newState;
    }


    @Override
    public String toString() {
        return this.state.toString() + "\n" + this.parameters.toString();
    }
}