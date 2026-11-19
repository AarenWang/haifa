const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const http = require("node:http");
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

test("rejects label/value bar data before making a network request", async () => {
  const originalFetch = global.fetch;
  let fetchCalls = 0;
  global.fetch = async () => {
    fetchCalls += 1;
    throw new Error("network must not be called");
  };
  try {
    await assert.rejects(
      runSpecs({
        tool: "generate_bar_chart",
        args: { data: [{ label: "A", value: 1 }] },
      }),
      /requires string field 'category'/,
    );
    assert.equal(fetchCalls, 0);
  } finally {
    global.fetch = originalFetch;
  }
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

test("completes a chart request against a fake AntV-compatible server", async () => {
  const outputFile = path.join(
    os.tmpdir(),
    `deerflow-chart-server-${process.pid}-${Date.now()}.png`,
  );
  let chartRequests = 0;
  const server = http.createServer((request, response) => {
    if (request.method === "POST" && request.url === "/api/gpt-vis") {
      chartRequests += 1;
      request.resume();
      request.on("end", () => {
        const address = server.address();
        response.writeHead(200, { "content-type": "application/json" });
        response.end(
          JSON.stringify({
            success: true,
            resultObj: `http://127.0.0.1:${address.port}/chart.png`,
          }),
        );
      });
      return;
    }
    if (request.method === "GET" && request.url === "/chart.png") {
      response.writeHead(200, { "content-type": "image/png" });
      response.end(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]));
      return;
    }
    response.writeHead(404).end();
  });

  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", resolve);
  });
  const originalServer = process.env.VIS_REQUEST_SERVER;
  process.env.VIS_REQUEST_SERVER = `http://127.0.0.1:${server.address().port}/api/gpt-vis`;
  try {
    await runSpecs(
      {
        tool: "generate_bar_chart",
        args: { data: [{ category: "中文分类", value: 42 }] },
      },
      { outputFile },
    );
    assert.equal(chartRequests, 1);
    assert.ok(fs.statSync(outputFile).size > 0);
  } finally {
    if (originalServer === undefined) delete process.env.VIS_REQUEST_SERVER;
    else process.env.VIS_REQUEST_SERVER = originalServer;
    await new Promise((resolve) => server.close(resolve));
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
