package me.egg82.avpn.sql.sqlite;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.BiConsumer;

import me.egg82.avpn.Config;
import me.egg82.avpn.core.ResultEventArgs;
import ninja.egg82.events.SQLEventArgs;
import ninja.egg82.exceptionHandlers.IExceptionHandler;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.events.EventHandler;
import ninja.egg82.patterns.Command;
import ninja.egg82.sql.ISQL;

public class SelectResultSQLiteCommand extends Command {
	//vars
	private ISQL sql = ServiceLocator.getService(ISQL.class);
	
	private UUID query = null;
	
	private String ip = null;
	
	private BiConsumer<Object, SQLEventArgs> sqlError = (s, e) -> onSQLError(e);
	private BiConsumer<Object, SQLEventArgs> sqlData = (s, e) -> onSQLData(e);
	
	private EventHandler<ResultEventArgs> onData = new EventHandler<ResultEventArgs>();
	
	//constructor
	public SelectResultSQLiteCommand(String ip) {
		super();
		
		this.ip = ip;
		
		sql.onError().attach(sqlError);
		sql.onData().attach(sqlData);
	}
	
	//public
	public EventHandler<ResultEventArgs> onData() {
		return onData;
	}
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		query = sql.parallelQuery("SELECT `value`, `created` FROM `antivpn` WHERE `ip`=? AND CURRENT_TIMESTAMP > DATETIME(`created`, ?);", ip, "+" + Math.floorDiv(Config.sourceCacheTime, 1000L) + " seconds");
	}
	
	private void onSQLData(SQLEventArgs e) {
		if (e.getUuid().equals(query)) {
			Exception lastEx = null;
			
			ResultEventArgs retVal = null;
			// Iterate rows
			for (Object[] o : e.getData().data) {
				try {
					// Grab all data and convert to more useful object types
					Boolean value = (((Number) o[0]).intValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
					long created = Timestamp.valueOf((String) o[1]).getTime();
					
					// Add new data
					retVal = new ResultEventArgs(ip, value, created);
				} catch (Exception ex) {
					ServiceLocator.getService(IExceptionHandler.class).silentException(ex);
					ex.printStackTrace();
					lastEx = ex;
				}
			}
			
			sql.onError().detatch(sqlError);
			sql.onData().detatch(sqlError);
			
			onData.invoke(this, retVal);
			
			if (lastEx != null) {
				throw new RuntimeException(lastEx);
			}
		}
	}
	private void onSQLError(SQLEventArgs e) {
		if (!e.getUuid().equals(query)) {
			return;
		}
		
		ServiceLocator.getService(IExceptionHandler.class).silentException(e.getSQLError().ex);
		// Wrap in a new exception and print to console. We wrap so we know where the error actually comes from
		new Exception(e.getSQLError().ex).printStackTrace();
		
		sql.onError().detatch(sqlError);
		sql.onData().detatch(sqlError);
		
		onData.invoke(this, null);
		
		throw new RuntimeException(e.getSQLError().ex);
	}
}
