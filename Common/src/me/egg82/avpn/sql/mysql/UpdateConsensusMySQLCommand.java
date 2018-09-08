package me.egg82.avpn.sql.mysql;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.core.UpdateConsensusEventArgs;
import me.egg82.avpn.utils.ValidationUtil;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.patterns.Command;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.events.EventHandler;
import ninja.egg82.sql.ISQL;

public class UpdateConsensusMySQLCommand extends Command {
    // vars
    private ISQL sql = ServiceLocator.getService(ISQL.class);

    private UUID insertQuery = null;
    private UUID selectQuery = null;
    private UUID finalQuery = null;

    private String ip = null;
    private double value = 0.0d;

    private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
    private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);

    private EventHandler<UpdateConsensusEventArgs> updated = new EventHandler<UpdateConsensusEventArgs>();

    // constructor
    public UpdateConsensusMySQLCommand(String ip, double value) {
        super();

        this.ip = ip;
        this.value = value;

        sql.onError().attach(sqlError);
        sql.onData().attach(sqlData);
    }

    // public
    public EventHandler<UpdateConsensusEventArgs> onUpdated() {
        return updated;
    }

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!ValidationUtil.isValidIp(ip)) {
            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);
            return;
        }

        insertQuery = sql.query("INSERT INTO `antivpn_consensus` (`ip`, `value`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `value`=?, `created`=CURRENT_TIMESTAMP();", ip, Double.valueOf(value),
            Double.valueOf(value));
    }

    private void onSQLData(SQLEventArgs e) {
        if (e.getUuid().equals(insertQuery)) {
            selectQuery = sql.parallelQuery("SELECT `created` FROM `antivpn_consensus` WHERE `ip`=?;", ip);
        } else if (e.getUuid().equals(selectQuery)) {
            Exception lastEx = null;

            Timestamp created = null;
            Timestamp updated = new Timestamp(System.currentTimeMillis());

            for (Object[] o : e.getData().data) {
                try {
                    created = (Timestamp) o[0];
                } catch (Exception ex) {
                    IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
                    if (handler != null) {
                        handler.sendException(ex);
                    }
                    ex.printStackTrace();
                    lastEx = ex;
                }
            }

            if (created != null) {
                finalQuery = sql.parallelQuery("INSERT INTO `antivpn_consensus_queue` (`ip`, `value`, `created`, `updated`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `updated`=?;", ip,
                    Double.valueOf(value), created, updated, updated);
                onUpdated().invoke(this, new UpdateConsensusEventArgs(ip, value, created.getTime()));
            } else {
                sql.onError().detatch(sqlError);
                sql.onData().detatch(sqlError);
                onUpdated().invoke(this, UpdateConsensusEventArgs.EMPTY);
            }

            if (lastEx != null) {
                throw new RuntimeException(lastEx);
            }
        } else if (e.getUuid().equals(finalQuery)) {
            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);
        }
    }

    private void onSQLError(SQLEventArgs e) {
        if (!e.getUuid().equals(insertQuery) && !e.getUuid().equals(selectQuery) && !e.getUuid().equals(finalQuery)) {
            return;
        }

        IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
        if (handler != null) {
            handler.sendException(e.getSQLError().ex);
        }
        // Wrap in a new exception and print to console. We wrap so we know where the
        // error actually comes from
        new Exception(e.getSQLError().ex).printStackTrace();

        if (e.getUuid().equals(selectQuery) || e.getUuid().equals(finalQuery)) {
            sql.onError().detatch(sqlError);
            sql.onData().detatch(sqlError);
        }

        if (e.getUuid().equals(insertQuery) || e.getUuid().equals(selectQuery)) {
            onUpdated().invoke(this, UpdateConsensusEventArgs.EMPTY);
        }

        throw new RuntimeException(e.getSQLError().ex);
    }
}
