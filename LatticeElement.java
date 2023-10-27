import soot.jimple.Stmt;

import javax.swing.plaf.nimbus.State;


// Receiver object of LatticeElement possess the existing dataflow
// fact at a programpoint.  x.join(y), here x, y are elements of type,
// LatticeElement and x is the receiver object.

// Method implementation should not modify the receiver object. A fresh
// object should be returned.

// Killdall's algorithm should access the dataflow facts only as type
// LatticeElement and should work on any implementation of
// LatticElement for any analysis.

public interface LatticeElement
{

    public LatticeElement join_op(LatticeElement r);
    // represents: "this" JOIN "r"
    // this - the existing dataflow fact
    // r    - the incoming dataflow fact

    public boolean equals(LatticeElement r);

    public LatticeElement tf_assign_stmt(Stmt st);
    public LatticeElement tf_if_stmt(Stmt st, boolean b);
    public LatticeElement tf_goto_stmt(Stmt st);

    public LatticeElement tf_nop_stmt(Stmt st);
    public LatticeElement tf_identity_stmt(Stmt st);

    public LatticeElement tf_table_switch_stmt(Stmt st);
    public LatticeElement tf_lookup_switch_stmt(Stmt st);
    public LatticeElement tf_invoke_stmt(Stmt st);
    public LatticeElement tf_return_stmt(Stmt st);
    public LatticeElement tf_return_void_stmt(Stmt st);
    public LatticeElement tf_enter_monitor_stmt(Stmt st);
    public LatticeElement tf_exit_monitor_stmt(Stmt st);

    public LatticeElement tf_throw_stmt(Stmt st);

    public LatticeElement tf_ret_stmt(Stmt st);

    public LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken);
}

