import soot.NullType;
import soot.RefType;
import soot.Value;
import soot.jimple.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PointsToLatticeElement implements LatticeElement {

    HashMap<String, HashSet<String>> state;

    HashSet<String> nullOnlySet = new HashSet<>();

    PointsToLatticeElement() {
        state = new HashMap<>();
        nullOnlySet.add("null");
    }

    PointsToLatticeElement(HashMap<String, HashSet<String>> state) {
        this.state = state;
        nullOnlySet.add("null");
    }

    public String toString() {
        StringBuilder ret = new StringBuilder("{");
        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            ret.append(entry.getKey()).append("=>").append(Arrays.toString(entry.getValue().toArray())).append("\n");
        }
        return ret.append("}").toString();
    }


    @Override
    public PointsToLatticeElement join_op(LatticeElement r) {
        PointsToLatticeElement other = (PointsToLatticeElement) r;
        HashMap<String, HashSet<String>> new_state = new HashMap<>();

        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            HashSet<String> base = clone(entry.getValue());
            base.addAll(other.state.getOrDefault(entry.getKey(), new HashSet<>()));

            new_state.put(entry.getKey(), base);
        }

        for (Map.Entry<String, HashSet<String>> entry : other.state.entrySet()) {
            HashSet<String> base = clone(entry.getValue());
            base.addAll(state.getOrDefault(entry.getKey(), new HashSet<>()));

            new_state.put(entry.getKey(), base);
        }

        return new PointsToLatticeElement(new_state);
    }

    @Override
    public boolean equals(LatticeElement r) {
        return state.equals(((PointsToLatticeElement) r).state);
    }

    @Override
    public LatticeElement tf_identity_fn() {
        return new PointsToLatticeElement(clone(state));
    }

    @Override
    public LatticeElement tf_assign_stmt(Stmt st) {
        AssignStmt assignStmt = (AssignStmt) st;


        Value lhs = assignStmt.getLeftOp();
        Value rhs = assignStmt.getRightOp();

        if (!(lhs.getType() instanceof RefType || lhs.getType() instanceof NullType) || !(rhs.getType() instanceof RefType || rhs.getType() instanceof NullType)) {
            return tf_identity_fn();
        }

        // TODO: What is static field? of global object or static object.
        // a or a.<class: type f>
        String lhsStr = Helper.getSimplifiedVarName(lhs.toString());
        String rhsStr = Helper.getSimplifiedVarName(rhs.toString());
        /*
         *  Following cases can occur.
            - a = b;
            - a = newXX;
            - a = null;
            - a = b.f;

            - a.f = b;
            - a.f = newXX;
            - a.f = null;
         */

        if (lhsStr.contains(".")) { // a.f = b
            String lhsVarStr = lhsStr.split("\\.")[0]; // a
            String lhsFieldStr = lhsStr.split("\\.")[1]; // f

            HashSet<String> RhsPointsTo;
            if (rhsStr.startsWith("new")) {
                HashSet<String> newXX = new HashSet<>();
                newXX.add(rhsStr);
                RhsPointsTo = newXX;
            } else if (rhsStr.equals("null")) {
                RhsPointsTo = clone(nullOnlySet);
            } else {
                RhsPointsTo = clone(state.getOrDefault(rhsStr, new HashSet<>()));
            }

            HashSet<String> varPointsTo = state.getOrDefault(lhsVarStr, new HashSet<>());

            HashMap<String, HashSet<String>> newState = clone(state);

            for (String p : varPointsTo) {
                if (p.equals("null")) continue;

                String newLhsStr = p + "." + lhsFieldStr;

                HashSet<String> curSet = newState.getOrDefault(newLhsStr, new HashSet<>());

                curSet.addAll(RhsPointsTo);

                newState.put(newLhsStr, curSet);
            }

            return new PointsToLatticeElement(newState);

        } else if (rhsStr.contains(".")) {
            HashMap<String, HashSet<String>> newState = clone(state);

            String rhsVarStr = rhsStr.split("\\.")[0];
            String rhsFieldStr = rhsStr.split("\\.")[1];

            HashSet<String> rhsVarPointsTo = state.getOrDefault(rhsVarStr, new HashSet<>());

            HashSet<String> lhsPointsTo = new HashSet<>();

            for (String p : rhsVarPointsTo) {
                if (p.equals("null")) continue;

                String newRhsStr = p + "." + rhsFieldStr;

                lhsPointsTo.addAll(state.getOrDefault(newRhsStr, new HashSet<>()));
            }

            newState.put(lhsStr, lhsPointsTo);
            return new PointsToLatticeElement(newState);
        } else {
            // strong update always.
            HashMap<String, HashSet<String>> newState = clone(state);
            if (rhsStr.equals("null")) {
                newState.put(lhsStr, clone(nullOnlySet));
            } else if (rhsStr.startsWith("new")) {
                HashSet<String> newXX = new HashSet<>();
                newXX.add(rhsStr);
                newState.put(lhsStr, newXX);
            } else {
                newState.put(lhsStr, newState.getOrDefault(rhsStr, new HashSet<>()));
            }
            return new PointsToLatticeElement(newState);
        }
    }

    @Override
    public LatticeElement tf_if_stmt(Stmt st, boolean b) {
        IfStmt ifStmt = (IfStmt) st;

        Value condition = ifStmt.getCondition();

        if (condition instanceof EqExpr) {
            if (!(((EqExpr) condition).getOp1().getType() instanceof RefType || ((EqExpr) condition).getOp1().getType() instanceof NullType) || !(((EqExpr) condition).getOp2().getType() instanceof RefType || ((EqExpr) condition).getOp2().getType() instanceof NullType)) {
                return tf_identity_fn();
            }

            Value op1 = ((EqExpr) condition).getOp1();
            Value op2 = ((EqExpr) condition).getOp2();

            String op1Str = Helper.getSimplifiedVarName(op1.toString());
            String op2Str = Helper.getSimplifiedVarName(op2.toString());

            return transfer_eq_eq(b, op1Str, op2Str);
        } else if (condition instanceof NeExpr) {
            if (!(((NeExpr) condition).getOp1().getType() instanceof RefType || ((NeExpr) condition).getOp1().getType() instanceof NullType) || !(((NeExpr) condition).getOp2().getType() instanceof RefType || ((NeExpr) condition).getOp2().getType() instanceof NullType)) {
                return tf_identity_fn();
            }
            Value op1 = ((NeExpr) condition).getOp1();
            Value op2 = ((NeExpr) condition).getOp2();

            String op1Str = Helper.getSimplifiedVarName(op1.toString());
            String op2Str = Helper.getSimplifiedVarName(op2.toString());
            System.out.println(op1Str + " " + op1Str + " " + !b);
            return transfer_eq_eq(!b, op1Str, op2Str);
        } else {
            return tf_identity_fn();
        }
    }

    private LatticeElement transfer_eq_eq(boolean taken, String op1Str, String op2Str) {
        // null == null
        if (op1Str.equals("null") && op2Str.equals("null")) {
            if (taken) return tf_identity_fn();
            else return new PointsToLatticeElement(); // bot.
        }
        // both of the above conditions can be merged into one, but kept separate for readability.
        // a == a
        if (op1Str.equals(op2Str)) {
            if (taken) return tf_identity_fn();
            else return new PointsToLatticeElement(); // In every path, `a` will always be equal to `a`, so bot.
        }

        if (op1Str.equals("null")) {
            op1Str = op2Str;
            op2Str = "null";
        }
        if (taken) { // if op1 == op2 is taken.
            // v1 == null
            HashSet<String> RhsSet;
            if (op2Str.equals("null")) {
                RhsSet = clone(nullOnlySet);
            } else { // v1 = v2; We don't have v1 = v2.f possibility here.
                RhsSet = state.getOrDefault(op2Str, new HashSet<>());
            }

            HashSet<String> lhsSet = clone(state.getOrDefault(op1Str, new HashSet<>()));

            lhsSet.retainAll(RhsSet);

            if (lhsSet.isEmpty()) {
                // no common element in lhs and rhs pointing set.
                return new PointsToLatticeElement();
            } else {
                HashMap<String, HashSet<String>> newState = clone(state);

                newState.put(op1Str, lhsSet);

                if (!op2Str.equals("null"))
                    newState.put(op2Str, lhsSet);

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
                } else { // if one is null and one is not null or both are not null, identity fn is correct.
                    return tf_identity_fn();
                }
            }

            return tf_identity_fn();
        }
    }


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
                setResult(tf_identity_fn());
            }
        };
        st.apply(stmtSwitch);
        return stmtSwitch.getResult();
    }

    public PointsToLatticeElement clone() {
        HashMap<String, HashSet<String>> clonedState = clone(state);
        return new PointsToLatticeElement(clonedState);
    }

    public HashSet<String> clone(HashSet<String> x) {
        return new HashSet<>(x);  // as String is immutable it is okay to shallow clone this.
    }

    public HashMap<String, HashSet<String>> clone(HashMap<String, HashSet<String>> x) {
        HashMap<String, HashSet<String>> cloned = new HashMap<>();

        for (Map.Entry<String, HashSet<String>> entry : x.entrySet()) {
            cloned.put(entry.getKey(), clone(entry.getValue()));
        }

        return cloned;
    }
}
