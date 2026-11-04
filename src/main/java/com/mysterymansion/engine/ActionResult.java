package com.mysterymansion.engine;

import com.mysterymansion.model.Outcome;
import java.util.List;

/**
 * What happened after a player action: messages for the story log, whether a monster
 * fight should start, and (only for a successful escape) the ending reached.
 */
public record ActionResult(List<String> messages, boolean combatTriggered, Outcome outcome) {

    public static ActionResult of(String... messages) {
        return new ActionResult(List.of(messages), false, null);
    }
}
