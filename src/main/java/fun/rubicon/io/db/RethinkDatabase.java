package fun.rubicon.io.db;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Json;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import fun.rubicon.core.ShutdownManager;
import fun.rubicon.entities.User;
import fun.rubicon.entities.impl.UserImpl;
import fun.rubicon.io.Data;
import fun.rubicon.provider.UserProvider;
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
    private final Logger logger = LoggerFactory.getLogger(RethinkDatabase.class);
    private int connectionAttempt;
    private Connection connection;

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

        if (dbHost == null || dbUser == null || dbPort == 0 || dbPassword == null || db == null)
            ShutdownManager.shutdown(RethinkDatabase.class, "One or more of the connection properties are null.");
        try {
            assert dbHost != null;
            assert dbUser != null;
            assert dbPassword != null;
            assert db != null;
            connection = r.connection().hostname(dbHost).port(dbPort).user(dbUser, dbPassword).db(db).connect();
        } catch (Exception e) {
            logger.error(String.format("Can't create a connection to %s", dbHost), e);
            connectionAttempt++;
            connect();
        }
    }

    //Entity Getter
    public User getUser(@Nonnull net.dv8tion.jda.core.entities.User jdaUser) {
        Map map = r.table(UserImpl.TABLE).get(jdaUser.getId()).run(connection);
        Gson gson = new Gson();
        JsonElement json = gson.toJsonTree(map);
        UserImpl user = gson.fromJson(json, UserImpl.class);
        if(user == null)
            user = new UserImpl(jdaUser, "No bio set.", 0L, "en-US", null, 0L, new HashMap<>());
        user.setJDAUser(jdaUser);
        logger.info("Premium: " + user.toString());
        UserProvider.addUser(user);
        return user;
    }

    public void save(@Nonnull RethinkDataset dataset) {
        if(dataset.getId() == null)
            return;
        checkConnection();
        logger.debug(String.format("Saving %s in %s", dataset.getId(), dataset.getTable()));
        r.table(dataset.getTable()).insert(r.array(new Json(new Gson().toJson(dataset, dataset.getClass())))).optArg("conflict", "replace").run(connection);
    }

    public void delete(@Nonnull RethinkDataset dataset) {
        checkConnection();
        logger.debug(String.format("Deleting %s from %s", dataset.getId(), dataset.getTable()));
        r.table(dataset.getTable()).get(dataset.getId()).delete().runNoReply(connection);
    }

    public Connection getConnection() {
        checkConnection();
        return connection;
    }

    public void closeConnection() {
        if (connection != null)
            connection.close();
    }

    private void checkConnection() {
        if (connection == null) {
            logger.warn("Connection is null. Trying to reconnect...");
            connect();
        }
    }
}
