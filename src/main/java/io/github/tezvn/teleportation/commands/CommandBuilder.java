package io.github.tezvn.teleportation.commands;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author TezVN
 */
public class CommandBuilder extends BukkitCommand {

    private final JavaPlugin plugin;

    private UUID uniqueId;

    private List<SubCommand> subCommands;

    private String noPermissionsMessage;

    private String noSubCommandFoundMessage;

    private String noConsoleAllowMessage;

    private String helpHeader;

    private String helpFooter;

    private String helpCommandColor;

    private String helpDescriptionColor;

    private int helpSuggestions;

    public CommandBuilder(JavaPlugin plugin, String name, String description, String usageMessage, List<String> aliases) {
        super(name.toLowerCase(), description, usageMessage,
                aliases.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));
        this.uniqueId = UUID.randomUUID();
        this.plugin = plugin;
        this.subCommands = Lists.newArrayList();
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            // Consoleo
            if (args.length > 0) {
                String name = args[0];
                Optional<SubCommand> optCommand = this.subCommands.stream().filter(new Predicate<SubCommand>() {
                    @Override
                    public boolean test(SubCommand command) {
                        boolean matchAliases = command.getAliases().contains(name);
                        return command.getName().equalsIgnoreCase(name) || matchAliases;
                    }
                }).findAny();

                if (!optCommand.isPresent()) {
                    sender.sendMessage(this.noSubCommandFoundMessage);
                    return true;
                }

                SubCommand command = optCommand.get();

                if (!command.allowConsole()) {
                    sender.sendMessage(this.noConsoleAllowMessage);
                    return true;
                }

                optCommand.get().consoleExecute(sender, args);
            }

            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            String name = args[0];
            Optional<SubCommand> optCommand = this.subCommands.stream().filter(new Predicate<SubCommand>() {
                @Override
                public boolean test(SubCommand command) {
                    boolean matchAliases = command.getAliases().contains(name);
                    return command.getName().equalsIgnoreCase(name) || matchAliases;
                }
            }).findAny();

            if (!optCommand.isPresent()) {
                sender.sendMessage(this.noSubCommandFoundMessage);
                return true;
            }

            SubCommand command = optCommand.get();
            if (!player.hasPermission(command.getPermission())) {
                sender.sendMessage(this.noPermissionsMessage);
                return true;
            }

