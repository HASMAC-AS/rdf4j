/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.backup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.RepositoryMaintenance;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.backup.SailBackupService;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BackupControllerTest {
	@TempDir
	Path temp;

	private static org.eclipse.rdf4j.sail.backup.BackupServiceStatus emptyStatus() {
		return new org.eclipse.rdf4j.sail.backup.BackupServiceStatus(
				java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
				java.util.Optional.empty(), java.util.OptionalLong.empty(), java.util.Optional.empty(), List.of());
	}

	@Test
	void statusIncludesServiceActivityEvenWithoutManagedJob() throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		var store = mock(LmdbStore.class);
		var service = mock(SailBackupService.class);
		when(store.getBackupService()).thenReturn(service);
		when(service.getStatus()).thenReturn(emptyStatus());
		var active = new org.eclipse.rdf4j.sail.backup.BackupOperationStatus("direct-backup", "BACKUP",
				java.util.Optional.of(org.eclipse.rdf4j.sail.backup.BackupType.FULL),
				java.time.Instant.parse("2026-11-12T09:00:00Z"), "COPYING");
		when(service.getStatus()).thenReturn(new org.eclipse.rdf4j.sail.backup.BackupServiceStatus(
				java.util.Optional.empty(), java.util.Optional.empty(), java.util.Optional.empty(),
				java.util.Optional.empty(), java.util.OptionalLong.empty(), java.util.Optional.empty(), List.of(),
				java.util.Optional.of(active)));
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var response = new MockHttpServletResponse();
			controller.handleRequest(request("GET", new SailRepository(store)), response);
			assertTrue(response.getContentAsString().contains("\"activeOperation\":{\"id\":\"direct-backup\""));
			assertTrue(response.getContentAsString().contains("\"phase\":\"COPYING\""));
			assertTrue(response.getContentAsString().contains("\"job\":null"));
			when(service.getStatus()).thenReturn(emptyStatus());
			var idle = new MockHttpServletResponse();
			controller.handleRequest(request("GET", new SailRepository(store)), idle);
			assertTrue(idle.getContentAsString().contains("\"activeOperation\":null"));
		}
	}

	@Test
	void unsupportedStoreHasNoFilesystemSideEffects() throws Exception {
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(new LocalRepositoryManager(temp.toFile()));
			var response = new MockHttpServletResponse();
			controller.handleRequest(request("GET", new SailRepository(new MemoryStore())), response);
			assertTrue(response.getContentAsString().contains("\"supported\":false"));
			assertFalse(Files.exists(temp.resolve("backups")));
		}
	}

	@Test
	void authenticatedNonAdminIsDenied() {
		try (var controller = new BackupController()) {
			var request = request("POST", null);
			request.setUserPrincipal(() -> "reader");
			assertThrows(ClientHTTPException.class,
					() -> controller.handleRequest(request, new MockHttpServletResponse()));
		}
	}

	@Test
	void rejectsTargetPaths() {
		assertThrows(ClientHTTPException.class, () -> BackupController.validateTargetId("../escape"));
		assertThrows(ClientHTTPException.class, () -> BackupController.validateTargetId("a/b"));
		assertDoesNotThrow(() -> BackupController.validateTargetId("restored-2026"));
	}

	@Test
	void jobRemainsVisibleWhileBusyAndRejectsConcurrentSubmission() throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		var store = mock(LmdbStore.class);
		var service = mock(SailBackupService.class);
		when(store.getBackupService()).thenReturn(service);
		when(service.getStatus()).thenReturn(emptyStatus());
		when(service.listBackups(any())).thenReturn(List.of());
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		when(service.createBackup(any())).thenAnswer(invocation -> {
			entered.countDown();
			if (!release.await(10, TimeUnit.SECONDS))
				throw new IllegalStateException("timeout");
			return null;
		});
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var repository = new SailRepository(store);
			var post = request("POST", repository);
			post.addParameter("action", "backup");
			var response = new MockHttpServletResponse();
			controller.handleRequest(post, response);
			assertEquals(202, response.getStatus());
			assertTrue(entered.await(5, TimeUnit.SECONDS));
			assertThrows(IllegalStateException.class, () -> RepositoryMaintenance.tryExclusive(manager, "source"));
			for (String path : List.of("/source", "/source/config")) {
				var interceptor = new RepositoryInterceptor();
				interceptor.setRepositoryManager(manager);
				var mutation = new MockHttpServletRequest(path.endsWith("config") ? "PUT" : "DELETE",
						"/repositories" + path);
				mutation.setPathInfo(path);
				var failure = assertThrows(ClientHTTPException.class,
						() -> interceptor.preHandle(mutation, new MockHttpServletResponse(), null));
				assertTrue(failure.getMessage().contains("busy"));
			}
			assertThrows(ClientHTTPException.class,
					() -> controller.handleRequest(post, new MockHttpServletResponse()));
			var status = new MockHttpServletResponse();
			controller.handleRequest(request("GET", repository), status);
			assertTrue(status.getContentAsString().contains("RUNNING"));
			release.countDown();
			awaitSuccess(controller, repository);
			try (var lease = RepositoryMaintenance.tryExclusive(manager, "source")) {
				assertNotNull(lease);
			}
		} finally {
			release.countDown();
		}
	}

	@Test
	void directServiceBackupAppearsWithoutManagedJob() throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		manager.init();
		manager.addRepositoryConfig(new RepositoryConfig("source", new SailRepositoryConfig(new LmdbStoreConfig())));
		var repository = manager.getRepository("source");
		var service = ((LmdbStore) ((SailRepository) repository).getSail()).getBackupService();
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var hook = service.getClass().getDeclaredMethod("setAfterFullBackupTransactionIdCaptured", Runnable.class);
		hook.setAccessible(true);
		hook.invoke(service, (Runnable) () -> {
			entered.countDown();
			try {
				if (!release.await(10, TimeUnit.SECONDS))
					throw new IllegalStateException("Timed out waiting for test");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
		});
		var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var future = executor.submit(() -> service.createBackup(org.eclipse.rdf4j.sail.backup.BackupRequest
					.builder(temp.resolve("external-backups"), org.eclipse.rdf4j.sail.backup.BackupType.FULL)
					.build()));
			try {
				assertTrue(entered.await(5, TimeUnit.SECONDS));
				var response = new MockHttpServletResponse();
				controller.handleRequest(request("GET", repository), response);
				assertTrue(response.getContentAsString().contains("\"phase\":\"COPYING\""));
				assertTrue(response.getContentAsString().contains("\"job\":null"));
			} finally {
				release.countDown();
				future.get(5, TimeUnit.SECONDS);
			}
			var idle = new MockHttpServletResponse();
			controller.handleRequest(request("GET", repository), idle);
			assertTrue(idle.getContentAsString().contains("\"activeOperation\":null"));
		} finally {
			release.countDown();
			executor.shutdownNow();
			manager.shutDown();
		}
	}

	@Test
	void backupAndRestoreCreateSeparateRepositoryWithoutChangingSource() throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		manager.init();
		manager.addRepositoryConfig(new RepositoryConfig("source", new SailRepositoryConfig(new LmdbStoreConfig())));
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var repository = manager.getRepository("source");
			try (var connection = repository.getConnection()) {
				connection.add(RDF.TYPE, RDF.TYPE, RDFS.CLASS);
			}
			var post = request("POST", repository);
			post.addParameter("action", "backup");
			controller.handleRequest(post, new MockHttpServletResponse());
			awaitSuccess(controller, repository);
			var service = ((LmdbStore) ((SailRepository) repository).getSail()).getBackupService();
			var backup = service.listBackups(temp.resolve("backups/source")).get(0);
			var restore = request("POST", repository);
			restore.addParameter("action", "restore");
			restore.addParameter("backupId", backup.getBackupId());
			restore.addParameter("targetRepositoryId", "restored");
			controller.handleRequest(restore, new MockHttpServletResponse());
			awaitSuccess(controller, repository);
			try (var connection = manager.getRepository("restored").getConnection()) {
				assertEquals(1, connection.size());
			}
			try (var connection = repository.getConnection()) {
				assertEquals(1, connection.size());
			}
			assertThrows(ClientHTTPException.class,
					() -> controller.handleRequest(restore, new MockHttpServletResponse()));
		} finally {
			manager.shutDown();
		}
	}

	@Test
	void restoreUsesSnapshotConfigurationAfterSourceIsRecreated() throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		manager.init();
		manager.addRepositoryConfig(new RepositoryConfig("source",
				new SailRepositoryConfig(new LmdbStoreConfig("spoc,posc").setInlineLiterals(false))));
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var original = manager.getRepository("source");
			try (var connection = original.getConnection()) {
				connection.add(RDF.TYPE, RDF.TYPE, RDFS.CLASS);
			}
			var post = request("POST", original);
			post.addParameter("action", "backup");
			controller.handleRequest(post, new MockHttpServletResponse());
			awaitSuccess(controller, original);
			var service = ((LmdbStore) ((SailRepository) original).getSail()).getBackupService();
			String backupId = service.listBackups(temp.resolve("backups/source")).get(0).getBackupId();
			assertTrue(manager.removeRepository("source"));
			manager.addRepositoryConfig(new RepositoryConfig("source",
					new SailRepositoryConfig(new LmdbStoreConfig("spoc").setInlineLiterals(true))));
			var current = manager.getRepository("source");
			var restore = request("POST", current);
			restore.addParameter("action", "restore");
			restore.addParameter("backupId", backupId);
			restore.addParameter("targetRepositoryId", "restored");
			controller.handleRequest(restore, new MockHttpServletResponse());
			awaitSuccess(controller, current);
			try (var connection = manager.getRepository("restored").getConnection()) {
				assertEquals(1, connection.size());
			}
			var restoredConfig = (LmdbStoreConfig) ((SailRepositoryConfig) manager.getRepositoryConfig("restored")
					.getRepositoryImplConfig()).getSailImplConfig();
			assertFalse(restoredConfig.getInlineLiterals());
			assertEquals("spoc,posc", restoredConfig.getTripleIndexes());
			try (var connection = current.getConnection()) {
				assertEquals(0, connection.size());
			}
		} finally {
			manager.shutDown();
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "missing", "malformed" })
	void restoreRejectsMissingOrMalformedCapturedConfiguration(String kind) throws Exception {
		var manager = new LocalRepositoryManager(temp.toFile());
		manager.init();
		manager.addRepositoryConfig(new RepositoryConfig("source", new SailRepositoryConfig(new LmdbStoreConfig())));
		var service = mock(SailBackupService.class);
		when(service.getStatus()).thenReturn(emptyStatus());
		var backup = new org.eclipse.rdf4j.sail.backup.BackupResult("captured",
				org.eclipse.rdf4j.sail.backup.BackupType.FULL,
				java.time.Instant.now(), 1, 1, java.util.OptionalLong.empty(), temp.resolve("artifact"), "checksum",
				true);
		when(service.listBackups(any())).thenReturn(List.of(backup));
		when(service.restore(any())).thenAnswer(call -> {
			var destination = ((org.eclipse.rdf4j.sail.backup.PointInTimeRestoreRequest) call.getArgument(0))
					.getRestoreDirectory();
			if (kind.equals("malformed"))
				Files.writeString(destination.resolve("config.ttl"), "not a turtle document");
			return destination;
		});
		var store = mock(LmdbStore.class);
		when(store.getBackupService()).thenReturn(service);
		var source = new SailRepository(store);
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(manager);
			var restore = request("POST", source);
			restore.addParameter("action", "restore");
			restore.addParameter("backupId", "captured");
			restore.addParameter("targetRepositoryId", "restored");
			controller.handleRequest(restore, new MockHttpServletResponse());
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			boolean failed = false;
			do {
				var response = new MockHttpServletResponse();
				controller.handleRequest(request("GET", source), response);
				String body = response.getContentAsString();
				assertFalse(body.contains("SUCCEEDED"), body);
				if (body.contains("FAILED")) {
					failed = true;
					break;
				}
				Thread.sleep(10);
			} while (System.nanoTime() < deadline);
			assertTrue(failed);
			assertFalse(manager.hasRepositoryConfig("restored"));
			assertFalse(Files.exists(manager.getRepositoryDir("restored").toPath()));
		} finally {
			manager.shutDown();
		}
	}

	@Test
	void backupDirectorySymlinkIsRejected() throws Exception {
		Path outside = Files.createDirectory(temp.resolve("outside"));
		Files.createSymbolicLink(temp.resolve("backups"), outside);
		try (var controller = new BackupController()) {
			controller.setRepositoryManager(new LocalRepositoryManager(temp.toFile()));
			var post = request("POST", new SailRepository(mock(LmdbStore.class)));
			post.addParameter("action", "backup");
			assertThrows(ClientHTTPException.class,
					() -> controller.handleRequest(post, new MockHttpServletResponse()));
			try (var children = Files.list(outside)) {
				assertEquals(0, children.count());
			}
		}
	}

	@Test
	void failedJobIsReportedWithoutExposingServerPaths() throws Exception {
		var service = mock(SailBackupService.class);
		when(service.createBackup(any())).thenThrow(new IllegalStateException("/private/server/path"));
		var store = mock(LmdbStore.class);
		when(store.getBackupService()).thenReturn(service);
		when(service.getStatus()).thenReturn(emptyStatus());
		try (var controller = new BackupController()) {
			var manager = new LocalRepositoryManager(temp.toFile());
			controller.setRepositoryManager(manager);
			var repository = new SailRepository(store);
			var post = request("POST", repository);
			post.addParameter("action", "backup");
			controller.handleRequest(post, new MockHttpServletResponse());
			long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			do {
				var response = new MockHttpServletResponse();
				controller.handleRequest(request("GET", repository), response);
				String body = response.getContentAsString();
				assertFalse(body.contains("/private/server/path"));
				if (body.contains("FAILED")) {
					try (var lease = RepositoryMaintenance.tryExclusive(manager, "source")) {
						assertNotNull(lease);
					}
					return;
				}
				Thread.sleep(10);
			} while (System.nanoTime() < deadline);
			fail("Job did not report failure");
		}
	}

	private static MockHttpServletRequest request(String method, Repository repository) {
		var request = new MockHttpServletRequest(method, "/repositories/source/backups");
		request.setAttribute("repositoryID", "source");
		request.setAttribute("repository", repository);
		return request;
	}

	private static void awaitSuccess(BackupController controller, Repository repository) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
		do {
			var response = new MockHttpServletResponse();
			controller.handleRequest(request("GET", repository), response);
			String json = response.getContentAsString();
			if (json.contains("SUCCEEDED"))
				return;
			assertFalse(json.contains("FAILED"), json);
			Thread.sleep(20);
		} while (System.nanoTime() < deadline);
		fail("Managed backup job did not complete");
	}
}
