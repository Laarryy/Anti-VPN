package me.egg82.avpn.sql.sqlite;

import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.Config;
import ninja.egg82.analytics.exceptions.IExceptionHandler;
import ninja.egg82.enums.BaseSQLType;
import ninja.egg82.events.CompleteEventArgs;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.Command;
import ninja.egg82.sql.ISQL;

public class ClearDataSQLiteCommand extends Command {
	//vars
	private ISQL sql = ServiceLocator.getService(ISQL.class);
	
	private UUID query = null;
	
	private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
	private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);
	
	//constructor
	public ClearDataSQLiteCommand() {
		super();
		
		sql.onError().attach(sqlError);
		sql.onData().attach(sqlData);
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		if (sql.getType() == BaseSQLType.SQLite) {
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
			
			onComplete().invoke(this, CompleteEventArgs.EMPTY);
			return;
		}
		
		query = sql.parallelQuery("DELETE FROM `antivpn` WHERE CURRENT_TIMESTAMP > DATETIME(`created`, ?);", "+" + Math.floorDiv(Config.sourceCacheTime, 1000L) + " seconds");
	}
	private void onSQLData(SQLEventArgs e) {
		if (e.getUuid().equals(query)) {
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
			
			onComplete().invoke(this, CompleteEventArgs.EMPTY);
		}
	}
	private void onSQLError(SQLEventArgs e) {
		if (!e.getUuid().equals(query)) {
			return;
		}
		
		IExceptionHandler handler = ServiceLocator.getService(IExceptionHandler.class);
		if (handler != null) {
			handler.sendException(e.getSQLError().ex);
		}
		// Wrap in a new exception and print to console. We wrap so we know where the error actually comes from
		new Exception(e.getSQLError().ex).printStackTrace();
		
		sql.onError().detatch(sqlError);
		sql.onData().detatch(sqlError);
		
		onComplete().invoke(this, CompleteEventArgs.EMPTY);
		
		throw new RuntimeException(e.getSQLError().ex);
	}
}
