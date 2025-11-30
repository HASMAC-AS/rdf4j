1. Global compatibility decisions

1.1 Spring Framework version vs Spring Boot BOM

	1.	Let Boot be the source of truth (recommended)
	•	Use Boot 3.5.8 and accept whatever Spring 6.x version its BOM pins.
	•	Set root <spring.version> to that same version, so non-Boot modules (tools/server-spring, etc.) are aligned.
	•	This maximizes coherence and minimizes dependency hell.

1.2 Java baseline

Root has:

<java.version>17</java.version>

Boot 3 requires ≥17; so no architectural change required here.

1.3 Jakarta migration scope

Everything relying on servlet/JSP/JSTL must move from javax.* to jakarta.*:
•	Types: HttpServletRequest, HttpServletResponse, Filter, FilterChain, ServletRequest, ServletResponse, ServletOutputStream, HttpServletResponseWrapper, MultipartConfigElement, HttpSession etc.
•	Static status codes: HttpServletResponse.SC_*.
•	JSP/JSTL APIs in the POMs.

You will have a transitional state where both javax.* and jakarta.* appear. The enforcer rule can help ensure you end in a “no javax.*” world.

⸻

2. Maven-level changes

2.1 Root pom.xml

Goals:
•	Align Spring to Boot 3.5.8’s Spring version.
•	Move servlet properties toward Jakarta.
•	Avoid logging conflicts with Boot’s BOM.

Changes:
1.	Spring Framework version property

Locate:

<spring.version>5.3.39</spring.version>

Change to:

<spring.version>6.x.y</spring.version>

Where 6.x.y is the Spring version actually used by the Boot 3.5.8 BOM (determine via mvn help:effective-pom in a Boot module).
Rationale: tools modules use ${spring.version}; this keeps them aligned with Boot.
2.	Servlet version property

Currently:

<servlet.version>4.0.0</servlet.version>  <!-- javax servlet 4 -->

Change semantics: this now represents Jakarta Servlet version, not javax.servlet-api. Example:

<servlet.version>6.0.0</servlet.version>  <!-- jakarta servlet -->

You’ll wire this into jakarta.servlet:jakarta.servlet-api instead of javax.servlet:javax.servlet-api in tools/pom.xml.
3.	Enforcer rule around javax/javaxee

You have:

<execution>
  <id>enforce-javaee-provided</id>
  ...
  <bannedDependencies>
    <excludes>
      <exclude>javax*</exclude>
    </excludes>
    <includes>
      <include>javax.servlet:javax.servlet-api:*:*:provided</include>
      <include>javax.servlet.jsp:jsp-api:*:*:provided</include>
      <include>javax.servlet:jstl:*:*:provided</include>
      <!-- ... test exceptions -->
    </includes>
  </bannedDependencies>
</rules>

Target state (post-migration):
•	You no longer want any javax.* servlet/JSP/JSTL; those should be gone.
•	You want to allow Jakarta servlet artifacts, but typically only as provided for non-Boot servlet apps.

Plan:
•	Step 1 (during migration): keep the rule as-is while replacing dependencies; it already blocks new javax.*.
•	Step 2 (final state): tighten rule to outright ban remaining javax.* (no includes), optionally add a new rule for Jakarta if you want jakarta.servlet:jakarta.servlet-api only as provided:

<bannedDependencies>
  <excludes>
    <exclude>javax*</exclude>
  </excludes>
  <!-- no includes: any javax* will now fail -->
</bannedDependencies>

<!-- optional: enforce jakarta servlet as provided/test only -->
<bannedDependencies>
  <excludes>
    <exclude>jakarta.servlet:jakarta.servlet-api</exclude>
    <exclude>jakarta.servlet.jsp:jakarta.servlet.jsp-api</exclude>
    <exclude>jakarta.servlet.jsp.jstl:*</exclude>
  </excludes>
  <includes>
    <include>jakarta.servlet:jakarta.servlet-api:*:*:provided</include>
    <include>jakarta.servlet:jakarta.servlet-api:*:*:test</include>
    <!-- similar for JSP/JSTL, if you need them direct -->
  </includes>
</bannedDependencies>

	4.	Logging properties

Currently:

<slf4j.version>1.7.36</slf4j.version>
<logback.version>1.2.13</logback.version>
<log4j.version>2.17.2</log4j.version>

