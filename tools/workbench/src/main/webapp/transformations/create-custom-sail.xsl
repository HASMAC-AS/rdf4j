<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sparql="http://www.w3.org/2005/sparql-results#"
	xmlns="http://www.w3.org/1999/xhtml">

	<xsl:include href="../locale/messages.xsl" />

	<xsl:variable name="title">
		<xsl:value-of select="$repository-create.title" />
	</xsl:variable>

	<xsl:include href="template.xsl" />

	<xsl:template match="sparql:sparql">
		<form action="create" method="post" id="custom-sail-form">
			<input type="hidden" id="stack-spec" name="stackSpec" />
			<input type="hidden" id="type" name="type" value="custom-sail" />

			<h2>Repository metadata</h2>
			<table class="dataentry">
				<tbody>
					<tr>
						<th>
							<xsl:value-of select="$repository-id.label" />
						</th>
						<td>
							<input type="text" id="id" name="id" size="16" />
						</td>
						<td></td>
					</tr>
					<tr>
						<th>
							<xsl:value-of select="$repository-title.label" />
						</th>
						<td>
							<input type="text" id="title" name="title" size="48" />
						</td>
						<td></td>
					</tr>
				</tbody>
			</table>

			<h2>Stack editor</h2>
			<div class="stack-controls">
				<select id="wrapper-type">
					<option value="">Add wrapper...</option>
					<option value="SHACL">SHACL Sail</option>
					<option value="RDFS">Schema-caching RDFS inferencer</option>
					<option value="LUCENE">Lucene Sail</option>
				</select>
				<input type="button" id="add-wrapper" value="Add wrapper" />
			</div>
			<div id="wrapper-list" class="stack-list"></div>

			<div class="base-store">
				<label for="base-store">Base store</label>
				<select id="base-store">
					<option value="MEMORY">Memory Store</option>
					<option value="NATIVE">Native Store</option>
					<option value="LMDB">LMDB Store</option>
				</select>
			</div>
			<div id="base-configs">
				<div class="base-config" data-base="MEMORY">
					<h3>Memory Store configuration</h3>
					<div class="field">
						<label>Persist</label>
						<input type="checkbox" data-config-key="persist" checked="checked" />
					</div>
					<div class="field">
						<label>Sync delay (ms)</label>
						<input type="number" data-config-key="syncDelay" value="0" />
					</div>
					<div class="field">
						<label>Iteration cache sync threshold</label>
						<input type="number" data-config-key="iterationCacheSyncThreshold" value="10000" />
					</div>
					<div class="field">
						<label>Default query evaluation mode</label>
						<select data-config-key="defaultQueryEvaluationMode">
							<option value="STRICT" selected="selected">strict</option>
							<option value="STANDARD">standard</option>
						</select>
					</div>
				</div>
				<div class="base-config" data-base="NATIVE">
					<h3>Native Store configuration</h3>
					<div class="field">
						<label>Triple indexes</label>
						<input type="text" data-config-key="tripleIndexes" value="spoc,posc" />
					</div>
					<div class="field">
						<label>Force sync</label>
						<input type="checkbox" data-config-key="forceSync" />
					</div>
					<div class="field">
						<label>Value cache size</label>
						<input type="number" data-config-key="valueCacheSize" />
					</div>
					<div class="field">
						<label>Value ID cache size</label>
						<input type="number" data-config-key="valueIDCacheSize" />
					</div>
					<div class="field">
						<label>Namespace cache size</label>
						<input type="number" data-config-key="namespaceCacheSize" />
					</div>
					<div class="field">
						<label>Namespace ID cache size</label>
						<input type="number" data-config-key="namespaceIDCacheSize" />
					</div>
					<div class="field">
						<label>Iteration cache sync threshold</label>
						<input type="number" data-config-key="iterationCacheSyncThreshold" value="10000" />
					</div>
					<div class="field">
						<label>Default query evaluation mode</label>
						<select data-config-key="defaultQueryEvaluationMode">
							<option value="STRICT" selected="selected">strict</option>
							<option value="STANDARD">standard</option>
						</select>
					</div>
				</div>
				<div class="base-config" data-base="LMDB">
					<h3>LMDB Store configuration</h3>
					<div class="field">
						<label>Triple indexes</label>
						<input type="text" data-config-key="tripleIndexes" value="spoc,posc" />
					</div>
					<div class="field">
						<label>Auto-grow</label>
						<input type="checkbox" data-config-key="autoGrow" checked="checked" />
					</div>
					<div class="field">
						<label>Triple DB size (bytes)</label>
						<input type="number" data-config-key="tripleDBSize" />
					</div>
					<div class="field">
						<label>Value DB size (bytes)</label>
						<input type="number" data-config-key="valueDBSize" />
					</div>
					<div class="field">
						<label>Force sync</label>
						<input type="checkbox" data-config-key="forceSync" />
					</div>
					<div class="field">
						<label>Value cache size</label>
						<input type="number" data-config-key="valueCacheSize" />
					</div>
					<div class="field">
						<label>Value ID cache size</label>
						<input type="number" data-config-key="valueIDCacheSize" />
					</div>
					<div class="field">
						<label>Namespace cache size</label>
						<input type="number" data-config-key="namespaceCacheSize" />
					</div>
					<div class="field">
						<label>Namespace ID cache size</label>
						<input type="number" data-config-key="namespaceIDCacheSize" />
					</div>
					<div class="field">
						<label>Value eviction interval (ms)</label>
						<input type="number" data-config-key="valueEvictionInterval" value="60000" />
					</div>
					<div class="field">
						<label>Iteration cache sync threshold</label>
						<input type="number" data-config-key="iterationCacheSyncThreshold" value="10000" />
					</div>
					<div class="field">
						<label>Default query evaluation mode</label>
						<select data-config-key="defaultQueryEvaluationMode">
							<option value="STRICT" selected="selected">strict</option>
							<option value="STANDARD">standard</option>
						</select>
					</div>
				</div>
			</div>

		<h2>Warnings</h2>
		<ul id="stack-warnings" class="message-list warning"></ul>

			<h2>Errors</h2>
			<ul id="stack-errors" class="message-list"></ul>

			<h2>Config preview</h2>
			<textarea id="config-preview" rows="14" cols="100" readonly="readonly"></textarea>
			<div class="preview-actions">
				<input type="button" id="download-config" value="Download config.ttl" />
			</div>

			<div class="form-actions">
				<input type="button" value="{$cancel.label}" style="float:right" data-href="repositories"
					onclick="document.location.href=this.getAttribute('data-href')" />
				<input id="create" type="button" value="{$create.label}" onclick="checkOverwrite()" />
			</div>
		</form>

		<style type="text/css">
			.stack-controls { margin-bottom: 10px; }
			.stack-list { border: 1px solid #ccc; padding: 10px; margin-bottom: 10px; }
			.stack-item { border: 1px solid #ddd; padding: 10px; margin-bottom: 8px; background: #fafafa; }
			.stack-item h3 { margin-top: 0; }
			.stack-actions { margin-top: 8px; }
			.field { margin-bottom: 6px; }
			.field label { display: inline-block; width: 220px; }
			.base-store { margin-bottom: 10px; }
			.message-list { margin: 0 0 15px 20px; color: #a94442; }
			.message-list.warning { color: #8a6d3b; }
			#config-preview { width: 100%; }
			.preview-actions { margin-top: 8px; }
			.form-actions { margin-top: 12px; }
		</style>
		<script src="../../scripts/create.js" type="text/javascript"></script>
		<script src="../../scripts/create-custom-sail.js" type="text/javascript"></script>
	</xsl:template>

</xsl:stylesheet>
