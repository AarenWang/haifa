const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const scriptPath = path.resolve(__dirname, "../scripts/generate.js");
const {
  CHART_TYPE_MAP,
  extractResultUrl,
  runSpecs,
} = require(scriptPath);

test("maps spreadsheet to the documented AntV chart type", () => {
  assert.equal(CHART_TYPE_MAP.generate_spreadsheet, "spreadsheet");
});

test("extracts an artifact URL from map-style content", () => {
  const result = {
    content: [{ type: "text", text: "Rendered: https://example.test/chart.png" }],
  };
  assert.equal(extractResultUrl(result), "https://example.test/chart.png");
});

test("rejects an unknown operation", async () => {
  await assert.rejects(
    runSpecs({ tool: "generate_missing_chart", args: {} }),
    /Unknown chart operation/,
  );
});

test("downloads a successful remote result to a non-empty local artifact", async () => {
  const originalFetch = global.fetch;
  const outputFile = path.join(
    os.tmpdir(),
    `deerflow-chart-${process.pid}-${Date.now()}.png`,
  );
  const requests = [];
  global.fetch = async (url, options = {}) => {
    requests.push({ url: String(url), options });
    if (options.method === "POST") {
      return new Response(
        JSON.stringify({
          success: true,
          resultObj: "https://assets.example.test/chart.png",
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      );
    }
    return new Response(Uint8Array.from([137, 80, 78, 71]), {
      status: 200,
      headers: { "content-type": "image/png" },
    });
  };

  try {
    await runSpecs(
      {
        tool: "generate_bar_chart",
        args: { data: [{ category: "A", value: 1 }] },
      },
      { outputFile },
    );
    assert.equal(requests.length, 2);
    const requestBody = JSON.parse(requests[0].options.body);
    assert.equal(requestBody.source, "chart-visualization-skills");
    assert.equal(requestBody.type, "bar");
    assert.ok(fs.statSync(outputFile).size > 0);
  } finally {
    global.fetch = originalFetch;
    if (fs.existsSync(outputFile)) fs.rmSync(outputFile);
  }
});

test("rejects multiple specs when one output file is requested", async () => {
  await assert.rejects(
    runSpecs(
      [
        { tool: "generate_bar_chart", args: {} },
        { tool: "generate_line_chart", args: {} },
      ],
      { outputFile: "chart.png" },
    ),
    /exactly one chart spec/,
  );
});
