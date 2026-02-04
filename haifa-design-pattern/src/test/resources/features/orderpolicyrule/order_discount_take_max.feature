Feature: Order discount policy - take max

  Scenario: TAKE_MAX selects the only supported order coupon
    Given order coupon code "C100-20"
    And the order has items:
      | sku | unitPriceCents | qty |
      | A-1 | 10000          | 1   |
    And item discount rules: ""
    And order discount rules: "Coupon100Minus20Rule,PromoCode10OffRule"
    And item policy "STACK_ALL"
    And order policy "TAKE_MAX"
    When the policy engine executes
    Then chosen order candidate ids should be:
      | id           |
      | C100_MINUS_20 |
