// This program will plot a CFG for a method using soot
// [ExceptionalUnitGraph feature].
// Arguements : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>

// Ref:
// 1) https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
// 2) https://github.com/soot-oss/soot/wiki/Tutorials


////////////////////////////////////////////////////////////////////////////////

import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.util.dot.DotGraph;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

////////////////////////////////////////////////////////////////////////////////


public class Analysis extends PAVBase {


    public Analysis() {

    }

    public static void main(String[] args) {

        String targetDirectory = args[0];
        String mClass = args[1];
        String tClass = args[2];
        String tMethod = args[3];
        boolean methodFound = false;


        List<String> procDir = new ArrayList<String>();
        procDir.add(targetDirectory);

        // Set Soot options
        soot.G.reset();
        Options.v().set_process_dir(procDir);
        // Options.v().set_prepend_classpath(true);
        Options.v().set_src_prec(Options.src_prec_only_class);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_keep_line_number(true);
        Options.v().setPhaseOption("cg.spark", "verbose:false");

        Scene.v().loadNecessaryClasses();

        SootClass entryClass = Scene.v().getSootClassUnsafe(mClass);
        SootMethod entryMethod = entryClass.getMethodByNameUnsafe("main");
        SootClass targetClass = Scene.v().getSootClassUnsafe(tClass);
        SootMethod targetMethod = entryClass.getMethodByNameUnsafe(tMethod);

        Options.v().set_main_class(mClass);
        Scene.v().setEntryPoints(Collections.singletonList(entryMethod));

        // System.out.println (entryClass.getName());
        System.out.println("tclass: " + targetClass);
        System.out.println("tmethod: " + targetMethod);
        System.out.println("tmethodname: " + tMethod);
        Iterator mi = targetClass.getMethods().iterator();
        while (mi.hasNext()) {
            SootMethod sm = (SootMethod) mi.next();
            // System.out.println("method: " + sm);
            if (sm.getName().equals(tMethod)) {
                methodFound = true;
                break;
            }
        }

        if (methodFound) {
            printInfo(targetMethod);

            // for each Unit, represent it with ProgramPoint.


            ArrayList<Helper.Point> pendingMethodSuccessors = new ArrayList<>();

            Queue<SootMethod> queue = new LinkedList<>();
            queue.add(targetMethod);


            while (!queue.isEmpty()) {
                SootMethod curMethod = queue.remove();
                AnalysisInfo.processedMethods.add(curMethod);
                preprocessMethod(AnalysisInfo.points, curMethod, queue, AnalysisInfo.processedMethods, pendingMethodSuccessors, AnalysisInfo.methodStart, AnalysisInfo.methodEnd);
            }

            for (int i = 0; i < pendingMethodSuccessors.size(); i++) {
                ArrayList<Integer> curSuccessors = new ArrayList<>(AnalysisInfo.points.get(pendingMethodSuccessors.get(i).index).successors);

                int callingStatementIndex = pendingMethodSuccessors.get(i).index;
                SootMethod calledMethod = pendingMethodSuccessors.get(i).method;

                int methodStartIndex = AnalysisInfo.methodStart.get(calledMethod);

                AnalysisInfo.points.get(callingStatementIndex).successors.clear();
                AnalysisInfo.points.get(callingStatementIndex).successors.add(methodStartIndex);

                ArrayList<Integer> methodEnds = AnalysisInfo.methodEnd.getOrDefault(calledMethod, new ArrayList<>());

                for (Integer end : methodEnds) {
                    for (Integer succs : curSuccessors) {
                        AnalysisInfo.points.get(end).successors.add(succs);
                    }
                }
                for (Integer succs : curSuccessors) {
                    AnalysisInfo.points.get(succs).correctEndCallString = AnalysisInfo.points.get(succs).method + ".in" + String.format("%02d", callingStatementIndex);
                }
                ArrayList<String> curPrefixes = AnalysisInfo.possiblePrefixes.getOrDefault(calledMethod.toString(), new ArrayList<>());

                curPrefixes.add(AnalysisInfo.points.get(callingStatementIndex).method + ".in" + String.format("%02d", callingStatementIndex));

                AnalysisInfo.possiblePrefixes.put(calledMethod.toString(), curPrefixes);
            }

            drawIntraProceduralUnAnalyzedCFG(AnalysisInfo.points, targetDirectory + File.separator + tClass + "." + tMethod + ".intra.debug");

            // Run the Kildall's

            ApproximateCallStringLatticeElement d0 = new ApproximateCallStringLatticeElement();
            d0.state.put(new ArrayDeque<>(), new PointsToLatticeElement());

            ArrayList<ArrayList<LatticeElement>> output = Kildalls.run(AnalysisInfo.points, d0, new ApproximateCallStringLatticeElement());


            // Write the output files.
//            writeFinalOutput(output.get(output.size() - 1), targetDirectory, tClass + "." + tMethod);
//            writeFullOutput(output, targetDirectory, tClass + "." + tMethod);
//
//
//            // Draw the CFG with states information to .dot file.
//            // Stmt and ProgramPoint have one to one mapping.
            Map<Unit, LatticeElement> mp = new HashMap<>();
            for (int i = 0; i < AnalysisInfo.points.size(); i++) {
                ProgramPoint p = AnalysisInfo.points.get(i);
                mp.put(p.stmt, output.get(output.size() - 1).get(i));
            }
            drawMethodDependenceGraph(targetMethod, AnalysisInfo.points, mp, targetDirectory + File.separator + tClass + "." + tMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }
    }

    /**
     * Write full output to targetDirectory/tMethod.fulloutput.txt, the output format is as follows.
     * For each kildall's iterations, for each program point state which changed from previous iteration,
     * that program point is added to the output, as follows:
     * tMethod: inXX: var: {var_points_to}
     * the lines are lexographically sorted.
     *
     * @param output          ArrayList<ArrayList> States of program elements.
     * @param targetDirectory
     * @param tMethod
     */
    private static void writeFullOutput(ArrayList<ArrayList<LatticeElement>> output, String targetDirectory, String tMethod) {
        ArrayList<String> allLines = new ArrayList<>();

        for (int i = 0; i < output.size(); i++) {
            ArrayList<LatticeElement> iteration = output.get(i);
            Set<ResultTuple> data = new HashSet<>();
            for (int j = 0; j < iteration.size(); j++) {
                PointsToLatticeElement e_ = (PointsToLatticeElement) iteration.get(j);
                for (Map.Entry<String, HashSet<String>> entry : e_.state.entrySet()) {
                    if (entry.getValue().isEmpty()) continue;
                    ResultTuple tuple = new ResultTuple(tMethod, "in" + String.format("%02d", j), entry.getKey(), new ArrayList<>(entry.getValue()));
                    data.add(tuple);
                }
            }

            String[] lines = fmtOutputData(data);

            allLines.addAll(Arrays.asList(lines));
            allLines.add("");
        }

        try {
            FileWriter writer = new FileWriter(targetDirectory + File.separator + tMethod + ".fulloutput.txt");
            for (String line : allLines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Can't write to the file:" + tMethod + ".fulloutput.txt");
            System.out.println("Writing output to stdout:");
            for (String line : allLines) {
                System.out.println(line);
            }
        }
    }

    /**
     * Write the final states info of all program point to targetDirectory/tMethod.output.txt
     * The output format is as mentioned.
     *
     * @param output          ArrayList of states of each program point in order of stmt.
     * @param targetDirectory
     * @param tMethod
     */
    private static void writeFinalOutput(ArrayList<LatticeElement> output, String targetDirectory, String tMethod) {
        Set<ResultTuple> data = new HashSet<>();
        int index = 0;
        for (LatticeElement e : output) {
            PointsToLatticeElement e_ = (PointsToLatticeElement) e;
            for (Map.Entry<String, HashSet<String>> entry : e_.state.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    ResultTuple tuple = new ResultTuple(tMethod, "in" + String.format("%02d", index), entry.getKey(), new ArrayList<>(entry.getValue()));
                    data.add(tuple);
                }
            }
            index++;
        }

        String[] lines = fmtOutputData(data);

        try {
            FileWriter writer = new FileWriter(targetDirectory + "/" + tMethod + ".output.txt");
            for (String line : lines) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();
        } catch (Exception e) {
            System.err.println("Can't write to the file:" + tMethod + ".output.txt");
            System.out.println("Writing output to stdout:");
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }


    private static void drawMethodDependenceGraph(SootMethod entryMethod, ArrayList<ProgramPoint> points, Map<Unit, LatticeElement> labels, String filename) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();

            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

            DotGraph d = new DotGraph(filename);
            for (int i = 0; i < points.size(); i++) {
                Unit unit = points.get(i).stmt;
                d.drawNode(labels.get(unit) + "\n" + unit.toString() + " hash: " + unit.hashCode());
            }
            for (int i = 0; i < points.size(); i++) {
                Unit unit = points.get(i).stmt;
                List<Integer> successors = points.get(i).successors;
                for (Integer succIndex : successors) {
                    Unit succ = points.get(succIndex).stmt;
//                    d.drawNode(labels.get(unit) + "\n" + unit.toString() + " hash: " + unit.hashCode());
//                    d.drawNode(labels.get(succ) + "\n" + succ.toString() + " hash: " + succ.hashCode());
                    d.drawEdge(labels.get(unit) + "\n" + unit + " hash: " + unit.hashCode(), labels.get(succ) + "\n" + succ + " hash: " + succ.hashCode());
                }
            }
            d.plot(filename + ".dot");
        }
    }

