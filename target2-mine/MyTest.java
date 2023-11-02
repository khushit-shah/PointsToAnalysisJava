public class MyTest {
    public static void fun1(int x) {
        int j = 10;
        MyTest n = new MyTest();

        if (n == null) {
            j++;
        }

        if (new MyTest() == n) {
            j++;
        }

        if (j == 0) {
            j = 567;
        } else {
            j = 134;
        }
        String i = "Red";
        switch (i) {
            case "Blue":
                j++;
                n = new MyTest();
                break;
            case "Red":
                j++;
                n = new MyTest();
                break;
        }
        {
            j++;
            n.f = new MyTest();
        }
    }

    static void SizeOneNULLTest() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        v1 = null;
        v2 = null;

        if (v2 == v1) {
            v2 = new MyTest();
        } else {
            v1.f = v2;
        }
        v1 = v2 = v1.f;
    }

    static MyTest some_test() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        MyTest v3 = new MyTest();
        v1.f = v2;
        v2.f = v3;
        v1.f = v2.f;
        v1 = v3.f;
        return v1;
    }

    MyTest f;
    static MyTest f1;

    static MyTest getTest() {
        return null;
    }

    static int getInt() {
        return 0;
    }

    static void fun2() {
        int i = getInt();
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        MyTest v3 = getTest();
        v2.f = v1;
        v3.f = new MyTest();
        i++;
        System.out.println(i);

        f1.f = v1;
        v2 = f1.f;
        v2.f = v1.f;
    }

    public static void assign_check() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        MyTest v3 = new MyTest();
        v1.f = v3;
        v1.f = v2;
        v2.f = v1;
        v2.f = v1.f;
    }

    public static void assignment_and_conditional_check() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        MyTest v3 = new MyTest();


        v1.f = v2.f = v3;
        if (v1.f == v2.f && v2.f == v3) {
            v2.f = null;
        } else v2.f = v1;

    }

    public static void empty_function_check() {
    }

    public static void assigning_object_to_field() {
        MyTest v2 = new MyTest();
        Object obj = new Object();
        v2.f = (MyTest) obj;
        if (v2.f == obj) {
            v2.f = v2;
        }
        // Need to verify this also as o/p is different
    }

    public static void object_comparision() {
        // doubt whether o/p is correct or not
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        // Using equals method for content-based comparison
        if (obj1.equals(obj2)) {
            obj3 = obj1;
        } else {
            obj3 = obj2;
        }

        // Using == operator for reference-based comparison
        if (obj1 == obj2) {
            obj3 = obj1;
        } else {
            obj1 = obj2;
        }
    }

    public static void conditional_check1() {

        MyTest v1 = new MyTest();
        v1.f = null;
        Object t1 = null;
        if (v1.f == t1) {
            v1.f = null;
        }
    }


    public static void comaprison_of_object_references() {
        int n = 10;
        MyTest v2 = new MyTest();
        MyTest v1 = new MyTest();
        if (v2 == v1) {
            n = 5;
        } else
            v2.f = v1.f;

        // MyTest v1=new MyTest();
        // MyTest v2= new MyTest();
        // v2=v1.f;
        // if(v1==v2)
        // {
        //     v1.f=v2.f=null;
        // }   
        // else v1.f=v2.f=null;
    }

    public static void circular_references() {
        MyTest v1 = new MyTest();
        MyTest v3 = new MyTest();
        v1.f = v1;
        v3.f = v1;
        Object v2 = null;
        if (v1.f == v2) {
            v1.f = null;
            v2 = v1;
            if (v2 != v1) {
                v1.f = v3.f;
            }

        }
    }

    public static void chained_assignments() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        MyTest v3 = new MyTest();
        MyTest v4 = new MyTest();
        v1.f = v2.f = v3.f = null;
        v4 = v3.f;
        v4.f = null;
    }

    public static void implict_type_conversion() {

        int integerNumber = 42;
        double doubleNumber = integerNumber;
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        if (doubleNumber == integerNumber) {
            v1.f = v2.f = v1 = v2;
        } else
            v1.f = null;
    }

    public static void compare_two_null_values() {
        // we successfully propagated bot to else branch as in java we know null==null is true always
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        if (null == null) {
            v1.f = v2;
        } else {
            v2.f = v1;
        }
    }

    public static void nullable_object() {
        Object obj = null;
        MyTest v1 = new MyTest();
        if (obj == null) {
            v1.f = null;
        } else v1.f = v1;

    }

    public static void conditional_check_bothNULL() {

        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        if (v1 == v2) {
            v1 = null;
        } else
            v2 = null;

        v1 = null;
        if (v1 == v2) {
            v2 = new MyTest();
        } else {
            v1 = new MyTest();
        }
    }

    public static void conditional_check_early_returns() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        v2.f = v1;
        // Intra-procedural Test Case with Early Returns
        if (v2.f == v2.f) {
            v1.f = null;
            v2.f = v1.f;

            return; // Early return here

        }
        v2 = new MyTest();
    }

    public static void conditional_check_multiple_if() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        if (v1 != v2) return;
        else if (v1 == v2) return;

        if (v1 == v2) {
            if (v1 != v2) {
                v1 = new MyTest();
            } else {
                v1 = null;
            }
        } else {
            if (v1 == v2) {
                v2 = new MyTest();
            } else {
                v2 = null;
            }
        }
        v1.f = null;
    }
    public static MyTest get_object()
    {
        MyTest v2= new MyTest();
        v2.f=v2;
        return v2;
    }

     public static void function_returns_object1()
    {
        MyTest v1= new MyTest();
        int x;
        v1=get_object().f;   
        v1.f=null;
    }

    public static void function_returns_object2()
    {
        MyTest v1= new MyTest();
        int x;
        v1.f=get_object().f;   
        v1.f=null;
    }

    public static void function_returns_object3()
    {
        MyTest v1= new MyTest();
        int x;
        v1.f=get_object();   
        v1.f=null;
    }


    public static int get_int()
    {
        int a =10;
        return a;
    }
   
    public static void function_returns_int()
    {
        int x=1;
        MyTest v1= new MyTest();
        x=get_int();
       
        if(x==10)
        {
           v1.f=v1;
        }
        else
        v1.f=null;

    }
    public static void conditional_check_both_equal() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();
        if (v1 == v1) v1.f = v2;
        else v1.f = null;
    }

    public static void conditional_check_multiple_conditionals_with_mixed_assignments() {
        MyTest v1 = new MyTest();
        MyTest v2 = new MyTest();


        if (condition1()) {
            v1 = null;
        } else {
            v2 = null;
        }

        if (condition2()) {
            v1 = new MyTest();
        } else {
            v2 = new MyTest();
        }


        if (condition3a()) {
            if (condition3b()) {
                v1 = null;
            } else {
                v1 = new MyTest();
            }
        } else {
            if (condition3c()) {
                v2 = null;
            } else {
                v2 = new MyTest();
            }
        }


    }

    public static boolean condition1() {

        return true;
    }

    public static boolean condition2() {

        return false;
    }

    public static boolean condition3a() {

        return true;
    }

    public static boolean condition3b() {

        return true;
    }

    public static boolean condition3c() {

        return true;
    }

    void non_static_function() {

        MyTest t1 = new MyTest();
        MyTest t2 = new MyTest();
        t1.f = t2.f = t1;
        if (t1.f == t2.f) t1.f = t2;
    }


}