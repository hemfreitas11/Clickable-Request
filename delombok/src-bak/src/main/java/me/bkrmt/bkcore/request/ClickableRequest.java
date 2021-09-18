package me.bkrmt.bkcore.request;

import me.bkrmt.bkcore.BkPlugin;
import me.bkrmt.bkcore.Utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class ClickableRequest implements Comparable<ClickableRequest> {
    private static final ConcurrentSkipListSet<ClickableRequest> clickableRequests = new ConcurrentSkipListSet<>();
    private final Player sender;
    private final BkPlugin plugin;
    private final String identifier;
    private Player target;
    private BukkitTask timeoutTimer;
    private ExpireRunnable expireRunnable;
    private int timeout;
    List<String> lines;
    private TextComponent request;
    private String[] commands;
    private String[] buttons;
    private String[] hovers;

    public ClickableRequest(BkPlugin plugin, String identifier, Player sender, Player target) {
        this.plugin = plugin;
        this.identifier = identifier;
        request = new TextComponent("");
        this.sender = sender;
        this.target = target;
        lines = null;
        timeout = -1;
        timeoutTimer = null;
        commands = new String[2];
        buttons = new String[2];
        hovers = new String[2];
    }

    public void sendRequest() {
        buildMessage();
        target.spigot().sendMessage(request);
        clickableRequests.add(this);
        if (timeout > 0) {
            ClickableRequest request = this;
            timeoutTimer = new BukkitRunnable() {
                @Override
                public void run() {
                    if (expireRunnable != null)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> expireRunnable.run(request));
                    removeInteraction(getIdentifier(), getSender().getUniqueId());
                }
            }.runTaskLater(plugin, 20 * timeout);
        }
    }

    private void buildMessage() {
        TextComponent buttonAccept = new TextComponent(buttons[0]);
        buttonAccept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commands[0].contains("/") ? commands[0] : "/" + commands[0]));
        if (hovers[0] != null && !hovers[0].isEmpty())
            buttonAccept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovers[0]).create()));

        TextComponent buttonDeny = new TextComponent(buttons[1]);
        buttonDeny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commands[1].contains("/") ? commands[1] : "/" + commands[1]));
        if (hovers[1] != null && !hovers[1].isEmpty())
            buttonDeny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovers[1]).create()));

        Iterator<String> iterator = lines.listIterator();
        while (iterator.hasNext()) {
            String line = Utils.translateColor(iterator.next());

            if (line.contains("{accept-button}") || line.contains("{deny-button}")) {
                String[] parts = line.split(" ");
                StringBuilder builder = new StringBuilder();
                for (int c = 0; c < parts.length; c++) {
                    if (parts[c].equals("{accept-button}")) {
                        appendRequest(buttonAccept, builder);
                        builder = new StringBuilder();
                    } else if (parts[c].equals("{deny-button}")) {
                        appendRequest(buttonDeny, builder);
                        builder = new StringBuilder();
                    } else {
                        builder.append(Utils.translateColor(parts[c] + " "));
                    }
                    if (c == parts.length - 1) {
                        request.addExtra(builder.toString());
                        request.addExtra(" ");
                    }
                }
            } else {
                addLine(line);
            }
            if (iterator.hasNext()) request.addExtra("\n");
        }
    }

    private void appendRequest(TextComponent button, StringBuilder builder) {
        request.addExtra(builder.toString());
        request.addExtra(button);
        request.addExtra(" ");
    }

    private void addLine(String line) {
        if (!line.isEmpty()) {
            line = line
                    .replace("{sender}", sender.getName())
                    .replace("{target}", target.getName())
                    .replace("{expire}", String.valueOf(timeout));
            request.addExtra(line);
        }
    }

    public static boolean isPlayerInteracting(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) && request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid))
                return true;
        }
        return false;
    }

    public static ClickableRequest getInteraction(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) && request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid)) {
                return request;
            }
        }
        return null;
    }

    public static ClickableRequest getInteraction(String identifier, UUID senderUuid, UUID targetUuid) {
        for (ClickableRequest request : clickableRequests) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) &&
                    (request.getSender().getUniqueId().equals(senderUuid) && request.getTarget().getUniqueId().equals(targetUuid)) ||
                    (request.getTarget().getUniqueId().equals(senderUuid) && request.getSender().getUniqueId().equals(targetUuid))) {
                return request;
            }
        }
        return null;
    }

    public static void removeInteraction(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) && request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid)) {
                if (request.getTimeoutTimer() != null) request.getTimeoutTimer().cancel();
                clickableRequests.remove(request);
            }
        }
    }

    public ExpireRunnable getExpireRunnable() {
        return expireRunnable;
    }

    public ClickableRequest setTimeout(int timeout, ExpireRunnable expireRunnable) {
        this.timeout = timeout;
        this.expireRunnable = expireRunnable;
        return this;
    }

    public ClickableRequest setLines(List<String> lines) {
        this.lines = lines;
        return this;
    }

    public ClickableRequest setCommands(String acceptCommand, String declineCommand) {
        this.commands[0] = acceptCommand;
        this.commands[1] = declineCommand;
        return this;
    }

    public ClickableRequest setButtons(String acceptButton, String declineButton) {
        this.buttons[0] = acceptButton;
        this.buttons[1] = declineButton;
        return this;
    }

    public ClickableRequest setHovers(String hoverAccept, String hoverDecline) {
        this.hovers[0] = hoverAccept;
        this.hovers[1] = hoverDecline;
        return this;
    }

    public BukkitTask getTimeoutTimer() {
        return timeoutTimer;
    }

    public BkPlugin getPlugin() {
        return plugin;
    }

    public Player getSender() {
        return sender;
    }

    public Player getTarget() {
        return target;
    }

    public String[] getCommands() {
        return commands;
    }

    public String[] getButtons() {
        return buttons;
    }

    public String[] getHovers() {
        return hovers;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public int compareTo(ClickableRequest request) {
        if (request.getIdentifier().equalsIgnoreCase(getIdentifier()) && request.getSender().getUniqueId().equals(getSender().getUniqueId()) && request.getTarget().getUniqueId().equals(getTarget().getUniqueId()))
            return 0;
        else return 1;
    }
}
