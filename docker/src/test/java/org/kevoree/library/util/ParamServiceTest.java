package org.kevoree.library.util;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by mleduc on 06/01/16.
 */
public class ParamServiceTest {

    private final ParamService paramService = new ParamService();

    @Test
    public void testEmpty() throws Exception {
        final List<String> strings = paramService.computeEnvs("");
        assertNotNull(strings);
        assertEquals(0, strings.size());
    }

    @Test
    public void testNull() throws Exception {
        final List<String> strings = paramService.computeEnvs("");
        assertNotNull(strings);
        assertEquals(0, strings.size());
    }

    @Test
    public void testValueEmpty() throws Exception {
        final List<String> strings = paramService.computeEnvs("KEY= KEY_2=ok");
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertEquals("KEY=", strings.get(0));
        assertEquals("KEY_2=ok", strings.get(1));
    }

    @Test
    public void testValueSpaceEscaped() throws Exception {
        final List<String> strings = paramService.computeEnvs("KEY1=1 KEY2=\"hello world\" KEY3=2");
        assertNotNull(strings);
        assertEquals(3, strings.size());
        assertEquals("KEY1=1", strings.get(0));
        assertEquals("KEY2=\"hello world\"", strings.get(1));
        assertEquals("KEY3=2", strings.get(2));
    }
}
