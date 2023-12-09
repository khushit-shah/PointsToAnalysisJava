# PAV Phase 2

## Analysis Method

The main problem while doing this analysis is the local state should be propagated to the successor of call instruction in the local method, this is hard because the only successor of call statement will be the starting point of the called method. And, as the method variables are not global, while calling we must clear the points to facts of this function (keeping the points to fact of heap objects).
 
At first it seems there are multiple ways the given analysis can be performed, We tried making all variable global, or storing the prev state before a function call is made, both of this methods are not correct, or atleast requires significantly more complex methods/transfer functions.

### The method we used to do intraprocedural analysis.

Add a sudo edge (dummy edge) between the call statement and the successor of call statement in the local method, if the successor is reachable from the called method (to handle the infinite recursion case). Also a NOP statement is added after all the call statements, 
it handles the cases where the called method return is unreachable (and there is only one return in the called method), the state at the NOP will be unreachable-return.

See the following figures:
1)![z drawio (2)](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/PubTest.test1.intra.debug.png?ref_type=heads)

2)![z drawio (2)](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/PubTest.test5_rec.intra.debug.png?ref_type=heads)


The new PointsToLatticeElement elements:

1) unreachable: this element says the given path is unreachable, when joined with another element, if that element is not reachable, the join is that element (i.e unreachable is bot.)

all transfer functions map unreachable to unreachable.

2) unreachable-return: this element will only occur when we return from called method and the state (element) passed to return was unreachable and there is only one return statement in the function, join of unreachable-return with any element is unreachable-return.

all transfer functions map unreachable-return to unreachable.

Imagine an infinite recursion happening on a condition (condition is always true, so the function is always called recursively), here the return statement of the function will be unreachable, and when we return from a unreachable state, we set the state of successor to unreachable-return, because of this any state passed by sudo edges to this successor will be lost, this is more precise because the successor is not reachable.

Look at the following example.

```
    static void test23(int x) {
        PubTest v1 = new PubTest();
        if(x <= 10) {
            v1.f = new PubTest(); --> *
            check_rec(null, null); // call edge A
            v1.f = new PubTest(); // this line is not reachable, unreachable-return will be return, so the state we get from sudo edge, 
            // (state at *) will be joined with unreachable-return and it will be unreachable-return.
            // which will make the statement uneffective.
         else {
            v1.f = new PubTest(); --> @
            check_rec(new PubTest(), null); // call edge B
            // this is reachable and the state from sudo edge (state of @) will be propagated.
            v1.f = new PubTest(); // this statement will be effective as it is reachable.
        }
    }

    static void check_rec(PubTest v1, PubTest v2) {
        if(v1 == v2) { // always true for callstring A, AC, CC.
            check_rec(v1, v2); // call edge C
        }
        return; // for callstring, A, AC, CC this will be unreachable, so we will return the state unreachable-return.
    }
```

#### The need of single return per method for preciseness

Consider the following code.

```
   static void test24() {
        // multiple returns
        test24_in(new PubTest());
    }

    static void test24_in(PubTest v1) {
        if(null != null) {
            return; // return here is unreachable, if we return unreachable-return, the state passed from return at A will also be lost, refer to the following state diagram. 
        }

        v1.f = null;
        return; // A
    }
```

states diagram:
![z drawio (2)](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/PubTest.test24.png?ref_type=heads)

As we can see from the figure, if there are multiple return in a function we can't return the state unreachable-return, so the most precise result we can get is to set the state to unreachable.

#### what is the need of NopStmt?

Assume  that NopStmt is not added.

consider the following case.
```
if(...) {
    // reachable.
    ...
} else {
    // reachable
    
    foo() // last statement in else, infinite recursionin
    // inside foo().
}
s1
```
Here, s1 has three predecessors, 
1) the last statement of if.
2) the calling foo() statement (sudo edge)
3) return statement of foo()

