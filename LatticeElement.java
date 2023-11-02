import soot.jimple.Stmt;


// Receiver object of LatticeElement possess the existing dataflow
// fact at a programpoint.  x.join(y), here x, y are elements of type,
// LatticeElement and x is the receiver object.

// Method implementation should not modify the receiver object. A fresh
// object should be returned.

// Killdall's algorithm should access the dataflow facts only as type
// LatticeElement and should work on any implementation of
// LatticElement for any analysis.

public interface LatticeElement {

    // represents: "this" JOIN "r"
    // this - the existing dataflow fact
    // r    - the incoming dataflow fact
    LatticeElement join_op(LatticeElement r);


    // return true of "r" == "this"
    boolean equals(LatticeElement r);


    // return new LatticeElement same as "this", but not the same object.
    LatticeElement tf_identity_fn();

    // return new LatticeElement with "this" applied to the given assignment statement.
    LatticeElement tf_assign_stmt(Stmt st);


    // return new LatticeElement with "this" applied to the given if statement and branch.
    LatticeElement tf_if_stmt(Stmt st, boolean b);

    LatticeElement transfer(Stmt st, boolean isConditional, boolean conditionTaken);
}

