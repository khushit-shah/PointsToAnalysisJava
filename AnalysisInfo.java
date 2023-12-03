import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class AnalysisInfo {
    public static HashMap<String, Integer> methodStart = new HashMap<>();
    public static ArrayList<ProgramPoint> points = new ArrayList<>();

    public static HashMap<String, ArrayList<String>> possiblePrevCallEdge = new HashMap<>();
    public static ArrayList<SootMethod> processedMethods = new ArrayList<>();
    public static HashMap<String, ArrayList<Integer>> methodEnd = new HashMap<>();
}
