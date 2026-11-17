package com.matt.forgehax.mixin.accessor;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code OptionInstance.set} rejects values its ValueSet does not accept (gamma is limited to
 * 0..1) and resets them to the initial value, so out-of-range values must be written directly.
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {

  @Accessor("value")
  void setValue(Object value);
}