Boot 3.x uses slf4j 2.x and logback 1.5.x. To avoid classpath schizophrenia:
•	Decide whether the root BOM is “logging authority” or Boot is.
•	Recommended: align root logging versions to Boot BOM:
•	Look up ch.qos.logback:logback-classic and org.slf4j:slf4j-api versions in Boot 3.5.8 BOM.
•	Set <slf4j.version> and <logback.version> to those values.
•	Keep <log4j.version> if needed for tests / Solr, but check for compatibility.

Where a module imports both Boot BOM and your own logging versions, Boot BOM tends to win for its dependencies, but unifying the properties reduces surprises, especially in non-Boot modules.

⸻

2.2 spring-components/pom.xml

Current:

<properties>
  <spring.boot.version>2.7.16</spring.boot.version>
</properties>
...
<dependencyManagement>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>${spring.boot.version}</version>
    <type>pom</type>
    <scope>import</scope>
  </dependency>
</dependencyManagement>

Planned changes:
1.	Update Boot version:

<spring.boot.version>3.5.8</spring.boot.version>

	2.	Leave the spring-boot-dependencies import where it is, but ensure it’s ordered after other BOMs whose versions it should override, or adjust order if needed.
	3.	Dependencies like:

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
  ...
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-test</artifactId>
  <scope>test</scope>
</dependency>

will automatically float to Spring 6.x / Boot 3.x via the BOM.

No new APIs at architecture level; but Boot 3 moves everything to Jakarta, which is why the servlet-side modules must be migrated.

⸻

2.3 spring-components/rdf4j-spring/pom.xml

Key bits:
•	Uses Spring Boot starters (starter-validation, starter-web, starter-test, configuration-processor).
•	Has explicit spring-tx and hibernate-validator dependencies.

Plan:
1.	No direct version tags to change; they’re inherited from the parent BOM.
2.	Verify Jakarta alignment:
•	Any code in rdf4j-spring using javax.* (especially javax.validation.*) must be changed to jakarta.* (e.g., jakarta.validation.*).
•	Check for explicit third-party libs that might still use old javax namespaces and bump to Jakarta-capable versions if needed.

At architectural level: same API surface, just package move.

⸻

2.4 spring-components/rdf4j-spring-demo/pom.xml

Already imports Boot BOM and sets Boot plugin version by ${spring.boot.version}.

Plan:
•	Update parent Boot version via spring-components parent; this POM just reuses it.
•	Verify there are no direct javax.* dependencies in sources; if any, migrate to jakarta.*.

⸻

2.5 spring-components/spring-boot-sparql-web/pom.xml

Current:
•	Boot starters: spring-boot-starter, spring-boot-starter-data-rest, spring-boot-starter-jetty.
•	One direct spring-context dependency.
•	Boot plugin version ${spring.boot.version}.

Plan:
1.	Boot version handled by parent, nothing else to add.
2.	Confirm no explicit servlet or JSP dependencies in this module; it should rely fully on the embedded Jetty from Boot 3.
3.	The only necessary code change here is in the Java sources (EvaluateResultHttpResponse, QueryResponder) where we migrate javax.servlet.* to jakarta.servlet.* (see section 3.1).

⸻

2.6 tools/pom.xml

This is the key bridging parent: non-Boot tools, but still using Spring web stack.

2.6.1 Servlet / JSP / JSTL dependencyManagement
Currently:

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>javax.servlet-api</artifactId>
      <version>${servlet.version}</version>
    </dependency>
    <dependency>
      <groupId>javax.servlet.jsp</groupId>
      <artifactId>jsp-api</artifactId>
      <version>2.2</version>
    </dependency>
    <dependency>
      <groupId>javax.servlet</groupId>
      <artifactId>jstl</artifactId>
      <version>1.2</version>
    </dependency>
    ...
  </dependencies>
</dependencyManagement>

Plan:
•	Replace all three with Jakarta artifacts:

