/*
 * Copyright (c) 2017 Rubicon Bot Development Team
 *
 * Licensed under the MIT license. The full license text is available in the LICENSE file provided with this project.
 */

package fun.rubicon.permission;

import fun.rubicon.RubiconBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 * Specifies a permission target.
 */
public class PermissionTarget {
    public enum Type {
        USER('u', "user"),
        ROLE('r', "role"),
        DISCORD_PERMISSION('d', "discord permission");

        private final char identifier;
        private final String name;

        Type(char identifier, String name) {
            this.identifier = identifier;
            this.name = name;
        }

        public char getIdentifier() {
            return identifier;
        }

        public String getName() {
            return name;
        }

        public static Type getByIdentifier(char identifier) {
            for (Type type : values())
                if (type.identifier == identifier)
                    return type;
            return null;
        }
    }
    private final Guild guild;

    private final Type type;
    private final long id;

    public PermissionTarget(Guild guild, Type type, long id) {
        this.guild = guild;
        this.type = type;
        this.id = id;
    }

    public Guild getGuild() {
        return guild;
    }

    public Type getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public User getUser() {
        return type == Type.USER ? guild.getJDA().getUserById(id) : null;
    }

    public Role getRole() {
        return type == Type.ROLE ? guild.getRoleById(id) : null;
    }

    public Permission getPermission() {
        return type == Type.DISCORD_PERMISSION ? Permission.getFromOffset((int) id) : null;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PermissionTarget // object type
                && guild.equals(((PermissionTarget) obj).guild) // guild
                && type == ((PermissionTarget) obj).type // target type
                && id == ((PermissionTarget) obj).id; // id
    }

    @Override
    public String toString() {
        return type == Type.USER ? getUser().getName() // user name
                : (type == Type.ROLE ? getRole().getName() // or role name
                : getPermission().getName()) // or permission name
                + " (" + type.getName() + ")"; // and type
    }

    public boolean exists() {
        switch (type) {
            case USER:
                return RubiconBot.getJDA().getUserById(id) != null;
            case ROLE:
                return RubiconBot.getJDA().getRoleById(id) != null;
            case DISCORD_PERMISSION:
                return Permission.getFromOffset((int) id) != Permission.UNKNOWN;
        }
        return false;
    }
}
