package fun.rubicon.io.db;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Db;
import com.rethinkdb.gen.ast.Json;
import com.rethinkdb.gen.exc.ReqlOpFailedError;
import com.rethinkdb.net.Connection;
import de.jakobjarosch.rethinkdb.pool.ConnectionPoolMetrics;
import de.jakobjarosch.rethinkdb.pool.RethinkDBPool;
import de.jakobjarosch.rethinkdb.pool.RethinkDBPoolBuilder;
import fun.rubicon.core.ShutdownManager;
import fun.rubicon.entities.*;
import fun.rubicon.entities.impl.*;
import fun.rubicon.io.Data;
import fun.rubicon.provider.GuildProvider;
import fun.rubicon.provider.UserProvider;
import fun.rubicon.util.RubiconInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ForYaSee / Yannick Seeger
 */
public class RethinkDatabase {

    public final RethinkDB r = RethinkDB.r;
    private String dbName;
    private final Logger logger = LoggerFactory.getLogger(RethinkDatabase.class);
    private int connectionAttempt;
    private RethinkDBPool pool;

    public RethinkDatabase() {
        connectionAttempt = 0;
        connect();
    }

    private void connect() {
        String dbHost = Data.cfg().has("rethinkdb_host") ? (String) Data.cfg().getElementFromArray("rethinkdb_host", connectionAttempt) : null;
        String dbUser = Data.cfg().has("rethinkdb_user") ? (String) Data.cfg().getElementFromArray("rethinkdb_user", connectionAttempt) : null;
        String rawDbPort = Data.cfg().has("rethinkdb_port") ? (String) Data.cfg().getElementFromArray("rethinkdb_port", connectionAttempt) : null;
        int dbPort;
        try {
            dbPort = rawDbPort == null ? 0 : Integer.parseInt(rawDbPort);
        } catch (NumberFormatException e) {
            dbPort = 0;
        }
        String dbPassword = Data.cfg().has("rethinkdb_password") ? (String) Data.cfg().getElementFromArray("rethinkdb_password", connectionAttempt) : null;
        String db = Data.cfg().has("rethinkdb_db") ? Data.cfg().getString("rethinkdb_db") : null;
        dbName = db;

        if (dbHost == null || dbUser == null || dbPort == 0 || dbPassword == null || db == null)
            ShutdownManager.shutdown(RethinkDatabase.class, "One or more of the connection properties are null.");
        try {
            assert dbHost != null;
            assert dbUser != null;
            assert dbPassword != null;
            assert db != null;
            r.connection().hostname(dbHost).port(dbPort).user(dbUser, dbPassword).db(db).connect();
            logger.info("Creating connection pool connections...");
            pool = new RethinkDBPoolBuilder().hostname(dbHost).port(dbPort).username(dbUser).password(dbPassword).database(db).maxConnections(1000).timeout(10).build();
            logger.info("Successfully created connection pool.");
            //CreateDefaults
            createDefaultTables();

        } catch (Exception e) {
            logger.error(String.format("Can't create a connection to %s", dbHost), e);
            connectionAttempt++;
            connect();
        }
    }

    //Entity Getter
    public User getUser(@Nonnull net.dv8tion.jda.core.entities.User jdaUser) {
        Map map = r.table(UserImpl.TABLE).get(jdaUser.getId()).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        UserImpl user = gson.fromJson(json, UserImpl.class);
        if (user == null)
            user = new UserImpl(jdaUser, "No bio set.", 0L, "en-US", null, 0L, new HashMap<>());
        user.setJDAUser(jdaUser);
        UserProvider.addUser(user);
        return user;
    }

    public Guild getGuild(@Nonnull net.dv8tion.jda.core.entities.Guild jdaGuild) {
        Map map = r.table(GuildImpl.TABLE).get(jdaGuild.getId()).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        GuildImpl guild = gson.fromJson(json, GuildImpl.class);
        if (guild == null)
            guild = new GuildImpl(jdaGuild, RubiconInfo.DEFAULT_PREFIX, null);
        //Set vars
        guild.setGuild(jdaGuild);
        guild.setJoinmessage(getJoinmessage(jdaGuild.getId()));
        guild.setLeavemessage(getLeavemessage(jdaGuild.getId()));
        guild.setJoinimage(getJoinimage(jdaGuild.getId()));
        guild.setAutochannel(getAutochannel(jdaGuild.getId()));
        guild.setPortalSettings(getPortalSettings(jdaGuild.getId()));
        guild.setVerificationSettings(getVerificationSettings(jdaGuild.getId()));
        //Save Guild in Cache
        GuildProvider.addGuild(guild);
        return guild;
    }

