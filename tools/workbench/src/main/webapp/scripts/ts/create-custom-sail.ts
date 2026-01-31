/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.

namespace workbench.customSail {
	interface RepoSpec {
		id: string;
		title: string;
	}

	interface SailLayerSpec {
		type: string;
		config: { [key: string]: any };
	}

	interface SailStackSpec {
		repo: RepoSpec;
		stack: SailLayerSpec[];
	}

	const wrapperList = $("#wrapper-list");
	const baseStoreSelect = $("#base-store");
	const previewField = $("#config-preview");
	const errorsList = $("#stack-errors");
	const warningsList = $("#stack-warnings");
	const stackSpecField = $("#stack-spec");

	function baseConfigContainer(): JQuery {
		const baseType = baseStoreSelect.val() as string;
		return $(".base-config[data-base='" + baseType + "']");
	}

	function readConfigFields(container: JQuery): { [key: string]: any } {
		const config: { [key: string]: any } = {};
		container.find("[data-config-key]").each(function () {
			const element = $(this);
			const key = element.data("config-key") as string;
			if (!key) {
				return;
			}
			if (element.is(":checkbox")) {
				config[key] = element.is(":checked");
				return;
			}
			const value = element.val() as string;
			if (value === null || value === undefined || value === "") {
				return;
			}
			if (element.is("input[type='number']")) {
				config[key] = Number(value);
				return;
			}
			config[key] = value;
		});
		return config;
	}

	function buildSpec(): SailStackSpec {
		const repo: RepoSpec = {
			id: $("#id").val() as string,
			title: $("#title").val() as string
		};

		const stack: SailLayerSpec[] = [];
		wrapperList.children(".stack-item").each(function () {
			const item = $(this);
			const type = item.data("layer-type") as string;
			stack.push({
				type: type,
				config: readConfigFields(item)
			});
		});

		const baseType = baseStoreSelect.val() as string;
		stack.push({
			type: baseType,
			config: readConfigFields(baseConfigContainer())
		});

		return { repo, stack };
	}

	function updateSpecField(): void {
		stackSpecField.val(JSON.stringify(buildSpec()));
	}

	function renderMessages(list: JQuery, items: string[]): void {
		list.empty();
		items.forEach((message) => {
			list.append("<li>" + message + "</li>");
		});
	}

	let previewTimeout: number | undefined;

	function schedulePreview(): void {
		if (previewTimeout) {
			clearTimeout(previewTimeout);
		}
		previewTimeout = window.setTimeout(updatePreview, 300);
	}

	function updatePreview(): void {
		const spec = buildSpec();
		$.ajax({
			url: "custom-sail-preview",
			type: "POST",
			contentType: "application/json",
			data: JSON.stringify(spec),
			success: function (data) {
				previewField.val(data.turtle || "");
				renderMessages(warningsList, data.warnings || []);
				renderMessages(errorsList, data.errors || []);
			},
			error: function () {
				errorsList.empty();
				warningsList.empty();
				previewField.val("");
			}
		});
	}

	function createWrapperContent(type: string): string {
		if (type === "SHACL") {
			return "" +
				"<div class='field'><label>Validation enabled</label><input type='checkbox' data-config-key='validationEnabled' checked='checked' /></div>" +
				"<div class='field'><label>Parallel validation</label><input type='checkbox' data-config-key='parallelValidation' checked='checked' /></div>" +
				"<div class='field'><label>RDFS subclass reasoning</label><input type='checkbox' data-config-key='rdfsSubClassReasoning' checked='checked' /></div>" +
				"<div class='field'><label>Results limit total</label><input type='number' data-config-key='validationResultsLimitTotal' value='1000000' /></div>" +
				"<div class='field'><label>Results per constraint</label><input type='number' data-config-key='validationResultsLimitPerConstraint' value='1000' /></div>";
		}
		if (type === "LUCENE") {
			return "" +
				"<div class='field'><label>Index directory</label><input type='text' data-config-key='indexDir' value='lucene' /></div>";
		}
		return "<div class='field'>No extra configuration required.</div>";
	}

	function addWrapper(type: string): void {
		if (!type) {
			return;
		}
		if (wrapperList.find(".stack-item[data-layer-type='" + type + "']").length > 0) {
			renderMessages(errorsList, ["Duplicate wrapper detected: " + type]);
			return;
		}
		const displayName = type === "RDFS" ? "Schema-caching RDFS inferencer" :
			type === "SHACL" ? "SHACL Sail" : "Lucene Sail";
		const item = $("<div class='stack-item' data-layer-type='" + type + "'></div>");
		item.append("<h3>" + displayName + "</h3>");
		item.append(createWrapperContent(type));
		const actions = $("<div class='stack-actions'></div>");
		actions.append("<input type='button' class='move-up' value='Up' />");
		actions.append("<input type='button' class='move-down' value='Down' />");
		actions.append("<input type='button' class='remove-wrapper' value='Remove' />");
		item.append(actions);
		wrapperList.append(item);
		schedulePreview();
	}

	function moveWrapper(element: JQuery, direction: number): void {
		if (direction < 0) {
			const previous = element.prev(".stack-item");
			if (previous.length) {
				element.insertBefore(previous);
			}
		} else {
			const next = element.next(".stack-item");
			if (next.length) {
				element.insertAfter(next);
			}
		}
		schedulePreview();
	}

	function updateBaseVisibility(): void {
		$(".base-config").hide();
		baseConfigContainer().show();
		schedulePreview();
	}

	function downloadConfig(): void {
		const content = previewField.val() as string;
		if (!content) {
			return;
		}
		const blob = new Blob([content], { type: "text/turtle;charset=utf-8" });
		const url = window.URL.createObjectURL(blob);
		const link = document.createElement("a");
		link.href = url;
		link.download = "config.ttl";
		link.click();
		window.URL.revokeObjectURL(url);
	}

	workbench.addLoad(function () {
		updateBaseVisibility();
		$("#add-wrapper").on("click", function () {
			const type = $("#wrapper-type").val() as string;
			addWrapper(type);
		});

		wrapperList.on("click", ".move-up", function () {
			moveWrapper($(this).closest(".stack-item"), -1);
		});
		wrapperList.on("click", ".move-down", function () {
			moveWrapper($(this).closest(".stack-item"), 1);
		});
		wrapperList.on("click", ".remove-wrapper", function () {
			$(this).closest(".stack-item").remove();
			schedulePreview();
		});

		$("#base-store").on("change", updateBaseVisibility);
		$("#custom-sail-form").on("change", "input, select", schedulePreview);
		$("#custom-sail-form").on("submit", function () {
			updateSpecField();
		});
		$("#download-config").on("click", downloadConfig);
		schedulePreview();
	});
}
