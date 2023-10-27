// This program will plot a CFG for a method using soot
// [ExceptionalUnitGraph feature].
// Arguements : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>

// Ref:
// 1) https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
// 2) https://github.com/soot-oss/soot/wiki/Tutorials


////////////////////////////////////////////////////////////////////////////////

import java.util.*;

////////////////////////////////////////////////////////////////////////////////

import soot.*;
import soot.jimple.*;
import soot.options.Options;

import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.util.Switch;
import soot.util.cfgcmd.CFGToDotGraph;
import soot.util.dot.DotGraph;

////////////////////////////////////////////////////////////////////////////////


public class Analysis extends PAVBase {
    private DotGraph dot = new DotGraph("callgraph");
    private static HashMap<String, Boolean> visited = new
            HashMap<String, Boolean>();

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

                System.out.println("Statement: " + u.toString());

                System.out.println("Use Boxes: " + u.getUseBoxes());
                System.out.println("Def Boxes: " + u.getDefBoxes());
                System.out.println("Unit Boxes: " + u.getUnitBoxes());

                index++;
            }

            // Calculating next Statement Index and target Statement Index.


            drawMethodDependenceGraph(targetMethod);
        } else {
            System.out.println("Method not found: " + tMethod);
        }

        System.out.println("\n\n\n\n ----- \n");
    }


    private static void drawMethodDependenceGraph(SootMethod entryMethod) {
        if (!entryMethod.isPhantom() && entryMethod.isConcrete()) {
            Body body = entryMethod.retrieveActiveBody();
            ExceptionalUnitGraph graph = new ExceptionalUnitGraph(body);
            //ExceptionalBlockGraph  graph = new ExceptionalBlockGraph (body);

            CFGToDotGraph cfgForMethod = new CFGToDotGraph();
            cfgForMethod.drawCFG(graph);
            DotGraph cfgDot = cfgForMethod.drawCFG(graph);
            cfgDot.plot("cfg.dot");
        }
    }

    public static void printUnit(int lineno, Body b, Unit u) {
        UnitPrinter up = new NormalUnitPrinter(b);
        u.toString(up);
        String linenostr = String.format("%02d", lineno) + ": ";
        System.out.println(linenostr + up.toString());
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

}


