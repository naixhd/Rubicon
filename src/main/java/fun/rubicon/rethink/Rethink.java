package fun.rubicon.rethink;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.ast.Db;
import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import fun.rubicon.util.Logger;

/**
 * @author ForYaSee / Yannick Seeger
 */
public class Rethink {

    public final RethinkDB rethinkDB;
    private final String host;
    private final int port;
    private final String dbName;
    private final String user;
    private final String password;
    public Connection connection;
    public Db db;

    public Rethink(String host, int port, String db, String user, String password) {
        this.host = host;
        this.port = port;
        this.dbName = db;
        this.user = user;
        this.password = password;

        rethinkDB = RethinkDB.r;
    }

    public void connect() {
        connection = rethinkDB.connection().hostname(host).port(port).user(user, password).connect();
        db = rethinkDB.db(dbName);
        Logger.info("RethinkDB connection success");
    }

    public void createTable(String name) {
        rethinkDB.db(db).tableCreate(name).run(connection);
    }

    public RethinkDB getRethinkDB() {
        return rethinkDB;
    }

    public Connection getConnection() {
        return connection;
    }
}