import soot.SootMethod;
import soot.jimple.Stmt;

import java.util.ArrayList;

/**
 * Represents a program point.
 */
public class ProgramPoint {
    public Stmt stmt;

    // second successor is always true branch in case of if statements.
    public ArrayList<Integer> successors;
    public LatticeElement state;
    public String callEdge;
    public SootMethod method;
    public int indexInPoints;
    public int callingEdge;

    ProgramPoint(Stmt stmt) {
        this.stmt = stmt;
        this.successors = new ArrayList<>();
    }
}

