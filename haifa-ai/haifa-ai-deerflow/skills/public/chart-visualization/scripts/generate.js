#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const DEFAULT_VIS_REQUEST_SERVER =
  "https://antv-studio.alipay.com/api/gpt-vis";
const DEFAULT_REQUEST_TIMEOUT_MS = 30_000;

// These are operation identifiers inside this script, not DeerFlow Tool names.
const CHART_TYPE_MAP = {
  generate_area_chart: "area",
  generate_bar_chart: "bar",
  generate_boxplot_chart: "boxplot",
  generate_column_chart: "column",
  generate_district_map: "district-map",
  generate_dual_axes_chart: "dual-axes",
  generate_fishbone_diagram: "fishbone-diagram",
  generate_flow_diagram: "flow-diagram",
  generate_funnel_chart: "funnel",
  generate_histogram_chart: "histogram",
  generate_line_chart: "line",
  generate_liquid_chart: "liquid",
  generate_mind_map: "mind-map",
  generate_network_graph: "network-graph",
  generate_organization_chart: "organization-chart",
  generate_path_map: "path-map",
  generate_pie_chart: "pie",
  generate_pin_map: "pin-map",
  generate_radar_chart: "radar",
  generate_sankey_chart: "sankey",
  generate_scatter_chart: "scatter",
  generate_spreadsheet: "spreadsheet",
  generate_treemap_chart: "treemap",
  generate_venn_chart: "venn",
  generate_violin_chart: "violin",
  generate_word_cloud_chart: "word-cloud",
};

const MAP_TOOLS = new Set([
  "generate_district_map",
  "generate_path_map",
  "generate_pin_map",
]);

function getVisRequestServer() {
  return process.env.VIS_REQUEST_SERVER || DEFAULT_VIS_REQUEST_SERVER;
}

function getRequestSource() {
  return process.env.VIS_REQUEST_SOURCE || "chart-visualization-skills";
}

function getServiceIdentifier() {
  return process.env.SERVICE_ID;
}

function getRequestTimeoutMs() {
  const configured = Number(process.env.VIS_REQUEST_TIMEOUT_MS);
  return Number.isFinite(configured) && configured > 0
    ? configured
    : DEFAULT_REQUEST_TIMEOUT_MS;
}

async function fetchWithTimeout(url, options = {}) {
  const controller = new AbortController();
  const timeoutMs = getRequestTimeoutMs();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } catch (error) {
    if (error && error.name === "AbortError") {
      throw new Error(`Request timed out after ${timeoutMs} ms: ${url}`);
    }
    throw error;
  } finally {
    clearTimeout(timer);
  }
}

