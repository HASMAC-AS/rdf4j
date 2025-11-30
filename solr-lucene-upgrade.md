
0. Big-picture constraints & decisions

Versions & runtime
•	Target:
•	Lucene: 9.12.3
•	Solr: 9.10.0
•	Solr 9.x requires at least Java 11 and is tested with Java 17.  ￼
Your root pom already uses java.version=11, so we’re OK from a minimum‑version standpoint.
•	Solr 9.10.0 ships with Lucene 9.12.3 internally.  ￼
That’s excellent: if we pin lucene.version to 9.12.3, we’ll stay aligned with Solr’s internal Lucene jars.

Key API changes we must respect
•	Lucene
•	LatLonBoundingBox moved to the sandbox module and package org.apache.lucene.sandbox.document.LatLonBoundingBox.  ￼
•	StoredFieldVisitor gained new methods (e.g. binaryField(FieldInfo, DataInput, int)), but existing stringField(FieldInfo, byte[]) remains.  ￼
•	Highlighter, query parser, classic similarity, etc., are still present.
•	Solr
•	Solr 9 removed deprecated SolrResourceLoader.locateSolrHome(); migration notes already warned it would vanish in Solr 9.  ￼
•	CloudSolrClient.Builder is still there, but the “no‑arg + withZkHost” pattern is deprecated in favour of constructors that take zk host lists directly.
•	Core SolrJ APIs like SolrClient.query, commit, rollback, deleteById, deleteByQuery still exist.

Architectural decision:
We’ll keep the existing LuceneSail/SolrSail abstractions and only adapt the implementation details where APIs changed (sandbox imports, embedded Solr bootstrap, CloudSolrClient builder, and schema types).

⸻

1. Root pom.xml: version properties & dependency management

File: /pom.xml

1.1. Version properties

Where: <properties> block (you pasted the relevant slice).
•	Change:

<lucene.version>9.12.3</lucene.version>
<solr.version>9.10.0</solr.version>

	•	Rationale:
	•	Keeps all Lucene modules coordinated on 9.12.3, matching Solr 9.10’s Lucene.  ￼

1.2. Javadoc plugin extra dependency

Where: maven-javadoc-plugin configuration (slice 800–840).
•	Keep the <additionalDependency> on solr-core but it will now resolve to 9.10.0 via ${solr.version}.
•	No signature change needed here, but call out in the plan:
•	Verify that javadoc generation doesn’t choke on Solr 9’s module structure; if it does, you may need to add additional lucene-* deps into additionalDependencies.

1.3. Optional: dependencyManagement for Lucene

To be extra deterministic that all Lucene artifacts (including those dragged in by Solr) are on 9.12.3, you can add a dependencyManagement block (if not already present) like:

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.apache.lucene</groupId>
      <artifactId>lucene-core</artifactId>
      <version>${lucene.version}</version>
      <scope>import</scope>
      <!-- or just standard dependency if you prefer -->
    </dependency>
    <!-- optionally enumerate lucene-queries, lucene-highlighter, lucene-spatial-extras,
         lucene-sandbox, lucene-backward-codecs, etc. -->
  </dependencies>
</dependencyManagement>

Architectural effect:
Ensures the whole tree — RDF4J modules and transitive Solr Lucene deps — use Lucene 9.12.3.

⸻

2. Lucene Sail API module (core/sail/lucene-api)

2.1. core/sail/lucene-api/pom.xml
•	Dependencies already using ${lucene.version}: lucene-queryparser. That will automatically bump to 9.12.3. No extra work unless you hit API breakage at compile time.
•	No direct Lucene spatial/geo dependencies here; those are in the impl module.

2.2. AbstractLuceneSailConfig & config schema classes

Files:
•	core/sail/lucene-api/src/main/java/org/eclipse/rdf4j/sail/lucene/config/AbstractLuceneSailConfig.java
•	LuceneSailConfigSchema.java
•	LuceneSailSchema.java

No Lucene/Solr classes are referenced here, just RDF4J config/vocabulary.
So:
•	No direct code changes required for the Lucene/Solr upgrade.
•	Architecturally, this is the right place if you later want to expose new Lucene tuning parameters (e.g., new similarity or analyzer options added in Lucene 9), but that’s optional.

2.3. LuceneSail, LuceneSailConnection, LuceneSailBuffer

From the codemap:
•	LuceneSail uses a SearchIndex abstraction and instantiates LuceneIndex via createSearchIndex(Properties parameters).
•	LuceneSailConnection and LuceneSailBuffer operate on RDF4J model + Lucene index abstraction (SearchIndex, SearchDocument, etc.), not directly on Lucene’s own types.

