/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
var workbench;
(function (workbench) {
    var customSail;
    (function (customSail) {
        var wrapperList = $("#wrapper-list");
        var baseStoreSelect = $("#base-store");
        var previewField = $("#config-preview");
        var errorsList = $("#stack-errors");
        var warningsList = $("#stack-warnings");
        var stackSpecField = $("#stack-spec");
        function baseConfigContainer() {
            var baseType = baseStoreSelect.val();
            return $(".base-config[data-base='" + baseType + "']");
        }
        function readConfigFields(container) {
            var config = {};
            container.find("[data-config-key]").each(function () {
                var element = $(this);
                var key = element.data("config-key");
                if (!key) {
                    return;
                }
                if (element.is(":checkbox")) {
                    config[key] = element.is(":checked");
                    return;
                }
                var value = element.val();
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
        function buildSpec() {
            var repo = {
                id: $("#id").val(),
                title: $("#title").val()
            };
            var stack = [];
            wrapperList.children(".stack-item").each(function () {
                var item = $(this);
                var type = item.data("layer-type");
                stack.push({
                    type: type,
                    config: readConfigFields(item)
                });
            });
            var baseType = baseStoreSelect.val();
            stack.push({
                type: baseType,
                config: readConfigFields(baseConfigContainer())
            });
            return { repo: repo, stack: stack };
        }
        function updateSpecField() {
            stackSpecField.val(JSON.stringify(buildSpec()));
        }
        function renderMessages(list, items) {
            list.empty();
            items.forEach(function (message) {
                list.append("<li>" + message + "</li>");
            });
        }
        var previewTimeout;
        function schedulePreview() {
            if (previewTimeout) {
                clearTimeout(previewTimeout);
            }
            previewTimeout = window.setTimeout(updatePreview, 300);
        }
        function updatePreview() {
            var spec = buildSpec();
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
        function createWrapperContent(type) {
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
        function addWrapper(type) {
            if (!type) {
                return;
            }
            if (wrapperList.find(".stack-item[data-layer-type='" + type + "']").length > 0) {
                renderMessages(errorsList, ["Duplicate wrapper detected: " + type]);
                return;
            }
            var displayName = type === "RDFS" ? "Schema-caching RDFS inferencer" :
                type === "SHACL" ? "SHACL Sail" : "Lucene Sail";
            var item = $("<div class='stack-item' data-layer-type='" + type + "'></div>");
            item.append("<h3>" + displayName + "</h3>");
            item.append(createWrapperContent(type));
            var actions = $("<div class='stack-actions'></div>");
            actions.append("<input type='button' class='move-up' value='Up' />");
            actions.append("<input type='button' class='move-down' value='Down' />");
            actions.append("<input type='button' class='remove-wrapper' value='Remove' />");
            item.append(actions);
            wrapperList.append(item);
            schedulePreview();
        }
        function moveWrapper(element, direction) {
            if (direction < 0) {
                var previous = element.prev(".stack-item");
                if (previous.length) {
                    element.insertBefore(previous);
                }
            }
            else {
                var next = element.next(".stack-item");
                if (next.length) {
                    element.insertAfter(next);
                }
            }
            schedulePreview();
        }
        function updateBaseVisibility() {
            $(".base-config").hide();
            baseConfigContainer().show();
            schedulePreview();
        }
        function downloadConfig() {
            var content = previewField.val();
            if (!content) {
                return;
            }
            var blob = new Blob([content], { type: "text/turtle;charset=utf-8" });
            var url = window.URL.createObjectURL(blob);
            var link = document.createElement("a");
            link.href = url;
            link.download = "config.ttl";
            link.click();
            window.URL.revokeObjectURL(url);
        }
        workbench.addLoad(function () {
            updateBaseVisibility();
            $("#add-wrapper").on("click", function () {
                var type = $("#wrapper-type").val();
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
    })(customSail = workbench.customSail || (workbench.customSail = {}));
})(workbench || (workbench = {}));
//# sourceMappingURL=create-custom-sail.js.map