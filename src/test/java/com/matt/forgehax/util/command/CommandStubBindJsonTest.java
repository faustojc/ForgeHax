package com.matt.forgehax.util.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.lwjgl.input.Keyboard;

public class CommandStubBindJsonTest {

  private final boolean[] glfw = new boolean[1];

  private Integer read(String json) throws IOException {
    return CommandStub.BindJson.read(new JsonReader(new StringReader(json)), glfw);
  }

  @Test
  public void unmarkedValueIsNative() throws IOException {
    assertEquals(Integer.valueOf(54), read("{\"bind\":54}"));
    assertFalse(glfw[0]);
  }

  @Test
  public void glfwMarkedValueIsTranslatedOnce() throws IOException {
    assertEquals(Integer.valueOf(Keyboard.KEY_RSHIFT), read("{\"bind\":344,\"codes\":\"glfw\"}"));
    assertTrue(glfw[0]);
    // marker ahead of the value
    assertEquals(Integer.valueOf(Keyboard.KEY_RSHIFT), read("{\"codes\":\"glfw\",\"bind\":344}"));
  }

  @Test
  public void glfwMarkedMouseButtonBecomesMouseCode() throws IOException {
    assertEquals(Integer.valueOf(-99), read("{\"bind\":1,\"codes\":\"glfw\"}"));
    assertTrue(glfw[0]);
  }

  @Test
  public void unknownMembersAreSkipped() throws IOException {
    assertEquals(
        Integer.valueOf(54), read("{\"bind\":54,\"extra\":{\"a\":[1,2]},\"other\":\"x\"}"));
    assertFalse(glfw[0]);
  }

  @Test
  public void missingBindIsNull() throws IOException {
    assertNull(read("{\"codes\":\"glfw\"}"));
  }
}
