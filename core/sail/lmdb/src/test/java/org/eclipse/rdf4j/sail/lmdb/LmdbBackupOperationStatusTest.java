/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.backup.BackupCompression;
import org.eclipse.rdf4j.sail.backup.BackupOperationStatus;
import org.eclipse.rdf4j.sail.backup.BackupRequest;
import org.eclipse.rdf4j.sail.backup.BackupSchedule;
import org.eclipse.rdf4j.sail.backup.BackupServiceStatus;
import org.eclipse.rdf4j.sail.backup.BackupType;
import org.eclipse.rdf4j.sail.backup.PointInTimeRestoreRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(20)
class LmdbBackupOperationStatusTest {
	@TempDir
	Path temp;

	@Test
	void serviceStatusExposesActiveOperation() throws Exception {
		assertNotNull(BackupServiceStatus.class.getMethod("getActiveOperation"));
		var status = new BackupServiceStatus(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
				OptionalLong.empty(), Optional.empty(), List.of());
		assertTrue(status.getActiveOperation().isEmpty(), "Old constructor must remain compatible");
	}

	@Test
	void directBackupStatusIsNonBlockingWhileOperationLockIsHeld() throws Exception {
		var store = new LmdbStore(temp.resolve("store").toFile());
		var repo = new SailRepository(store);
		repo.init();
		var service = (LmdbBackupServiceImpl) store.getBackupService();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		service.setAfterFullBackupTransactionIdCaptured(() -> {
			entered.countDown();
			await(release);
		});
		try (var executor = Executors.newFixedThreadPool(2)) {
			var future = executor.submit(() -> service.createBackup(full()));
			try {
				assertTrue(entered.await(5, TimeUnit.SECONDS));
				var first = executor.submit(service::getStatus)
						.get(1, TimeUnit.SECONDS)
						.getActiveOperation()
						.orElseThrow();
				assertEquals("BACKUP", first.getOperation());
				assertEquals("COPYING", first.getPhase());
				assertEquals(Optional.of(BackupType.FULL), first.getBackupType());
				assertEquals(first.getOperationId(),
						service.getStatus().getActiveOperation().orElseThrow().getOperationId());
				release.countDown();
				future.get(5, TimeUnit.SECONDS);
				assertEquals("COPYING", first.getPhase(), "Snapshots must not mutate when the operation advances");
				assertTrue(service.getStatus().getActiveOperation().isEmpty());
			} finally {
				release.countDown();
			}
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void scheduledBackupsExposeTheSameRunningStatus() throws Exception {
		var store = new LmdbStore(temp.resolve("store").toFile());
		var repo = new SailRepository(store);
		repo.init();
		var service = (LmdbBackupServiceImpl) store.getBackupService();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		service.setAfterFullBackupTransactionIdCaptured(() -> {
			entered.countDown();
			await(release);
		});
		var scheduleId = service.schedule(new BackupSchedule(Duration.ofMillis(30), full()));
		try {
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			var active = service.getStatus().getActiveOperation().orElseThrow();
			assertEquals("BACKUP", active.getOperation());
			assertEquals("COPYING", active.getPhase());
			assertTrue(service.cancelSchedule(scheduleId));
			release.countDown();
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (service.getStatus().getActiveOperation().isPresent() && System.nanoTime() < deadline)
				Thread.sleep(10);
			assertTrue(service.getStatus().getActiveOperation().isEmpty());
			assertTrue(service.getStatus().getLastSuccessfulBackup().isPresent());
		} finally {
			service.cancelSchedule(scheduleId);
			release.countDown();
			repo.shutDown();
		}
	}

	@Test
	void phasesAreAccurateAndRestoreKeepsOneOperationIdentity() {
		var store = new LmdbStore(temp.resolve("store").toFile());
		var repo = new SailRepository(store);
		repo.init();
		var service = (LmdbBackupServiceImpl) store.getBackupService();
		List<BackupOperationStatus> observations = new ArrayList<>();
		service.setOperationPhaseListener(observations::add);
		try {
			var backup = service.createBackup(BackupRequest.builder(temp.resolve("backups"), BackupType.FULL)
					.compression(BackupCompression.ZIP)
					.build());
			assertEquals(List.of("PREPARING", "COPYING", "COMPRESSING", "CHECKSUMMING", "PUBLISHING"),
					observations.stream().map(BackupOperationStatus::getPhase).toList());
			observations.clear();
			service.restore(new PointInTimeRestoreRequest(temp.resolve("backups"), temp.resolve("restored"),
					backup.getEndTransactionId(), true));
			assertEquals(List.of("PREPARING", "VERIFYING", "RESTORING", "REPLAYING", "PUBLISHING"),
					observations.stream().map(BackupOperationStatus::getPhase).toList());
			assertTrue(observations.stream().allMatch(s -> s.getOperation().equals("RESTORE")));
			assertEquals(1, observations.stream().map(BackupOperationStatus::getOperationId).distinct().count());
			assertEquals(1, observations.stream().map(BackupOperationStatus::getStartedAt).distinct().count());
			assertTrue(service.getStatus().getActiveOperation().isEmpty());
		} finally {
			repo.shutDown();
		}
	}

	@Test
	void failuresNeverLeaveAStaleActiveOperation() {
		var store = new LmdbStore(temp.resolve("store").toFile());
		var repo = new SailRepository(store);
		repo.init();
		var service = (LmdbBackupServiceImpl) store.getBackupService();
		try {
			service.setAfterFullBackupTransactionIdCaptured(() -> {
				throw new IllegalStateException("injected failure");
			});
			assertThrows(IllegalStateException.class, () -> service.createBackup(full()));
			assertTrue(service.getStatus().getActiveOperation().isEmpty());
			assertThrows(RuntimeException.class, () -> service.restore(new PointInTimeRestoreRequest(
					temp.resolve("missing"), temp.resolve("restored"), 0, true)));
			assertTrue(service.getStatus().getActiveOperation().isEmpty());
		} finally {
			repo.shutDown();
		}
	}

	private BackupRequest full() {
		return BackupRequest.builder(temp.resolve("backups"), BackupType.FULL).build();
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(10, TimeUnit.SECONDS))
				throw new AssertionError("Timeout waiting for test release");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}
}
