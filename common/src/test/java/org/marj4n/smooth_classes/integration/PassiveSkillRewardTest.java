package org.marj4n.smooth_classes.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PassiveSkillRewardTest {
    @Test
    void providersCanInitializeInEitherOrderWithoutCollisions() {
        for (boolean classesFirst : List.of(false, true)) {
            var claims = new HashSet<String>();
            var classes =
                    PassiveSkillReward.registrationIds(true).stream()
                            .map(Object::toString)
                            .toList();
            var simplySkills = List.of("puffish_skills:passive_skill");
            var providers =
                    classesFirst ? List.of(classes, simplySkills) : List.of(simplySkills, classes);
            for (var provider : providers) {
                for (var id : provider) assertTrue(claims.add(id), "Duplicate reward: " + id);
            }
            assertTrue(claims.contains("smooth_classes:passive_skill"));
            assertTrue(claims.contains("puffish_skills:passive_skill"));
        }
    }

    @Test
    void standaloneClassesRetainsLegacyDatapackCompatibility() {
        assertEquals(
                Set.of("smooth_classes:passive_skill", "puffish_skills:passive_skill"),
                new HashSet<>(
                        PassiveSkillReward.registrationIds(false).stream()
                                .map(Object::toString)
                                .toList()));
    }

    @Test
    void bundledTreesUseTheirOwnRewardNamespace() throws Exception {
        Path root =
                Path.of(
                        "../common/src/main/resources/data/smooth_classes/puffish_skills/categories");
        int markers = 0;
        try (var files = Files.walk(root)) {
            for (Path path :
                    files.filter(p -> p.getFileName().toString().equals("definitions.json"))
                            .toList()) {
                markers += check(JsonParser.parseString(Files.readString(path)), path);
            }
        }
        assertTrue(markers > 100, "The actual bundled class trees must be examined");
    }

    private static int check(JsonElement value, Path file) {
        int count = 0;
        if (value.isJsonObject()) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                var child = entry.getValue();
                if (entry.getKey().equals("type")
                        && child.isJsonPrimitive()
                        && child.getAsString().endsWith(":passive_skill")) {
                    assertEquals(
                            "smooth_classes:passive_skill", child.getAsString(), file.toString());
                    count++;
                } else count += check(child, file);
            }
        } else if (value.isJsonArray()) {
            for (var child : value.getAsJsonArray()) count += check(child, file);
        }
        return count;
    }
}