Plan:
•	No API‑level changes needed for these classes to adapt to Lucene 9: they are insulated by the SearchIndex / Document* interfaces.
•	Just ensure any exception types or semantics thrown by LuceneIndex still match expectations (e.g., IOException vs RuntimeException), but that’s runtime validation, not structural change.

⸻

3. Lucene implementation module (core/sail/lucene)

This is where most Lucene version friction lives.

3.1. core/sail/lucene/pom.xml

Current Lucene dependencies:

<dependency> lucene-core </dependency>
<dependency> lucene-queries </dependency>
<dependency> lucene-highlighter </dependency>
<dependency> lucene-analyzers-common </dependency>
<dependency> lucene-queryparser </dependency>
<dependency> lucene-spatial-extras </dependency>
<dependency> lucene-backward-codecs </dependency>

Change 1 – bump all of them to 9.12.3 via root property (already done in step 1).

Change 2 – add sandbox module

Because LatLonBoundingBox is now in the sandbox module and package org.apache.lucene.sandbox.document, you need a new dependency:

<dependency>
  <groupId>org.apache.lucene</groupId>
  <artifactId>lucene-sandbox</artifactId>
  <version>${lucene.version}</version>
</dependency>

Rationale:
Your geo indexing code in LuceneDocument still wants LatLonBoundingBox; it just moved into this module in Lucene 9.  ￼

3.2. LuceneDocument.java

File: core/sail/lucene/src/main/java/org/eclipse/rdf4j/sail/lucene/impl/LuceneDocument.java

Current imports (geo part):

import org.apache.lucene.document.LatLonBoundingBox;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.LatLonShape;
import org.apache.lucene.geo.Line;
import org.apache.lucene.geo.Polygon;
import org.apache.lucene.geo.Rectangle;
import org.apache.lucene.geo.SimpleWKTShapeParser;

Change A – LatLonBoundingBox import
In Lucene 9, the class is in the sandbox package:

import org.apache.lucene.sandbox.document.LatLonBoundingBox;

Adjust the import accordingly and drop the old org.apache.lucene.document.LatLonBoundingBox import.

Change B – constructor signature sanity check
The code:

Rectangle box = (Rectangle) shape;
doc.add(new LatLonBoundingBox(
GEO_FIELD_PREFIX + field,
box.minLat, box.minLon, box.maxLat, box.maxLon));

Plan:
•	Verify in Lucene 9.12.3 javadoc that LatLonBoundingBox constructor still takes (String field, double minLat, double minLon, double maxLat, double maxLon). If the parameter order or type changed, update accordingly.
•	This is purely a design note; implementation will be trivial after you confirm the constructor.

Side‑effects:
•	This change is self-contained to geo indexing; it doesn’t affect text indexing or highlighting.

3.3. LuceneIndex.java

File: core/sail/lucene/src/main/java/org/eclipse/rdf4j/sail/lucene/impl/LuceneIndex.java

This class touches a lot of Lucene APIs. For the upgrade, the crucial points:
1.	Directory and analyzer creation
•	Uses FSDirectory, RAMDirectory, NIOFSDirectory (via LuceneIndexNIOFS), and StandardAnalyzer.
•	All of these still exist in Lucene 9.12.
Plan:
No structural changes; just recompile and fix any import-level differences if compiler complains (e.g., deprecated constructors).
2.	Spatial / geo
•	Uses:
•	org.apache.lucene.spatial.SpatialStrategy
•	RecursivePrefixTreeStrategy
•	SpatialPrefixTreeFactory
•	SpatialOperation
•	Coordinates these with Spatial4j’s SpatialContext and shapes.
Plan:
•	Keep the existing createSpatialStrategyMapper implementation.
•	Confirm that SpatialPrefixTreeFactory.makeSPT(...) and RecursivePrefixTreeStrategy still exist in lucene-spatial-extras 9.12.3.
•	If Lucene’s spatial extras changed configuration keys, you might need to adjust parameters passed into SpatialContextFactory.makeSpatialContext / SpatialPrefixTreeFactory.makeSPT, but the pattern is unchanged at a high level.
3.	Geo query
•	Distance query:
Uses LatLonPoint.newDistanceQuery(POINT_FIELD_PREFIX + geoField, p.getY(), p.getX(), distance) where distance is currently whatever unit the RDF4J Geo API passes (you already had this mismatch pre-upgrade).
•	Relation query:
Uses LatLonShape.newBoxQuery, newPolygonQuery, etc.
Plan:
•	Confirm that LatLonPoint.newDistanceQuery and LatLonShape.* methods have the same parameter order in 9.12.3.
•	No architectural change needed unless the signature or semantic changed – then adjust the wrapper methods to keep RDF4J’s distance semantics unchanged.
4.	Readers/writers
•	DirectoryReader.indexExists(directory)
•	IndexWriterConfig, IndexWriter.deleteUnusedFiles()
•	BooleanQuery.setMaxClauseCount(int)
Plan:
•	If deleteUnusedFiles() is deprecated or removed in 9.12, replace it with the recommended cleanup method (e.g., IndexWriter.forceMergeDeletes() or rely on background merges).
•	Keep logic around invalidateReaders(); it’s purely internal; just make it compile.
5.	StoredFieldVisitor subclass
Inner class: DocumentStoredFieldVisitor extends StoredFieldVisitor and overrides:

