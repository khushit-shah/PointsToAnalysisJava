import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AnalysisInfo {
    public static HashMap<String, Integer> methodStart = new HashMap<>();
    public static ArrayList<ProgramPoint> points = new ArrayList<>();

    public static HashMap<String, HashSet<String>> possiblePrevCallEdge = new HashMap<>();
    public static ArrayList<SootMethod> processedMethods = new ArrayList<>();
    public static HashMap<String, ArrayList<Integer>> methodEnd = new HashMap<>();

    public static PointsToLatticeElement unreachableState;

    public static PointsToLatticeElement returnUnreachableState;

    static {
        unreachableState = new PointsToLatticeElement();
        unreachableState.state.put("unreachable", new HashSet<>());

        returnUnreachableState = new PointsToLatticeElement();
        returnUnreachableState.state.put("unreachable-return", new HashSet<>());
    }
}
