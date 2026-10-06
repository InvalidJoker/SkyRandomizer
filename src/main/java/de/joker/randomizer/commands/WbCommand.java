package de.joker.randomizer.commands;

import de.joker.randomizer.manager.ServiceManager;
import org.bukkit.entity.Player;

import java.util.List;

public class WbCommand extends AbstractCommand {

    public WbCommand(ServiceManager serviceManager) {
        super(serviceManager);
    }

    @Override
    protected String name() {
        return "wb";
    }

    @Override
    protected List<String> aliases() {
        return List.of(
                "workbench",
                "werkbank",
                "work-bench"
        );
    }

    @Override
    protected void execute(Player player) {
        player.openWorkbench(null, true);
    }
}