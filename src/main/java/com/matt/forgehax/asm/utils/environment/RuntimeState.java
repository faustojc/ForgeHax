package com.matt.forgehax.asm.utils.environment;

import com.matt.forgehax.asm.utils.remapping.NonObfuscatedStateMapper;

/**
 * Created on 5/26/2017 by fr1kin
 */
public class RuntimeState {

  /**
   * Used for remapping Notch to SRG and obfuscated naming conventions
   */
  private static IStateMapper remapper = null;
  /**
   * Default state use (unless specified not to)
   */
  private static State defaultState = State.OBFUSCATED;
  /**
   * Always assume the state is obfuscated because core minecraft classes will load before forge
   * feeds us the obfuscation state
   */
  private static final ThreadLocal<State> localState =
      ThreadLocal.withInitial(RuntimeState::getDefaultState);

  public static State getDefaultState() {
    return defaultState;
  }

  public static State getState() {
    return localState.get();
  }

  public static void setState(State state) {
    localState.set(state);
  }

  public static void releaseState() {
    localState.remove();
  }

  public static void markDefaultAsNormal() {
    defaultState = State.NORMAL;
  }

  public static void markDefaultAsSrg() {
    defaultState = State.SRG;
  }

  public static void markDefaultAsObfuscated() {
    defaultState = State.OBFUSCATED;
  }

  public static boolean isNormal() {
    return getState().equals(State.NORMAL);
  }

  public static boolean isSrg() {
    return getState().equals(State.SRG);
  }

  public static boolean isObfuscated() {
    return getState().equals(State.OBFUSCATED);
  }

  public static IStateMapper getMapper() {
    // 1.20.1 uses official mappings, so there is no runtime SRG/notch remapping left.
    return remapper == null ? remapper = NonObfuscatedStateMapper.getInstance() : remapper;
  }
}
