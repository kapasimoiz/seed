/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.core.internal.command;

import com.google.common.base.Strings;
import org.seedstack.seed.core.api.SeedException;
import org.seedstack.seed.core.spi.command.Argument;
import org.seedstack.seed.core.spi.command.Command;

import javax.inject.Inject;
import java.util.*;

/**
 * This built-in SEED command displays a list of all available commands or can display available options for a particular
 * command.
 *
 * @author adrien.lauer@mpsa.com
 */
@org.seedstack.seed.core.spi.command.CommandDefinition(scope = "", name = "help", description = "Display a list of available commands or help on a specific command")
public class HelpCommand implements Command {
    @Argument(index = 0, description = "The command name to get help for", mandatory = false)
    private String commandName;

    @Inject
    private Map<String, CommandDefinition> commandDefinitions;

    @Override
    public Object execute(Object object) {
        if (commandName == null) {
            StringBuilder builder = new StringBuilder();

            SortedMap<String, SortedSet<CommandDefinition>> allCommands = new TreeMap<String, SortedSet<CommandDefinition>>();
            for (CommandDefinition commandDefinitionDefinition : commandDefinitions.values()) {
                SortedSet<CommandDefinition> commandDefinitions = allCommands.get(commandDefinitionDefinition.getScope());
                if (commandDefinitions == null) {
                    commandDefinitions = new TreeSet<CommandDefinition>();
                    allCommands.put(commandDefinitionDefinition.getScope(), commandDefinitions);
                }

                commandDefinitions.add(commandDefinitionDefinition);
            }

            boolean firstScope = true;
            for (Map.Entry<String, SortedSet<CommandDefinition>> allCommandsEntry : allCommands.entrySet()) {
                if (firstScope) {
                    firstScope = false;
                } else {
                    builder.append("\n");
                }

                if (Strings.isNullOrEmpty(allCommandsEntry.getKey())) {
                    builder.append("<global>").append(":");
                } else {
                    builder.append(allCommandsEntry.getKey()).append(":");
                }

                for (CommandDefinition commandDefinition : allCommandsEntry.getValue()) {
                    builder.append("\n\t").append(String.format("%-10s %-10s", commandDefinition.getName(), commandDefinition.getDescription()));
                }

                builder.append("\n");
            }

            return builder.toString();
        } else {
            CommandDefinition commandDefinitionDefinition = commandDefinitions.get(commandName);
            if (commandDefinitionDefinition == null) {
                throw SeedException.createNew(CommandErrorCode.COMMAND_DEFINITION_NOT_FOUND).put("command", commandName);
            }

            StringBuilder builder = new StringBuilder();

            builder.append(commandDefinitionDefinition.getDescription());

            builder.append("\n\nusage:\n\t").append(commandDefinitionDefinition.getQualifiedName()).append(commandDefinitionDefinition.getOptionDefinitions().isEmpty() ? " " : " [OPTIONS] ");

            if (!commandDefinitionDefinition.getArgumentDefinitions().isEmpty()) {
                for (ArgumentDefinition argument : commandDefinitionDefinition.getArgumentDefinitions()) {
                    if (!argument.isMandatory()) {
                        builder.append("[");
                    }

                    builder.append(argument.getName());

                    if (!argument.isMandatory()) {
                        builder.append("]");
                    }

                    builder.append(" ");
                }

                builder.append("\n\narguments:\n");
                for (ArgumentDefinition argument : commandDefinitionDefinition.getArgumentDefinitions()) {
                    builder.append("\t").append(String.format("%-10s %-10s", argument.getName(), argument.getDescription())).append("\n");
                }
            }

            if (!commandDefinitionDefinition.getOptionDefinitions().isEmpty()) {
                builder.append("\n\noptions:\n");
                for (OptionDefinition option : commandDefinitionDefinition.getOptionDefinitions()) {
                    if (option.getName() != null && option.getLongName() == null) {
                        builder.append("\t").append(String.format("-%-5s %-10s", option.getName(), option.getDescription())).append("\n");
                    } else if (option.getName() == null && option.getLongName() != null) {
                        builder.append("\t").append(String.format("--%-10s %-10s", option.getLongName(), option.getDescription())).append("\n");
                    } else {
                        builder.append("\t").append(String.format("-%-5s --%-10s %-10s", option.getName(), option.getLongName(), option.getDescription())).append("\n");
                    }
                }
            }

            return builder.toString();
        }
    }
}