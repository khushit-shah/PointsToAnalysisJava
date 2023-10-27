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


    public LatticeElement transfer_assignment_lhs_var(Statement st) {
        return null;
    }

    public LatticeElement transfer_assignment_lhs_field(Statement st) {
        // p.f = x
        return null;
    }

    @Override
    public LatticeElement transfer(Statement st, boolean isConditional, boolean conditionTaken) {
        return null;
    }

}
