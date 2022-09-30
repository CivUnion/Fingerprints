package com.github.longboyy.fingerprints.model;

import org.bukkit.Location;
import org.bukkit.World;
import vg.civcraft.mc.civmodcore.CivModCorePlugin;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.XZWCoord;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.BlockBasedChunkMeta;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableBasedBlockChunkMeta;
import vg.civcraft.mc.civmodcore.world.locations.chunkmeta.block.table.TableStorageEngine;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FPContainerDAO extends TableStorageEngine<FingerprintContainer> {

	private boolean batchMode;
	private List<List<FingerprintContainerTuple>> batches;


	public FPContainerDAO(Logger logger, ManagedDatasource db) {
		super(logger, db);

	}

	public void setBatchMode(boolean batch) {
		this.batchMode = batch;
		batches = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			batches.add(new ArrayList<>());
		}
	}

	public void cleanupBatches(){
		long currentTime = System.currentTimeMillis();
		try (Connection conn = db.getConnection();
			 PreparedStatement deleteContainer = conn.prepareStatement(
					 "DELETE FROM fingerprint_containers WHERE chunk_x = ? AND chunk_z = ? AND world_id = ? AND "
							 + "x_offset = ? AND y = ? AND z_offset = ?;");) {
			conn.setAutoCommit(false);
			for (FingerprintContainerTuple fp : batches.get(2)) {
				setDeleteDataStatement(deleteContainer, fp.container, fp.coord);
				deleteContainer.addBatch();
			}
			logger.info("Batch 2: " + (System.currentTimeMillis() - currentTime) + " ms");
			logger.info("Batch 2 Size: " + batches.get(2).size());
			batches.get(2).clear();
			deleteContainer.executeBatch();
			conn.setAutoCommit(true);
			logger.info("Batch 2 Finish: " + (System.currentTimeMillis() - currentTime) + " ms");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to delete fingerprint container from db: ", e);
		}
		try (Connection conn = db.getConnection();
			 PreparedStatement insertContainer = conn.prepareStatement(
					 "INSERT INTO fingerprint_containers(chunk_x, chunk_z, world_id, x_offset, y, z_offset) " +
							 "VALUES(?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);) {
			conn.setAutoCommit(false);
			for (FingerprintContainerTuple fp : batches.get(0)) {
				setInsertDataStatement(insertContainer, fp.container, fp.coord);
				insertContainer.addBatch();
			}
			logger.info("Batch 0: " + (System.currentTimeMillis() - currentTime) + " ms");
			logger.info("Batch 0 Size: " + batches.get(0).size());
			int[] affectedRows = insertContainer.executeBatch();
			try(ResultSet generatedKeys = insertContainer.getGeneratedKeys()){
				int i = 0;
				while(generatedKeys.next()){
					if(affectedRows[i] != 0) {
						int id = generatedKeys.getInt(1);
						batches.get(0).get(i).container.id = id;
					}
				}
			}
			batches.get(0).clear();
			conn.setAutoCommit(true);
			logger.info("Batch 0 Finish: " + (System.currentTimeMillis() - currentTime) + " ms");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to insert fingerprint container into db: ", e);
		}

		try (Connection conn = db.getConnection();
			 PreparedStatement deleteFp = conn.prepareStatement("DELETE FROM fingerprints WHERE id = ?;");) {
			conn.setAutoCommit(false);

			for (FingerprintContainerTuple fct : batches.get(1)) {
				Fingerprint fp;
				while((fp = fct.container.deletions.poll()) != null){
					deleteFp.setInt(1, fp.id);
					deleteFp.addBatch();
				}
			}
			logger.info("Batch 1 part 1: " + (System.currentTimeMillis() - currentTime) + " ms");
			logger.info("Batch 1 part 1 Size: " + batches.get(1).size());
			//updateRein.executeBatch();
			deleteFp.executeBatch();
			conn.setAutoCommit(true);
			logger.info("Batch 1 part 1 Finish: " + (System.currentTimeMillis() - currentTime) + " ms");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update fingerprint in db: ", e);
		}

		try (Connection conn = db.getConnection();
			 PreparedStatement insertFp = conn.prepareStatement("INSERT INTO fingerprints(container_id, reason, player_uuid, metadata) VALUES(?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);) {
			conn.setAutoCommit(false);
			List<Fingerprint> insertedPrints = new ArrayList<>();
			for (FingerprintContainerTuple fct : batches.get(1)) {

				logger.info("Running update batch");

				Fingerprint fp;
				while((fp = fct.container.inserts.poll()) != null){
					insertFp.setInt(1, fct.container.id);
					insertFp.setString(2, fp.getReason().name());
					insertFp.setString(3, fp.getPlayerId().toString());
					Blob blob = conn.createBlob();
					if(fp.getMetadata() != null && !fp.getMetadata().isEmpty()){
						try(ByteArrayOutputStream os = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(os);){
							oos.writeObject(fp.getMetadata());
							oos.flush();
							blob.setBytes(0, os.toByteArray());
						}catch (IOException e) {
							logger.log(Level.SEVERE, "Failed to serialize metadata for fingerprint: ", e);
						}
					}
					insertFp.setBlob(4, blob);
					insertFp.addBatch();
					insertedPrints.add(fp);
				}
			}
			logger.info("Batch 1 part 2: " + (System.currentTimeMillis() - currentTime) + " ms");
			logger.info("Batch 1 part 2 Size: " + batches.get(1).size());
			//updateRein.executeBatch();
			int[] affectedRows = insertFp.executeBatch();
			conn.setAutoCommit(true);
			try(ResultSet generatedKeys = insertFp.getGeneratedKeys()){
				int i = 0;
				while(generatedKeys.next()){
					if(affectedRows[i] != 0) {
						insertedPrints.get(i).id = generatedKeys.getInt(1);
						i++;
					}
				}
			}
			batches.get(1).clear();
			logger.info("Batch 1 part 2 Finish: " + (System.currentTimeMillis() - currentTime) + " ms");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update fingerprint in db: ", e);
		}


	}

	@Override
	public void registerMigrations() {
		db.registerMigration(0, false, "CREATE TABLE IF NOT EXISTS fingerprint_containers( " +
				"id INT unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
				"x_offset TINYINT unsigned NOT NULL, " +
				"y INT NOT NULL, " +
				"z_offset INT unsigned NOT NULL, " +
				"chunk_x INT NOT NULL, " +
				"chunk_z INT NOT NULL, " +
				"world_id SMALLINT unsigned NOT NULL " +
				");",

				"CREATE TABLE IF NOT EXISTS fingerprints( " +
				"id INT unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
				"container_id INT unsigned NOT NULL, " +
				"reason TINYTEXT NOT NULL, " +
				"created_at TIMESTAMP NOT NULL DEFAULT now(), " +
				"player_uuid VARCHAR(36) NOT NULL, " +
				"metadata BLOB, " +
				"CONSTRAINT `fk_container_id` " +
				"FOREIGN KEY(container_id) REFERENCES fingerprint_containers(id) " +
				"ON DELETE CASCADE " +
				"ON UPDATE RESTRICT" +
				");"
		);

	}

	@Override
	public void insert(FingerprintContainer data, XZWCoord coord) {
		if(batchMode){
			batches.get(0).add(new FingerprintContainerTuple(data, coord));
			return;
		}

		try(Connection conn = db.getConnection();
			PreparedStatement insertContainer = conn.prepareStatement(
					"INSERT INTO fingerprint_containers(chunk_x, chunk_z, world_id, x_offset, y, z_offset) " +
							"VALUES(?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS
			);){
			setInsertDataStatement(insertContainer, data, coord);
			int affectedRows = insertContainer.executeUpdate();
			if(affectedRows == 0){
				return;
			}
			try(ResultSet generatedKeys = insertContainer.getGeneratedKeys()){
				if(generatedKeys.next()){
					int id = generatedKeys.getInt(1);
					data.id = id;
				}
			}

		}catch(SQLException e){
			logger.severe("Failed to insert fingerprint into db: ");
		}
	}

	private static void setInsertDataStatement(PreparedStatement statement, FingerprintContainer data, XZWCoord coord) throws SQLException {
		statement.setInt(1, coord.getX());
		statement.setInt(2, coord.getZ());
		statement.setShort(3, coord.getWorldID());
		statement.setByte(4, (byte) BlockBasedChunkMeta.modulo(data.getLocation().getBlockX()));
		statement.setShort(5, (short) data.getLocation().getBlockY());
		statement.setByte(6, (byte) BlockBasedChunkMeta.modulo(data.getLocation().getBlockZ()));
	}

	@Override
	public void update(FingerprintContainer data, XZWCoord coord) {
		if(batchMode){
			batches.get(1).add(new FingerprintContainerTuple(data, coord));
			return;
		}

		try(Connection conn = db.getConnection()){
			Fingerprint fp;
			while((fp = data.deletions.poll()) != null){
				if(fp.id != -1){
					try(PreparedStatement deleteFp = conn.prepareStatement("DELETE FROM fingerprints WHERE id = ?;")){
						deleteFp.setInt(1, fp.id);
						deleteFp.execute();
					}
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update fingerprints in db: ", e);
		}

		try(Connection conn = db.getConnection()){
			Fingerprint fp;
			while((fp = data.inserts.poll()) != null){
				if(fp.id != -1){
					//INSERT INTO fingerprints(container_id, reason, player_uuid) VALUES(?, ?, ?);
					try(PreparedStatement insertFp = conn.prepareStatement("INSERT INTO fingerprints(" +
							"container_id, reason, player_uuid, metadata) VALUES(?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)){
						insertFp.setInt(1, data.id);
						insertFp.setString(2, fp.getReason().name());
						insertFp.setString(3, fp.getPlayerId().toString());
						Blob blob = conn.createBlob();
						if(fp.getMetadata() != null && !fp.getMetadata().isEmpty()){
							try(ByteArrayOutputStream os = new ByteArrayOutputStream();
								ObjectOutputStream oos = new ObjectOutputStream(os);){
								oos.writeObject(fp.getMetadata());
								oos.flush();
								blob.setBytes(0, os.toByteArray());
							}catch (IOException e) {
								logger.log(Level.SEVERE, "Failed to serialize metadata for fingerprint: ", e);
							}
						}
						insertFp.setBlob(4, blob);

						int rowsAffected = insertFp.executeUpdate();
						if(rowsAffected != 0){
							try(ResultSet generatedKeys = insertFp.getGeneratedKeys()){
								int id = generatedKeys.getInt(1);
								fp.id = id;
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to update fingerprints in db: ", e);
		}
	}

	@Override
	public void delete(FingerprintContainer data, XZWCoord coord) {
		if(batchMode){
			batches.get(2).add(new FingerprintContainerTuple(data, coord));
			return;
		}

		try(Connection conn = db.getConnection();
			PreparedStatement deleteFp = conn.prepareStatement("DELETE FROM fingerprint_containers WHERE " +
					"chunk_x = ? AND chunk_z = ? AND world_id = ? AND x_offset = ? AND y = ? AND z_offset = ?");){
			setDeleteDataStatement(deleteFp, data, coord);
			deleteFp.execute();
		}catch(SQLException e){
			logger.log(Level.SEVERE, "Failed to load fingerprint container from db: ", e);
		}
	}

	private static void setDeleteDataStatement(PreparedStatement statement, FingerprintContainer data, XZWCoord coord) throws SQLException {
		statement.setInt(1, coord.getX());
		statement.setInt(2, coord.getZ());
		statement.setShort(3, coord.getWorldID());
		statement.setByte(4, (byte) BlockBasedChunkMeta.modulo(data.getLocation().getBlockX()));
		statement.setShort(5, (short) data.getLocation().getBlockY());
		statement.setByte(6, (byte) BlockBasedChunkMeta.modulo(data.getLocation().getBlockZ()));
	}

	@Override
	public void fill(TableBasedBlockChunkMeta<FingerprintContainer> chunkData, Consumer<FingerprintContainer> insertFunction) {
		int preMultipliedX = chunkData.getChunkCoord().getX() * 16;
		int preMultipliedZ = chunkData.getChunkCoord().getZ() * 16;
		//ReinforcementTypeManager typeMan = Citadel.getInstance().getReinforcementTypeManager();
		World world = chunkData.getChunkCoord().getWorld();
		try (Connection conn = db.getConnection();
			 PreparedStatement selectContainer = conn.prepareStatement(
					 "SELECT x_offset, y, z_offset "
							 + "FROM fingerprint_containers WHERE chunk_x = ? AND chunk_z = ? AND world_id = ?;");) {
			selectContainer.setInt(1, chunkData.getChunkCoord().getX());
			selectContainer.setInt(2, chunkData.getChunkCoord().getZ());
			selectContainer.setShort(3, chunkData.getChunkCoord().getWorldID());
			try (ResultSet rs = selectContainer.executeQuery()) {
				while (rs.next()) {
					int xOffset = rs.getByte(1);
					int x = xOffset + preMultipliedX;
					int y = rs.getShort(2);
					int zOffset = rs.getByte(3);
					int z = zOffset + preMultipliedZ;

					FingerprintContainer container = this.getForLocation(x, y, z, chunkData.getChunkCoord().getWorldID(), chunkData.getPluginID());
					insertFunction.accept(container);
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load fingerprints from db: ", e);
		}
	}

	@Override
	public FingerprintContainer getForLocation(int x, int y, int z, short worldID, short pluginID) {
		int chunkX = BlockBasedChunkMeta.toChunkCoord(x);
		int chunkZ = BlockBasedChunkMeta.toChunkCoord(z);

		try(Connection conn = db.getConnection();
			PreparedStatement selectContainer = conn.prepareStatement("SELECT id FROM fingerprint_containers WHERE " +
					"chunk_x = ? AND " +
					"chunk_z = ? AND " +
					"world_id = ? AND " +
					"x_offset = ? AND " +
					"y = ? AND " +
					"z_offset = ?;")){

			selectContainer.setInt(1, chunkX);
			selectContainer.setInt(2, chunkZ);
			selectContainer.setShort(3, worldID);
			selectContainer.setByte(4, (byte) BlockBasedChunkMeta.modulo(x));
			selectContainer.setShort(5, (short) y);
			selectContainer.setByte(6, (byte) BlockBasedChunkMeta.modulo(z));

			try(ResultSet crs = selectContainer.executeQuery()){
				if(!crs.next()){
					return null;
				}
				int containerId = crs.getInt(1);

				World world = CivModCorePlugin.getInstance().getWorldIdManager().getWorldByInternalID(worldID);
				Location loc = new Location(world, x, y, z);
				FingerprintContainer container = new FingerprintContainer(containerId, loc, false);

				try(Connection conn1 = db.getConnection();
				PreparedStatement selectFp = conn1.prepareStatement("SELECT id, reason, created_at, player_uuid, metadata FROM fingerprints WHERE container_id = ? ORDER BY created_at DESC;")){
					selectFp.setInt(1, containerId);
					try(ResultSet frs = selectFp.executeQuery()){
						while(frs.next()) {
							int id = frs.getInt(1);
							String reason = frs.getString(2);
							Timestamp created_at = frs.getTimestamp(3);
							String player_uuid = frs.getString(4);
							Blob meta = frs.getBlob(5);

							Map<String, Object> metadata = new HashMap<>();

							if(meta != null){
								try(InputStream is = meta.getBinaryStream();
									ObjectInputStream ois = new ObjectInputStream(is);){
									Map<String, Object> loadedMeta = (Map<String, Object>)ois.readObject();
									metadata.putAll(loadedMeta);
								} catch (IOException | ClassNotFoundException e) {
									throw new RuntimeException(e);
								}
							}

							FingerprintReason fpReason = FingerprintReason.valueOf(reason);
							UUID playerUUID = UUID.fromString(player_uuid);

							container.fingerprints.add(new Fingerprint(id, loc, created_at.getTime(), playerUUID, fpReason, metadata));
							//container.addFingerprint();
						}
					}
				}

				return container;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to load fingerprint from db: ", e);
			return null;
		}
	}

	@Override
	public Collection<XZWCoord> getAllDataChunks() {
		List<XZWCoord> result = new ArrayList<>();
		try(Connection conn = db.getConnection();
			PreparedStatement selectChunks = conn.prepareStatement(
					"SELECT chunk_x, chunk_z, world from fingerprint_containers group by chunk_x, chunk_z, world"
			);
			ResultSet rs = selectChunks.executeQuery()){
			while(rs.next()){
				int chunkX = rs.getInt(1);
				int chunkZ = rs.getInt(2);
				short worldId = rs.getShort(3);
				result.add(new XZWCoord(chunkX, chunkZ, worldId));
			}
		}catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to select populated chunks from db: ", e);
		}
		return result;
	}

	@Override
	public boolean stayLoaded() {
		return false;
	}

	private class FingerprintContainerTuple {
		private FingerprintContainer container;
		private XZWCoord coord;
		public FingerprintContainerTuple(FingerprintContainer container, XZWCoord coord){
			this.container = container;
			this.coord = coord;
		}
	}
}
