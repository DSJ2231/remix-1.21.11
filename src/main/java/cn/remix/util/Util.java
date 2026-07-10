package cn.remix.util;

import cn.remix.Client;
import lombok.experimental.UtilityClass;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@UtilityClass
public class Util implements IMinecraft{
    public int offGroundTicks, onGroundTicks;

    private void addChatMessage(String message) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal(message), false);
    }

    public void log(String message) {
        addChatMessage(Formatting.DARK_GRAY + "[" + Formatting.AQUA + Client.name + Formatting.DARK_GRAY + "] " + Formatting.RESET + message);
    }

    public void debug(String message) {
        addChatMessage(Formatting.DARK_GRAY + "[" + Formatting.RED + "Debug" + Formatting.DARK_GRAY + "] " + Formatting.RESET + message);
    }
}
