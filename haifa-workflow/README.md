# haifa-workflow

基于 Flowable 的工作流引擎学习模块。

## 场景
- 员工请假申请审批流程
- 银行贷款信审流程（信审 + 风控 + 签约）

## 如何运行
- 在 IDE 中运行 `org.wrj.workflow.demo.LeaveWorkflowDemoApp` 或 `org.wrj.workflow.demo.LoanWorkflowDemoApp`
- 演示使用内存 H2 数据库，并从 `src/main/resources/processes` 部署 BPMN

## 说明
- 示例会按顺序自动完成任务，展示典型路径
- 可修改代码中的 `approved` 变量来观察拒绝路径
