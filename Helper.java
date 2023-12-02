import soot.Local;
import soot.RefType;
import soot.SootMethod;
import soot.Value;
import soot.jimple.CastExpr;
import soot.jimple.InstanceFieldRef;
import soot.jimple.NullConstant;

public class Helper {

    /**
     * @param Value
     * @return
     */
    public static String getSimplifiedVarName(Value var, SootMethod method) {
        String other = getSimplifiedVarNameInternal(var);
        if (is_val_valid(var) && !(var instanceof NullConstant) && !other.startsWith("new")) {
            other += "$$$" + method.toString();
        }
        return other;
    }

    public static String getSimplifiedVarNameInternal(Value var) {
        if (var instanceof Local) {
            return ((Local) var).getName();
        } else if (var instanceof CastExpr) {
            return getSimplifiedVarNameInternal(((CastExpr) var).getOp());
        } else if (var instanceof InstanceFieldRef) {
            return getSimplifiedVarNameInternal(((InstanceFieldRef) var).getBase()) + "." + ((InstanceFieldRef) var).getFieldRef().name();
        } else {
            return "null";
        }
    }

    /**
     * If a value is Local or NullConstant or InstanceFieldRef or CastExpr with castType of RefType, returns true.
     * else false.
     * This ensure, all the operand we operate on is ref type.
     *
     * @param op
     * @return
     */
    public static boolean is_val_valid(Value op) {
        if (op instanceof Local && op.getType() instanceof RefType) return true;
        if (op instanceof NullConstant) return true;
        if (op instanceof InstanceFieldRef) return true;
        return op instanceof CastExpr && ((CastExpr) op).getCastType() instanceof RefType;
    }

    public static class Point {
        Integer index;
        SootMethod method;

        public Point(Integer index, SootMethod method) {
            this.index = index;
            this.method = method;
        }
    }

}
