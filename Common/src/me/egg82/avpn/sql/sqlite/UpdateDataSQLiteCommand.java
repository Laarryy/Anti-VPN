package me.egg82.avpn.sql.sqlite;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.core.UpdateEventArgs;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.events.EventHandler;
import ninja.egg82.patterns.Command;
import ninja.egg82.sql.ISQL;

public class UpdateDataSQLiteCommand extends Command {
    // vars
    private ISQL sql = ServiceLocator.getService(ISQL.class);

    private UUID insertQuery = null;
    private UUID finalQuery = null;

    private String ip = null;
    private boolean value = false;
    private long createdTime = -1L;

    private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
    private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);

    private EventHandler<UpdateEventArgs> updated = new EventHandler<UpdateEventArgs>();

    // constructor
    public UpdateDataSQLiteCommand(String ip, boolean value) {
        super();

        this.ip = ip;
        this.value = value;

        sql.onError().attach(sqlError);
        sql.onData().attach(sqlData);
    }

    public UpdateDataSQLiteCommand(String ip, boolean value, long created) {
        super();

        this.ip = ip;
        this.value = value;
        this.createdTime = created;

        sql.onError().attach(sqlError);
        sql.onData().attach(sqlData);
    }

    // public
    public EventHandler<UpdateEventArgs> onUpdated() {
        return updated;
    }

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!ValidationUtil.isValidIp(ip)) {
            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);
            return;
        }

        if (createdTime == -1L) {
            insertQuery = sql.query("INSERT OR REPLACE INTO `antivpn` (`ip`, `value`) VALUES (?, ?);", ip, Integer.valueOf((value) ? 1 : 0));
        } else {
            insertQuery = sql.query("INSERT OR REPLACE INTO `antivpn` (`ip`, `value`, `created`) VALUES (?, ?, ?);", ip, Integer.valueOf((value) ? 1 : 0), new Timestamp(createdTime));
        }
    }

    private void onSQLData(SQLEventArgs e) {
        if (e.getUuid().equals(insertQuery)) {
            finalQuery = sql.parallelQuery("SELECT `created` FROM `antivpn` WHERE `ip`=?;", ip);
        } else if (e.getUuid().equals(finalQuery)) {
            if (createdTime != -1L) {
                sql.onError().detatch(sqlError);
                sql.onData().detatch(sqlError);
                onUpdated().invoke(this, new UpdateEventArgs(ip, value, createdTime));
                return;
            }

            Exception lastEx = null;

            long created = -1L;

            for (Object[] o : e.getData().data) {
                try {
                    created = Timestamp.valueOf((String) o[0]).getTime();
                } catch (Exception ex) {
                    IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                    if (handler != null) {
                        handler.sendException(ex);
                    }
                    ex.printStackTrace();
                    lastEx = ex;
                }
            }

            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);

            onUpdated().invoke(this, new UpdateEventArgs(ip, value, created));

            if (lastEx != null) {
                throw new RuntimeException(lastEx);
            }
        }
    }

    private void onSQLError(SQLEventArgs e) {
        if (!e.getUuid().equals(insertQuery) && !e.getUuid().equals(finalQuery)) {
            return;
        }

        IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
        if (handler != null) {
            handler.sendException(e.getSQLError().ex);
        }
        // Wrap in a new exception and print to console. We wrap so we know where the
        // error actually comes from
        new Exception(e.getSQLError().ex).printStackTrace();

        sql.onError().detatch(sqlError);
        sql.onData().detatch(sqlError);

        onUpdated().invoke(this, UpdateEventArgs.EMPTY);

        throw new RuntimeException(e.getSQLError().ex);
    }
}