Example structure (version numbers chosen to match Jakarta EE 10 / Boot 3):

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>jakarta.servlet</groupId>
      <artifactId>jakarta.servlet-api</artifactId>
      <version>${servlet.version}</version> <!-- now Jakarta version -->
    </dependency>
    <dependency>
      <groupId>jakarta.servlet.jsp</groupId>
      <artifactId>jakarta.servlet.jsp-api</artifactId>
      <version>${jakarta.jsp.version}</version>
    </dependency>
    <dependency>
      <groupId>jakarta.servlet.jsp.jstl</groupId>
      <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
      <version>${jakarta.jstl.version}</version>
    </dependency>
    <!-- rest unchanged -->
  </dependencies>
</dependencyManagement>

You may introduce new properties <jakarta.jsp.version>, <jakarta.jstl.version> if you want central control, or lift the versions from Boot BOM for consistency.

2.6.2 Spring dependencyManagement
Currently:

<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-core</artifactId>
  <version>${spring.version}</version>
  ...
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-aop</artifactId>
  <version>${spring.version}</version>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-webmvc</artifactId>
  <version>${spring.version}</version>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-test</artifactId>
  <version>${spring.version}</version>
  <scope>test</scope>
</dependency>

Plan:
•	Leave structure as-is, but update <spring.version> at root to the Spring version used by Boot 3.5.8 BOM (as discussed).
•	That gives tools modules a Spring 6.x baseline matching Boot.

⸻

2.7 tools/server-spring/pom.xml

Currently:

<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>javax.servlet-api</artifactId>
  <scope>provided</scope>
</dependency>
...
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-aop</artifactId>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.springframework</groupId>
  <artifactId>spring-webmvc</artifactId>
</dependency>

Plan:
1.	Switch to Jakarta servlet API:

<dependency>
  <groupId>jakarta.servlet</groupId>
  <artifactId>jakarta.servlet-api</artifactId>
  <scope>provided</scope>
</dependency>

Version comes from tools dependencyManagement, already updated.
2.	Spring dependencies stay as-is; they now resolve to Spring 6.x via the updated ${spring.version}.

No structural change to this module’s architecture, only dependency coordinates + Java imports.

⸻

2.8 tools/server-boot/pom.xml

Currently:
•	<spring.boot.version>2.7.16</spring.boot.version>
•	dependencyManagement imports Boot 2.7 BOM plus explicit spring-web/spring-webmvc with ${spring.version}.
•	Dependencies include spring-boot-starter-web, spring-web, spring-boot-starter-tomcat, tomcat-embed-jasper, javax.servlet:jstl, and RDF4J server modules.

Plan:
1.	Upgrade Boot version property:

<spring.boot.version>3.5.8</spring.boot.version>

	2.	dependencyManagement:
	•	Keep the spring-boot-dependencies import (it becomes 3.5.8).
	•	Reconsider the explicit spring-web/spring-webmvc entries:
	•	Either remove them and rely entirely on the Boot BOM
	•	Or keep them but drop explicit version tags so BOM versions are used.
Example if you keep them:

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring.boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-webmvc</artifactId>
    </dependency>
  </dependencies>
</dependencyManagement>


	3.	Replace any remaining javax.* dependencies:
	•	javax.servlet:jstl -> the Jakarta JSTL coordinate from Boot BOM (jakarta.servlet.jsp.jstl:…) or from your tools BOM. Here you can probably drop the explicit dependency and let tomcat-embed-jasper / Boot bring JSP/JSTL in, but if you keep it, make it Jakarta.
	4.	Boot plugin:

<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <version>${spring.boot.version}</version>
  ...
</plugin>

No structural update; the plugin stays the same, just a version bump.

⸻

3. Code-level Jakarta migration

Now the “change javax to jakarta” sweep, but with eyes open for upstream API changes.

3.1 spring-components/spring-boot-sparql-web

EvaluateResultHttpResponse.java
Current imports:

import javax.servlet.http.HttpServletResponse;

Plan:
•	Change to:

import jakarta.servlet.http.HttpServletResponse;

No method signatures change; HttpServletResponse API is effectively identical for your use case (setContentType, getContentType, getOutputStream).

QueryResponder.java
Current:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
...
response.sendError(HttpServletResponse.SC_BAD_REQUEST);

Plan:
•	Replace imports with:

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

	•	Static constants SC_BAD_REQUEST continue to work; only the package changes.
	•	Method signatures remain identical:

public void sparqlPostURLencoded(..., HttpServletRequest request, HttpServletResponse response) throws IOException

