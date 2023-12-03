// DO NOT MODIFY this file.
// You can make modifications by overriding the methods in Analysis.java

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PAVBase {

    protected static String getPrgPointName(int st1) {
        String name1 = "in" + String.format("%02d", st1);
        return name1;
    }

    protected static String fmtOutputLine(ResultTuple tup, String prefix) {
        String line = tup.m + ": " + tup.p + ": " + tup.v + ": " + "{";
        List<String> pointerValues = tup.pV;
        for (String pointers : pointerValues) {
            line += pointers + ", ";
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

    public static class ResultTuple {
        public final String m;
        public final String p;
        public final String v;

        public final List<String> cS;

        public final List<String> pV;

        public ResultTuple(String method, String prgpoint, List<String> cS, String varname, List<String> pointerValues) {
            this.m = method;
            this.p = prgpoint;
            if (varname.startsWith("new")) {
                String f = varname.substring(varname.lastIndexOf(".") + 1);
                String s = varname.substring(0, varname.lastIndexOf("."));
                String md = s.substring(s.indexOf("_") + 1);
                String nn = s.substring(0, s.indexOf("_"));
                varname = md + "." + nn + "." + f;
            }
            this.v = varname;
            this.cS = cS;
            ArrayList<String> pv_ = new ArrayList<>();
            for (String s : pointerValues) {
                if (s.startsWith("new")) {
                    String md = s.substring(s.indexOf("_") + 1);
                    String nn = s.substring(0, s.indexOf("_"));
                    pv_.add(md + "." + nn);
                } else {
                    pv_.add(s);
                }
            }
            this.pV = pv_;
        }
    }
}