async function httpPost(url, payload) {
  const response = await fetchWithTimeout(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const body = await response.text();

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${body}`);
  }

  try {
    return JSON.parse(body);
  } catch (error) {
    throw new Error(`Remote renderer returned invalid JSON: ${error.message}`);
  }
}

function assertSuccessfulResponse(data) {
  if (!data || data.success !== true) {
    throw new Error((data && data.errorMessage) || "Unknown remote renderer error");
  }
  if (data.resultObj === undefined || data.resultObj === null) {
    throw new Error("Remote renderer returned no resultObj");
  }
  return data.resultObj;
}

async function generateChart(chartType, options) {
  const data = await httpPost(getVisRequestServer(), {
    ...options,
    type: chartType,
    source: getRequestSource(),
  });
  return assertSuccessfulResponse(data);
}

async function generateMap(tool, inputData) {
  const data = await httpPost(getVisRequestServer(), {
    serviceId: getServiceIdentifier(),
    tool,
    input: inputData,
    source: getRequestSource(),
  });
  return assertSuccessfulResponse(data);
}

function extractResultUrl(result) {
  if (typeof result === "string") {
    const match = result.match(/https?:\/\/[^\s<>"']+/);
    return match ? match[0].replace(/[),.;\]}]+$/, "") : null;
  }
  if (Array.isArray(result)) {
    for (const item of result) {
      const url = extractResultUrl(item);
      if (url) return url;
    }
    return null;
  }
  if (result && typeof result === "object") {
    for (const key of ["url", "imageUrl", "resultObj", "content", "text"]) {
      if (Object.prototype.hasOwnProperty.call(result, key)) {
        const url = extractResultUrl(result[key]);
        if (url) return url;
      }
    }
  }
  return null;
}

async function downloadArtifact(url, outputFile) {
  let parsed;
  try {
    parsed = new URL(url);
  } catch (error) {
    throw new Error(`Remote renderer returned an invalid artifact URL: ${url}`);
  }
  if (!new Set(["http:", "https:"]).has(parsed.protocol)) {
    throw new Error(`Unsupported artifact URL protocol: ${parsed.protocol}`);
  }

  const response = await fetchWithTimeout(parsed.toString());
  if (!response.ok) {
    throw new Error(`Artifact download failed with HTTP ${response.status}`);
  }
  const contentType = response.headers.get("content-type") || "";
  if (
    contentType &&
    !contentType.toLowerCase().startsWith("image/") &&
    !contentType.toLowerCase().includes("octet-stream")
  ) {
    throw new Error(`Artifact response is not an image: ${contentType}`);
  }

  const bytes = Buffer.from(await response.arrayBuffer());
  if (bytes.length === 0) {
    throw new Error("Artifact download returned an empty body");
  }

  const destination = path.resolve(outputFile);
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.writeFileSync(destination, bytes);
  if (!fs.statSync(destination).isFile() || fs.statSync(destination).size === 0) {
    throw new Error(`Renderer did not create a non-empty output: ${destination}`);
  }
  return destination;
}

function parseCliArgs(argv) {
  let specArg;
  let outputFile;
  for (let index = 0; index < argv.length; index += 1) {
    const argument = argv[index];
    if (argument === "--output-file") {
      outputFile = argv[index + 1];
      if (!outputFile) throw new Error("--output-file requires a path");
      index += 1;
    } else if (argument.startsWith("--")) {
      throw new Error(`Unknown option: ${argument}`);
    } else if (specArg === undefined) {
      specArg = argument;
    } else {
      throw new Error(`Unexpected argument: ${argument}`);
    }
  }
  if (!specArg) {
    throw new Error(
      "Usage: node generate.js <spec_json_or_file> [--output-file <image_path>]",
    );
  }
  return { specArg, outputFile };
}

function readSpec(specArg) {
  const raw = fs.existsSync(specArg)
    ? fs.readFileSync(specArg, "utf-8")
    : specArg;
  try {
    return JSON.parse(raw);
  } catch (error) {
    throw new Error(`Error parsing spec: ${error.message}`);
  }
}

function printResult(result) {
  if (typeof result === "string") {
    console.log(result);
  } else {
    console.log(JSON.stringify(result));
  }
}

async function runSpec(item, outputFile) {
  if (!item || typeof item !== "object" || Array.isArray(item)) {
    throw new Error("Each chart spec must be an object");
  }
  const tool = item.tool;
  if (!tool) {
    throw new Error(`'tool' field missing in spec: ${JSON.stringify(item)}`);
  }
  const chartType = CHART_TYPE_MAP[tool];
  if (!chartType) {
    throw new Error(`Unknown chart operation '${tool}'`);
  }

  const args = item.args || {};
  const result = MAP_TOOLS.has(tool)
    ? await generateMap(tool, args)
    : await generateChart(chartType, args);

  if (!outputFile) {
    printResult(result);
    return result;
  }

  const url = extractResultUrl(result);
  if (!url) {
    throw new Error(`Remote renderer returned no downloadable image URL for '${tool}'`);
  }
  const destination = await downloadArtifact(url, outputFile);
  console.log(`Chart generated: ${destination}`);
  console.log(`Source URL: ${url}`);
  return { result, destination, url };
}

async function runSpecs(spec, options = {}) {
  const specs = Array.isArray(spec) ? spec : [spec];
  if (specs.length === 0) throw new Error("Chart spec array must not be empty");
  if (options.outputFile && specs.length !== 1) {
    throw new Error("--output-file accepts exactly one chart spec per invocation");
  }
  const results = [];
  for (const item of specs) {
    results.push(await runSpec(item, options.outputFile));
  }
  return results;
}

async function main(argv = process.argv.slice(2)) {
  const { specArg, outputFile } = parseCliArgs(argv);
  await runSpecs(readSpec(specArg), { outputFile });
}

if (require.main === module) {
  main().catch((error) => {
    console.error(`Chart generation failed: ${error.message}`);
    process.exitCode = 1;
  });
}

module.exports = {
  CHART_TYPE_MAP,
  downloadArtifact,
  extractResultUrl,
  generateChart,
  generateMap,
  httpPost,
  main,
  parseCliArgs,
  readSpec,
  runSpec,
  runSpecs,
};