As return statement of foo() is unreachable, it will return the state unreachable-return, when joined with state we got from if and the calling foo() statement, we get unreachable-return state.

This is incorrect, as from if the statement is reachable.

This is why we add a NopStmt after every call statement. NopStmt is tought as being inside of the else. hence after the NopStmt the state from else will be unreachable. When joined with the state from if we get the correct result.

## Program Flow

We run BFS from the target method, creating CFG of all the called methods similarly to what we did in Phase 1, except for staticinvoke with return type as ref are not converted to new.. in AssignmentStmt.

While doing it we save all the call statements.

After all the methods have been processed, for each call statement we add the start of the called method as successor and A NopStmt as the successor of the called method return statements. The successors of NopStmt is same as the previous successor of calling statement. So, at this point calling statement has one successor (starting of calledmethod), calledmethod return has NopStmt as successor, and NopStmt has all the prev successors of calling statement.

Now, if the NopStmt is reachable from the calledmethod start we add a sudo edge from the calling statement to the NopStmt. This checks the cases where there is infinite recursion.

After this we just simply run the Kildalls on the intraprocedural cfg, **Note we haven't changed the Kildalls implementation at all from phase 1.**

Then same as phase 1, CFG with states, full output and output files are written as respective locations.

## Running

We have implemented around ~25 private tests, which reside in the PubTest file. `run-analysis.sh` file calls all of this tests.

## Important files and methods

Other than Phase 1 files, following are important files/methods:

- `AnalysisInfo` class contains global static fields that are used all over the analysis, like all the points, methodStart and methodEnd indices of a particular method, possible previous call edges of a method. 

- `InterProcPointsToLatticeElement` class contains the LatticeElement used for InterProcedural Analysis.

- `Analysis.processthod` adds the CFG of the given method to the global CFG, processing the given method as done in phase 1.

## Analysis

**Our code is independent of K**, for any K value code will work without any change, by default K=2.


The Lattice element is a pair (a, b), a is function that maps callstring to PointsToLatticeElement. b is a function that maps callstring to list of set of newXX, b defines the parameter passed to the function

### Join operation is as follows
(a, b) join (p, q) is (x, y) where 

for each callstring c in x, it pairwise joins a(c) join p(c)

for each callstring c in y, it pairwize unions b(c) union q(c)

### Transfer functions

#### Assignment transfer function
Points wise transfer function of PointsToLatticeElement for each callstring.

parameter function is same (identity)

#### Conditional transfer function
Points wise transfer function of PointsToLatticeElement for each callstring.

parameter function is same (identity)

#### Call transfer function

for each callString x, the new callstring xA where A is the calledge, in the PointsToLatticeElement only the facts for heap objects are passed. the parameter list is also created and passed as parameter for the given callstring.


#### Return transfer function

for each callstring AxY, where the calledge Y matches the appropriate return edge, the new callstring is pred(A)Ax, the new PointsToLatticeElement is only heap objects, the parameter set of the callstring is empty.

#### IdentityStmt transfer function
p0 = @paramter(index), (p, q)
for each callstring x, map p0 to q(x)[index] in p(x).

## Different cases covered
All the cases in Phase 1, plus the following.

```
a = fun()
a.f = fun()
fun()
fun(null)
return null;
return;
return x;
```


# PAV Phase 1

## Program Flow

  

We get the active body of the method from the boilerplate code already given, we extract all the `Stmt` using the `getUnits()` method. We then use `BriefUnitGraph`, to get the control  flow of the program, which we use to find the `successor` of each program statement. We store the statement and its successor in a `ProgramPoint` Object.

We also change `x = new ...` and `x = fun()` types of Stmt to `x = newXX` before calling Kildalls.

This gives a list of 	`ProgramPoint` objects, in order of Jimple statements. This list is passed to 
`Kildalls.run(points: points, d0: bot, bot: bot)`, which takes a list of points to run Kildalls on, `d0 `is initial state (initial `LatticeElement`), `bot` LatticeElement. 

