package org.eclipse.rdf4j.serverboot;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class Rdf4jServerForwardFilter implements Filter {

	private static final String SERVER_PREFIX = "/rdf4j-server";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		if (httpRequest.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI) != null) {
			chain.doFilter(request, response);
			return;
		}

		String contextPath = httpRequest.getContextPath();
		String requestUri = httpRequest.getRequestURI();
		String prefix = contextPath + SERVER_PREFIX;
		if (requestUri.startsWith(prefix)) {
			String remainder = requestUri.substring(prefix.length());
			if (remainder.isEmpty()) {
				remainder = "/";
			}
			RequestDispatcher dispatcher = request.getRequestDispatcher(remainder);
			if (dispatcher != null) {
				dispatcher.forward(request, response);
				return;
			}
		}

		chain.doFilter(request, response);
	}
}