            optCommand.get().playerExecute(sender, args);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        return new CommandCompleter(this).onTabComplete(sender, args);
    }

    /**
     * Add sub command to your main command
     *
     * @param commands Sub command to add
     */
    public CommandBuilder addSubCommand(SubCommand... commands) {
        List<SubCommand> filter = Arrays.asList(commands).stream()
                .filter(c -> !isRegistered(c)).collect(Collectors.toList());
        this.subCommands.addAll(filter);
        return this;
    }

    public boolean isRegistered(SubCommand command) {
        return this.subCommands.stream().anyMatch(c -> c.getName().equals(command.getName()));
    }

    /**
     * Register command to server in {@code onEnable()} method
     */
    public CommandBuilder register() {
        try {
            addSubCommand(new AbstractHelpCommand(this));
            if (!getKnownCommands().containsKey(getName())) {
                getKnownCommands().put(getName(), this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + getName(), this);
            }
            for (String alias : getAliases()) {
                if (getKnownCommands().containsKey(alias))
                    continue;
                getKnownCommands().put(alias, this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + alias, this);
            }
            register(getCommandMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Unregister command from server in {@code onDisable()} method
     */
    public CommandBuilder unregister() {
        try {
            unregister(getCommandMap());
            getKnownCommands().entrySet().removeIf(entry ->
                    entry.getValue() instanceof CommandBuilder
                            && ((CommandBuilder) entry.getValue()).getUniqueId().equals(this.getUniqueId())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() throws Exception {
        Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        cmField.setAccessible(true);
        CommandMap cm = (CommandMap) cmField.get(Bukkit.getServer());
        cmField.setAccessible(false);
        Method method = cm.getClass().getDeclaredMethod("getKnownCommands");
        return (Map<String, Command>) method.invoke(cm, new Object[]{});
    }

    /**
     * Get unique id of this command
     *
     * @return Comamnd unique id
     */
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Set message when player don't have permission to access to sub command
     *
     * @param noPermissionsMessage Message to set
     */
    public CommandBuilder setNoPermissionsMessage(String noPermissionsMessage) {
        this.noPermissionsMessage = noPermissionsMessage.replace("&", "§");
        return this;
    }

    /**
     * Set message when player's input match no sub command
     *
     * @param noSubCommandFoundMessage Message to set
     */
    public CommandBuilder setNoSubCommandFoundMessage(String noSubCommandFoundMessage) {
        this.noSubCommandFoundMessage = noSubCommandFoundMessage.replace("&", "§");
        return this;
    }

    /**
     * Set message when command is not allowed for console to use
     *
     * @param noConsoleAllowMessage Message to set
     */
    public CommandBuilder setNoConsoleAllowMessage(String noConsoleAllowMessage) {
        this.noConsoleAllowMessage = noConsoleAllowMessage.replace("&", "§");
        return this;
    }

    private int getHelpSuggestions() {
        return helpSuggestions;
    }

    public CommandBuilder setHelpSuggestions(int helpSuggestions) {
        this.helpSuggestions = helpSuggestions;
        if (this.helpSuggestions < 1)
            return setHelpSuggestions(5);
        return this;
    }

    private String getHelpHeader() {
        return this.helpHeader;
    }

    public CommandBuilder setHelpHeader(String helpHeader) {
        this.helpHeader = helpHeader.replace("&", "§");
        return this;
    }

    private String getHelpFooter() {
        return this.helpFooter;
    }

    public CommandBuilder setHelpFooter(String helpFooter) {
        this.helpFooter = helpFooter.replace("&", "§");
        return this;
    }

    private String getHelpCommandColor() {
        return helpCommandColor == null ? "&a" : this.helpCommandColor;
    }

    public CommandBuilder setHelpCommandColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public CommandBuilder setHelpCommandColor(String color) {
        this.helpCommandColor = color.replace("&", "§");
        return this;
    }

    private String getHelpDescriptionColor() {
        return helpDescriptionColor == null ? "&7" : this.helpDescriptionColor;
    }

    public CommandBuilder setHelpDescriptionColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public CommandBuilder setHelpDescriptionColor(String color) {
        this.helpDescriptionColor = color.replace("&", "§");
        return this;
    }

    /**
     * Get list of registered sub commands
     *
     * @return List of sub commands
     */
    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    public static abstract class SubCommand {

        public SubCommand() {
        }

        /**
         * Get name of sub command
         *
         * @return Sub command name
         */
        public abstract String getName();

        /**
         * Get permission of sub command
         *
         * @return Sub command permission
         */
        public abstract String getPermission();

        /**
         * Get description of sub command
         *
         * @return Sub command description
         */
        public abstract String getDescription();

        /**
         * Get usage of sub command
         *
         * @return Sub command usage
         */
        public abstract String getUsage();

        /**
         * Get list of aliases of sub command
         *
         * @return Sub command aliases
         */
        public abstract List<String> getAliases();

        /**
         * Allow console to use this command
         *
         * @return True if allow, otherwise false
         */
        public abstract boolean allowConsole();

        /**
         * Player execution
         */
        public abstract void playerExecute(CommandSender sender, String[] args);

        /**
         * Console execution
         */
        public abstract void consoleExecute(CommandSender sender, String[] args);

        /**
         * Tab complete for sub command
         */
        public abstract List<String> tabComplete(CommandSender sender, String[] args);
    }

    protected static class CommandCompleter {

        private final List<SubCommand> commands;

        protected CommandCompleter(CommandBuilder handle) {
            this.commands = handle.getSubCommands();
        }

        private List<String> getMainSuggestions(CommandSender sender, String start) {
            return this.commands.stream().filter(command -> {
                if (start.length() < 1) {
                    return sender.hasPermission(command.getPermission());
                } else {
                    return command.getName().startsWith(start) && sender.hasPermission(command.getPermission());
                }
            })
                    .map(SubCommand::getName)
                    .collect(Collectors.toList());
        }

        private List<String> getSubSuggestions(CommandSender sender, String[] args) {
            Optional<SubCommand> optCommand = this.commands.stream()
                    .filter(command -> command.getName().equalsIgnoreCase(args[0])
                            && sender.hasPermission(command.getPermission()))
                    .findAny();

            return optCommand.map(subCommand -> subCommand.tabComplete(sender, args)).orElse(null);
        }

        public List<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return this.getMainSuggestions(sender, args[0]);
            } else if (args.length > 1) {
                return this.getSubSuggestions(sender, args);
            }
            return null;
        }
    }

    protected static class AbstractHelpCommand extends SubCommand {

        private final List<SubCommand> subCommands;

        private final CommandBuilder handle;

        public AbstractHelpCommand(CommandBuilder handle) {
            this.handle = handle;
            this.subCommands = handle.getSubCommands();
        }

        @Override
        public String getName() {
            return "help";
        }

        @Override
        public String getPermission() {
            return "";
        }

        @Override
        public String getDescription() {
            return "Shows available commands.";
        }

        @Override
        public String getUsage() {
            return "&7Syntax: &6/" + handle.getName() + " help <page>";
        }

        @Override
        public List<String> getAliases() {
            return Collections.singletonList("?");
        }

        @Override
        public boolean allowConsole() {
            return true;
        }

        @Override
        public void playerExecute(CommandSender sender, String[] args) {
            if (args.length == 1) {
                handleCommands(sender, 0);
                return;
            }
            int page = getPage(args[1]);
            handleCommands(sender, page);
        }

        @Override
        public void consoleExecute(CommandSender sender, String[] args) {
            if (args.length == 1) {
                handleCommands(sender, 0);
                return;
            }
            int page = getPage(args[1]);
            handleCommands(sender, page);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 2) {
                int max = handle.getSubCommands().size() / handle.getHelpSuggestions();
                List<Integer> index = Lists.newArrayList();
                for (int i = 0; i < max; i++) {
                    index.add(i);
                }
                return index.stream().map(String::valueOf).filter(i -> i.startsWith(args[1])).collect(Collectors.toList());
            }
            return null;
        }

        private int getPage(String str) {
            try {
                int page = Integer.parseInt(str);
                return Math.max(page, 0);
            } catch (Exception e) {
                return 0;
            }
        }

        private void handleCommands(CommandSender sender, int page) {
            List<SubCommand> filter = subCommands.stream()
                    .filter(c -> sender.hasPermission(c.getPermission()))
                    .collect(Collectors.toList());
            int max = Math.min(handle.getHelpSuggestions() * (page + 1), filter.size());
            if (handle.getHelpHeader() != null)
                sender.sendMessage(handle.getHelpHeader());
            for (int i = page * handle.getHelpSuggestions(); i < max; i++) {
                SubCommand command = filter.get(i);
                TextComponent clickableCommand = createClickableCommand(command);
                if(sender instanceof Player)
                    ((Player) sender).spigot().sendMessage(clickableCommand);
                else
                    sender.sendMessage(handle.getHelpCommandColor() + "/" + handle.getName() + " " + command.getName() + ": "
                            + handle.getHelpDescriptionColor() + command.getDescription());
            }
            if(sender instanceof Player) {
                Player player = (Player) sender;
                TextComponent previousPage = createClickableButton("&e&l«",
                        "/" + handle.getName() + " help " + (page - 1),
                        "&7Previous page");
                TextComponent nextPage = createClickableButton("&e&l»",
                        "/" + handle.getName() + " help " + (page + 1),
                        "&7Next page");
                TextComponent pageInfo = createClickableButton(" &e&l" + (page + 1) + " ",
                        null, "&7You're in page " + (page + 1));
                ClickableText spacing = new ClickableText("                       ");
                boolean canNextPage = handle.getHelpSuggestions() * (page + 1) < filter.size();
                if (page < 1) {
                    if (canNextPage)
                        player.spigot().sendMessage(spacing.build(), pageInfo, nextPage);
                    else
                        player.spigot().sendMessage(spacing.build(), pageInfo);

                } else {
                    if (canNextPage)
                        player.spigot().sendMessage(spacing.build(), previousPage, pageInfo, nextPage);
                    else
                        player.spigot().sendMessage(spacing.build(), previousPage, pageInfo);
                }
            }
            if (handle.getHelpFooter() != null)
                sender.sendMessage(handle.getHelpFooter());
        }

        private TextComponent createClickableCommand(SubCommand command) {
            return new ClickableText(handle.getHelpCommandColor() + "/" + handle.getName() + " " + command.getName() + ": "
                    + handle.getHelpDescriptionColor() + command.getDescription())
                    .setHoverAction(HoverEvent.Action.SHOW_TEXT, "&7Click to get this command.")
                    .setClickAction(ClickEvent.Action.SUGGEST_COMMAND, "/" + handle.getName() + " " + command.getName())
                    .build();
        }

        private TextComponent createClickableButton(String name, String clickAction, String... hoverAction) {
            ClickableText clickableText = new ClickableText(name);
            if (hoverAction.length > 0)
                clickableText.setHoverAction(HoverEvent.Action.SHOW_TEXT, hoverAction);
            if (clickAction != null)
                clickableText.setClickAction(ClickEvent.Action.RUN_COMMAND, clickAction);
            return clickableText.build();
        }
    }

    public static class ClickableText {

        private String text;

        private HoverEvent hoverAction;

        private ClickEvent clickAction;

        public ClickableText(String text) {
            Objects.requireNonNull(text);
            this.text = text.replace("&", "§");
        }

        public ClickableText setHoverAction(HoverEvent.Action action, String... content) {
            TextComponent[] texts = Arrays.stream(content)
                    .map(e -> new TextComponent(e.replace("&", "§")))
                    .collect(Collectors.toList()).toArray(new TextComponent[content.length]);
            this.hoverAction = new HoverEvent(action, texts);
            return this;
        }

        public ClickableText setClickAction(ClickEvent.Action action, String value) {
            this.clickAction = new ClickEvent(action, value);
            return this;
        }

        public ClickableText setText(String text) {
            this.text = text;
            return this;
        }

        public TextComponent build() {
            TextComponent component = new TextComponent();
            component.setText(this.text);
            if (this.hoverAction != null)
                component.setHoverEvent(this.hoverAction);
            if (this.clickAction != null)
                component.setClickEvent(this.clickAction);
            return component;
        }
    }
}