package dev.mruniverse.pixelmotd.global.shared;

import dev.mruniverse.pixelmotd.global.Control;
import dev.mruniverse.pixelmotd.global.Extras;
import dev.mruniverse.pixelmotd.global.enums.GuardianFiles;
import dev.mruniverse.pixelmotd.global.enums.MotdEventFormat;
import dev.mruniverse.pixelmotd.spigot.PixelMOTDBuilder;
import dev.mruniverse.pixelmotd.spigot.utils.PlaceholderParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpigotExtras implements Extras {

    private final PixelMOTDBuilder plugin;

    private final int max;

    private final Pattern varRegex = Pattern.compile("%player_([0-9]+)%");

    public SpigotExtras(PixelMOTDBuilder plugin) {
        this.plugin = plugin;
        this.max = Bukkit.getMaxPlayers();
    }


    @Override
    public String getVariables(String message,int customOnline,int customMax) {
        if(plugin.hasPAPI()) {
            message = PlaceholderParser.parse(plugin.getStorage().getLogs(),message);
        }
        return getWorlds(message).replace("%online%","" + Bukkit.getOnlinePlayers().size())
                .replace("%max%","" + max)
                .replace("%fake_online%","" + customOnline)
                .replace("%plugin_author%","MrUniverse44")
                .replace("%whitelist_author%", getWhitelistAuthor())
                .replace("%plugin_version%", plugin.getDescription().getVersion())
                .replace("%fake_max%","" + customMax);
    }

    @Override
    public List<String> getConvertedLines(List<String> lines,int more) {
        List<String> array = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("<hasOnline>") || line.contains("<hasMoreOnline>")) {
                int size = Bukkit.getOnlinePlayers().size();
                if (line.contains("<hasOnline>") && size >= 1) {
                    line = line.replace("<hasOnline>", "");
                    String replaceOnlineVariable = replaceOnlineVariable(line);
                    if(!replaceOnlineVariable.contains("%canNotFindX02_")) {
                        array.add(replaceOnlineVariable);
                    }
                    continue;
                }
                if(size >= more) {
                    more--;
                    int fixedSize = size - more;
                    line = line.replace("<hasMoreOnline>","")
                            .replace("%more_online%","" + fixedSize);
                    array.add(line);
                }
                continue;
            }
            array.add(line);
        }
        return array;
    }

    private String replaceOnlineVariable(String text) {
        Matcher matcher = varRegex.matcher(text);
        List<? extends Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if(players.size() >= 1) {
            while (matcher.find()) {
                int number = Integer.parseInt(matcher.group(1));
                if (players.size() >= number && number != 0) {
                    text = text.replace("%player_" + number + "%", players.get(number - 1).getName());
                } else {
                    text = text.replace("%player_","%canNotFindX02_");
                }
            }
        } else {
            text = text.replace("%player_","%canNotFindX02_");
        }
        return text;
    }

    private String getWhitelistAuthor() {
        Control whitelist = plugin.getStorage().getFiles().getControl(GuardianFiles.WHITELIST);
        if(!whitelist.getString("whitelist.author").equalsIgnoreCase("CONSOLE")) {
            return whitelist.getString("whitelist.author");
        } else {
            if(whitelist.getStatus("whitelist.customConsoleName.toggle")) {
                return whitelist.getString("whitelist.customConsoleName.name");
            }
            return "Console";
        }
    }

    private String getWorlds(String message){
        if(message.contains("%online_")) {
            for (World world : plugin.getServer().getWorlds()) {
                message = message.replace("%online_" + world.getName() + "%", world.getPlayers().size() + "");
            }
        }
        return getEvents(message);
    }

    @Override
    public String getEvents(String message) {
        if(message.contains("%event_")) {
            Control events = plugin.getStorage().getFiles().getControl(GuardianFiles.EVENTS);
            if(events.getStatus("events-toggle")) {
                Date CurrentDate = new Date();
                for (String event : events.getContent("events", false)) {
                    try {
                        String timeLeft;
                        long difference = getEventDate(events, event).getTime() - CurrentDate.getTime();
                        MotdEventFormat format = MotdEventFormat.getFromText(events.getString("events." + event + ".format-Type"));
                        if (difference >= 0L) {
                            timeLeft = convertTime(events,difference,format);
                        } else {
                            timeLeft = events.getColoredString("events." + event + ".endMessage");
                        }
                        message = message.replace("%event_" + event + "_name%", events.getString("events." + event + ".eventName"))
                                .replace("%event_" + event + "_TimeZone%", events.getString("events." + event + ".TimeZone"))
                                .replace("%event_" + event + "_TimeLeft%", timeLeft);
                    }catch (Throwable ignored) {
                        plugin.getStorage().getLogs().info("Can't load event info of " + event);
                    }
                }
            }
            return message;
        }
        return message;
    }

    public String convertTime(Control control,long time,MotdEventFormat format) {
        StringJoiner joiner = new StringJoiner(" ");
        if (format == MotdEventFormat.SECOND) {
            long seconds = time / 1000;
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                joiner.add(unitValue + ":");
            }
            if (seconds > 0 || joiner.length() == 0) {
                joiner.add(seconds + "");
            }

        } else if(format == MotdEventFormat.FIRST) {
            long seconds = time / 1000;
            String unit;
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                if (unitValue == 1) {
                    unit = control.getString("timer.week");
                } else {
                    unit = control.getString("timer.weeks");
                }
                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                if (unitValue == 1) {
                    unit = control.getString("timer.day");
                } else {
                    unit = control.getString("timer.days");
                }
                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                if (unitValue == 1) {
                    unit = control.getString("timer.hour");
                } else {
                    unit = control.getString("timer.hours");
                }

                joiner.add(unitValue + " " + unit);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                if (unitValue == 1) {
                    unit = control.getString("timer.minute");
                } else {
                    unit = control.getString("timer.minutes");
                }

                joiner.add(unitValue + " " + unit);
            }
            if (seconds > 0 || joiner.length() == 0) {
                if (seconds == 1) {
                    unit = control.getString("timer.second");
                } else {
                    unit = control.getString("timer.seconds");
                }

                joiner.add(seconds + " " + unit);
            }
        } else if(format == MotdEventFormat.THIRD) {
            long seconds = time / 1000;
            String separator = control.getString("timer.separator");
            int unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(7));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(7);
                joiner.add(unitValue + control.getString("timer.w") + separator);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.DAYS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.DAYS.toSeconds(1);
                joiner.add(unitValue + control.getString("timer.d") + separator);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.HOURS.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.HOURS.toSeconds(1);
                joiner.add(unitValue + control.getString("timer.h") + separator);
            }
            unitValue = Math.toIntExact(seconds / TimeUnit.MINUTES.toSeconds(1));
            if (unitValue > 0) {
                seconds %= TimeUnit.MINUTES.toSeconds(1);
                joiner.add(unitValue + control.getString("timer.m") + separator);
            }
            if (seconds > 0 || joiner.length() == 0) {
                joiner.add(seconds + control.getString("timer.s"));
            }
        }
        if(format == MotdEventFormat.SECOND) {
            return joiner.toString().replace(" ","");
        } else {
            return joiner.toString();
        }
    }

    public Date getEventDate(Control control,String eventName) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(control.getString("pattern","MM/dd/yy HH:mm:ss"));
        format.setTimeZone(TimeZone.getTimeZone(control.getString("events." + eventName + ".TimeZone")));
        return format.parse(control.getString("events." + eventName + ".eventDate"));
    }

    @Override
    public String getCentered(String message) {
        return message;
    }
}
