public class Helper {

    /**
     * given a or a.<class: type f>
     * to,   a or a.f
     *
     * @param string
     * @return
     */
    public static String getSimplifiedVarName(String string) {
        // a or a.<class: type f>
        if (string.contains(".")) {
            String[] splits1 = string.split("\\.");
            String[] splits2 = splits1[splits1.length - 1].split(" ");

            return splits1[0] + "." + splits2[splits2.length - 1].substring(0, splits2[splits2.length - 1].length() - 1);
        } else {
            return string;
        }
    }

}
