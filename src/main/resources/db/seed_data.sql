-- =============================================================================
--  Telecom Plan Advisor Copilot — Dummy Seed Data
--  Run against MySQL 8.x after the schema has been created by Hibernate
--  (spring.jpa.hibernate.ddl-auto=update or create)
--
--  Execution order matters due to FK constraints:
--    1. plans_catalog
--    2. promotions
--    3. customers          (FK → plans_catalog)
--    4. customer_usage     (FK → customers)
--    5. plan_transactions  (FK → customers, plans_catalog, promotions)
--    6. copilot_interactions (FK → customers)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 0. Clean slate (safe re-run)
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE copilot_interactions;
TRUNCATE TABLE plan_transactions;
TRUNCATE TABLE customer_usage;
TRUNCATE TABLE customers;
TRUNCATE TABLE promotions;
TRUNCATE TABLE plans_catalog;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. plans_catalog
--    plan_type values: BASE_PLAN | TRAVEL_ADD_ON | DATA_BOOSTER
--    data_limit_gb = 9999 → Unlimited
-- =============================================================================
INSERT INTO plans_catalog (plan_name, plan_type, monthly_cost, data_limit_gb, description) VALUES
-- Base Plans
('Starter 2GB',      'BASE_PLAN',   9.99,  2,    'Entry-level plan with 2 GB data, 200 voice minutes and 100 SMS.'),
('Basic 5GB',        'BASE_PLAN',  19.99,  5,    'Great for light users: 5 GB data, unlimited voice and 500 SMS.'),
('Standard 15GB',    'BASE_PLAN',  34.99, 15,    'Mid-tier plan with 15 GB data, unlimited voice & SMS.'),
('Premium 30GB',     'BASE_PLAN',  49.99, 30,    'Power user plan with 30 GB data, unlimited voice, SMS and HD streaming.'),
('Ultimate Unlimited','BASE_PLAN', 69.99, 9999,  'Truly unlimited data, voice and SMS. 5G ready. Hotspot included.'),
-- Travel Add-ons
('US Roaming Pack',  'TRAVEL_ADD_ON', 14.99,  3, '3 GB roaming data valid in the United States for 30 days.'),
('Europe Bundle',    'TRAVEL_ADD_ON', 19.99,  5, '5 GB roaming data across 35 European countries for 30 days.'),
('Asia Explorer',    'TRAVEL_ADD_ON', 24.99,  8, '8 GB roaming data across 12 Asian countries for 30 days.'),
('Global Roaming XL','TRAVEL_ADD_ON', 39.99, 15, '15 GB worldwide roaming data valid in 100+ countries for 30 days.'),
-- Data Boosters
('1 GB Top-Up',      'DATA_BOOSTER',  3.99,  1,  'Instant 1 GB domestic data add-on. No expiry within current billing cycle.'),
('5 GB Top-Up',      'DATA_BOOSTER',  9.99,  5,  'Instant 5 GB domestic data add-on. No expiry within current billing cycle.'),
('10 GB Top-Up',     'DATA_BOOSTER', 14.99, 10,  'Instant 10 GB domestic data add-on. No expiry within current billing cycle.');

-- =============================================================================
-- 2. promotions
-- =============================================================================
INSERT INTO promotions (promo_name, discount_percentage, min_tenure_months, is_active) VALUES
('Welcome Offer',          10,  0,  TRUE),   -- available to all customers
('6-Month Loyalty Reward', 15,  6,  TRUE),   -- requires 6+ months tenure
('1-Year Loyalty Bonus',   20, 12,  TRUE),   -- requires 12+ months tenure
('2-Year Champion Deal',   25, 24,  TRUE),   -- requires 24+ months tenure
('Seasonal Summer Sale',   12,  0,  TRUE),   -- open to everyone
('Legacy Plan Migration',  30,  0,  FALSE);  -- inactive — disabled promo

