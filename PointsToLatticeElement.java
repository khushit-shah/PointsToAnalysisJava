import soot.Local;
import soot.RefType;
import soot.Value;
import soot.jimple.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PointsToLatticeElement implements LatticeElement {
    // NOTE: In alot of places, we are using the function clone(),
    // it is extremely important to deep clone the current internal state and then create the new LatticeElement with the cloned internal state.
    // as otherwise, if the created LatticeElement is updated the internal state of this will also change.


    // the internal state, maps var or newXX.field to set of var union newXX.
    HashMap<String, HashSet<String>> state;

    // set only containing "null".
    HashSet<String> nullOnlySet = new HashSet<>();

    PointsToLatticeElement() {
        state = new HashMap<>();
        nullOnlySet.add("null");
    }

    PointsToLatticeElement(HashMap<String, HashSet<String>> state) {
        this.state = state;
        nullOnlySet.add("null");
    }

    // used for debugging.
    public String toString() {
        StringBuilder ret = new StringBuilder("{");
        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            ret.append(entry.getKey()).append("=>").append(Arrays.toString(entry.getValue().toArray())).append("\n");
        }
        return ret.append("}").toString();
    }

    /**
     * Joins this with r.
     * it performs point-wise union of pointsTo set of all var or newXX.field.
     *
     * @param r the PointsToLattice element to join with.
     * @return new PointsToLatticeElement with the joined internal state.
     */
    @Override
    public PointsToLatticeElement join_op(LatticeElement r) {
        PointsToLatticeElement other = (PointsToLatticeElement) r;
        HashMap<String, HashSet<String>> new_state = new HashMap<>();

        // adds all entries in this to the new state.
        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            HashSet<String> base = clone(entry.getValue());
            base.addAll(other.state.getOrDefault(entry.getKey(), new HashSet<>()));

            new_state.put(entry.getKey(), base);
        }

        // adds all entries in r to the new state.
        for (Map.Entry<String, HashSet<String>> entry : other.state.entrySet()) {
            HashSet<String> base = clone(entry.getValue());
            base.addAll(state.getOrDefault(entry.getKey(), new HashSet<>()));

            new_state.put(entry.getKey(), base);
        }

        return new PointsToLatticeElement(new_state);
    }


    /**
     * Checks if the two LatticeElement object are the same, by checking the internal state.
     *
     * @param r LatticeElement to check.
     * @return
     */
    @Override
    public boolean equals(LatticeElement r) {
        return state.equals(((PointsToLatticeElement) r).state);
    }

    /**
     * Returns new LatticeElement with the same internal states.
     *
     * @return
     */
    @Override
    public LatticeElement tf_identity_fn() {
        return new PointsToLatticeElement(clone(state));
    }

    /**
     * Applies the assignment statement to "this"
     *
     * @param st the Assignment statement to apply.
     * @return new LatticeElement with internal state modified respectively.
     */
    @Override
    public LatticeElement tf_assign_stmt(Stmt st) {
        AssignStmt assignStmt = (AssignStmt) st;

        Value lhs = assignStmt.getLeftOp();
        Value rhs = assignStmt.getRightOp();


        // if lhs or rhs is not a valid type of value. return identity fn.
        if (!is_val_valid(lhs) || !is_val_valid(rhs)) {
            return tf_identity_fn();
        }

        // Get the simplified name of lhs and rhs.
        // from a or a.<class: type f>
        // to a or a.f
        String lhsStr = Helper.getSimplifiedVarName(lhs);
        String rhsStr = Helper.getSimplifiedVarName(rhs);

        /*
         *  Following cases can occur.
            - a = b;
            - a = newXX;
            - a = null;
            - a = b.f;

            - a.f = b;
            - a.f = newXX;
            - a.f = null;
            - (a.f = b.f can never occur as Jimple is 3 Address Code)
         */
        if (lhsStr.contains(".")) { // a.f = b|newXX|null
            String lhsVarStr = lhsStr.split("\\.")[0]; // a
            String lhsFieldStr = lhsStr.split("\\.")[1]; // f

            HashSet<String> RhsPointsTo; // set of vars' rhs points to.
            if (rhsStr.startsWith("new")) {  // a.f = newXX, rhs points to set {'newXX'}
                HashSet<String> newXX = new HashSet<>();
                newXX.add(rhsStr);
                RhsPointsTo = newXX;
            } else if (rhsStr.equals("null")) { // a.f = null, rhs points to set {'null'}
                RhsPointsTo = clone(nullOnlySet);
            } else { // a.f = b, rhs points to state(b)
                RhsPointsTo = clone(state.getOrDefault(rhsStr, new HashSet<>()));
            }

            // whatever 'a' is pointing to.
            HashSet<String> varPointsTo = state.getOrDefault(lhsVarStr, new HashSet<>());

            // the new internal state.
            HashMap<String, HashSet<String>> newState = clone(state);

            for (String p : varPointsTo) {
                if (p.equals("null")) continue;

                String newLhsStr = p + "." + lhsFieldStr;

                // get current pointsTo set of p.f.
                HashSet<String> curSet = newState.getOrDefault(newLhsStr, new HashSet<>());

                // union it with RhsPointsTO.
                curSet.addAll(RhsPointsTo);

                // add it to the newState.
                newState.put(newLhsStr, curSet);
            }

            return new PointsToLatticeElement(newState);

        } else if (rhsStr.contains(".")) { // a = b.f
            HashMap<String, HashSet<String>> newState = clone(state);

            String rhsVarStr = rhsStr.split("\\.")[0]; // b
            String rhsFieldStr = rhsStr.split("\\.")[1]; // f

            // every var b points to. In this java it will never be another variable. it will always be newXX
            HashSet<String> rhsVarPointsTo = state.getOrDefault(rhsVarStr, new HashSet<>());

            // as this is a strong update, take an empty set, that represents what lhs will point to after the statement.
            HashSet<String> lhsPointsTo = new HashSet<>();

            // take union of all pointsTo Sets of (rhsVarPointsTo.f)
            for (String p : rhsVarPointsTo) {
                if (p.equals("null")) continue;

                String newRhsStr = p + "." + rhsFieldStr;

                lhsPointsTo.addAll(state.getOrDefault(newRhsStr, new HashSet<>())); // union.
            }

            newState.put(lhsStr, lhsPointsTo);

            return new PointsToLatticeElement(newState);
        } else { // a == (b|null|newXX)
            // strong update always.
            HashMap<String, HashSet<String>> newState = clone(state);

            if (rhsStr.equals("null")) { // a = null
                newState.put(lhsStr, clone(nullOnlySet));
            } else if (rhsStr.startsWith("new")) { // a = newXX
                HashSet<String> newXX = new HashSet<>();
                newXX.add(rhsStr);
                newState.put(lhsStr, newXX);
            } else { // a = b
                newState.put(lhsStr, newState.getOrDefault(rhsStr, new HashSet<>()));
            }

            return new PointsToLatticeElement(newState);
        }
    }

    /**
     * Applies the if statement to "this"
     *
     * @param st    IfStmt.
     * @param taken branch taken or not.
     * @return
     */
    @Override
    public LatticeElement tf_if_stmt(Stmt st, boolean taken) {
        IfStmt ifStmt = (IfStmt) st;

        // get the condition statement.
        Value condition = ifStmt.getCondition();

        // if it's a == b.
        if (condition instanceof EqExpr) {
            Value op1 = ((EqExpr) condition).getOp1();
            Value op2 = ((EqExpr) condition).getOp2();

            // check if both the operators are valid.
            if (!is_val_valid(op1) || !is_val_valid(op2)) {
                return tf_identity_fn();
            }

            //  NOTE: a.f or b.f can never occur as Jimple is 3 addresss code and if <a> op <b> goto <label>,  a, b, label are addresses

            // a
            String op1Str = Helper.getSimplifiedVarName(op1);
            // b
            String op2Str = Helper.getSimplifiedVarName(op2);

            return transfer_eq_eq(taken, op1Str, op2Str);
        } else if (condition instanceof NeExpr) { // if it's a != b
            // if a or b is not RefType or null, just return identity function.
            Value op1 = ((NeExpr) condition).getOp1();
            Value op2 = ((NeExpr) condition).getOp2();

            if (!is_val_valid(op1) || !is_val_valid(op2)) {
                return tf_identity_fn();
            }

            String op1Str = Helper.getSimplifiedVarName(op1);
            String op2Str = Helper.getSimplifiedVarName(op2);

            // a != b, taken  === a == b, not taken.
            // a != b, not taken === a == b, taken.
            return transfer_eq_eq(!taken, op1Str, op2Str);
        } else { // otherwise it's just identity function.
            return tf_identity_fn();
        }
    }

    /**
     * Transfer function for statements of type a == b.
     *
     * @param taken  if the branch is taken or not.
     * @param op1Str lhs string
     * @param op2Str rhs string
     * @return new LatticeElement with the transfer function applied.
     */
    private LatticeElement transfer_eq_eq(boolean taken, String op1Str, String op2Str) {
        // null == null
        if (op1Str.equals("null") && op2Str.equals("null")) {
            if (taken) return tf_identity_fn(); // if taken then it's just identity function.
            else return new PointsToLatticeElement(); // if in false branch, we can never be here so bot.
        }

        // both of the above conditions can be merged into one, but kept separate for readability.
        // a == a, both op1 and op2 are same variable, then in each program execution path, the branch is always taken.
        if (op1Str.equals(op2Str)) {
            if (taken) return tf_identity_fn(); // so, if the branch is taken, it is identity function.
            else return new PointsToLatticeElement(); // if branch is not taken it is bot.
        }

        // ensure that op1 always not null.
        if (op1Str.equals("null")) {
            op1Str = op2Str;
            op2Str = "null";
        }

        if (taken) { // if op1 == op2 is taken.

            HashSet<String> RhsSet; // set of var' rhs is pointing to.

            if (op2Str.equals("null")) { // v1 == null
                RhsSet = clone(nullOnlySet);
            } else { // v1 = v2; We don't have v1 = v2.f possibility here.
                RhsSet = clone(state.getOrDefault(op2Str, new HashSet<>()));
            }

            HashSet<String> lhsSet = clone(state.getOrDefault(op1Str, new HashSet<>())); // set of var' lhs is pointing to.

            lhsSet.retainAll(RhsSet); // intersection.

            if (lhsSet.isEmpty()) { // if there is no common element, then true branch is bot.
                return new PointsToLatticeElement();
            } else { // set both op1 and op2 to point to the intersection of both.
                HashMap<String, HashSet<String>> newState = clone(state);

                newState.put(op1Str, lhsSet);

                if (!op2Str.equals("null")) newState.put(op2Str, clone(lhsSet));

                return new PointsToLatticeElement(newState);
            }
        } else { // if op1 == op2 is not taken.
            // only case we can determine is when both op1 and op2 are singleton and atleast one is null.

            HashSet<String> op1Set = state.getOrDefault(op1Str, new HashSet<>());
            HashSet<String> op2Set;
            if (op2Str.equals("null")) {
                op2Set = clone(nullOnlySet);
            } else {
                op2Set = state.getOrDefault(op2Str, new HashSet<>());
            }
            if (op1Set.size() == 1 && op2Set.size() == 1) {
                if (op1Set.equals(nullOnlySet) && op2Set.equals(nullOnlySet)) { // both of them are null, false branch is not legit.
                    return new PointsToLatticeElement();
                }
            }

            // for all other cases in false branch it is identity function.
            return tf_identity_fn();
        }
    }


    /**
     * Given the statement, calls appriopiate transfer function and returns new LatticeElement with transfer function applied on "this"
     *
     * @param st             Stmt
     * @param isConditional  is it a conditional statement?
     * @param conditionTaken is the condition taken?
     * @return
     */
    @Override
    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken) {
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assign_stmt(stmt));
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
                setResult(tf_if_stmt(stmt, conditionTaken));
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

    /**
     * Deep clones "this"
     *
     * @return
     */
    public PointsToLatticeElement clone() {
        HashMap<String, HashSet<String>> clonedState = clone(state);
        return new PointsToLatticeElement(clonedState);
    }

    /**
     * Deep clones given HashSet.
     *
     * @param x
     * @return
     */
    public HashSet<String> clone(HashSet<String> x) {
        return new HashSet<>(x);  // as String is immutable it is okay to shallow clone this.
    }

    /**
     * Deep clones given, HashMap<String, HashSet<String>> x
     *
     * @param x
     * @return
     */
    public HashMap<String, HashSet<String>> clone(HashMap<String, HashSet<String>> x) {
        HashMap<String, HashSet<String>> cloned = new HashMap<>();

        for (Map.Entry<String, HashSet<String>> entry : x.entrySet()) {
            cloned.put(entry.getKey(), clone(entry.getValue()));
        }

        return cloned;
    }


    /**
     * If a value is Local or NullConstant or InstanceFieldRef or CastExpr with castType of RefType, returns true.
     * else false.
     * This ensure, all the operand we operate on is ref type.
     *
     * @param op
     * @return
     */
    private boolean is_val_valid(Value op) {
        if (op instanceof Local && op.getType() instanceof RefType) return true;
        if (op instanceof NullConstant) return true;
        if (op instanceof InstanceFieldRef) return true;
        return op instanceof CastExpr && ((CastExpr) op).getCastType() instanceof RefType;
    }
}
