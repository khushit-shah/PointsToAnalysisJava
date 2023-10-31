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

    public static void conditional_check() {
        int i = 0;
        MyTest v1 = new MyTest();
        v1.f = null;
        Object t1 = null;
        Object t3 = null;
        if (v1.f == t1) {
            v1.f = null;
        }
    }
}