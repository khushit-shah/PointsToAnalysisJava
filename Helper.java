import soot.Local;
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

}
