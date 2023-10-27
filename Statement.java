import soot.jimple.Stmt;

import java.util.ArrayList;

public class Statement {
    public Stmt stmt;

    public enum StatementType {
        ASSIGNMENT,
        CONDITIONAL
    }

    public StatementType type;

    public boolean hasNext;

    public boolean hasTarget;
    public int nextIndex;
    public int targetIndex;

    public String rhs;
    public String lhs;

}

