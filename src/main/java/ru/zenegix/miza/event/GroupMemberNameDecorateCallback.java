package ru.zenegix.miza.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.text.Text;

public interface GroupMemberNameDecorateCallback {

    Event<GroupMemberNameDecorateCallback> EVENT = EventFactory.createArrayBacked(
            GroupMemberNameDecorateCallback.class,
            (listeners) -> (target, name) -> {
                Text result = name;

                for (GroupMemberNameDecorateCallback listener : listeners) {
                    result = listener.decorate(target.toLowerCase(), result);
                }

                return result;
            });

    Text decorate(String target, Text name);
}
