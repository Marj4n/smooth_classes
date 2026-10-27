package org.marj4n.smooth_classes.network;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only failed casts produce messages. Avoid chat prefixes and long debug details. */
public final class SkillFeedback {
    private static final Pattern SECONDS = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?\\s*s(?:econds)?\\b)");
    private SkillFeedback() {}

    public static void failure(ServerPlayerEntity player, String raw) {
        if (player == null || raw == null || raw.isBlank()) return;
        String msg = raw.replaceFirst("(?i)^\\s*\\[(?:Smooth Classes|Origin)\\]\\s*", "").trim();
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("cooldown") || lower.contains("charge in ") || lower.contains("again in ")) {
            Matcher seconds = SECONDS.matcher(msg);
            msg = seconds.find() ? "Cooldown: " + seconds.group(1).replace(" ", "") : "On cooldown.";
        } else if (lower.contains("aim at one of your bound summons")) msg = "Aim at your summon.";
        else if (lower.contains("no death list recipe matches")) msg = "Invalid soul recipe.";
        else if (lower.contains("summon ingredient")) msg = "Hold a summon ingredient.";
        else if (lower.contains("soul is not in your death list")) msg = "Soul not collected.";
        else if (lower.contains("could not manifest")) msg = "Cannot summon here.";
        else if (lower.contains("no summon charge")) msg = "No summon charges.";
        else if (lower.contains("no unlocked signature") || lower.contains("no signature ability")) msg = "No Signature skill unlocked.";
        else if (lower.contains("no ascendancy ability")) msg = "No Ascendancy skill unlocked.";
        if (msg.length() > 72) msg = msg.substring(0, 69).stripTrailing() + "...";
        player.sendMessage(Text.literal(msg), true);
    }
}