    public Joinmessage getJoinmessage(String guildId) {
        Map map = r.table(JoinmessageImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        JoinmessageImpl joinmessage = gson.fromJson(json, JoinmessageImpl.class);
        if (joinmessage == null)
            return null;
        return joinmessage;
    }

    public Leavemessage getLeavemessage(String guildId) {
        Map map = r.table(LeavemessageImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        LeavemessageImpl leavemessage = gson.fromJson(json, LeavemessageImpl.class);
        if (leavemessage == null)
            return null;
        return leavemessage;
    }

    public Joinimage getJoinimage(String guildId) {
        Map map = r.table(JoinImageImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        JoinImageImpl joinImage = gson.fromJson(json, JoinImageImpl.class);
        if (joinImage == null)
            return null;
        return joinImage;
    }

    public Autochannel getAutochannel(String guildId) {
        Map map = r.table(AutochannelImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        AutochannelImpl autochannel = gson.fromJson(json, AutochannelImpl.class);
        if (autochannel == null)
            return null;
        return autochannel;
    }

    public PortalSettings getPortalSettings(String guildId) {
        Map map = r.table(PortalSettingsImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        PortalSettingsImpl portalSettings = gson.fromJson(json, PortalSettingsImpl.class);
        if (portalSettings == null)
            return null;
        return portalSettings;
    }

    public VerificationSettings getVerificationSettings(String guildId) {
        Map map = r.table(VerificationSettingsImpl.TABLE).get(guildId).run(getConnection());
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        VerificationSettingsImpl verificationSettings = gson.fromJson(json, VerificationSettingsImpl.class);
        if (verificationSettings == null)
            return null;
        return verificationSettings;
    }

    //TODO Implement Database things
    public Member getMember(net.dv8tion.jda.core.entities.Member member) {
        return new MemberImpl(member);
    }

    //RethinkDataset methods
    public void save(@Nonnull RethinkDataset dataset) {
        if (dataset.getId() == null)
            return;
        logger.debug(String.format("Saving %s in %s", dataset.getId(), dataset.getTable()));
        r.table(dataset.getTable()).insert(r.array(new Json(new Gson().toJson(dataset, dataset.getClass())))).optArg("conflict", "replace").run(getConnection());
    }

    public void delete(@Nonnull RethinkDataset dataset) {
        logger.debug(String.format("Deleting %s from %s", dataset.getId(), dataset.getTable()));
        r.table(dataset.getTable()).get(dataset.getId()).delete().run(getConnection());
    }

    public Connection getConnection() {
        return getConnection(-1);
    }

    public Connection getConnection(int timeout) {
        if (pool.getMetrics().getPoolHealth().equals(ConnectionPoolMetrics.PoolHealth.FULL)) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (timeout == -1)
            return pool.getConnection();
        return pool.getConnection(timeout);
    }

    public void closePool() {
        if (pool != null)
            pool.shutdown();
    }

    private void createDefaultTables() {
        String[] tables = {
                "users",
                "guilds",
                "mutesettings",
                "joinmessages",
                "joinimages",
                "leavemessages",
                "autochannels",
                "autoroles",
                "punishments",
                "lavanodes",
                "permissions",
                "youtube",
                "verification_settings",
                "verification_users",
                "keys",
                "warn_punishments",
                "warns",
                "reminders",
                "portals",
                "portal_invites",
                "portal_settings",
                "votes",
                "giveaways",
                "premiums"
        };
        Db db = r.db(dbName);
        db.config().update(r.hashMap("write_acks", "single")).run(getConnection());
        for (String table : tables) {
            try {
                db.tableCreate(table).run(getConnection());
                db.table(table).reconfigure().optArg("shards", 4).optArg("replicas", 4).run(getConnection());
                db.table(table).optArg("read_mode", "outdated").run(getConnection());
                db.table(table).update(r.hashMap("durability", "soft")).run(getConnection());
            } catch (ReqlOpFailedError ignored) {
                //ignored because its working like -> CREATE TABLE IF NOT EXIST
            }
        }
    }
}
