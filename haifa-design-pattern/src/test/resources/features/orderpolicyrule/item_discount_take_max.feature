Feature: Item discount policy - take max

  Scenario: TAKE_MAX keeps only the biggest discount
    Given a user tier "VIP"
    And the order has items:
      | sku  | unitPriceCents | qty |
      | FS-2 | 10000          | 2   |
    And item discount rules: "FlashSaleRule,VipPriceRule,AddOnVoucherRule"
    And order discount rules: ""
    And item policy "TAKE_MAX"
    And order policy "STACK_ALL"
    When the policy engine executes
    Then item "FS-2" itemDiscountCents should be 4000
    And chosen item candidate ids for "FS-2" should be:
      | id         |
      | FLASH_SALE |
    And rejected item candidate ids for "FS-2" should be:
      | id            | reason            |
      | VIP_PRICE     | TAKE_MAX_REJECTED |
      | ADDON_VOUCHER | TAKE_MAX_REJECTED |
