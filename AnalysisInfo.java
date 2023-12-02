import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;

// Stores global analysis info, which is needed to be shared between Analysis, ApproximateCallStringLatticeElement, and PointsToLatticeElement.
public class AnalysisInfo {
    public static ArrayList<ProgramPoint> points = new ArrayList<>();
    public static ArrayList<SootMethod> processedMethods = new ArrayList<>();

    public static HashMap<String, ArrayList<String>> possiblePrefixes = new HashMap<>();

    public static HashMap<SootMethod, Integer> methodStart = new HashMap<>();
    public static HashMap<SootMethod, ArrayList<Integer>> methodEnd = new HashMap<>();
}
