package org.kevoree.library.util;

import com.spotify.docker.client.messages.PortBinding;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by mleduc on 05/01/16.
 * <p>
 * 127.0.0.1::80 + 127.0.0.1:1325:80 todo
 */
public class PortServiceTest {

    private final PortsService portsService = new PortsService();

    @Test
    public void testPortNull() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts(null);
        assertNotNull(stringListMap);
        assertEquals(0, stringListMap.size());
    }

    @Test
    public void testPortEmptyString() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("");
        assertNotNull(stringListMap);
        assertEquals(0, stringListMap.size());
    }

    @Test
    public void testOneDefinedPort() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("123:456");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("456"));
        assertEquals(1, stringListMap.get("456").size());
        assertEquals("123", stringListMap.get("456").get(0).hostPort());
        assertEquals("0.0.0.0", stringListMap.get("456").get(0).hostIp());
    }

    @Test
    public void testPortBindinTwoDefinedPort() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("123:456 789:456");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("456"));
        assertEquals(2, stringListMap.get("456").size());
        assertEquals("123", stringListMap.get("456").get(0).hostPort());
        assertEquals("0.0.0.0", stringListMap.get("456").get(0).hostIp());
        assertEquals("789", stringListMap.get("456").get(1).hostPort());
        assertEquals("0.0.0.0", stringListMap.get("456").get(1).hostIp());
    }

    @Test
    public void testPortBindingWithoutHostPort() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("456");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("456"));
        assertEquals(1, stringListMap.get("456").size());
        assertEquals("", stringListMap.get("456").get(0).hostPort());
        assertEquals("0.0.0.0", stringListMap.get("456").get(0).hostIp());
    }

    @Test
    public void testPortBindingWithIpAndHostPort() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("123.456.0.1:95:23");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("23"));
        final List<PortBinding> portBindings = stringListMap.get("23");
        assertEquals(1, portBindings.size());
        final PortBinding portBinding = portBindings.get(0);
        assertEquals("95", portBinding.hostPort());
        assertEquals("123.456.0.1", portBinding.hostIp());
    }

    @Test
    public void testPortBindingWithIp() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("123.456.0.1::23");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("23"));
        final List<PortBinding> portBindings = stringListMap.get("23");
        assertEquals(1, portBindings.size());
        final PortBinding portBinding = portBindings.get(0);
        assertEquals("", portBinding.hostPort());
        assertEquals("123.456.0.1", portBinding.hostIp());
    }

    @Test
    public void testRandomPortDefinedWithColon() throws  Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts(":456");
        assertEquals(1, stringListMap.size());
        assertTrue(stringListMap.containsKey("456"));
        assertEquals(1, stringListMap.get("456").size());
        assertEquals("", stringListMap.get("456").get(0).hostPort());
        assertEquals("0.0.0.0", stringListMap.get("456").get(0).hostIp());
    }

    @Test
    public void advancedTest() throws Exception {
        final Map<String, List<PortBinding>> stringListMap = portsService.computePorts("1.0.2.1::880 14:880 36:96");
        assertEquals(2, stringListMap.size());
        assertTrue(stringListMap.containsKey("880"));
        assertTrue(stringListMap.containsKey("96"));
        final List<PortBinding> portBindings = stringListMap.get("880");
        assertEquals(2, portBindings.size());
        assertEquals("", portBindings.get(0).hostPort());
        assertEquals("1.0.2.1", portBindings.get(0).hostIp());
        assertEquals("14", portBindings.get(1).hostPort());
        assertEquals("0.0.0.0", portBindings.get(1).hostIp());
        final List<PortBinding> portBindings1 = stringListMap.get("96");
        assertEquals(1, portBindings1.size());
        assertEquals("36", portBindings1.get(0).hostPort());
        assertEquals("0.0.0.0", portBindings1.get(0).hostIp());
    }


}
