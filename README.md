
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

![z drawio (2)](https://github.com/khushit-shah/PAVPhase1/assets/42430171/4c755edc-7745-4c66-b667-0879575e922d)


## Running

We have implemented around ~35 private tests, which reside in the MyTest file. `run-analysis.sh` file calls all of this tests.

### Note: You might get an error related to newline while running `make`.
There might be some newline issues, as we used the Windows Subsystem of Linux for development.

error-related newline might occur if you use our  `run-analysis.sh`  or `Makefile`. so, before running `make`,
please run `dos2unix */*`, it converts Windows newline `CLRF` to unix newline`RF`.

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
![Screenshot 2023-11-03 215908](https://github.com/khushit-shah/PAVPhase1/assets/42430171/a7fef5c3-7ea2-4d32-a510-a45cfc732c49)
here, `newXX` is the set of all dummy variables that represent allocation sites, `fields` returns all the fields that are accessed by the given dummy variable.

Then the lattice is as follows.
![image](https://github.com/khushit-shah/PAVPhase1/assets/42430171/ee52be6a-bd1e-4228-842e-a50ab7a8fac8)

### Join operation is as follows
![Screenshot 2023-11-03 215001](https://github.com/khushit-shah/PAVPhase1/assets/42430171/7d9ec0d2-da4f-4c27-8846-89f098437d61)

### Transfer functions

#### Assignment transfer function
![Screenshot 2023-11-03 215342](https://github.com/khushit-shah/PAVPhase1/assets/42430171/8d69b810-6d50-49d4-a2b0-09e9ce9aff82)

#### Conditional transfer function
![Screenshot 2023-11-03 215754](https://github.com/khushit-shah/PAVPhase1/assets/42430171/34932a66-1199-4709-a0bb-62439977b0b4)

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
