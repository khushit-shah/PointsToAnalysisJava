# PAV Phase 2

## Analysis Method

The main problem while doing this analysis is the local state should be propagated to the successor of call instruction in the local method, this is hard because the only successor of call statement will be the starting point of the called method. And, as the method variables are not global, while calling we must clear the points to facts of this function (keeping the points to fact of heap objects).
 
At first it seems there are multiple ways the given analysis can be performed, We tried making all variable global, or storing the prev state before a function call is made, both of this methods are not correct, or atleast requires significantly more complex methods/transfer functions.

### The method we used to do intraprocedural analysis.

Add a sudo edge (dummy edge) between the call statement and the successor of call statement in the local method, if the successor is reachable from the called method (to handle the infinite recursion case). Also a NOP statement is added after all the call statements, 
it handles the cases where the called method return is unreachable (and there is only one return in the called method), the state at the NOP will be unreachable-return.

The new PointsToLatticeElement elements:

1) unreachable: this element says the given path is unreachable, when joined with another element, if that element is not reachable, the join is that element (i.e unreachable is bot.)

transfer functions map unreachable to bot.

2) unreachable-return: this element will only occur when we return from called method and the state (element) passed to return was unreachable and there is only one return statement in the function, join of unreachable-return with any element is unreachable-return.

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