-- =============================================================================
-- 3. customers  (current_plan_id references plans_catalog IDs 1-5)
-- =============================================================================
INSERT INTO customers (customer_id, name, phone_number, current_plan_id, tenure_months, billing_cycle_date) VALUES
('CUST001', 'Alice Johnson',    '+1-555-0101', 2, 36, '2026-03-15'),  -- Basic 5GB,        3yr tenure
('CUST002', 'Bob Martinez',     '+1-555-0102', 3, 14, '2026-03-20'),  -- Standard 15GB,   14m tenure
('CUST003', 'Carol Williams',   '+1-555-0103', 4, 28, '2026-03-10'),  -- Premium 30GB,    28m tenure
('CUST004', 'David Brown',      '+1-555-0104', 5,  3, '2026-03-25'),  -- Ultimate Unlimited, 3m tenure
('CUST005', 'Eva Garcia',       '+1-555-0105', 1,  0, '2026-03-01'),  -- Starter 2GB,     new customer
('CUST006', 'Frank Lee',        '+1-555-0106', 3, 60, '2026-03-18'),  -- Standard 15GB,    5yr tenure
('CUST007', 'Grace Kim',        '+1-555-0107', 2,  8, '2026-03-12'),  -- Basic 5GB,        8m tenure
('CUST008', 'Henry Patel',      '+1-555-0108', 4, 24, '2026-03-08'),  -- Premium 30GB,    24m tenure
('CUST009', 'Irene Nguyen',     '+1-555-0109', 1,  1, '2026-03-22'),  -- Starter 2GB,      1m tenure
('CUST010', 'James O\'Connor',  '+1-555-0110', 5, 48, '2026-03-05');  -- Ultimate Unlimited, 4yr tenure

-- =============================================================================
-- 4. customer_usage  (one record per customer = current billing period)
--    billing_period_start = first day of the current billing cycle
-- =============================================================================
INSERT INTO customer_usage (customer_id, billing_period_start, data_used_gb, roaming_used_mb, last_updated) VALUES
('CUST001', '2026-03-15',  4.20,    0.00, '2026-03-05 09:00:00'),  -- nearly at 5GB limit
('CUST002', '2026-03-20',  7.85,  512.50, '2026-03-05 09:05:00'),  -- 7.8GB of 15GB, some roaming
('CUST003', '2026-03-10', 18.40, 1024.00, '2026-03-05 09:10:00'),  -- heavy user, heavy roaming
('CUST004', '2026-03-25', 55.20,    0.00, '2026-03-05 09:15:00'),  -- unlimited plan, high usage
('CUST005', '2026-03-01',  0.85,    0.00, '2026-03-05 09:20:00'),  -- light usage on Starter
('CUST006', '2026-03-18', 11.30,  256.00, '2026-03-05 09:25:00'),  -- moderate Standard usage
('CUST007', '2026-03-12',  4.95,    0.00, '2026-03-05 09:30:00'),  -- almost at 5GB cap
('CUST008', '2026-03-08', 22.10, 2048.00, '2026-03-05 09:35:00'),  -- frequent traveller
('CUST009', '2026-03-22',  0.30,    0.00, '2026-03-05 09:40:00'),  -- barely started
('CUST010', '2026-03-05', 38.90,  768.00, '2026-03-05 09:45:00');  -- power user with roaming

-- =============================================================================
-- 5. plan_transactions  (historical plan change ledger)
--    Covers upgrades, downgrades, and promo-assisted changes
-- =============================================================================
INSERT INTO plan_transactions
    (transaction_id, customer_id, old_plan_id, new_plan_id, promo_applied_id, prorated_billed_amount, executed_at)
VALUES
-- CUST001: Upgraded from Starter(1) → Basic(2), 1-yr loyalty promo applied
('TXN-2025-0001', 'CUST001', 1, 2, 3,  8.00, '2025-09-10 14:32:00'),
-- CUST002: Upgraded from Basic(2) → Standard(3), no promo
('TXN-2025-0002', 'CUST002', 2, 3, NULL, 25.50, '2025-10-20 10:15:00'),
-- CUST003: Upgraded from Standard(3) → Premium(4), 2-yr champion deal applied
('TXN-2024-0003', 'CUST003', 3, 4, 4,  28.12, '2024-11-05 16:45:00'),
-- CUST004: New customer, direct sign-up to Ultimate(5)
('TXN-2025-0004', 'CUST004', NULL, 5, 1, 69.99, '2025-12-01 11:00:00'),
-- CUST006: Upgraded from Basic(2) → Standard(3), seasonal sale applied
('TXN-2021-0005', 'CUST006', 2, 3, 5,  22.00, '2021-03-01 08:00:00'),
-- CUST008: Upgraded from Standard(3) → Premium(4), 2-yr loyalty applied
('TXN-2024-0006', 'CUST008', 3, 4, 4,  31.24, '2024-03-08 13:22:00'),
-- CUST010: Upgraded from Premium(4) → Ultimate(5), 2-yr loyalty applied
('TXN-2022-0007', 'CUST010', 4, 5, 4,  39.99, '2022-03-05 09:00:00'),
-- CUST001: Added US Roaming pack(6) as add-on
('TXN-2026-0008', 'CUST001', NULL, 6, NULL, 14.99, '2026-02-14 17:05:00'),
-- CUST003: Added Global Roaming XL(9) before a business trip
('TXN-2026-0009', 'CUST003', NULL, 9, NULL, 39.99, '2026-01-28 12:30:00'),
-- CUST007: Purchased 5GB top-up booster(11)
('TXN-2026-0010', 'CUST007', NULL, 11, NULL, 9.99, '2026-03-03 08:55:00');