Same for sparqlGet.

⸻

3.2 tools/server-boot filters & application

CssPathFilter.java
Current imports:

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

Plan:
•	Change all of these to jakarta.servlet.* equivalents:

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

No behavioral change; the wrapper and streaming logic remain identical.

ErrorLoggingFilter.java
Current:

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

Plan:
•	Switch these imports to jakarta.servlet.*.

OncePerRequestFilter from Spring automatically uses Jakarta in Spring 6.

ServerPrefixForwardFilter.java
Same story:

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

Plan: change all to jakarta.servlet.*.

ServerRootDummyPageFilter.java
Same pattern:

import javax.servlet.Filter;
import javax.servlet.FilterChain;
...
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan: move to jakarta.servlet.*.

Rdf4jServerWorkbenchApplication.java
Current:

import javax.servlet.MultipartConfigElement;
...
registration.setMultipartConfig(new MultipartConfigElement(""));

Plan:
•	Switch to:

import jakarta.servlet.MultipartConfigElement;

Boot 3’s embedded Tomcat uses Jakarta servlets, so this aligns correctly.

⸻

3.3 tools/server-spring controllers / interceptors / views

Everything here is classic Spring MVC controllers & views that rely on javax.servlet and need a straight migration to jakarta.servlet.

Pattern: no functional logic changes, only imports and static constants.

3.3.1 Core utility
HttpServerUtil.java

Imports:

import javax.servlet.http.HttpServletRequest;

Plan:
•	Replace with jakarta.servlet.http.HttpServletRequest.
•	All method signatures using HttpServletRequest remain identical.

3.3.2 Interceptors
CommonValuesHandlerInterceptor.java

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan: swap to jakarta.servlet.http.*.

MessageHandlerInterceptor.java

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

Plan: swap to jakarta.servlet.http.*.

NavigationHandlerInterceptor.java

Same import set: HttpServletRequest, HttpServletResponse, HttpSession. Move to Jakarta.

No public API changes; interceptors’ method signatures stay:

public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav)

3.3.3 System controllers
SystemInfoController.java, SystemOverviewController.java

Both:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan: change to jakarta.servlet.http.*. The controllers’ handleRequest signature remains the same.

3.3.4 HTTP protocol controller
ProtocolController.java

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan: swap to Jakarta.

3.3.5 Repository controllers
AbstractRepositoryController.java

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan:
•	Move imports to jakarta.servlet.http.*.
•	The abstract method signatures for handleRequestInternal remain unchanged.

ConfigController.java / ConfigView.java
•	Change imports:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

-> jakarta.servlet.http.*.

	•	Static status codes (e.g. HttpServletResponse.SC_BAD_REQUEST) will now be from jakarta.servlet.http.HttpServletResponse.

No change needed to the business logic.

3.3.6 Repository request handlers
QueryRequestHandler.java and RepositoryRequestHandler.java

Interfaces:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

Plan:
•	Change to jakarta.servlet.http.*.
•	All implementing classes (DefaultQueryRequestHandler, DefaultRepositoryRequestHandler) must adapt their imports and override signatures accordingly.

DefaultQueryRequestHandler.java
•	Imports:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;


	•	Plan:
	•	Replace imports with jakarta.servlet.http.HttpServletRequest/Response.
	•	Update static imports:

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE;


	•	Method signatures remain compatible.

DefaultRepositoryRequestHandler.java
•	Imports javax.servlet.http.HttpServletRequest; change to jakarta.servlet.http.HttpServletRequest.

No extra API changes.

3.3.7 Repository resolver
RepositoryResolver.java, DefaultRepositoryResolver.java
•	Both import javax.servlet.http.HttpServletRequest.
•	Plan: change to jakarta.servlet.http.HttpServletRequest.

All public method signatures remain the same.

3.3.8 Views: query results, config, statements, transactions
GraphQueryResultView.java
•	Imports:

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;


	•	Plan:
	•	Replace imports with jakarta.servlet.http.*.
	•	Update static imports to jakarta.servlet.http.HttpServletResponse.*.

ExplainQueryResultView.java
•	imports HttpServletRequest, HttpServletResponse from javax.servlet.http.
•	Plan: move to jakarta.servlet.http.*.
•	response.sendError(HttpServletResponse.SC_BAD_REQUEST, ...) remains valid.

