import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class TestJoin {
    public static void main(String[] args) {
        PointsToLatticeElement l1, l2;

        /**
         * Tests basic
         */
        HashMap<String, HashSet<String>> l1_state = new HashMap<>();
        HashMap<String, HashSet<String>> l2_state = new HashMap<>();

        l1_state.put("p", new HashSet<>((Arrays.asList("x", "y"))));
        l1_state.put("x", new HashSet<>((Arrays.asList("a", "b"))));
        l1_state.put("a", new HashSet<>((Arrays.asList("a", "b"))));

        l2_state.put("p", new HashSet<>((Arrays.asList("z"))));
        l2_state.put("x", new HashSet<>((Arrays.asList("p", "q"))));
        l2_state.put("q", new HashSet<>((Arrays.asList("a", "b"))));

        l1 = new PointsToLatticeElement(l1_state);
        l2 = new PointsToLatticeElement(l2_state);

        System.out.println(l1.join_op(l2));


        /**
         * Check with null.
         */


        l1_state = new HashMap<>();
        l2_state = new HashMap<>();

        l1_state.put("p", new HashSet<>((Arrays.asList(""))));
        l1_state.put("x", new HashSet<>((Arrays.asList("a", "b"))));
        l1_state.put("a", new HashSet<>((Arrays.asList("a", "b"))));

        l2_state.put("p", new HashSet<>((Arrays.asList("z"))));
        l2_state.put("x", new HashSet<>((Arrays.asList("p", "q"))));
        l2_state.put("q", new HashSet<>((Arrays.asList("", "b"))));

        l1 = new PointsToLatticeElement(l1_state);
        l2 = new PointsToLatticeElement(l2_state);

        System.out.println(l1.join_op(l2));


        l1_state = new HashMap<>();
        l2_state = new HashMap<>();

        l1_state.put("p", new HashSet<>());
        l1_state.put("x", new HashSet<>((Arrays.asList("a", "b"))));
        l1_state.put("a", new HashSet<>((Arrays.asList("a", "b"))));

        l2_state.put("p", new HashSet<>((Arrays.asList("z"))));
        l2_state.put("x", new HashSet<>());
        l2_state.put("q", new HashSet<>((Arrays.asList("", "b"))));

        l1 = new PointsToLatticeElement(l1_state);
        l2 = new PointsToLatticeElement(l2_state);

        System.out.println(l1.join_op(l2));
    }
}
