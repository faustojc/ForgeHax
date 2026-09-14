package com.matt.forgehax.mixin;

import com.matt.forgehax.asm.ForgeHaxHooks;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * {@code sendPacket} is the single funnel for both direct sends and the queue flush, so it is the
 * only outgoing injection point needed.
 */
@Mixin(Connection.class)
public abstract class MixinConnection {

  @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
  private void forgehax$onSendingPacket(
      Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
    if (ForgeHaxHooks.onSendingPacket(packet)) {
      ci.cancel();
    }
  }

  @Inject(method = "sendPacket", at = @At("RETURN"))
  private void forgehax$onSentPacket(
      Packet<?> packet, @Nullable PacketSendListener listener, CallbackInfo ci) {
    ForgeHaxHooks.onSentPacket(packet);
  }

  @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
  private void forgehax$onPreReceived(
      ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
    if (ForgeHaxHooks.onPreReceived(packet)) {
      ci.cancel();
    }
  }

  @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("RETURN"))
  private void forgehax$onPostReceived(
      ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
    ForgeHaxHooks.onPostReceived(packet);
  }
}
