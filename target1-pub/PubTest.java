public class PubTest {
    PubTest f;


    // treat each test* method below as an entry point, and
    // hence analyze it under `@' (i.e,, epsilon) context

    static void test1() {
        // example 1 starts here
        PubTest k1 = new PubTest();   // first object
        PubTest k2 = new PubTest();   // second object
        k1.f = k2;
        PubTest k3 = new PubTest();   // third object
        foo1(k1, k3);
        // first object's f points to second object, second object's f will point to third object here, and third object's f points to fourth object, and fourth object's f points to null
    }

    static void foo1(PubTest k1, PubTest k3) {
        // called from a single context. In this context, k1 points to first object, k3 points to third object, and first object's f points to second object
        PubTest k2 = k1.f;
        k2.f = k3;
        PubTest t = new PubTest(); // fourth object
//        foo1(k1, k3); check with this.
        k3.f = t;
        t.f = null;
    }

    static PubTest test2() {
        PubTest k1 = new PubTest(); // first object
        PubTest k2 = foo2(k1);
        PubTest k3 = new PubTest(); // second object
        PubTest k4 = foo2(k3);
        PubTest k5 = null;
        // k2 and k4 will point to both objects because the call depth was more than two
        if (k2 == k4) {
            k5 = k2;
        }
        return k5;
    }

    static PubTest foo2(PubTest k5) { // two contexts (both of length 1) reach here. In the first context, k5 points to first object. In the second context k5 points to second object.
        PubTest k7 = bar2(k5);
        // under both both contexts, k7 will point to both objects. (I think so. Abshishek and Devansh: Please confirm).
        return k7;
    }

    static PubTest bar2(PubTest k5) {
        // two contexts (both of length 2) reach here.  In the first context, k5 points to first object. In the second context k5 points to second object.
        PubTest k7 = bar3(k5);

        // under both both contexts, k7 will point to both objects.
        return k7;
    }

    static PubTest bar3(PubTest k6) {
        // due to length bound of two, only one context reaches here. Under this context, k6 can point to first object or second object.
        return k6;
    }

    static PubTest test3() {
        PubTest k8 = new PubTest(); // first object
        PubTest k9 = baz2(k8);
        PubTest k10 = new PubTest(); // second object
        PubTest k11 = baz2(k10);
        PubTest k12 = null;

        if (k9 == k11) {
            k12 = k9;
        }
        return k12;
    }

    static PubTest baz2(PubTest k5) {
        // two contexts (both of length 1) reach here.  In the first context, k5 points to first object. In the second context k5 points to second object.
        PubTest k7 = baz3(k5);

        // k7 will point to first object in first context, and will point to second object in second context

        return k7;
    }

    static PubTest baz3(PubTest k6) {
        // two contexts (both of length 2) reach here.  In the first context, k6 points to first object. In the second context k6 points to second object.
        return k6;
    }


    static PubTest test4(PubTest v1, PubTest v2) {
        v1.f = new PubTest();
        v2 = v1;

        return v2.f;
    }

    static PubTest test5_rec(PubTest v1, PubTest v2) {
        v1.f = v2;

        v1 = test5_rec(v1, v2);

        v1.f = v2;

        return v1;

    }

    static PubTest test6() {
        PubTest k1 = new PubTest(); // first object
        PubTest k2 = foo3(k1);
        // PubTest k3 = new PubTest(); // second object
        PubTest k4 = foo3(k1);
        PubTest k5 = k1;
        // k2 and k4 will point to both objects because the call depth was more than two
        if (k2 != k4) {
            k5 = k2;
            k4=null;
        }
        return k5;
    }



    static PubTest foo3(PubTest k5) { // two contexts (both of length 1) reach here. In the first context, k5 points to first object. In the second context k5 points to second object.
        // PubTest k7 = bar2(k5);
        PubTest k7 = new PubTest();
        // under both both contexts, k7 will point to both objects. (I think so. Abshishek and Devansh: Please confirm).
        // return k7;
        return k5;
    }

    static PubTest test7(){
        PubTest v2= new PubTest();
        v2.f=test7_foo2(v2);
        PubTest v1=new PubTest();
        v1=v2.f;
        return v1;
    }
    static PubTest test7_foo2(PubTest v2)
    {
        return null;
    }

    static PubTest test8(){
        PubTest v2= new PubTest();
        v2.f=test8_foo2(v2);
        PubTest v1=new PubTest();
        v1.f=test8_foo1(v1,v2);
        v1=v2.f;
        return v1;
    }
    static PubTest test8_foo1(PubTest v1,PubTest v2)
    {
        v1=null;
        return null;
    }
    static PubTest test8_foo2(PubTest v1)
    {
        v1.f=null;
        return null;
    }

    static PubTest test9()
    {
        PubTest v1= new PubTest();
        PubTest v2= new PubTest();
        int a=10;
        // v1=v2;
        test9_fun1(v1,v2,a);
         PubTest v3= new PubTest();
         v2=null;
        v3.f=v2;
        return null;
    }

    static void test9_fun1(PubTest v1 , PubTest v2,int a )
    {
        if(a==10)
        v1.f=v2.f=null;
        PubTest v3= new PubTest();
        v3.f=v3;
        test9_fun2(v2,v1,v3);
        
    }

    static void test9_fun2(PubTest v1, PubTest v2,PubTest v3)
    {
        PubTest v4= new PubTest();
        v4.f=null;
        test9_fun3();
    }

    static void test9_fun3()
    {
        PubTest v5=new PubTest();
        v5.f=null;
    }

    static PubTest test10()
    {
        PubTest v1=new PubTest();
        v1.f=null;
        return v1.f;
    }

    static PubTest test11()
    {
        PubTest v1= new PubTest();
        PubTest v2= new PubTest();
        int a=10;
        // v1=v2;
        test11_fun1(v1,v2,a);
         PubTest v3= new PubTest();
         v2=null;
        v3.f=v2;
        return null;
    }

    static void test11_fun1(PubTest v1 , PubTest v2,int a )
    {
        if(a==10)
        v1.f=v2.f=null;
        test11_fun1(v2,v1,a);
        PubTest v3= new PubTest();
        v3.f=v3;
    }



    static PubTest fibonnaci(int i, PubTest p1, PubTest p2) {
        PubTest p3 = new PubTest();
        PubTest p4 = new PubTest();

        if (p1 != null) {
            p1.f = p3;
        }
        if (p2 != null) {
            p2.f = p3;
        }
        if (i > 0) {
            fibonnaci(i - 1, p1, p2);
        }
        return p1;
    }

    static PubTest fibonnaci1(int i, PubTest p1, PubTest p2) {
        PubTest p3 = new PubTest();
        PubTest p4 = new PubTest();

        if (p1 != null) {
            p1.f = p3;
        }
        if (p2 != null) {
            p2.f = p3;
        }
        if (i > 0) {
            fibonnaci1(i - 1, p3, p4);
        }
        return p1;
    }

    static PubTest MyTest2(PubTest v1, PubTest v2) {
        v1 = new PubTest();
        v2 = new PubTest();
        MyTest2(v1, v2);
        v1.f = v2;
        return v1;
    }



}

