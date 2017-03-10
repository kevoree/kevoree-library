package org.kevoree.library;

import static org.junit.Assert.*;

import org.junit.Test;
import org.python.core.PyException;

/**
 * Created by mleduc on 18/11/15.
 */
public class JythonEngineTest {

    private final JythonEngine jythonEngine = new JythonEngine();

    @Test
    public void test() throws Exception {
        final String res = jythonEngine.eval("1", "a", "b = int(a)+1", "b");
        assertEquals("2", res);
    }

    @Test(expected = PyException.class)
    public void testNotInteger() throws Exception {
        final String res = jythonEngine.eval("azerty", "a", "b = int(a)+1", "b");
        assertEquals("2", res);
    }
}
