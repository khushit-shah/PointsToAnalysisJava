import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;
import java.util.HashMap;

public class AnalysisInfo {
    public static HashMap<String, Integer> methodStart = new HashMap<>();
    public static ArrayList<ProgramPoint> points = new ArrayList<>();

    public static HashMap<String, ArrayList<String>> possiblePrevCallEdge = new HashMap<>();
}