    public static void printUnit(int lineno, Body b, Unit u) {
        UnitPrinter up = new NormalUnitPrinter(b);
        u.toString(up);
        String linenostr = String.format("%02d", lineno) + ": ";
        System.out.println(linenostr + up);
    }


    private static void printInfo(SootMethod entryMethod) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();

            int lineno = 0;
            for (Unit u : body.getUnits()) {
                if (!(u instanceof Stmt)) {
                    continue;
                }
                printUnit(lineno, body, u);
                lineno++;
            }

        }
    }

    protected static String fmtOutputLine(ResultTuple tup, String prefix) {
        String line = tup.m + ": " + tup.p + ": " + tup.v + ": " + "{";
        List<String> pointerValues = tup.pV;
        Collections.sort(pointerValues);
        for (int i = 0; i < pointerValues.size(); i++) {
            if (i != pointerValues.size() - 1) line += pointerValues.get(i) + ", ";
            else line += pointerValues.get(i);
        }
        line = line + "}";
        return (prefix + line);
    }


    protected static String fmtOutputLine(ResultTuple tup) {
        return fmtOutputLine(tup, "");
    }

    protected static String[] fmtOutputData(Set<ResultTuple> data, String prefix) {
        String[] outputlines = new String[data.size()];

        int i = 0;
        for (ResultTuple tup : data) {
            outputlines[i] = fmtOutputLine(tup, prefix);
            i++;
        }

        Arrays.sort(outputlines);
        return outputlines;
    }

    protected static String[] fmtOutputData(Set<ResultTuple> data) {
        return fmtOutputData(data, "");
    }


    private static void preprocessMethod(ArrayList<ProgramPoint> points, SootMethod method, Queue<SootMethod> methodsToPreprocess, ArrayList<SootMethod> processedMethods, ArrayList<Helper.Point> pendingMethodSuccessors, HashMap<SootMethod, Integer> methodStart, HashMap<SootMethod, ArrayList<Integer>> methodEnd) {
        System.out.println("pre processing " + method);
        int prevPointSize = points.size();

        UnitPatchingChain units = method.retrieveActiveBody().getUnits();

        // Update each assignment statement containing new expr or InvokeExpr, then change it to newXX.
        int index = 0;
        for (Unit u : units) {
            Stmt cur = (Stmt) u;

            // initialize the current program point.
            ProgramPoint curPoint = new ProgramPoint(cur);
            curPoint.indexInPoints = prevPointSize + index;
            curPoint.method = method;

            if (index == 0) {
                methodStart.put(method, prevPointSize);
            }
            int finalIndex = index;
            cur.apply(new AbstractStmtSwitch() {
                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    Value rightOp = stmt.getRightOp();
                    if (rightOp instanceof InvokeExpr) {
                        SootMethod invokedMethod = ((InvokeExpr) rightOp).getMethod();
                        // if the method of InvokeExpr not in methodsToPreProcess add it.
                        if (!methodsToPreprocess.contains(invokedMethod) && !processedMethods.contains(invokedMethod)) {
                            methodsToPreprocess.add(invokedMethod);
                        }
                        // add the index of this statement
                        pendingMethodSuccessors.add(new Helper.Point(prevPointSize + finalIndex, invokedMethod));
                    }
                    if (rightOp instanceof NewExpr) { // x = new ...
                        stmt.setRightOp(Jimple.v().newLocal("new" + String.format("%02d", finalIndex) + "_" + method, ((NewExpr) rightOp).getBaseType()));
                    }
                }

                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    if (stmt.getInvokeExpr() instanceof SpecialInvokeExpr) return;
                    SootMethod invokedMethod = stmt.getInvokeExpr().getMethod();
                    // if the method of InvokeExpr not in methodsToPreProcess add it.
                    if (!methodsToPreprocess.contains(invokedMethod) && !processedMethods.contains(invokedMethod)) {
                        methodsToPreprocess.add(invokedMethod);
                    }
                    // add the index of this statement
                    pendingMethodSuccessors.add(new Helper.Point(prevPointSize + finalIndex, invokedMethod));
                }
            });

            // add the updated statement to program point.
            points.add(curPoint);
            index++;
        }

        // Build CFG to find successors.
        BriefUnitGraph cfg = new BriefUnitGraph(method.getActiveBody());

        // for each statement, find the successors, and add the successor info to corresponding ProgramPoint info.
        for (int i = prevPointSize; i < points.size(); i++) {
            List<Unit> successors = cfg.getSuccsOf(points.get(i).stmt);
            if (successors.isEmpty()) {
                ArrayList<Integer> curEnds = methodEnd.getOrDefault(method, new ArrayList<>());
                curEnds.add(i);
                methodEnd.put(method, curEnds);
            }
            for (Unit successor : successors) {
                int j = points.stream().map((ProgramPoint p) -> p.stmt).collect(Collectors.toList()).indexOf((Stmt) successor);
                if (j != -1) {
                    points.get(i).successors.add(j);
                }
            }
        }
    }


    private static void drawIntraProceduralUnAnalyzedCFG(ArrayList<ProgramPoint> points, String filename) {

        DotGraph d = new DotGraph(filename);

        for (int i = 0; i < points.size(); i++) {
            Unit unit = points.get(i).stmt;
            d.drawNode(unit.toString() + " hash: " + unit.hashCode());
        }
        for (int i = 0; i < points.size(); i++) {
            List<Integer> successors = points.get(i).successors;
            Unit unit = points.get(i).stmt;
            for (Integer succIndex : successors) {
                Unit succ = points.get(succIndex).stmt;
                d.drawEdge(unit + " hash: " + unit.hashCode(), succ + " hash: " + succ.hashCode());
            }
        }
        d.plot(filename + ".dot");
    }
}


