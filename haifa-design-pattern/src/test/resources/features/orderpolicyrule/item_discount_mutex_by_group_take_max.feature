Feature: Item discount policy - mutex by group take max

  Scenario: PRICE_PROMO group is mutex, ADDON stacks
    Given a user tier "VIP"
    And the order has items:
      | sku    | unitPriceCents | qty |
      | FS-001 | 10000          | 2   |
    And item discount rules: "FlashSaleRule,VipPriceRule,AddOnVoucherRule"
    And order discount rules: ""
    And item policy "MUTEX_BY_GROUP_TAKE_MAX"
    And order policy "STACK_ALL"
    When the policy engine executes
    Then item "FS-001" itemDiscountCents should be 4300
    And chosen item candidate ids for "FS-001" should contain:
      | id            |
      | FLASH_SALE    |
      | ADDON_VOUCHER |
    And rejected item candidate ids for "FS-001" should be:
      | id        | reason              |
      | VIP_PRICE | GROUP_MUTEX_REJECTED |
