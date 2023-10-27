import java.util.HashSet;

public class Kildalls {

    public static LatticeElement[] run(Statement[] statements, LatticeElement d0, LatticeElement bot) {

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

            if (statements[curPoint].type == Statement.StatementType.CONDITIONAL) {
                // Propagate State to true branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements[curPoint], true, true);
                    propagate(states, statements[curPoint].targetIndex, nextNewState, marked);
                }
                // Propagate state to false branch.
                {
                    LatticeElement nextNewState = curState.transfer(statements[curPoint], true, false);
                    if (statements[curPoint].hasNext) {
                        propagate(states, statements[curPoint].nextIndex, nextNewState, marked);
                    }
                }
            } else {
                LatticeElement nextNewState = curState.transfer(statements[curPoint], false, false);
                if (statements[curPoint].hasNext) {
                    propagate(states, statements[curPoint].nextIndex, nextNewState, marked);
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
