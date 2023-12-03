import java.util.ArrayList;
import java.util.HashSet;

public class Kildalls {

    /**
     * Runs Kildall's LFP Algo on the given program points.
     *
     * @param points the list of program points.
     * @param d0     the initial state of the first program point p0.
     * @param bot    bot LatticeElement.
     * @return
     */
    public static ArrayList<ArrayList<LatticeElement>> run(ArrayList<ProgramPoint> points, LatticeElement d0, LatticeElement bot) {
        // set of marked points.
        HashSet<Integer> marked = new HashSet<>();

        // variable to store result of each iteration.
        ArrayList<ArrayList<LatticeElement>> returnValue = new ArrayList<>();

        ArrayList<LatticeElement> states = new ArrayList<>();

        // set all states to bot, mark all points.
        for (int i = 0; i < points.size(); i++) {
            marked.add(i);
            points.get(i).state = bot;
        }

        // set initial point to d0
        points.get(0).state = d0;

        while (!marked.isEmpty()) {
            Integer curPoint = marked.iterator().next();

            // unmark the current point.
            marked.remove(curPoint);

            LatticeElement curState = points.get(curPoint).state;
            for (int i = 0; i < points.get(curPoint).successors.size(); i++) {
                //  transfer the curState through the statement and get the new state.
                LatticeElement nextNewState = curState.transfer(points.get(curPoint), points.get(curPoint).stmt.branches(), i != 0, i);
                // propagate the new state to the successor.
                propagate(points, points.get(curPoint).successors.get(i), nextNewState, marked);
            }
            states.clear();
            for (ProgramPoint point : points) {
                states.add(point.state);
            }

            returnValue.add(new ArrayList<>(states)); // this is okay because we are not changing LatticeElement anywhere.
        }

        return returnValue;
    }

    /**
     * Propagates the newState to the given program point.
     *
     * @param states          list of states.
     * @param to              the point to propagate the state to.
     * @param statePropagated the new state to propagte.
     * @param marked          the  Set of marked points, which will be updated.
     */
    private static void propagate(ArrayList<ProgramPoint> points, int to, LatticeElement statePropagated, HashSet<Integer> marked) {
        LatticeElement nextCurState = points.get(to).state; // current state at `to` point.

        LatticeElement joined = statePropagated.join_op(nextCurState); // join curState and stateToPropagate.

        if (!joined.equals(nextCurState)) { // if the nextCurState <= joined
            // update and mark.
            points.get(to).state = joined; // set the new state.
            marked.add(to); // mark the point.
        }
    }
}
