/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.handler.protocol.api.ProtocolSelector;
import net.ccbluex.liquidbounce.ui.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.elements.ToolDropdown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends MixinGuiScreen {

    private GuiButton toolButton;
    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        buttonList.add(toolButton = new GuiButton(997, 5, 8, 138, 20, "Tools"));
        buttonList.add(new GuiButton(998, width - 94, 5, 88, 20, "Alt Manager"));
        buttonList.add(new GuiButton(1151, 4, height - 24, 68, 20, "Protocol"));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {
        if (button.id == 997)
            ToolDropdown.toggleState();
        if (button.id == 998) {
            mc.displayGuiScreen(new GuiAltManager((GuiScreen) (Object) this));
        }
        if (button.id == 1151) {
            mc.displayGuiScreen(new ProtocolSelector((GuiScreen) (Object) this));
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void injectToolDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        ToolDropdown.handleDraw(toolButton);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void injectToolClick(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (mouseButton == 0)
            if (ToolDropdown.handleClick(mouseX, mouseY, toolButton))
                callbackInfo.cancel();
    }

    @Inject(method="connectToServer", at=@At(value="HEAD"))
    public void connectToServer(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getNetHandler() != null) {
            minecraft.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText(""));
        }
    }

}