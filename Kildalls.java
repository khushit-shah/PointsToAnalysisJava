import soot.UnitBox;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class Kildalls {

    public static ArrayList<LatticeElement> run(ArrayList<Stmt> statements, Map<Stmt, Integer> indices, LatticeElement d0, LatticeElement bot) {
        HashSet<Integer> marked = new HashSet<>();
        ArrayList<LatticeElement> states = new ArrayList<>(statements.size());

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

            if (statements.get(curPoint) instanceof IfStmt) {
                // Propagate State to true branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements.get(curPoint), true, true);
                    propagate(states, indices.get((Stmt) statements.get(curPoint).getUnitBoxes().get(0).getUnit()), nextNewState, marked);
                }
                // Propagate state to false branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements.get(curPoint), true, false);
                    if (curPoint + 1 < statements.size()) {
                        propagate(states, curPoint + 1, nextNewState, marked);
                    }
                }
            } else {
                LatticeElement nextNewState = curState.transfer(statements.get(curPoint), false, false);
                if (statements.get(curPoint).getUnitBoxes().isEmpty()) {
                    if (curPoint + 1 < statements.size()) {
                        propagate(states, curPoint + 1, nextNewState, marked);
                    }
                } else {
                    // goto or switch kind of statements.
                    for (UnitBox u : statements.get(curPoint).getUnitBoxes()) {
                        propagate(states, indices.get((Stmt) u.getUnit()), nextNewState, marked);
                    }
                }
            }
        }

        return states;
    }

    private static void propagate(ArrayList<LatticeElement> states, int to, LatticeElement nextNewState, HashSet<Integer> marked) {
        LatticeElement nextCurState = states.get(to);

        LatticeElement joined = nextNewState.join_op(nextCurState);

        if (!joined.equals(nextCurState)) {
            // update and mark.
            states.set(to, joined);
            marked.add(to);
        }
    }
}
