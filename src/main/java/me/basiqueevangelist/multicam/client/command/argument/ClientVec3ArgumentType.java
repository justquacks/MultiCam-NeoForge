package me.basiqueevangelist.multicam.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ClientVec3ArgumentType implements ArgumentType<ClientPosArgument> {
    private static final Collection<String> EXAMPLES = Arrays.asList(
        "0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5"
    );

    private final boolean centerIntegers;

    public ClientVec3ArgumentType(boolean centerIntegers) {
        this.centerIntegers = centerIntegers;
    }

    public static ClientVec3ArgumentType vec3() {
        return new ClientVec3ArgumentType(true);
    }

    public static ClientVec3ArgumentType vec3(boolean centerIntegers) {
        return new ClientVec3ArgumentType(centerIntegers);
    }

    public static Vec3 getVec3(CommandContext<?> context, String name, LocalPlayer player, ClientLevel level) {
        return context.getArgument(name, ClientPosArgument.class)
            .toAbsolutePos(player, level);
    }

    public static ClientPosArgument getPosArgument(CommandContext<?> context, String name) {
        return context.getArgument(name, ClientPosArgument.class);
    }

    @Override
    public ClientPosArgument parse(StringReader reader) throws CommandSyntaxException {
        return reader.canRead() && reader.peek() == '^'
            ? ClientLookingPosArgument.parse(reader)
            : ClientDefaultPosArgument.parse(reader, this.centerIntegers);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!(context.getSource() instanceof SharedSuggestionProvider)) {
            return Suggestions.empty();
        }

        String remaining = builder.getRemaining();
        Collection<SharedSuggestionProvider.TextCoordinates> coords;

        if (!remaining.isEmpty() && remaining.charAt(0) == '^') {
            coords = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
        } else {
            coords = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_GLOBAL);
        }

        return SharedSuggestionProvider.suggestCoordinates(remaining, coords, builder, Commands.createValidator(this::parse));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
