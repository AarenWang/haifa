package org.wrj.haifa.designpattern.orderpolicyrule.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.wrj.haifa.designpattern.orderpipeline.model.LineItem;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderContext;
import org.wrj.haifa.designpattern.orderpipeline.model.OrderRequest;
import org.wrj.haifa.designpattern.orderpolicyrule.engine.OrderPolicyEngine;
import org.wrj.haifa.designpattern.orderpolicyrule.engine.PolicyExecutionResult;
import org.wrj.haifa.designpattern.orderpolicyrule.engine.ItemPolicyDecision;
import org.wrj.haifa.designpattern.orderpolicyrule.model.ConflictMatrix;
import org.wrj.haifa.designpattern.orderpolicyrule.model.DiscountCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.model.PolicyResult;
import org.wrj.haifa.designpattern.orderpolicyrule.model.RejectedCandidate;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.ConflictMatrixPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.DiscountPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.MutexByGroupTakeMaxPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.StackAllPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.policy.TakeMaxPolicy;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.AddOnVoucherRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.Coupon100Minus20Rule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.FlashSaleRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.ItemDiscountRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.OrderDiscountRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.PromoCode10OffRule;
import org.wrj.haifa.designpattern.orderpolicyrule.rule.VipPriceRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OrderPolicyEngineSteps {

    private OrderRequest request;
    private OrderContext context;

    private final List<ItemDiscountRule> itemRules = new ArrayList<>();
    private final List<OrderDiscountRule> orderRules = new ArrayList<>();

    private DiscountPolicy itemPolicy = new StackAllPolicy();
    private DiscountPolicy orderPolicy = new StackAllPolicy();

    private PolicyExecutionResult executionResult;

    @Given("a user tier {string}")
    public void a_user_tier(String userTier) {
        ensureRequest();
        request.setUserTier(userTier);
    }

    @Given("order country {string} and channel {string}")
    public void order_country_and_channel(String country, String channel) {
        ensureRequest();
        request.setCountry(country);
        request.setChannel(channel);
    }

    @Given("order coupon code {string}")
    public void order_coupon_code(String couponCode) {
        ensureRequest();
        if (couponCode != null && !couponCode.isBlank()) {
            request.setCouponCode(couponCode);
        }
    }

    @Given("the order has items:")
    public void the_order_has_items(DataTable table) {
        ensureRequest();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        List<LineItem> items = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String sku = row.get("sku");
            int unitPriceCents = Integer.parseInt(row.get("unitPriceCents"));
            int qty = Integer.parseInt(row.get("qty"));
            items.add(new LineItem(sku, unitPriceCents, qty));
        }
        request.setItems(items);
    }

    @Given("item discount rules: {string}")
    public void item_discount_rules(String ruleList) {
        itemRules.clear();
        for (String token : splitCsv(ruleList)) {
            itemRules.add(instantiateItemRule(token));
        }
    }

    @Given("order discount rules: {string}")
    public void order_discount_rules(String ruleList) {
        orderRules.clear();
        for (String token : splitCsv(ruleList)) {
            orderRules.add(instantiateOrderRule(token));
        }
    }

    @Given("item policy {string}")
    public void item_policy(String policyKey) {
        this.itemPolicy = instantiatePolicy(policyKey);
    }

    @Given("order policy {string}")
    public void order_policy(String policyKey) {
        this.orderPolicy = instantiatePolicy(policyKey);
    }

    @When("the policy engine executes")
    public void the_policy_engine_executes() {
        ensureRequest();
        this.context = new OrderContext(request);
        OrderPolicyEngine engine = new OrderPolicyEngine(itemRules, orderRules, itemPolicy, orderPolicy);
        this.executionResult = engine.execute(context);
        assertNotNull(executionResult);
    }

    @Then("item {string} itemDiscountCents should be {int}")
    public void item_item_discount_should_be(String sku, int expectedDiscountCents) {
        LineItem item = findItemBySku(sku);
        assertEquals(expectedDiscountCents, item.getItemDiscountCents());
    }

    @Then("chosen item candidate ids for {string} should be:")
    public void chosen_item_candidate_ids_for_should_be(String sku, DataTable table) {
        ItemPolicyDecision decision = findItemDecisionBySku(sku);
        List<String> expected = table.asMaps(String.class, String.class).stream()
                .map(row -> row.get("id"))
                .toList();

        List<String> actual = decision.getPolicyResult().getChosenCandidates().stream()
                .map(DiscountCandidate::getId)
                .toList();

        assertEquals(expected, actual, "chosen candidates must match in order");
    }

    @Then("chosen item candidate ids for {string} should contain:")
    public void chosen_item_candidate_ids_for_should_contain(String sku, DataTable table) {
        ItemPolicyDecision decision = findItemDecisionBySku(sku);
        List<String> expected = table.asMaps(String.class, String.class).stream()
                .map(row -> row.get("id"))
                .toList();

        List<String> actual = decision.getPolicyResult().getChosenCandidates().stream()
                .map(DiscountCandidate::getId)
                .toList();

        for (String id : expected) {
            assertTrue(actual.contains(id), "expected chosen candidates to contain: " + id + ", actual=" + actual);
        }
    }

    @Then("rejected item candidate ids for {string} should be:")
    public void rejected_item_candidate_ids_for_should_be(String sku, DataTable table) {
        ItemPolicyDecision decision = findItemDecisionBySku(sku);
        List<Map<String, String>> expectedRows = table.asMaps(String.class, String.class);

        List<RejectedCandidate> rejected = decision.getPolicyResult().getRejectedCandidates();
        assertEquals(expectedRows.size(), rejected.size(), "rejected size mismatch");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expected = expectedRows.get(i);
            RejectedCandidate actual = rejected.get(i);
            assertEquals(expected.get("id"), actual.getCandidate().getId());
            assertEquals(expected.get("reason"), actual.getReason());
        }
    }

    @Then("chosen order candidate ids should be:")
    public void chosen_order_candidate_ids_should_be(DataTable table) {
        PolicyResult result = getOrderPolicyResult();
        List<String> expected = table.asMaps(String.class, String.class).stream()
                .map(row -> row.get("id"))
                .toList();

        List<String> actual = result.getChosenCandidates().stream()
                .map(DiscountCandidate::getId)
                .toList();

        assertEquals(expected, actual, "order chosen candidates must match in order");
    }

    @Then("chosen order candidate ids should contain:")
    public void chosen_order_candidate_ids_should_contain(DataTable table) {
        PolicyResult result = getOrderPolicyResult();
        List<String> expected = table.asMaps(String.class, String.class).stream()
                .map(row -> row.get("id"))
                .toList();

        List<String> actual = result.getChosenCandidates().stream()
                .map(DiscountCandidate::getId)
                .toList();

        for (String id : expected) {
            assertTrue(actual.contains(id), "expected order chosen candidates to contain: " + id + ", actual=" + actual);
        }
    }

    @Then("rejected order candidate ids should be:")
    public void rejected_order_candidate_ids_should_be(DataTable table) {
        PolicyResult result = getOrderPolicyResult();
        List<Map<String, String>> expectedRows = table.asMaps(String.class, String.class);

        List<RejectedCandidate> rejected = result.getRejectedCandidates();
        assertEquals(expectedRows.size(), rejected.size(), "order rejected size mismatch");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expected = expectedRows.get(i);
            RejectedCandidate actual = rejected.get(i);
            assertEquals(expected.get("id"), actual.getCandidate().getId());
            assertEquals(expected.get("reason"), actual.getReason());
        }
    }

    private void ensureRequest() {
        if (request == null) {
            request = new OrderRequest();
            request.setChannel("WEB");
            request.setCountry("CN");
            request.setUserTier("NORMAL");
        }
    }

    private LineItem findItemBySku(String sku) {
        assertNotNull(context, "context not initialized; did you run 'When the policy engine executes'?");
        return context.getRequest().getItems().stream()
                .filter(i -> sku.equals(i.getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("item not found: " + sku));
    }

    private ItemPolicyDecision findItemDecisionBySku(String sku) {
        assertNotNull(executionResult, "executionResult not initialized; did you run 'When the policy engine executes'?");
        return executionResult.getItemDecisions().stream()
                .filter(d -> sku.equals(d.getItem().getSku()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("item decision not found: " + sku));
    }

    private PolicyResult getOrderPolicyResult() {
        assertNotNull(executionResult, "executionResult not initialized; did you run 'When the policy engine executes'?");
        return executionResult.getOrderPolicyResult();
    }

    private static List<String> splitCsv(String value) {
        if (value == null) {
            return List.of();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String[] parts = trimmed.split(",");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) {
                tokens.add(t);
            }
        }
        return tokens;
    }

    private static ItemDiscountRule instantiateItemRule(String simpleName) {
        return switch (simpleName) {
            case "FlashSaleRule" -> new FlashSaleRule();
            case "VipPriceRule" -> new VipPriceRule();
            case "AddOnVoucherRule" -> new AddOnVoucherRule();
            default -> throw new IllegalArgumentException("Unknown item rule: " + simpleName);
        };
    }

    private static OrderDiscountRule instantiateOrderRule(String simpleName) {
        return switch (simpleName) {
            case "Coupon100Minus20Rule" -> new Coupon100Minus20Rule();
            case "PromoCode10OffRule" -> new PromoCode10OffRule();
            default -> throw new IllegalArgumentException("Unknown order rule: " + simpleName);
        };
    }

    private static DiscountPolicy instantiatePolicy(String policyKey) {
        String key = policyKey == null ? "" : policyKey.trim();
        return switch (key) {
            case "STACK_ALL" -> new StackAllPolicy();
            case "TAKE_MAX" -> new TakeMaxPolicy();
            case "MUTEX_BY_GROUP_TAKE_MAX" -> new MutexByGroupTakeMaxPolicy();
            case "CONFLICT_MATRIX" -> new ConflictMatrixPolicy(new MutexByGroupTakeMaxPolicy(), new ConflictMatrix());
            default -> throw new IllegalArgumentException("Unknown policy key: " + policyKey);
        };
    }
}