-- =============================================================================
-- 6. copilot_interactions  (AI advisor conversation audit log)
-- =============================================================================
INSERT INTO copilot_interactions
    (interaction_id, customer_id, identified_intent, llm_summary, created_at)
VALUES
('INT-001', 'CUST001', 'DATA_BOOSTER',
 'Customer asked about running out of data. Recommended 5GB Top-Up booster or upgrading to Standard 15GB. Highlighted pro-rated cost of $8.00 for rest of cycle.',
 '2026-03-01 10:15:00'),

('INT-002', 'CUST002', 'TRAVEL_INQUIRY',
 'Customer planning US trip next month. Recommended US Roaming Pack ($14.99) or Global Roaming XL ($39.99) based on usage. Customer is eligible for 1-Year Loyalty Bonus (20% off).',
 '2026-03-02 14:30:00'),

('INT-003', 'CUST003', 'PLAN_UPGRADE',
 'Customer regularly exceeds 15GB monthly. Recommended upgrade to Premium 30GB. Pro-rated cost calculated at $28.12. 2-Year Champion Deal (25% off) applied.',
 '2026-03-03 09:45:00'),

('INT-004', 'CUST004', 'BILLING_INQUIRY',
 'Customer queried about current bill. Explained unlimited data plan at $69.99/month. No overage charges apply. Suggested adding hotspot booster for work-from-anywhere scenario.',
 '2026-03-03 16:00:00'),

('INT-005', 'CUST005', 'PLAN_UPGRADE',
 'New customer on Starter 2GB nearly exhausted allowance. Recommended upgrading to Basic 5GB ($19.99) with Welcome Offer (10% off). Clear comparison shown.',
 '2026-03-04 11:20:00'),

('INT-006', 'CUST006', 'PROMO_INQUIRY',
 'Customer with 60-month tenure enquired about loyalty discounts. Presented all four active promotions. Eligible for 2-Year Champion Deal (25% off) on any plan upgrade.',
 '2026-03-04 13:55:00'),

('INT-007', 'CUST007', 'DATA_BOOSTER',
 'Customer at 99% of 5GB cap. Recommended immediate 1GB Top-Up ($3.99) to avoid throttling or upgrade to Standard 15GB before next billing cycle.',
 '2026-03-05 08:10:00'),

('INT-008', 'CUST008', 'TRAVEL_INQUIRY',
 'Frequent traveller with 2048MB roaming usage. Recommended Asia Explorer ($24.99) or Global Roaming XL ($39.99) for upcoming trip. 2-Year Champion Deal eligible.',
 '2026-03-05 09:00:00'),

('INT-009', 'CUST009', 'GENERAL_INQUIRY',
 'New customer asked about available plans. Provided full plan catalogue summary with pricing and features. Suggested Basic 5GB as the best value entry point.',
 '2026-03-05 09:30:00'),

('INT-010', 'CUST010', 'PLAN_DOWNGRADE',
 'Customer considering cost saving options. Currently on Ultimate Unlimited ($69.99). Explained trade-offs of downgrading to Premium 30GB ($49.99). Advised against given current usage of 38.9GB.',
 '2026-03-05 10:00:00');

-- =============================================================================
-- Verification queries (uncomment to run after INSERT)
-- =============================================================================
-- SELECT COUNT(*) AS plan_count        FROM plans_catalog;       -- expect 12
-- SELECT COUNT(*) AS promo_count       FROM promotions;          -- expect 6
-- SELECT COUNT(*) AS customer_count    FROM customers;           -- expect 10
-- SELECT COUNT(*) AS usage_count       FROM customer_usage;      -- expect 10
-- SELECT COUNT(*) AS txn_count         FROM plan_transactions;   -- expect 10
-- SELECT COUNT(*) AS interaction_count FROM copilot_interactions; -- expect 10

-- Quick customer-plan join check:
-- SELECT c.customer_id, c.name, p.plan_name, c.tenure_months,
--        u.data_used_gb, p.data_limit_gb
-- FROM   customers c
-- JOIN   plans_catalog p ON c.current_plan_id = p.plan_id
-- JOIN   customer_usage u ON c.customer_id    = u.customer_id
-- ORDER  BY c.customer_id;

