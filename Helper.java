import soot.Value;

public class Helper {

    public static String getSimplifiedVarName(String string) {
        if(string.contains(".")) {
            String[] splits1 = string.split("\\.");
            String[] splits2 = splits1[1].split(" ");

            return splits1[0] + "." + splits2[splits2.length - 1].substring(0, splits2[splits2.length - 1].length() - 1);
        } else {
            return string;
        }
    }

}