The appropriate transfer functions are implemented in `PointsToLatticeElement` which implements `LatticeElements`, which are called from `Kildalls`.

`Kildalls.run` returns `ArrayList<ArrayList<LatticeElements>>` which is the output of each iteration of `Kildalls`.

Kildalls output is used to write the last iteration output to `targetDirectory/tClass.tMethod.output.txt`, all the iterations are printed to file  `targetDirectory/tClass.tMethod.fulloutput.txt`.

The CFG of the program with states at each program point  is printed to the file `targetDirectory/tClass.tMethod.dot`.

Which is then converted to `.png` file with `dot` command in `run-analysis-one.sh`

![z drawio (2)](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/z.drawio__2_.png)


## Running

We have implemented around ~35 private tests, which reside in the MyTest file. `run-analysis.sh` file calls all of this tests.

### Note: You might get an error related to newline while running `make`.
There might be some newline issues, as we used the Windows Subsystem of Linux for development.

error-related newline might occur if you use our  `run-analysis.sh`  or `Makefile`. so, before running `make`,
please run `dos2unix` and `dos2unix */*`, it converts Windows newline `CLRF` to unix newline`RF`.
like the following error:
```
/usr/bin/env: ‘bash\r’: No such file or directory
```

## Important files and methods
`ProgramPoint` class represents a program point, It contains Stmt and list of index of it's `successors`.

`Kildalls.run` runs Kildalls LFP algorithm

`Kildalls.propagate` propagates newState to the given program point. and sets it to be marked if the join of newState and oldState is different then oldState.

`PointsToLatticeElement.transfer` takes Stmt, and applies an appropriate transfer function based on the type of statement.

`PointsToLatticeElement.tf_if_stmt` takes IfStmt, a transfer function for condition statements, calls `transfer_eq_eq()` internally for both `EqExpr`, `NeExpr`.

`PointsToLatticeElement.tf_assign_stmt` takes AssignStmt, a transfer function for assignment statements.

`Helper.getSimplifiedName()` takes Value, and returns a simplified name, for e.g. maps
```
a -> a
(Cast) a -> a
a.<class: type f> -> a.f
null -> null
```

`PointsToLatticeElement.is_val_valid` takes Value, and returns true if it is a valid, all the valid types is as follows.
  - Local and Local.type instance of RefType
  - NullConstant
  - InstanceFieldRef
  - CastExpr and CastExpr.castType instanse of RefType

`PointsToLatticeElement.clone` takes `HashMap` or `HashSet` and returns a deep cloned version of it.

## Analysis

We will try to mathematically define all the transfer functions we used, 
Let,
![Screenshot 2023-11-03 215908](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/Screenshot_2023-11-03_215908.png)

here, `newXX` is the set of all dummy variables that represent allocation sites, `fields` returns all the fields that are accessed by the given dummy variable.

Then the lattice is as follows.
![image](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/Screenshot_2023-11-03_220545.png)

### Join operation is as follows
![Screenshot 2023-11-03 215001](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/Screenshot_2023-11-03_215001.png)

### Transfer functions

#### Assignment transfer function
![Screenshot 2023-11-03 215342](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/Screenshot_2023-11-03_215342.png)

#### Conditional transfer function
![Screenshot 2023-11-03 215754](https://gitlab.com/khushit_gnani/pav-2023-team-02/-/raw/master/doc-images/Screenshot_2023-11-03_215754.png)

## Different cases covered
We are handling the following cases.
#### assignment cases.
```
a = b;
a = (Cast) b;
a = b.f;
a.f = b;
a = null;
a.f = null;
a = new ...;
a.f = new ...;
a = fun();
a.f = fun();
```
In Jimple we cant have `a.f = b.f `, as it is a 3ac.
#### Condition statement cases.
```
if x == y
if x != y
if null == null
if null != null
if x == x
if x != x
```
