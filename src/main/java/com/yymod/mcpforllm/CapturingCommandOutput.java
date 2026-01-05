package com.yymod.mcpforllm;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapturingCommandOutput implements CommandSource {
    private final List<String> lines = new ArrayList<>();

    @Override
    public void sendSystemMessage(Component component) {
        lines.add(component.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }
}
