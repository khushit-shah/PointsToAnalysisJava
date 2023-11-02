import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class Kildalls {

    public static ArrayList<ArrayList<LatticeElement>> run(ArrayList<ProgramPoint> statements, Map<Stmt, Integer> indices, LatticeElement d0, LatticeElement bot) {
        HashSet<Integer> marked = new HashSet<>();

        ArrayList<LatticeElement> states = new ArrayList<>(statements.size());
        ArrayList<ArrayList<LatticeElement>> returnValue = new ArrayList<>();

        // set all states to bot, mark all points.
        for (int i = 0; i < statements.size(); i++) {
            marked.add(i);
            states.add(i, bot);
        }
        // set initial point to d0
        states.set(0, d0);

        while (!marked.isEmpty()) {
            Integer curPoint = marked.iterator().next();
            // unmark the current point.
            marked.remove(curPoint);

            LatticeElement curState = states.get(curPoint);
            for (int i = 0; i < statements.get(curPoint).successors.size(); i++) {
                LatticeElement nextNewState = curState.transfer(statements.get(curPoint).stmt, statements.get(curPoint).stmt.branches(), i != 0);
                propagate(states, statements.get(curPoint).successors.get(i), nextNewState, marked);
            }

            returnValue.add(new ArrayList<>(states)); // this is okay because we are not changing LatticeElement anywhere.
        }

        return returnValue;
    }

    private static void propagate(ArrayList<LatticeElement> states, int to, LatticeElement statePropagated, HashSet<Integer> marked) {
        LatticeElement nextCurState = states.get(to); // current state at `to` point.

        LatticeElement joined = statePropagated.join_op(nextCurState);

        if (!joined.equals(nextCurState)) {
            // update and mark.
            states.set(to, joined);
            marked.add(to);
        }
    }
}
