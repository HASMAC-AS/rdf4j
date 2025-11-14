/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddServlet extends TransformationServlet {

	private static final String URL = "url";
	private static final String ISOLATION_LEVEL = "isolation-level";

	private final Logger logger = LoggerFactory.getLogger(AddServlet.class);

	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws IOException, RepositoryException, QueryResultHandlerException {
		try {
			IsolationLevel isolationLevel = getIsolationLevel(req);
			String baseURI = req.getParameter("baseURI");
			String contentType = req.getParameter("Content-Type");
			if (req.isParameterPresent(CONTEXT)) {
				Resource context = req.getResource(CONTEXT);
				if (req.isParameterPresent(URL)) {
					add(req.getUrl(URL), baseURI, contentType, isolationLevel, context);
				} else {
					add(req.getContentParameter(), baseURI, contentType, req.getContentFileName(),
							isolationLevel, context);
				}
			} else {
				if (req.isParameterPresent(URL)) {
					add(req.getUrl(URL), baseURI, contentType, isolationLevel);
				} else {
					add(req.getContentParameter(), baseURI, contentType, req.getContentFileName(),
							isolationLevel);
				}
			}
			resp.sendRedirect("summary");
		} catch (BadRequestException exc) {
			logger.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			String baseURI = req.getParameter("baseURI");
			String context = req.getParameter(CONTEXT);
			String contentType = req.getParameter("Content-Type");
			String isolationLevelParam = req.getParameter(ISOLATION_LEVEL);
			renderForm(builder, xslPath, exc.getMessage(), baseURI, context, contentType,
					isolationLevelParam, toKnownIsolationLevelName(isolationLevelParam));
		}
	}

	private void add(InputStream stream, String baseURI, String contentType, String contentFileName,
			IsolationLevel isolationLevel, Resource... context)
			throws BadRequestException, RepositoryException, IOException {
		if (contentType == null) {
			throw new BadRequestException("No Content-Type provided");
		}

		RDFFormat format;
		if ("autodetect".equals(contentType)) {
			format = Rio.getParserFormatForFileName(contentFileName)
					.orElseThrow(() -> new BadRequestException(
							"Could not automatically determine Content-Type for content: " + contentFileName));
		} else {
			format = Rio.getParserFormatForMIMEType(contentType)
					.orElseThrow(() -> new BadRequestException("Unknown Content-Type: " + contentType));
		}

		try (RepositoryConnection con = repository.getConnection()) {
			setIsolationLevel(con, isolationLevel);
			con.add(stream, baseURI, format, context);
		} catch (RDFParseException | IllegalArgumentException exc) {
			throw new BadRequestException(exc.getMessage(), exc);
		}
	}

	private void add(URL url, String baseURI, String contentType, IsolationLevel isolationLevel,
			Resource... context) throws BadRequestException, RepositoryException, IOException {
		if (contentType == null) {
			throw new BadRequestException("No Content-Type provided");
		}

		RDFFormat format;
		if ("autodetect".equals(contentType)) {
			format = Rio.getParserFormatForFileName(url.getFile())
					.orElseThrow(() -> new BadRequestException(
							"Could not automatically determine Content-Type for content: " + url.getFile()));
		} else {
			format = Rio.getParserFormatForMIMEType(contentType)
					.orElseThrow(() -> new BadRequestException("Unknown Content-Type: " + contentType));
		}

		try {
			try (RepositoryConnection con = repository.getConnection()) {
				setIsolationLevel(con, isolationLevel);
				con.add(url, baseURI, format, context);
			}
		} catch (RDFParseException | MalformedURLException | IllegalArgumentException exc) {
			throw new BadRequestException(exc.getMessage(), exc);
		}
	}

	private IsolationLevel getIsolationLevel(WorkbenchRequest req) throws BadRequestException {
		String isolationLevelName = req.getParameter(ISOLATION_LEVEL);
		if (isolationLevelName == null || isolationLevelName.isBlank()) {
			return null;
		}

		try {
			return IsolationLevels.valueOf(isolationLevelName.toUpperCase(Locale.ENGLISH));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Unknown isolation level: " + isolationLevelName, e);
		}
	}

	private void setIsolationLevel(RepositoryConnection connection, IsolationLevel isolationLevel)
			throws BadRequestException {
		if (isolationLevel == null) {
			return;
		}

		try {
			connection.setIsolationLevel(isolationLevel);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(
					"Unsupported isolation level for this repository: " + isolationLevel, e);
		}
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws Exception {
		TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
		String baseURI = req.getParameter("baseURI");
		String context = req.getParameter(CONTEXT);
		String contentType = req.getParameter("Content-Type");
		String isolationLevelParam = req.getParameter(ISOLATION_LEVEL);
		renderForm(builder, xslPath, null, baseURI, context, contentType, isolationLevelParam,
				toKnownIsolationLevelName(isolationLevelParam));
	}

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		renderForm(builder, xslPath, null, null, null, null, null, null);
	}

	private void renderForm(TupleResultBuilder builder, String xslPath, String errorMessage, String baseURI,
			String context, String contentType, String isolationLevelParam, String selectedIsolationLevel)
			throws RepositoryException, QueryResultHandlerException {
		builder.transform(xslPath, "add.xsl");
		if (errorMessage != null) {
			builder.start("error-message", "baseURI", CONTEXT, "Content-Type", ISOLATION_LEVEL);
		} else {
			builder.start("baseURI", CONTEXT, "Content-Type", ISOLATION_LEVEL);
		}
		builder.link(List.of(INFO));
		if (errorMessage != null) {
			builder.result(errorMessage, baseURI, context, contentType, isolationLevelParam);
		} else if (baseURI != null || context != null || contentType != null || isolationLevelParam != null) {
			builder.result(baseURI, context, contentType, isolationLevelParam);
		}
		addIsolationLevelBindings(builder, selectedIsolationLevel);
		builder.end();
	}

	private void addIsolationLevelBindings(TupleResultBuilder builder, String selectedIsolationLevel)
			throws QueryResultHandlerException {
		for (IsolationLevels level : IsolationLevels.values()) {
			builder.namedResult("available-isolation-level",
					level.name() + " " + isolationLevelLabel(level));
		}
		if (selectedIsolationLevel != null) {
			builder.namedResult("selected-isolation-level", selectedIsolationLevel);
		}
	}

	private String toKnownIsolationLevelName(String isolationLevelParam) {
		if (isolationLevelParam == null) {
			return null;
		}
		String trimmed = isolationLevelParam.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		String normalized = trimmed.toUpperCase(Locale.ENGLISH);
		try {
			IsolationLevels.valueOf(normalized);
			return normalized;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private String isolationLevelLabel(IsolationLevels level) {
		String name = level.name().toLowerCase(Locale.ENGLISH);
		StringBuilder label = new StringBuilder(name.length());
		boolean capitalizeNext = true;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch == '_') {
				label.append(' ');
				capitalizeNext = true;
			} else if (capitalizeNext) {
				label.append(Character.toTitleCase(ch));
				capitalizeNext = false;
			} else {
				label.append(ch);
			}
		}
		return label.toString();
	}

}
