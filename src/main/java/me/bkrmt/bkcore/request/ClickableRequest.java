package me.bkrmt.bkcore.request;

import me.bkrmt.bkcore.BkPlugin;
import me.bkrmt.bkcore.clickablemessage.Button;
import me.bkrmt.bkcore.clickablemessage.ClickableMessage;
import me.bkrmt.bkcore.clickablemessage.Hover;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClickableRequest implements Comparable<ClickableRequest> {
    private static final ConcurrentHashMap<UUID, ClickableRequest> clickableRequests = new ConcurrentHashMap<>();
    private final Player sender;
    private final BkPlugin plugin;
    private final String identifier;
    private Player target;
    private BukkitTask timeoutTimer;
    private ExpireRunnable expireRunnable;
    private int timeout;
    private List<String> lines;
    private final String[] commands;
    private final String[] buttons;
    private final String[] hovers;

    public ClickableRequest(BkPlugin plugin, String identifier, Player sender, Player target) {
        this.plugin = plugin;
        this.identifier = identifier;
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
        new ClickableMessage(lines)
                .addReceiver(target)
                .addButton(
                    new Button(
                            buttons[0],
                            new Hover(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovers[0]).create()),
                            commands[0],
                            "{accept-button}"
                    )
                )
                .addButton(
                    new Button(
                            buttons[1],
                            new Hover(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovers[1]).create()),
                            commands[1],
                            "{deny-button}"
                    )
                )
                .buildMessage()
                .sendMessage();
        clickableRequests.put(sender.getUniqueId(), this);
        if (timeout > 0) {
            ClickableRequest request = this;
            timeoutTimer = new BukkitRunnable() {
                @Override
                public void run() {
                    if (expireRunnable != null)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> expireRunnable.run(request));
                    removeInteraction(getIdentifier(), getSender().getUniqueId());
                }
            }.runTaskLater(plugin, 20L * timeout);
        }
    }

    public static boolean isPlayerInteracting(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests.values()) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) &&
                    (request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid))) {
                return true;
            }
        }
        return false;
    }

    public static ClickableRequest getInteraction(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests.values()) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) &&
                    (request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid))) {
                return request;
            }
        }
        return null;
    }

    public static ClickableRequest getInteraction(String identifier, UUID senderUuid, UUID targetUuid) {
        for (ClickableRequest request : clickableRequests.values()) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) &&
                    (request.getSender().getUniqueId().equals(senderUuid) && request.getTarget().getUniqueId().equals(targetUuid)) ||
                    (request.getTarget().getUniqueId().equals(senderUuid) && request.getSender().getUniqueId().equals(targetUuid))) {
                return request;
            }
        }
        return null;
    }

    public static void removeInteraction(String identifier, UUID uuid) {
        for (ClickableRequest request : clickableRequests.values()) {
            if (request.getIdentifier().equalsIgnoreCase(identifier) &&
                    (request.getSender().getUniqueId().equals(uuid) || request.getTarget().getUniqueId().equals(uuid))) {
                if (request.getTimeoutTimer() != null) request.getTimeoutTimer().cancel();
                clickableRequests.remove(request.getSender().getUniqueId());
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
