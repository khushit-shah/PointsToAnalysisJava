import fj.Hash;
import org.omg.CORBA.portable.ResponseHandler;
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
        StringBuilder ret = new StringBuilder();
        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            ret.append(entry.getKey()).append("=>").append(Arrays.toString(entry.getValue().toArray())).append("\n");
        }
        return ret.toString();
    }


    @Override
    public PointsToLatticeElement join_op(LatticeElement r) {
        PointsToLatticeElement other = (PointsToLatticeElement) r;
        HashMap<String, HashSet<String>> new_state = new HashMap<>();

        for (Map.Entry<String, HashSet<String>> entry : state.entrySet()) {
            HashSet<String> base = (HashSet<String>) entry.getValue().clone();
            base.addAll(other.state.getOrDefault(entry.getKey(), new HashSet<>()));

            new_state.put(entry.getKey(), base);
        }

        for (Map.Entry<String, HashSet<String>> entry : other.state.entrySet()) {
            HashSet<String> base = (HashSet<String>) entry.getValue().clone();
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
        return new PointsToLatticeElement((HashMap<String, HashSet<String>>) state.clone());
    }

    @Override
    public LatticeElement tf_assign_stmt(Stmt st) {
        System.out.println("State:" + toString());
        System.out.println(st);
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
            a = b;
            a = newXX;
            a = null;
            a = b.f;

            a.f = b;
            a.f = newXX;
            a.f = null;
         */

        if (lhsStr.contains(".")) {
            String lhsVarStr = lhsStr.split("\\.")[0];
            String lhsFieldStr = lhsStr.split("\\.")[1];

            HashSet<String> RhsPointsTo;
            if (rhsStr.startsWith("new")) {
                HashSet<String> newXX = new HashSet<>();
                newXX.add(rhsStr);
                RhsPointsTo = newXX;
            } else if (rhsStr.equals("null")) {
                RhsPointsTo = (HashSet<String>) nullOnlySet.clone();
            } else {
                RhsPointsTo = (HashSet<String>) state.getOrDefault(rhsStr, new HashSet<>()).clone();
            }

            HashSet<String> varPointsTo = state.getOrDefault(lhsVarStr, new HashSet<>());

            HashMap<String, HashSet<String>> newState = (HashMap<String, HashSet<String>>) state.clone();

            for (String p : varPointsTo) {
                if (p.equals("null")) continue;

                String newLhsStr = p + "." + lhsFieldStr;

                HashSet<String> curSet = newState.getOrDefault(newLhsStr, new HashSet<String>());

                curSet.addAll(RhsPointsTo);

                newState.put(newLhsStr, curSet);
            }

            return new PointsToLatticeElement(newState);

        } else if (rhsStr.contains(".")) {
            HashMap<String, HashSet<String>> newState = (HashMap<String, HashSet<String>>) state.clone();

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
            HashMap<String, HashSet<String>> newState = (HashMap<String, HashSet<String>>) state.clone();
            System.out.println("." + rhsStr + ".");
            if (rhsStr.equals("null")) {
                newState.put(lhsStr, (HashSet<String>) nullOnlySet.clone());
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
            if (!(((EqExpr) condition).getOp1().getType() instanceof RefType || ((EqExpr) condition).getOp1().getType() instanceof NullType)
                    || !(((EqExpr) condition).getOp2().getType() instanceof RefType || ((EqExpr) condition).getOp2().getType() instanceof NullType)) {
                return tf_identity_fn();
            }

            Value op1 = ((EqExpr) condition).getOp1();
            Value op2 = ((EqExpr) condition).getOp2();

            String op1Str = Helper.getSimplifiedVarName(op1.toString());
            String op2Str = Helper.getSimplifiedVarName(op2.toString());

            // null == null
            if (op1Str.equals("null") && op2Str.equals("null")) {
                return tf_identity_fn();
            }

            if (b) { // if op1 == op2 is taken.
                // v1 == null
                if (op2Str.equals("null")) {
                    op1Str = op2Str;
                    op2Str = "null";

                    // {} == null
                    HashMap<String, HashSet<String>> newState = (HashMap<String, HashSet<String>>) state.clone();
                    newState.put(op1Str, (HashSet<String>) nullOnlySet.clone());

                    return new PointsToLatticeElement(newState);
                } else { // v1 = v2;

                }
            } else { // if op1 == op2 is not taken.

            }

        } else if (condition instanceof NeExpr) {

        } else {
            return tf_identity_fn();
        }

        return null;
    }

    @Override
    public LatticeElement tf_identity_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken) {
        AbstractStmtSwitch<LatticeElement> stmtSwitch = new AbstractStmtSwitch<LatticeElement>() {
            @Override
            public void caseAssignStmt(AssignStmt stmt) {
                setResult(tf_assign_stmt(stmt));
                System.out.println();
                System.out.println("State After:" + getResult());
            }

            @Override
            public void caseIdentityStmt(IdentityStmt stmt) {
                setResult(tf_identity_fn());
            }

            @Override
            public void caseIfStmt(IfStmt stmt) {
//                setResult(tf_if_stmt(stmt, conditionTaken));
                setResult(tf_identity_fn());
            }

            @Override
            public void defaultCase(Object obj) {
                setResult(tf_identity_fn());
            }
        };
        st.apply(stmtSwitch);
        return stmtSwitch.getResult();
    }

    public LatticeElement transfer_assignment_lhs_var(Stmt st) {
        return null;
    }

    public LatticeElement transfer_assignment_lhs_field(Stmt st) {
        // p.f = x
        return null;
    }
}
