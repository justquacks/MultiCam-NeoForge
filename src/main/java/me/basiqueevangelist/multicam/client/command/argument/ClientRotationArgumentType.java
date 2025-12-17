package me.basiqueevangelist.multicam.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;

public class ClientRotationArgumentType implements ArgumentType<ClientPosArgument> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~-5 ~5");
    public static final SimpleCommandExceptionType INCOMPLETE_ROTATION_EXCEPTION = new SimpleCommandExceptionType(
        Component.translatable("argument.rotation.incomplete")
    );

    public static ClientRotationArgumentType rotation() {
        return new ClientRotationArgumentType();
    }

    public static ClientPosArgument getRotation(CommandContext<?> context, String name) {
        return context.getArgument(name, ClientPosArgument.class);
    }

    @Override
    public ClientPosArgument parse(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        if (!stringReader.canRead()) {
            throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
        } else {
            WorldCoordinate coordinateArgument = WorldCoordinate.parseDouble(stringReader, false);
            if (stringReader.canRead() && stringReader.peek() == ' ') {
                stringReader.skip();
                WorldCoordinate coordinateArgument2 = WorldCoordinate.parseDouble(stringReader, false);
                // Create a zero coordinate for Z axis in rotation
                WorldCoordinate zeroCoord = new WorldCoordinate(true, 0);
                // Create a WorldCoordinates object for rotation (pitch, yaw, 0)
                WorldCoordinates coords = new WorldCoordinates(coordinateArgument2, coordinateArgument, zeroCoord);
                return new ClientDefaultPosArgument(coords);
            } else {
                stringReader.setCursor(i);
                throw INCOMPLETE_ROTATION_EXCEPTION.createWithContext(stringReader);
            }
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
