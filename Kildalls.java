import soot.Unit;
import soot.UnitBox;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;

import java.util.HashSet;
import java.util.Map;

public class Kildalls {

    public static LatticeElement[] run(Stmt[] statements, Map<Stmt, Integer> indices, LatticeElement d0, LatticeElement bot) {
        HashSet<Integer> marked = new HashSet<>();
        LatticeElement[] states = new LatticeElement[statements.length];

        // set all states to bot, mark all points.
        for (int i = 0; i < statements.length; i++) {
            marked.add(i);
            states[i] = bot;
        }

        // set initial point to d0
        states[0] = d0;

        while (!marked.isEmpty()) {
            Integer curPoint = marked.iterator().next();

            // unmark the current point.
            marked.remove(curPoint);

            LatticeElement curState = states[curPoint];

            if (statements[curPoint] instanceof IfStmt) {
                // Propagate State to true branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements[curPoint], true, true);
                    propagate(states, indices.get((Stmt) statements[curPoint].getUnitBoxes().get(0).getUnit()), nextNewState, marked);
                }
                // Propagate state to false branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements[curPoint], true, false);
                    if (curPoint + 1 < statements.length) {
                        propagate(states, curPoint + 1, nextNewState, marked);
                    }
                }
            } else {
                LatticeElement nextNewState = curState.transfer(statements[curPoint], false, false);

                if (statements[curPoint].getUnitBoxes().isEmpty()) {
                    if (curPoint + 1 < statements.length) {
                        propagate(states, curPoint + 1, nextNewState, marked);
                    }
                } else {
                    // goto or switch kind of statements.
                    for (UnitBox u : statements[curPoint].getUnitBoxes()) {
                        propagate(states, indices.get((Stmt) u.getUnit()), nextNewState, marked);
                    }
                }
            }
        }

        return states;
    }

    private static void propagate(LatticeElement[] states, int to, LatticeElement nextNewState, HashSet<Integer> marked) {
        LatticeElement nextCurState = states[to];

        LatticeElement joined = nextNewState.join_op(nextCurState);

        if (!joined.equals(nextCurState)) {
            // update and mark.
            states[to] = joined;
            marked.add(to);
        }
    }
}