@Override
public Status needsField(FieldInfo fieldInfo) { ... }

@Override
public void stringField(FieldInfo fieldInfo, byte[] value) { ... }

Lucene 9.12 adds new binaryField(...) overloads but keeps stringField(FieldInfo, byte[]).  ￼
Plan:
•	No change required; your override remains valid.
•	Optionally implement binaryField(FieldInfo, byte[]) if you want to support binary stored fields in the future, but not necessary for current usage.

3.4. LuceneDocumentResult, LuceneDocumentScore, LuceneDocumentDistance, ReaderMonitor, LuceneIndexNIOFS, LuceneQuery
•	These use:
•	ScoreDoc, TopDocs, IndexSearcher, Highlighter, QueryScorer, SimpleHTMLFormatter, NIOFSDirectory, etc.
•	In Lucene 9.12, all of these still exist with compatible signatures.

Plan:
•	Update imports where necessary if any classes were moved (none are known, apart from LatLonBoundingBox).
•	Confirm Highlighter APIs still accept TokenStream and QueryScorer the way you use them.
•	LuceneQuery and SolrSearchQuery are already marked deprecated and unused; no need to touch them for the upgrade unless they stop compiling. If compilation breaks and they are truly unused, consider removing them entirely as a cleanup.

⸻

4. Solr implementation module (core/sail/solr)

4.1. core/sail/solr/pom.xml

Dependencies:
•	solr-solrj ${solr.version}
•	solr-core ${solr.version} (optional)
•	Overrides for:
•	org.apache.zookeeper:zookeeper (${zookeeper.version})
•	org.xerial.snappy:snappy-java (${snappy.version})

Change A – bump to Solr 9.10.0
•	Simply picked up by the root <solr.version> property.

Change B – Zookeeper & Snappy versions
Solr 9.10 brings its own ZK/Snappy versions and CVE patches. Overriding them may or may not still be necessary.

Architectural plan:
•	Check Solr 9.10 POM’s transitive versions of ZooKeeper and Snappy.
•	Either:
•	align zookeeper.version and snappy.version with Solr’s own versions, or
•	drop these overrides to let Solr define them, unless you have strict CVE requirements.

Change C – Restlet repo
•	The extra maven-restlet repo was probably used to resolve old transitive dependencies. Solr 9.10 may no longer require it.
•	Plan: see if build still succeeds after removing the repository. If yes, kill it. If not, keep as is.

4.2. SolrClientFactory and client factories

4.2.1. SolrClientFactory interface
No change; it’s your own interface:

public interface SolrClientFactory {
SolrClient create(String spec);
}

4.2.2. HTTP client factory
File: core/sail/solr/src/main/java/org/eclipse/rdf4j/sail/solr/client/http/Factory.java

public class Factory implements SolrClientFactory {

    @Override
    public SolrClient create(String spec) {
        return new HttpSolrClient.Builder(spec).build();
    }
}

	•	HttpSolrClient.Builder(String baseUrl) still exists in Solr 9.
	•	Plan: no change required, aside from potentially dealing with deprecation warnings.

4.2.3. Cloud client factory
File: core/sail/solr/src/main/java/org/eclipse/rdf4j/sail/solr/client/cloud/Factory.java

Current code:

public class Factory implements SolrClientFactory {

    @Override
    public SolrClient create(String spec) {
        List<String> zkHosts = Lists.newArrayList(spec.substring("cloud:".length()));
        return new CloudSolrClient.Builder().withZkHost(zkHosts).build();
    }
}

Problems in Solr 9:
•	Zero‑arg CloudSolrClient.Builder() + withZkHost usage is deprecated and may break in future. Newer guidance is to use the constructor that accepts zk host list directly.

Planned change:
•	Replace Guava’s Lists.newArrayList and withZkHost call with the new builder pattern.

