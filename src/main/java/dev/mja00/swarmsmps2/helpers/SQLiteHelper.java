package dev.mja00.swarmsmps2.helpers;

import dev.mja00.swarmsmps2.objects.BlockEventObject;
import dev.mja00.swarmsmps2.objects.MobKillObject;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

/* This class is where we connect to our SQLite database, and do any related queries.
*  We have a JDBC connector that we can use, specifically the Xerial driver */
public class SQLiteHelper {
    private String dbPath;
    private Connection connection;
    private static Logger LOGGER = LogManager.getLogger("SQLITEHELPER");

    public SQLiteHelper(String dbPath) {
        this.dbPath = dbPath;
    }

    public void connect() throws SQLException {
        if (this.connection == null) {
            attemptDBConnection();
        } else if (this.connection.isClosed()) {
            // Re-open our connection
            attemptDBConnection();
        }
    }

    public void close() throws SQLException {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    private void attemptDBConnection() throws SQLException {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.dbPath);
        } catch (SQLException e) {
            if (e instanceof SQLTimeoutException) {
                LOGGER.error("Timed out while connecting to SQLite database: " + e.getMessage());
            } else {
                LOGGER.error("Error while connecting to SQLite database: " + e.getMessage());
            }
            // If we can't connect, we should really shut down the server
            // We'll just throw upstream and let the server handle it
            throw e;
        }
    }

    // Our setup db method
    public void setup() {
        // We'll want to create a handful of tables here for our needs
        // We'll want a table for all world altering events
        String createWorldEventsTable = """
                CREATE TABLE IF NOT EXISTS world_events (
                	id integer PRIMARY KEY,
                	event_type text NOT NULL,
                	player text NOT NULL,
                	block_x integer NOT NULL,
                	block_y integer NOT NULL,
                	block_z integer NOT NULL,
                	block_data text NOT NULL,
                	block_count integer NOT NULL DEFAULT 1,
                	event_time integer NOT NULL
                );""";
        // We'll want a table for all player join/leave events
        String createPlayerEventsTable = """
                CREATE TABLE IF NOT EXISTS player_events (
                	id integer PRIMARY KEY,
                	event_type text NOT NULL,
                	player text NOT NULL,
                	event_time integer NOT NULL
                );""";
        // Mob kills
        String createPlayerMobKillsTables = """
                CREATE TABLE IF NOT EXISTS player_mob_kills (
                    id integer PRIMARY KEY,
                    player text NOT NULL,
                    mob_type text NOT NULL,
                    event_time integer NOT NULL
                );""";
        // Now create our tables
        try {
            this.connection.createStatement().execute(createWorldEventsTable);
            this.connection.createStatement().execute(createPlayerEventsTable);
            this.connection.createStatement().execute(createPlayerMobKillsTables);
        } catch (SQLException e) {
            LOGGER.error("Error while creating tables: " + e.getMessage());
        }
    }

    public void migrateData() {
        // Add block_count to world_events if it's not there
        String alterWorldEventsTable = """
                ALTER TABLE world_events ADD COLUMN block_count integer NOT NULL DEFAULT 1;""";
        try {
            if (!columnExists("world_events", "block_count")) {
                this.connection.createStatement().execute(alterWorldEventsTable);
                LOGGER.info("Added block_count column to world_events table");
            }
        } catch (SQLException e) {
            LOGGER.error("Error while migrating data: " + e.getMessage());
        }
    }

    public boolean columnExists(String table, String column) {
        String statement = """
                PRAGMA table_info(%s);
                """;
        try {
            ResultSet resultSet = this.connection.createStatement().executeQuery(String.format(statement, table));
            while (resultSet.next()) {
                if (resultSet.getString("name").equals(column)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error while checking if column exists: " + e.getMessage());
        }
        return false;
    }

    public void createWorldEvent(BlockEventObject eventData) {
        // Create our insert statement
        String insertStatement = """
                INSERT INTO world_events (event_type, player, block_x, block_y, block_z, block_data, event_time, block_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(insertStatement);
            preparedStatement.setString(1, eventData.getEvent());
            preparedStatement.setString(2, eventData.getPlayerUUID().toString());
            preparedStatement.setInt(3, eventData.getX());
            preparedStatement.setInt(4, eventData.getY());
            preparedStatement.setInt(5, eventData.getZ());
            preparedStatement.setString(6, eventData.getBlockName());
            preparedStatement.setLong(7, System.currentTimeMillis());
            preparedStatement.setInt(8, eventData.getBlockCount());
            // Execute our statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error while creating world event: " + e.getMessage());
        }
    }

    public BlockEventObject[] getPlayerWorldEvents(String player, int limit, int offset) {
        // Create our select statement
        String selectStatement = """
                SELECT * FROM world_events WHERE player = ? ORDER BY event_time DESC LIMIT ? OFFSET ?;""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(selectStatement);
            preparedStatement.setString(1, player);
            preparedStatement.setInt(2, limit);
            preparedStatement.setInt(3, offset);
            return runBlockEventListPrepared(preparedStatement, limit);
        } catch (SQLException e) {
            LOGGER.error("Error while getting player world events: " + e.getMessage());
            return null;
        }
    }

    public BlockEventObject[] getWorldEventsAtBlock(BlockPos coords, int limit, int offset) {
        String selectStatement = """
                SELECT * FROM world_events WHERE block_x = ? AND block_y = ? AND block_z = ? ORDER BY event_time DESC LIMIT ? OFFSET ?;""";
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(selectStatement);
            preparedStatement.setInt(1, coords.getX());
            preparedStatement.setInt(2, coords.getY());
            preparedStatement.setInt(3, coords.getZ());
            preparedStatement.setInt(4, limit);
            preparedStatement.setInt(5, offset);
            return runBlockEventListPrepared(preparedStatement, limit);
        } catch (SQLException e) {
            LOGGER.error("Error while getting world events at block: " + e.getMessage());
            return null;
        }
    }

    public BlockEventObject[] getWorldEventsAtBlocks(BlockPos[] coords, int limit, int offset) {
        // This will be a list of coordinates, we need to get all events at any of these coordinates
        // We'll need to create a string of question marks for our prepared statement
        String selectStatement = getString(coords);
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(selectStatement);
            preparedStatement.setInt(1, limit);
            preparedStatement.setInt(2, offset);
            return runBlockEventListPrepared(preparedStatement, limit);
        } catch (SQLException e) {
            LOGGER.error("Error while getting world events at blocks: " + e.getMessage());
            return null;
        }
    }

    private static String getString(BlockPos[] coords) {
        StringBuilder questionMarks = new StringBuilder();
        for (BlockPos coord : coords) {
            questionMarks.append("(").append(coord.getX()).append(", ").append(coord.getY()).append(", ").append(coord.getZ()).append(")");
            questionMarks.append(", ");
        }
        questionMarks.deleteCharAt(questionMarks.length() - 1);
        questionMarks.deleteCharAt(questionMarks.length() - 1);
        // Create our select statement
        return """
                WITH cte(x, y, z) AS (VALUES %s) SELECT * FROM world_events WHERE (block_x, block_y, block_z) in cte ORDER BY event_time DESC LIMIT ? OFFSET ?;""".formatted(questionMarks.toString());
    }

    private BlockEventObject[] runBlockEventListPrepared(PreparedStatement preparedStatement, int limit) throws SQLException{
        ResultSet resultSet = preparedStatement.executeQuery();
        // Create our array of BlockEventObjects
        BlockEventObject[] blockEvents = new BlockEventObject[limit];
        // Iterate through our result set
        int i = 0;
        while (resultSet.next()) {
            // Get our data
            String playerUUID = resultSet.getString("player");
            String blockName = resultSet.getString("block_data");
            int x = resultSet.getInt("block_x");
            int y = resultSet.getInt("block_y");
            int z = resultSet.getInt("block_z");
            String eventType = resultSet.getString("event_type");
            long timestamp = resultSet.getLong("event_time");
            int blockCount = resultSet.getInt("block_count");
            // Create our BlockEventObject
            BlockEventObject blockEvent = new BlockEventObject(playerUUID, blockName, eventType, x, y, z, timestamp, blockCount);
            // Add it to our array
            blockEvents[i] = blockEvent;
            i++;
        }
        // Return our array
        return blockEvents;
    }

    public void createPlayerEvent(String eventType, String playerName) {
        // Create our insert statement
        String insertStatement = """
                INSERT INTO player_events (event_type, player, event_time)
                VALUES (?, ?, ?);""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(insertStatement);
            preparedStatement.setString(1, eventType);
            preparedStatement.setString(2, playerName);
            preparedStatement.setLong(3, System.currentTimeMillis());
            // Execute our statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error while creating player event: " + e.getMessage());
        }
    }

    public void createMobKillEvent(String mob, String playerUUID) {
        // Create our insert statement
        String insertStatement = """
                INSERT INTO player_mob_kills (player, mob_type, event_time)
                VALUES (?, ?, ?);""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(insertStatement);
            preparedStatement.setString(1, playerUUID);
            preparedStatement.setString(2, mob);
            preparedStatement.setLong(3, System.currentTimeMillis());
            // Execute our statement
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Error while creating mob kill event: " + e.getMessage());
        }
    }

    public MobKillObject[] getPlayerMobKills(String player, int limit, int offset) {
        // Create our select statement
        String selectStatement = """
                SELECT * FROM player_mob_kills WHERE player = ? ORDER BY event_time DESC LIMIT ? OFFSET ?;""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(selectStatement);
            preparedStatement.setString(1, player);
            preparedStatement.setInt(2, limit);
            preparedStatement.setInt(3, offset);
            return runMobKillListPrepared(preparedStatement, limit);
        } catch (SQLException e) {
            LOGGER.error("Error while getting player mob kills: " + e.getMessage());
            return null;
        }
    }

    public MobKillObject[] getPlayersFromMob(String mob, int limit, int offset) {
        // Create our select statement
        String selectStatement = """
                SELECT * FROM player_mob_kills WHERE mob_type = ? ORDER BY event_time DESC LIMIT ? OFFSET ?;""";
        // Create our prepared statement
        try {
            PreparedStatement preparedStatement = this.connection.prepareStatement(selectStatement);
            preparedStatement.setString(1, mob);
            preparedStatement.setInt(2, limit);
            preparedStatement.setInt(3, offset);
            return runMobKillListPrepared(preparedStatement, limit);
        } catch (SQLException e) {
            LOGGER.error("Error while getting players from mob: " + e.getMessage());
            return null;
        }
    }

    private MobKillObject[] runMobKillListPrepared(PreparedStatement preparedStatement, int limit) throws SQLException{
        ResultSet resultSet = preparedStatement.executeQuery();
        // Create our array of BlockEventObjects
        MobKillObject[] mobEvents = new MobKillObject[limit];
        // Iterate through our result set
        int i = 0;
        while (resultSet.next()) {
            // Get our data
            String playerUUID = resultSet.getString("player");
            String mobName = resultSet.getString("mob_type");
            long timestamp = resultSet.getLong("event_time");
            // Create our BlockEventObject
            MobKillObject mobEvent = new MobKillObject(mobName, playerUUID, timestamp);
            // Add it to our array
            mobEvents[i] = mobEvent;
            i++;
        }
        // Return our array
        return mobEvents;
    }
}
