package com.matt.forgehax.mods.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class MacroEntryTest {

  // anonymous (empty name) so no KeyMapping is registered, which needs a running game
  private static MacroCommand.MacroEntry load(String json) throws IOException {
    MacroCommand.MacroEntry entry = new MacroCommand.MacroEntry("");
    entry.deserialize(new JsonReader(new StringReader(json)));
    return entry;
  }

  private static JsonObject save(MacroCommand.MacroEntry entry) throws IOException {
    StringWriter out = new StringWriter();
    entry.serialize(new JsonWriter(out));
    return new JsonParser().parse(out.toString()).getAsJsonObject();
  }

  @Test
  public void unmarkedEntryIsTranslatedOnceAndRewrittenMarked() throws IOException {
    // 0x1C is LWJGL2 KEY_RETURN
    MacroCommand.MacroEntry entry = load("{\"key\":28,\"commands\":[[\"say hi\"]]}");
    assertEquals(GLFW.GLFW_KEY_ENTER, entry.getKey());

    JsonObject saved = save(entry);
    assertEquals("glfw", saved.get("codes").getAsString());
    assertEquals(GLFW.GLFW_KEY_ENTER, saved.get("key").getAsInt());

    // reloading the rewritten form must not translate again
    assertEquals(GLFW.GLFW_KEY_ENTER, load(saved.toString()).getKey());
  }

  @Test
  public void markedEntryIsNotTranslated() throws IOException {
    MacroCommand.MacroEntry entry =
        load("{\"key\":" + GLFW.GLFW_KEY_F + ",\"codes\":\"glfw\",\"commands\":[[\"x\"]]}");
    assertEquals(GLFW.GLFW_KEY_F, entry.getKey());
    assertEquals(1, entry.getCommands().size());
  }

  @Test
  public void markedMouseIndexFromTheOldBugIsRecovered() throws IOException {
    MacroCommand.MacroEntry entry = load("{\"key\":1,\"codes\":\"glfw\",\"commands\":[[\"x\"]]}");
    assertEquals(-99, entry.getKey());
  }
}