Example design:

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Factory implements SolrClientFactory {

    @Override
    public SolrClient create(String spec) {
        String zkHostSpec = spec.substring("cloud:".length());
        // Preserve previous semantics: treat the whole substring as a single zk host spec
        List<String> zkHosts = Collections.singletonList(zkHostSpec);

        CloudSolrClient.Builder builder = new CloudSolrClient.Builder(zkHosts, Optional.empty());
        return builder.build();
    }
}

	•	If you decide to interpret spec as a comma-separated host list later, you can change Collections.singletonList to Arrays.asList(zkHostSpec.split(",")) without touching the rest of the integration.

4.2.4. Embedded client factory
File: core/sail/solr/src/main/java/org/eclipse/rdf4j/sail/solr/client/embedded/Factory.java

Current implementation:

Path solrHome = SolrResourceLoader.locateSolrHome();
Path configFile = solrHome.resolve(SolrXmlConfig.SOLR_XML_FILE);
return new EmbeddedSolrServer(CoreContainer.createAndLoad(solrHome, configFile), "embedded");

Issue:
•	SolrResourceLoader.locateSolrHome() was deprecated and announced to be removed for Solr 9.0.  ￼

Design change:
•	Stop using SolrResourceLoader.locateSolrHome().
•	Instead, rely on System.getProperty("solr.solr.home"), which compliance tests already set (SolrSailTest, SolrSailGeoSPARQLTest, etc.).

Example structure:

import java.nio.file.Path;
import java.nio.file.Paths;

public class Factory implements SolrClientFactory {

    @Override
    public SolrClient create(String spec) {
        String solrHomeProp = System.getProperty("solr.solr.home");
        if (solrHomeProp == null || solrHomeProp.isEmpty()) {
            throw new IllegalStateException(
                "System property 'solr.solr.home' must be set for embedded Solr usage");
        }

        Path solrHome = Paths.get(solrHomeProp);
        Path configFile = solrHome.resolve(SolrXmlConfig.SOLR_XML_FILE);
        CoreContainer container = CoreContainer.createAndLoad(solrHome, configFile);
        return new EmbeddedSolrServer(container, "embedded");
    }
}

Notes:
•	CoreContainer.createAndLoad(Path, Path) is still available in Solr 9.
•	This approach keeps the embedded Solr bootstrap logic simple and explicit.
•	Outside of tests, any user relying on embedded mode must make sure solr.solr.home is set (which was implicitly required before).

4.3. SolrIndex.java and helpers

Files:
•	core/sail/solr/src/main/java/org/eclipse/rdf4j/sail/solr/SolrIndex.java
•	SolrSearchDocument
•	SolrDocumentResult / SolrDocumentScore / SolrDocumentDistance
•	SolrBulkUpdater
•	SolrUtil
•	SolrSearchQuery (deprecated)

These pieces use the SolrJ public APIs:
•	SolrClient.add, deleteById, deleteByQuery, query, commit, rollback.
•	SolrQuery, QueryResponse, SolrDocument, SolrDocumentList.
•	Spatial params: SpatialParams.FIELD, SpatialParams.POINT, SpatialParams.DISTANCE.
•	Highlighting: q.setHighlight(true), addHighlightField, getHighlighting().

For Solr 9.10 these are still in place.

Design plan:
•	No structural changes required.
•	At the code level, things to watch for during implementation:
•	If the compiler flags any method as removed or moved, adapt to the new location. For example, if q.addField("score") is replaced by a different scoring param, adjust accordingly.
•	If any spatial function names change, adapt the query builders:
•	"{!geofilt score=recipDistance}" and geodist() are stable, but if not, refactor to the new geospatial API.

⸻

5. Solr compliance module & embedded configs (compliance/solr)

5.1. compliance/solr/pom.xml
•	Bump rdf4j-sail-solr test dependency via ${project.version} (already done).
•	solr-core test dependency now resolves to 9.10.0.
•	The maven-restlet repository might not be needed anymore – once the core upgrade is done, try to remove it. If tests still resolve all dependencies, it can be deleted.

5.2. solrconfig.xml

File: compliance/solr/solr/cores/embedded/conf/solrconfig.xml

Current:

<config>
    <luceneMatchVersion>8.9.0</luceneMatchVersion>
    <dataDir>target/test-data</dataDir>
    <requestHandler name="/select" class="solr.SearchHandler"/>
    <requestHandler name="/get" class="solr.RealTimeGetHandler"/>
    <directoryFactory class="org.apache.solr.core.RAMDirectoryFactory"/>
    <indexConfig>
        <lockType>single</lockType>
    </indexConfig>
</config>

