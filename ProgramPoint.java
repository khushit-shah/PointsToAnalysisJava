import soot.SootMethod;
import soot.jimple.Stmt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a program point.
 */
public class ProgramPoint {
    public Stmt stmt;

    // second successor is always true branch in case of if statements.
    public ArrayList<Integer> successors;
    public String correctEndCallString = "";
    // method in which the point resides.
    public SootMethod method;
    int indexInPoints;
    HashMap<ArrayDeque<String>, PointsToLatticeElement> callStringStates = new HashMap<>(); // used by the @parameter statements.
    HashMap<ArrayDeque<String>, ArrayList<String>> callStringArgList = new HashMap<>();
    ApproximateCallStringLatticeElement element;

    ProgramPoint(Stmt stmt) {
        this.stmt = stmt;
        this.successors = new ArrayList<>();
    }
}

