/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.lftj;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class IndexSelectorLoggingTest {

	private Logger logger;
	private Level originalLevel;
	private ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	void setUpLogger() {
		logger = (Logger) LoggerFactory.getLogger(IndexSelector.class);
		originalLevel = logger.getLevel();
		logger.setLevel(Level.INFO);
		listAppender = new ListAppender<>();
		listAppender.start();
		logger.addAppender(listAppender);
	}

	@AfterEach
	void tearDownLogger() {
		logger.detachAppender(listAppender);
		logger.setLevel(originalLevel);
	}

	@Test
	@Disabled
	void logsAvailableAndSuggestedIndexes() {
		QuadPattern pattern = QuadPattern.of(
				QuadPatternTerm.variable("s"),
				QuadPatternTerm.constant(2L),
				QuadPatternTerm.variable("o"),
				QuadPatternTerm.constant(1L));

		List<QuadKeyOrder> candidates = List.of(
				QuadKeyOrder.of(Slot.O, Slot.S, Slot.P, Slot.C),
				QuadKeyOrder.of(Slot.P, Slot.O, Slot.S, Slot.C));

		List<String> variableOrder = List.of("s", "o", "c");

		QuadKeyOrder chosen = IndexSelector.chooseBestOrder(pattern, variableOrder, candidates);

		assertThat(chosen.fieldSequence()).isEqualTo("ospc");

		String logs = listAppender.list.stream()
				.map(ILoggingEvent::getFormattedMessage)
				.collect(Collectors.joining("\n"));

		assertThat(logs).contains("Available indexes: [ospc", "posc");
		assertThat(logs).contains("picked: ospc");
		assertThat(logs).contains("Better index to enable: socp");
	}
}
