/*
 * Copyright (c) 2017 Rubicon Bot Development Team
 *
 * Licensed under the MIT license. The full license text is available in the LICENSE file provided with this project.
 */

package fun.rubicon.command2;

import fun.rubicon.RubiconBot;
import fun.rubicon.util.Colors;
import fun.rubicon.util.Info;
import fun.rubicon.util.Logger;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Maintains command invocation associations.
 * @author tr808axm
 */
public class CommandManager extends ListenerAdapter {
    private static final long defaultDeleteIntervalSeconds = 15;

    private final Map<String, CommandHandler> commandAssociations = new HashMap<>();

    public void registerCommandHandlers(CommandHandler... commandHandlers) {
        for(CommandHandler commandHandler : commandHandlers)
            registerCommandHandler(commandHandler);
    }

    /**
     * Registers a CommandHandler with it's invocation aliases.
     * @param commandHandler the {@link CommandHandler} to be registered.
     */
    public void registerCommandHandler(CommandHandler commandHandler) {
        for(String invokeAlias : commandHandler.getInvokeAliases())
            // only register if alias is not taken
            if(commandAssociations.containsKey(invokeAlias.toLowerCase()))
                Logger.error("WARNING: The '" + commandHandler.toString()
                        + "' CommandHandler tried to register the alias '" + invokeAlias
                        + "' which is already taken by the '" + commandAssociations.get(invokeAlias).toString()
                        + "' CommandHandler.");
            else
                commandAssociations.put(invokeAlias.toLowerCase(), commandHandler);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        super.onMessageReceived(event);
        ParsedCommandInvocation commandInvocation = parse(event.getMessage());
        if(commandInvocation != null) // if it is a command invocation
            call(commandInvocation);
    }

    /**
     * Call the CommandHandler for commandInvocation.
     * @param parsedCommandInvocation the parsed message.
     */
    public void call(ParsedCommandInvocation parsedCommandInvocation) {
        CommandHandler commandHandler = commandAssociations.get(parsedCommandInvocation.invocationCommand);
        Message response;
        if (commandHandler == null)
            response = new MessageBuilder().setEmbed(new EmbedBuilder()
                    .setAuthor("Unknown command", null, RubiconBot.getJDA().getSelfUser().getEffectiveAvatarUrl())
                    .setDescription("'" + parsedCommandInvocation.serverPrefix + parsedCommandInvocation.invocationCommand
                            + "' could not be resolved to a command.\nType '" + parsedCommandInvocation.serverPrefix
                            + "help' to get a list of all commands.")
                    .setColor(Colors.COLOR_ERROR)
                    .setFooter(RubiconBot.getNewTimestamp(), null)
                    .build()).build();
        else
            response = commandHandler.call(parsedCommandInvocation);

        // respond
        if (response != null)
            // send response message and delete it after defaultDeleteIntervalSeconds
            parsedCommandInvocation.invocationMessage.getChannel().sendMessage(response)
                    .queue(msg -> msg.delete().queueAfter(defaultDeleteIntervalSeconds, TimeUnit.SECONDS));
    }

    /**
     * Parses a raw message into command components.
     * @param message the discord message to parse.
     * @return a {@link ParsedCommandInvocation} with the parsed arguments or null if the message could not be
     * resolved to a command.
     */
    private static ParsedCommandInvocation parse(Message message) {
        // get server prefix
        String prefix = message.getChannelType() == ChannelType.TEXT
                ? RubiconBot.getMySQL().getGuildValue(message.getGuild(), "prefix")
                : Info.BOT_DEFAULT_PREFIX;

        // resolve messages with '<server-bot-prefix>majorcommand [arguments...]'
        if(message.getContent().startsWith(prefix)) {
            // cut off command prefix
            String beheaded = message.getContent().substring(prefix.length(), message.getContent().length());
            // split arguments
            String[] allArgs = beheaded.split(" ");
            // create an array of the actual command arguments (exclude invocation arg)
            String[] args = new String[allArgs.length - 1];
            System.arraycopy(allArgs, 1, args, 0, args.length);

            return new ParsedCommandInvocation(message, prefix, allArgs[0], args);
        }
        // TODO resolve messages with '@botmention majorcommand [arguments...]'
        // return null if no strategy could parse a command.
        return null;
    }

    /**
     * @param invocationAlias the key property to the CommandHandler.
     * @return the associated CommandHandler or null if none is associated.
     */
    public CommandHandler getCommandHandler(String invocationAlias) {
        return commandAssociations.get(invocationAlias);
    }

    /**
     * @return a clone of all registered command associations.
     */
    public Map<String, CommandHandler> getCommandAssociations() {
        return new HashMap<>(commandAssociations);
    }

    public static final class ParsedCommandInvocation {
        public final Message invocationMessage;
        public final String serverPrefix;
        public final String invocationCommand;
        public final String[] args;

        private ParsedCommandInvocation(Message invocationMessage, String serverPrefix, String invocationCommand, String[] args) {
            this.invocationMessage = invocationMessage;
            this.serverPrefix = serverPrefix;
            this.invocationCommand = invocationCommand;
            this.args = args;
        }
    }
}