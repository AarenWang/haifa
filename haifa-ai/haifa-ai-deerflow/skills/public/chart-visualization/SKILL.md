---
name: chart-visualization
description: Create deterministic charts, comparison dashboards, and data infographics with exact labels and values, including Chinese/CJK text. Use for bar, line, radar, pie, statistical, relationship, flow, map, or other data visualizations where textual accuracy matters; do not route these requests to a generative image model.
---

# Chart Visualization

Use deterministic rendering for charts. Never call `chart-visualization` as a Tool; this Skill provides instructions and scripts that run through generic `bash` or `run_script` capabilities.

## Intelligent Chart Selection

Analyze the data features, choose the most suitable chart type, and consult the matching file in `references/` for its detailed specification:

- **Time Series**: Use `generate_line_chart` (trends) or `generate_area_chart` (accumulated trends). Use `generate_dual_axes_chart` for two different scales.
- **Comparisons**: Use `generate_bar_chart` (categorical) or `generate_column_chart`. Use `generate_histogram_chart` for frequency distributions.
- **Part-to-Whole**: Use `generate_pie_chart` or `generate_treemap_chart` (hierarchical).
- **Relationships & Flow**: Use `generate_scatter_chart` (correlation), `generate_sankey_chart` (flow), or `generate_venn_chart` (overlap).
- **Maps**: Use `generate_district_map` (regions), `generate_pin_map` (points), or `generate_path_map` (routes).
- **Hierarchies & Trees**: Use `generate_organization_chart` or `generate_mind_map`.
- **Specialized**:
  - `generate_radar_chart`: Multi-dimensional comparison.
  - `generate_funnel_chart`: Process stages.
  - `generate_liquid_chart`: Percentage/Progress.
  - `generate_word_cloud_chart`: Text frequency.
  - `generate_boxplot_chart` or `generate_violin_chart`: Statistical distribution.
  - `generate_network_graph`: Complex node-edge relationships.
  - `generate_fishbone_diagram`: Cause-effect analysis.
  - `generate_flow_diagram`: Process flow.
  - `generate_spreadsheet`: Tabular data or pivot tables for structured data display and cross-tabulation.

## Workflow

1. Choose a chart type that matches the data. Consult the matching file in `references/` when its schema is useful.
2. For a comparison dashboard made of labeled bar panels, use the local Pillow renderer. For another supported chart, use the bundled AntV client.
3. Write one UTF-8 JSON specification under `/mnt/user-data/workspace/` and choose an output path under `/mnt/user-data/outputs/`.
4. Run the selected script through generic `run_script` (preferred) or `bash`. Treat any non-zero exit as failure; never convert it into a success response.
5. Check that the requested output file exists and is non-empty, then call `present_files`. A remote URL alone is not a delivered DeerFlow artifact.

## AntV remote renderer

Use `scripts/generate.js` for the operation names listed above, including `generate_spreadsheet`. The JSON `tool` field is only an operation selector understood by this script; it is not a DeerFlow Tool and needs no Tool implementation class.

```json
{
  "tool": "generate_line_chart",
  "args": {
    "title": "月度收入趋势",
    "data": [
      {"time": "2026-01", "value": 120},
      {"time": "2026-02", "value": 148}
    ]
  }
}
```

```bash
node /mnt/skills/public/chart-visualization/scripts/generate.js \
  /mnt/user-data/workspace/monthly-revenue.json \
  --output-file /mnt/user-data/outputs/monthly-revenue.png
```

When using `run_script(language=node)`, load `generate.js` directly in that Node process and call its exported `main(process.argv.slice(2))`. Do not use `execSync`, `spawn`, or another `node ...` command from inside the wrapper script; that creates an unnecessary second runtime lookup. Pass the spec path and `--output-file` values through the Tool's `args` array.

```json
{
  "language": "node",
  "code": "require('/mnt/skills/public/chart-visualization/scripts/generate.js').main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });",
  "args": ["/mnt/user-data/workspace/monthly-revenue.json", "--output-file", "/mnt/user-data/outputs/monthly-revenue.png"],
  "purpose": "Render the validated chart spec in the current Node process"
}
```

For `generate_bar_chart`, every `args.data` item must contain `category` (string) and `value` (finite number). The bundled client rejects `label/value` before any network request.

The script requires Node.js 18+ and outbound HTTPS. It sends the chart request, validates the API response, downloads the returned image, and exits non-zero on parse, operation, API, download, or empty-file failures. Invoke it once per output image when using `--output-file`.

The default endpoint is `https://antv-studio.alipay.com/api/gpt-vis`. Override it with `VIS_REQUEST_SERVER`; override the source marker with `VIS_REQUEST_SOURCE` and the 30-second timeout with `VIS_REQUEST_TIMEOUT_MS`. The default endpoint is suitable as a convenience service, but no public production SLA is part of this Skill. For sensitive data or availability guarantees, configure an approved compatible endpoint or a local renderer. Map operations may additionally require `SERVICE_ID` from the selected backend.

## Comparison dashboard renderer

The bundled script depends only on Pillow. It discovers common Windows, Linux, and macOS CJK fonts and validates that the selected font contains Chinese glyphs.

```json
{
  "title": "天涯 vs 主流社区 四维战略对比",
  "footer": "数据来源：示例",
  "panels": [
    {
      "title": "核心用户群：36–45岁占比",
      "labels": ["天涯社区", "小红书", "知乎", "豆瓣"],
      "values": [41.8, 9.3, 22.5, 28.4],
      "max_value": 50,
      "unit": "%"
    }
  ]
}
```

```bash
python /mnt/skills/public/chart-visualization/scripts/render_bar_dashboard.py \
  --spec-file /mnt/user-data/workspace/comparison.json \
  --output-file /mnt/user-data/outputs/comparison.png
```

Set `DEERFLOW_CJK_FONT` or pass `--font-file` when the runtime font is not in a standard location.

## CJK text rules

- Never use `DejaVuSans.ttf`, `arial.ttf`, or `ImageFont.load_default()` for Chinese text.
- Never silently replace a missing CJK font. Stop with a non-zero exit and report the missing runtime dependency.
- On Windows, prefer Microsoft YaHei (`msyh.ttc`) or SimHei (`simhei.ttf`).
- In the Docker sandbox, install `fonts-noto-cjk` in the image and prefer `NotoSansCJK-Regular.ttc`.
- For custom Matplotlib/Seaborn code, configure the same explicit font file; installing a Python plotting package alone does not provide Chinese glyphs.

## Generative image boundary

Do not use MiniMax, Gemini, or another image model for charts that require exact Chinese text, values, legends, or axis labels. For a decorative AIGC background, generate it without text and overlay verified text with a deterministic renderer in a second step.

## License

The chart specifications in `references/` derive from [antvis/chart-visualization-skills](https://github.com/antvis/chart-visualization-skills) under the MIT License.