Design changes:
1.	luceneMatchVersion
•	Update to 9.12.3, matching both Lucene version and the version Solr 9.10 bundles.  ￼
2.	Other elements (/select, /get, RAMDirectoryFactory) are still supported in Solr 9.

5.3. managed-schema

File: compliance/solr/solr/cores/embedded/conf/managed-schema

Current issues:
•	Uses removed Trie*Field classes:

<fieldType name="double" class="solr.TrieDoubleField" ... />
<fieldType name="float"  class="solr.TrieFloatField" ... />
<fieldType name="int"    class="solr.TrieIntField" ... />
<fieldType name="long"   class="solr.TrieLongField" ... />

The Trie*Field family has been deprecated for a long time and is removed in Solr 9. They’re also not really needed for your RDF4J tests except for _version_.

Design options:
1.	Easiest and safest:
Copy Solr 9.10’s managed-schema from the _default configset and merge your custom fields:
•	Keep:
•	<uniqueKey>id</uniqueKey>
•	id, uri, context, text fields.
•	http://www.opengis.net/ont/geosparql#asWKT geo field.
•	<dynamicField name="*" type="text"/> or the appropriate type from the default schema.
•	Keep _version_ definition as in the default schema (which will use the appropriate point field type, e.g. plong).
2.	Minimal patch (if you want to edit by hand):
•	Replace fieldType definitions using Trie*Field with modern equivalents, e.g.:

<fieldType name="pdouble" class="solr.DoublePointField" docValues="true" stored="true"/>
<fieldType name="pfloat"  class="solr.FloatPointField"  docValues="true" stored="true"/>
<fieldType name="pint"    class="solr.IntPointField"    docValues="true" stored="true"/>
<fieldType name="plong"   class="solr.LongPointField"   docValues="true" stored="true"/>

<field name="_version_" type="plong" indexed="true" stored="true"/>


	•	Or simply refer to built‑in types defined in Solr’s default config set (e.g. type="plong" without redefining).

Architectural recommendation:
Use option 1 (copy from the Solr 9.10 default configset) — it aligns you with Solr’s standard schema and reduces the risk of subtle numeric or versioning issues.

5.4. solr.xml

File: compliance/solr/solr/solr.xml

Currently just:

<?xml version="1.0" encoding="UTF-8"?>
<solr/>

This is still valid for Solr 9; no changes required.

⸻

6. Lucene compliance module (compliance/lucene)

File: compliance/lucene/pom.xml
•	The tests depend on:
•	rdf4j-sail-lucene
•	rdf4j-lucene-testsuite
•	rdf4j-queryalgebra-geosparql
•	All Lucene versioning is controlled by root lucene.version.

Architectural plan:
•	No new dependencies; compile and adjust only if lucene API changes (mostly covered by the core/sail/lucene changes) break tests.

Tests:
•	LuceneSailTest, LuceneSailIndexedPropertiesTest — use RAMDir and standard Lucene features; they should still work after the Lucene 9 upgrade once the implementation compiles.
•	LuceneGeoSPARQLTest is already disabled for lack of JTS; no impact.

⸻

7. Risk points & follow‑up checks

To make sure this upgrade doesn’t leak dragons into production, here are the architectural “watch this” items:
1.	LatLonBoundingBox + sandbox
•	Must import from the sandbox package and add lucene-sandbox dependency.
•	Verify constructor signature and semantics.
2.	StoredFieldVisitor API
•	Our override relies on stringField(FieldInfo, byte[]).
•	Lucene 9.12 keeps it; new binary methods are optional.  ￼
3.	Embedded Solr bootstrap
•	SolrResourceLoader.locateSolrHome() no longer exists; replacing it with System.getProperty("solr.solr.home") is mandatory.
•	Compliance tests already set this property, so behaviour should remain equivalent.
4.	CloudSolrClient builder
•	Switch from deprecated new CloudSolrClient.Builder().withZkHost(...) to new CloudSolrClient.Builder(List<String> zkHosts, Optional<String> zkChroot).
•	This future‑proofs you somewhat against the Solr 10+ changes, which are moving more towards URL‑based clients.  ￼
5.	Solr schema numeric field types
•	Removing Trie*Field usage is non‑optional for Solr 9.
•	Reusing the 9.10 default schema pattern is the safest approach.

⸻

That’s the implementation map: where to touch, what to change, why those changes are necessary, and what might explode if they’re done wrong.

The actual coding work is mostly surgical: a handful of imports, one embedded factory rewrite, one cloud factory tweak, and regenerating the test core schema from a modern Solr configset. The real fun comes when you re-run the compliance suites and see which bits of geo / highlight behaviour changed subtly, but structurally the design should hold.