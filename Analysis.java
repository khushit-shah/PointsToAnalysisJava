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

////////////////////////////////////////////////////////////////////////////////


public class Analysis extends PAVBase {

    public Analysis() {
        /*************************************************************
         * XXX you can implement your analysis here
         ************************************************************/
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

            UnitPatchingChain units = targetMethod.getActiveBody().getUnits();

            ArrayList<Stmt> statements = new ArrayList<>();
            Map<Stmt, Integer> indices = new HashMap<>();

            int index = 0;
            for (Unit u : units) {

                Stmt cur = (Stmt) u;

                indices.put(cur, statements.size());

                statements.add(cur);

                int finalIndex = index;
                cur.apply(new AbstractStmtSwitch() {
                    @Override
                    public void caseAssignStmt(AssignStmt stmt) {
                        Value rightOp = stmt.getRightOp();
                        if (rightOp instanceof InvokeExpr) {
                            if (((InvokeExpr) rightOp).getMethod().getReturnType() instanceof RefType) {
                                stmt.setRightOp(Jimple.v().newLocal("new" + String.format("%02d", finalIndex), RefType.v("java.lang.Object")));
                            }
                        } else if (rightOp instanceof NewExpr) {
                            stmt.setRightOp(Jimple.v().newLocal("new" + String.format("%02d", finalIndex), ((NewExpr) rightOp).getBaseType()));
                        }
                    }
                });
                index++;
            }

            ArrayList<ProgramPoint> points = new ArrayList<>();
            for (Stmt stmt : statements)
                points.add(new ProgramPoint(stmt));

            BriefUnitGraph cfg = new BriefUnitGraph(targetMethod.getActiveBody());

            for (Unit unit : cfg) {
                int i = statements.indexOf((Stmt) unit);
                if (i == -1) continue;
                List<Unit> successors = cfg.getSuccsOf(unit); // Get successors of the current unit
                System.out.println("For: " + unit);
                for (Unit successor : successors) {
                    int j = statements.indexOf((Stmt) successor);
                    if (j != -1) {
                        System.out.println("\t: " + statements.get(j));
                        points.get(i).successors.add(j);
                    }
                }
            }

            ArrayList<ArrayList<LatticeElement>> output = Kildalls.run(points, indices, new PointsToLatticeElement(), new PointsToLatticeElement());

            writeFinalOutput(output.get(output.size() - 1), targetDirectory, tClass + "." + tMethod);
            writeFullOutput(output, targetDirectory, tClass + "." + tMethod);

            Map<Unit, LatticeElement> mp = new HashMap<>();
            for (int i = 0; i < points.size(); i++) {
                ProgramPoint p = points.get(i);
                mp.put(p.stmt, output.get(output.size() - 1).get(i));
            }
            drawMethodDependenceGraph(targetMethod, mp, targetDirectory + File.separator + tClass + "." + tMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }

    }

    private static void writeFullOutput(ArrayList<ArrayList<LatticeElement>> output, String targetDirectory, String tMethod) {
        ArrayList<String> allLines = new ArrayList<>();

        for (int i = 0; i < output.size(); i++) {
            ArrayList<LatticeElement> iteration = output.get(i);
            Set<ResultTuple> data = new HashSet<>();
            for (int j = 0; j < iteration.size(); j++) {
                PointsToLatticeElement e_ = (PointsToLatticeElement) iteration.get(j);
                if (i != 0 && e_.equals(output.get(i - 1).get(j)))
                    continue; // only output the states that changed from previous iterations.
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


    private static void drawMethodDependenceGraph(SootMethod entryMethod, Map<Unit, LatticeElement> labels, String filename) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();

            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);

            DotGraph d = new DotGraph(filename);

            int i = 0;
            for (Unit unit : graph) {
                List<Unit> successors = graph.getSuccsOf(unit);
                for (Unit succ : successors) {
                    d.drawNode(labels.get(unit) + "\n" + unit.toString() + " hash: " + unit.hashCode());
                    d.drawNode(labels.get(succ) + "\n" + succ.toString() + " hash: " + succ.hashCode());
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
                Stmt s = (Stmt) u;
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
}


