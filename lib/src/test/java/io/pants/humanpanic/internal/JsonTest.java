package io.pants.humanpanic.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {

    private final ObjectMapper jackson = new ObjectMapper();

    private JsonNode roundTrip(Object value) throws Exception {
        return jackson.readTree(Json.write(value));
    }

    @Test
    void escapesEveryCharacterThatJsonRequires() throws Exception {
        String nasty = "quote\" backslash\\ newline\n cr\r tab\t bell\u0007 nul\u0000 bs\b ff\f "
                + "ls  ps  unicode é 漢字 emoji 😀";

        assertEquals(nasty, roundTrip(nasty).asText());
    }

    @Test
    void writesNestedStructuresThatJacksonParses() throws Exception {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("n", 42L);
        inner.put("d", 1.5d);
        inner.put("b", true);
        inner.put("null", null);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("list", Arrays.asList(1, "two", inner, List.of()));
        root.put("empty", Map.of());

        JsonNode node = roundTrip(root);

        assertEquals(42, node.at("/list/2/n").asLong());
        assertEquals(1.5, node.at("/list/2/d").asDouble());
        assertTrue(node.at("/list/2/b").asBoolean());
        assertTrue(node.at("/list/2/null").isNull());
        assertEquals("two", node.at("/list/1").asText());
        assertTrue(node.at("/list/3").isArray());
        assertTrue(node.get("empty").isObject());
    }

    @Test
    void topLevelNull() {
        assertEquals("null\n", Json.write(null));
    }

    @Test
    void preservesKeyOrder() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("z", 1);
        m.put("a", 2);

        String json = Json.write(m);

        assertTrue(json.indexOf("\"z\"") < json.indexOf("\"a\""));
    }

    @Test
    void rejectsUnsupportedTypes() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
        assertThrows(IllegalArgumentException.class, () -> Json.write(Map.of(1, "non-string key")));
    }

    @Test
    void rejectsNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Json.write(Float.POSITIVE_INFINITY));
    }
}