ExportStatementsView.java, TransactionExportStatementsView.java, ConfigView.java
•	All use HttpServletRequest/HttpServletResponse and static SC_OK.
•	Plan: change imports to jakarta.servlet.http.* and static import to jakarta.servlet.http.HttpServletResponse.SC_OK.

EmptySuccessView.java
•	Uses:

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


	•	Plan: swap to jakarta.servlet.http.* and update static import.

SimpleResponseView.java, SimpleCustomResponseView.java
•	Both use HttpServletRequest, HttpServletResponse, and ServletOutputStream (the latter in SimpleCustomResponseView).
•	Plan:
•	Use jakarta.servlet.http.HttpServletRequest/Response.
•	For output stream: jakarta.servlet.ServletOutputStream.

3.3.9 Filters
PathFilter.java

Imports:

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

Plan:
•	Replace with jakarta.servlet.* packages.
•	Implementation stays identical.

3.3.10 Repository endpoints: namespaces, contexts, graphs, size, transactions
NamespacesController.java
•	Imports HttpServletRequest/HttpServletResponse and throws exceptions with HttpServletResponse.SC_METHOD_NOT_ALLOWED.
•	Plan: switch to jakarta.servlet.http.*, adjust static imports (if any).

NamespaceController.java
•	Imports:

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


	•	Plan: switch to jakarta.servlet.http.* for both static and class imports.

ContextsController.java, SizeController.java, GraphController.java, TransactionStartController.java
•	All use HttpServletRequest/HttpServletResponse, and some use static status codes (e.g. SC_BAD_REQUEST, SC_UNSUPPORTED_MEDIA_TYPE, SC_CREATED).
•	Plan: consistently replace imports and static imports with jakarta.servlet.http.*.

3.3.11 System/logging controllers
LoggingOverviewController.java
•	Uses HttpServletRequest/HttpServletResponse.
•	Plan: imports -> jakarta.servlet.http.*. No logic changes.

ProxySettingsController.java
•	Uses HttpServletResponse for setProxies method signature.
•	Plan: update import to jakarta.servlet.http.HttpServletResponse.

No behavior change.

⸻

4. Hidden / other modules

You’ve already noted:
•	Workbench module, compliance Solr modules, and some tests use javax.servlet.* (e.g., HttpServerUtilTest, Solr test harness).
•	They’re not in the snippet, but they must be treated identically:
•	Update POMs to use Jakarta servlet artifacts.
•	Update Java imports to jakarta.servlet.*.
•	Bump any Solr/Jetty/Tomcat test containers to Jakarta-capable versions if they still expect javax.servlet.

Architecturally, the pattern is uniform: no API redesign, just moving to Jakarta types and aligning container dependencies.

⸻

5. Suggested implementation order

To minimize pain:
1.	Boot & Spring versions (Maven only)
•	Update spring.boot.version (in spring-components and tools/server-boot) to 3.5.8.
•	Import Boot 3 BOMs.
•	Determine Spring core version from Boot’s BOM and update root <spring.version> accordingly.
2.	Servlet/Jakarta POM alignment
•	Update tools/pom.xml dependencyManagement to Jakarta servlet/JSP/JSTL.
•	Update tools/server-spring/pom.xml and any others that directly reference javax.servlet dependencies.
•	Update tools/server-boot/pom.xml JSTL dependency and any other direct javax coordinates.
3.	Jakarta code migration (per module)
•	spring-boot-sparql-web: EvaluateResultHttpResponse, QueryResponder.
•	tools/server-boot: all filters + Rdf4jServerWorkbenchApplication.
•	tools/server-spring: sweep all controllers, views, interceptors, filters, resolvers, utilities listed above.
•	Other modules (workbench, solr, tests) with javax.* imports.
4.	Logging property harmonization
•	Align <slf4j.version>, <logback.version> to Boot BOM; remove conflicting explicit versions in child modules if present.
5.	Tighten enforcer rule
•	Once everything compiles with jakarta.*, remove javax.servlet includes from the enforce-javaee-provided rule so any reintroduced javax.* dependencies are hard failures.
•	Optionally add a rule to enforce Jakarta servlet API as provided/test only in non-Boot modules.
