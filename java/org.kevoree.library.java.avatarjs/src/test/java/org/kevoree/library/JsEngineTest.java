package org.kevoree.library;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mleduc on 18/11/15.
 */
public class JsEngineTest {
    private final JsEngine jsEngine = new JsEngine();

    @Test
    public void testBasic() throws Exception {
        final Object res = jsEngine.evaluateFunction("function a() { return 1; }", "a", null);
        Assert.assertEquals(1, res);
    }

    @Test
    public void testCastStringParam1() throws Exception {
        final Object res = jsEngine.evaluateFunction("function a(z) { return z+1; }", "a", "1");
        Assert.assertEquals("11", res);
    }

    @Test
    public void testCastStringParam2() throws Exception {
        final Object res = jsEngine.evaluateFunction("function a(z) { return parseInt(z)+1; }", "a", "1.0");
        Assert.assertEquals(2.0, res);
    }

    @Test(expected = java.lang.NoSuchMethodException.class)
    public void testWrongFunctionName() throws Exception {
        final Object res = jsEngine.evaluateFunction("function a() { return 1; }", "b", null);
        Assert.assertEquals(1, res);
    }
}
