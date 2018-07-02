package me.egg82.avpn.sql.mysql;

import java.util.UUID;
import java.util.function.BiConsumer;

import ninja.egg82.bukkit.services.ConfigRegistry;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.exceptionHandlers.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IVariableRegistry;
import ninja.egg82.patterns.Command;
import ninja.egg82.sql.ISQL;

public class CreateTablesMySQLCommand extends Command {
	//vars
	private ISQL sql = ServiceLocator.getService(ISQL.class);
	
	private UUID mainQuery = null;
	private UUID queueQuery = null;
	
	private UUID finalQuery = null;
	
	private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
	private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);
	
	//constructor
	public CreateTablesMySQLCommand() {
		super();
		
		sql.onError().attach(sqlError);
		sql.onData().attach(sqlData);
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		IVariableRegistry<String> configRegistry = ServiceLocator.getService(ConfigRegistry.class);
		mainQuery = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='antivpn';", configRegistry.getRegister("sql.mysql.database", String.class));
		queueQuery = sql.query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='antivpn_queue';", configRegistry.getRegister("sql.mysql.database", String.class));
	}
	private void onSQLData(SQLEventArgs e) {
		if (e.getUuid().equals(mainQuery)) {
			if (e.getData().data.length > 0 && e.getData().data[0].length > 0 && ((Number) e.getData().data[0][0]).intValue() != 0) {
				return;
			}
			
			sql.query(
				"CREATE TABLE `antivpn` ("
						+ "`ip` VARCHAR(45) NOT NULL,"
						+ "`value` BOOLEAN NOT NULL,"
						+ "`created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
				+ ");"
			);
			sql.query("ALTER TABLE `antivpn` ADD UNIQUE (`ip`);");
		} else if (e.getUuid().equals(queueQuery)) {
			if (e.getData().data.length > 0 && e.getData().data[0].length > 0 && ((Number) e.getData().data[0][0]).intValue() != 0) {
				sql.onError().detatch(sqlError);
				sql.onData().detatch(sqlError);
				return;
			}
			
			sql.query(
				"CREATE TABLE `antivpn_queue` ("
						+ "`ip` VARCHAR(45) NOT NULL,"
						+ "`value` BOOLEAN NOT NULL,"
						+ "`created` TIMESTAMP NOT NULL,"
						+ "`updated` TIMESTAMP NOT NULL"
				+ ");"
			);
			finalQuery = sql.query("ALTER TABLE `antivpn_queue` ADD UNIQUE (`ip`);");
		} else if (e.getUuid().equals(finalQuery)) {
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
		}
	}
	private void onSQLError(SQLEventArgs e) {
		if (!e.getUuid().equals(mainQuery) && !e.getUuid().equals(queueQuery) && !e.getUuid().equals(finalQuery)) {
			return;
		}
		
		ServiceLocator.getService(IExceptionHandler.class).silentException(e.getSQLError().ex);
		// Wrap in a new exception and print to console. We wrap so we know where the error actually comes from
		new Exception(e.getSQLError().ex).printStackTrace();
		
		if (e.getUuid().equals(queueQuery) || e.getUuid().equals(finalQuery)) {
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
		}
		
		throw new RuntimeException(e.getSQLError().ex);
	}
}
