package org.marj4n.smooth_classes.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import net.spell_engine.api.spell.Spell;
import org.junit.jupiter.api.Test;

class HealingSpellResourceTest {
    private static final Path SPELLS =
            Path.of("../common/src/main/resources/data/smooth_classes/spell");
    private static final Gson GSON = new GsonBuilder().create();

    @Test
    void bundledHealingActionsDecodeWithUsableHealParameters() throws Exception {
        int healingActions = 0;
        try (var files = Files.list(SPELLS)) {
            for (var file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                var spell = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (!spell.has("impacts")) continue;
                for (var impact : spell.getAsJsonArray("impacts")) {
                    var raw = impact.getAsJsonObject().getAsJsonObject("action");
                    if (raw == null || !"HEAL".equals(raw.get("type").getAsString())) continue;
                    var action = GSON.fromJson(raw, Spell.Impact.Action.class);
                    assertNotNull(action.heal, file + ": Spell Engine reads action.heal");
                    assertNull(action.damage, file + ": HEAL must not use damage parameters");
                    assertTrue(Float.isFinite(action.heal.spell_power_coefficient));
                    assertTrue(action.heal.spell_power_coefficient > 0, file.toString());
                    healingActions++;
                }
            }
        }
        assertTrue(healingActions > 0, "Must inspect the actual bundled HEAL actions");
    }

    @Test
    void lesserSacredOrbRetainsItsPointTwoHealingCoefficient() throws Exception {
        var spell =
                JsonParser.parseString(Files.readString(SPELLS.resolve("sacred_orb_lesser.json")))
                        .getAsJsonObject();
        var raw = spell.getAsJsonArray("impacts").get(0).getAsJsonObject().get("action");
        var action = GSON.fromJson(raw, Spell.Impact.Action.class);
        assertEquals(Spell.Impact.Action.Type.HEAL, action.type);
        assertNotNull(action.heal);
        assertEquals(0.2F, action.heal.spell_power_coefficient);
        assertNull(action.damage);
    }
}
