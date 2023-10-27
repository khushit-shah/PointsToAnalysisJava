import soot.jimple.Stmt;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PointsToLatticeElement implements  LatticeElement {

    HashMap<String, HashSet<String>> state;

    PointsToLatticeElement() {
        state = new HashMap<>();
    }

    PointsToLatticeElement(HashMap<String, HashSet<String>> state) {
        this.state = state;
    }

    @Override
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
        return null;
    }

    @Override
    public LatticeElement tf_if_stmt(Stmt st, boolean b) {
        return null;
    }

    @Override
    public LatticeElement tf_goto_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_nop_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_identity_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_table_switch_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_lookup_switch_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_invoke_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_return_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_return_void_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_enter_monitor_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_exit_monitor_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_throw_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement tf_ret_stmt(Stmt st) {
        return null;
    }

    @Override
    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken) {
        return null;
    }

    public LatticeElement transfer_assignment_lhs_var(Stmt st) {
        return null;
    }

    public LatticeElement transfer_assignment_lhs_field(Stmt st) {
        // p.f = x
        return null;
    }
}
