import soot.Local;
import soot.SootMethod;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;

public class Helper {

    /**
     * @param Value
     * @return
     */
    public static String getSimplifiedVarName(Value var) {
        if (var instanceof Local) {
            return ((Local) var).getName();
        } else if (var instanceof CastExpr) {
            return getSimplifiedVarName(((CastExpr) var).getOp());
        } else if (var instanceof InstanceFieldRef) {
            return getSimplifiedVarName(((InstanceFieldRef) var).getBase()) + "." + ((InstanceFieldRef) var).getFieldRef().name();
        } else {
            return "null";
        }
    }

    public static String getCallString(ProgramPoint pt) {
        return pt.method.getName() + ".in" + String.format("%02d", pt.indexInPoints - AnalysisInfo.methodStart.get(pt.method.getName()));
    }

    public static ProgramPoint pointFromCallString(String callString) {
        String methodStr = callString.substring(0, callString.lastIndexOf(".in"));
        int indexInMethod = Integer.parseInt(callString.substring(callString.lastIndexOf(".in") + 3));
        int methodStart = AnalysisInfo.methodStart.get(methodStr);
        return AnalysisInfo.points.get(methodStart + indexInMethod);
    }

    public static class Point {
        public int index;
        public SootMethod method;
        public Point(int i, SootMethod invokedMethod) {
            index = i;
            method = invokedMethod;
        }
    }
}
