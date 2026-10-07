-- ============================================================================
-- Vehicle Service and Fuel Station Management System
-- Microsoft SQL Server Sample Data Script
-- Target Database: fuelstationdb
-- Purpose: Deterministic sample data with explicit IDs using IDENTITY_INSERT
-- ============================================================================

USE fuelstationdb;
GO

-- Disable foreign key constraints temporarily to allow clean data insertion
EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT ALL";
GO

-- ----------------------------------------------------------------------------
-- 1. CLEAN EXISTING DATA
-- ----------------------------------------------------------------------------
DELETE FROM dbo.notification;
DELETE FROM dbo.customer_feedback;
DELETE FROM dbo.payment_record;
DELETE FROM dbo.invoice;
DELETE FROM dbo.job_card;
DELETE FROM dbo.service_booking;
DELETE FROM dbo.vehicle;
DELETE FROM dbo.service_bay;
DELETE FROM dbo.leave_request;
DELETE FROM dbo.attendance_record;
DELETE FROM dbo.shift_schedule;
DELETE FROM dbo.staff;
DELETE FROM dbo.fuel_sale;
DELETE FROM dbo.fuel_pump;
DELETE FROM dbo.fuel_price;
DELETE FROM dbo.spare_part;
DELETE FROM dbo.fuel_inventory;
DELETE FROM dbo.supplier;
DELETE FROM dbo.app_user;
GO

-- ----------------------------------------------------------------------------
-- 2. INSERT SAMPLE DATA WITH EXPLICIT IDENTIFIERS
-- ----------------------------------------------------------------------------

-- Table 1: App Users
SET IDENTITY_INSERT dbo.app_user ON;
INSERT INTO dbo.app_user (id, username, password, full_name, email, role, status, created_at) VALUES
(1, 'admin',    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Milinda Kalum',     'admin@fuelcore.com',    'Admin',    'Active', CURRENT_TIMESTAMP),
(2, 'manager',  '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Sunil Perera',      'manager@fuelcore.com',  'Manager',  'Active', CURRENT_TIMESTAMP),
(3, 'cashier',  '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Kamal Fernando',    'cashier@fuelcore.com',  'Cashier',  'Active', CURRENT_TIMESTAMP),
(4, 'mechanic', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Nimal Jayawardena', 'mechanic@fuelcore.com', 'Mechanic', 'Active', CURRENT_TIMESTAMP),
(5, 'customer', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a', 'Kasun Dias',        'kasun@gmail.com',       'Customer', 'Active', CURRENT_TIMESTAMP);
SET IDENTITY_INSERT dbo.app_user OFF;
GO

-- Table 2: Suppliers
SET IDENTITY_INSERT dbo.supplier ON;
INSERT INTO dbo.supplier (id, supplier_name, company, contact_number, email, fuel_types_supplied, delivery_schedule, status) VALUES
(1, 'Ceylon Petroleum Corp', 'CPC Sri Lanka', '+94 11 243 0000', 'orders@ceypetco.gov.lk', 'Lanka Petrol 92, Lanka Petrol 95, Lanka Auto Diesel, Lanka Super Diesel, Lanka Kerosene', 'Weekly Every Tuesday', 'Active'),
(2, 'Lanka IOC PLC', 'LIOC Colombo', '+94 11 247 5000', 'distribution@lankaioc.com', 'Lanka Petrol 92, Lanka Auto Diesel, Premium Lubricants', 'Bi-weekly on Demand', 'Active'),
(3, 'Toyota Lanka Genuine Parts', 'Toyota Lanka (Pvt) Ltd', '+94 11 293 9000', 'parts@toyota.lk', 'Engine Parts, Filters, Brake Pads, Oils', 'Monthly', 'Active'),
(4, 'Castrol Lubricants Lanka', 'Castrol Industrial', '+94 11 471 2000', 'supply@castrol.lk', 'Engine Oil 5W-30, Coolant, Transmission Fluids', 'Fortnightly', 'Active');
SET IDENTITY_INSERT dbo.supplier OFF;
GO

-- Table 3: Fuel Inventory (Underground Tanks)
SET IDENTITY_INSERT dbo.fuel_inventory ON;
INSERT INTO dbo.fuel_inventory (id, fuel_type, current_stock_litres, min_stock_warning, max_capacity_litres, supplier, supplier_id, last_delivery_date, status) VALUES
(1, 'Lanka Petrol 92',   7500.0, 1500.0, 12000.0, 'Ceylon Petroleum Corp', 1, DATEADD(DAY, -2, CURRENT_TIMESTAMP), 'Normal'),
(2, 'Lanka Petrol 95',   4200.0, 1000.0,  8000.0, 'Ceylon Petroleum Corp', 1, DATEADD(DAY, -4, CURRENT_TIMESTAMP), 'Normal'),
(3, 'Lanka Auto Diesel', 8800.0, 2000.0, 15000.0, 'Ceylon Petroleum Corp', 1, DATEADD(DAY, -1, CURRENT_TIMESTAMP), 'Normal'),
(4, 'Lanka Super Diesel', 850.0, 1000.0,  6000.0, 'Ceylon Petroleum Corp', 1, DATEADD(DAY, -6, CURRENT_TIMESTAMP), 'Low Stock'),
(5, 'Lanka Kerosene',    2500.0,  500.0,  5000.0, 'Ceylon Petroleum Corp', 1, DATEADD(DAY, -5, CURRENT_TIMESTAMP), 'Normal');
SET IDENTITY_INSERT dbo.fuel_inventory OFF;
GO

-- Table 4: Fuel Prices
SET IDENTITY_INSERT dbo.fuel_price ON;
INSERT INTO dbo.fuel_price (id, fuel_type, current_price, previous_price, last_updated, updated_by) VALUES
(1, 'Lanka Petrol 92',   340.0, 350.0, DATEADD(DAY, -3, CURRENT_TIMESTAMP), 'System Admin'),
(2, 'Lanka Petrol 95',   375.0, 380.0, DATEADD(DAY, -3, CURRENT_TIMESTAMP), 'System Admin'),
(3, 'Lanka Auto Diesel', 320.0, 330.0, DATEADD(DAY, -3, CURRENT_TIMESTAMP), 'System Admin'),
(4, 'Lanka Super Diesel', 360.0, 365.0, DATEADD(DAY, -3, CURRENT_TIMESTAMP), 'System Admin'),
(5, 'Lanka Kerosene',    220.0, 225.0, DATEADD(DAY, -3, CURRENT_TIMESTAMP), 'System Admin');
SET IDENTITY_INSERT dbo.fuel_price OFF;
GO

-- Table 5: Fuel Pumps
SET IDENTITY_INSERT dbo.fuel_pump ON;
INSERT INTO dbo.fuel_pump (id, pump_name, pump_number, fuel_type, location, status, assigned_operator, current_meter_reading, opening_reading, closing_reading, todays_sales) VALUES
(1, 'Pump 01', 'PUMP-A1', 'Lanka Petrol 92',   'Forecourt Island 1', 'Active', 'Ruwan Silva',     14250.0, 13800.0, 14250.0, 450.0),
(2, 'Pump 02', 'PUMP-A2', 'Lanka Petrol 95',   'Forecourt Island 1', 'Active', 'Ruwan Silva',      8520.0,  8200.0,  8520.0, 320.0),
(3, 'Pump 03', 'PUMP-B1', 'Lanka Auto Diesel', 'Forecourt Island 2', 'Active', 'Chaminda Perera', 21400.0, 20850.0, 21400.0, 550.0),
(4, 'Pump 04', 'PUMP-B2', 'Lanka Super Diesel','Forecourt Island 2', 'Active', 'Chaminda Perera',  6100.0,  5950.0,  6100.0, 150.0);
SET IDENTITY_INSERT dbo.fuel_pump OFF;
GO

-- Table 6: Fuel Sales
SET IDENTITY_INSERT dbo.fuel_sale ON;
INSERT INTO dbo.fuel_sale (id, receipt_number, sale_date, pump_id, pump_name, fuel_type, inventory_id, quantity_litres, current_price, total_amount, customer_name, vehicle_number, payment_method, cashier) VALUES
(1, 'REC-7A1B2C3D', DATEADD(HOUR, -6, CURRENT_TIMESTAMP), 1, 'Pump 01', 'Lanka Petrol 92',   1, 25.0, 340.0,  8500.0, 'Kasun Dias',     'WP-CAA-1234', 'Cash', 'Kamal Fernando'),
(2, 'REC-8B2C3D4E', DATEADD(HOUR, -5, CURRENT_TIMESTAMP), 2, 'Pump 02', 'Lanka Petrol 95',   2, 40.0, 375.0, 15000.0, 'Samantha Silva', 'WP-CAB-5678', 'Card', 'Kamal Fernando'),
(3, 'REC-9C3D4E5F', DATEADD(HOUR, -3, CURRENT_TIMESTAMP), 3, 'Pump 03', 'Lanka Auto Diesel', 3, 50.0, 320.0, 16000.0, 'Praveen Fonseka','WP-KX-9012',  'Cash', 'Kamal Fernando'),
(4, 'REC-0D4E5F6A', DATEADD(HOUR, -2, CURRENT_TIMESTAMP), 1, 'Pump 01', 'Lanka Petrol 92',   1, 15.5, 340.0,  5270.0, 'Dilshan Silva',  'WP-KV-3412',  'Card', 'Kamal Fernando'),
(5, 'REC-1E5F6A7B', DATEADD(HOUR, -1, CURRENT_TIMESTAMP), 4, 'Pump 04', 'Lanka Super Diesel',4, 30.0, 360.0, 10800.0, 'Mahesh Kumara',  'CP-HG-7821',  'Card', 'Kamal Fernando');
SET IDENTITY_INSERT dbo.fuel_sale OFF;
GO

-- Table 7: Spare Parts
SET IDENTITY_INSERT dbo.spare_part ON;
INSERT INTO dbo.spare_part (id, part_name, category, stock_quantity, min_stock_warning, buying_price, selling_price, supplier, supplier_id, status) VALUES
(1, 'Toyota Oil Filter (90915-YZZE1)',  'Filters',       45, 10,  1500.0,  2200.0, 'Toyota Lanka Genuine Parts', 3, 'In Stock'),
(2, 'Air Filter Element (17801-21050)',  'Filters',       28,  8,  2200.0,  3100.0, 'Toyota Lanka Genuine Parts', 3, 'In Stock'),
(3, 'Ceramic Brake Pads (Front Set)',   'Brake Parts',   18,  5,  6500.0,  8900.0, 'Toyota Lanka Genuine Parts', 3, 'In Stock'),
(4, 'Castrol EDGE 5W-30 (4 Litres)',     'Engine Oils',   32,  6,  9500.0, 12500.0, 'Castrol Lubricants Lanka',  4, 'In Stock'),
(5, 'Iridium Spark Plug (Set of 4)',    'Ignition',      14,  4,  4800.0,  6500.0, 'Toyota Lanka Genuine Parts', 3, 'In Stock'),
(6, 'Radiator Coolant Premium (1L)',    'Fluids',        50, 15,  1200.0,  1800.0, 'Castrol Lubricants Lanka',  4, 'In Stock'),
(7, 'Amaron 12V 45Ah Battery',          'Electrical',     4,  3, 24000.0, 29500.0, 'Toyota Lanka Genuine Parts', 3, 'Low Stock'),
(8, 'Bosch Wiper Blade Pair (24"/16")', 'Accessories',   25,  5,  2500.0,  3600.0, 'Toyota Lanka Genuine Parts', 3, 'In Stock');
SET IDENTITY_INSERT dbo.spare_part OFF;
GO

-- Table 8: Registered Vehicles
SET IDENTITY_INSERT dbo.vehicle ON;
INSERT INTO dbo.vehicle (id, license_plate, make, model, owner_name, owner_contact, registered_date) VALUES
(1, 'WP-CAA-1234', 'Toyota',     'Premio 2018',  'Kasun Dias',        '+94 77 123 4567', '2026-01-15'),
(2, 'WP-CAB-5678', 'Honda',      'Vezel 2020',   'Samantha Silva',    '+94 71 234 5678', '2026-02-10'),
(3, 'WP-KX-9012',  'Mitsubishi', 'Montero 2019', 'Praveen Fonseka',   '+94 76 345 6789', '2026-03-05'),
(4, 'WP-KV-3412',  'Suzuki',     'Wagon R 2018', 'Dilshan Silva',     '+94 70 456 7890', '2026-04-20'),
(5, 'CP-HG-7821',  'Nissan',     'X-Trail 2021', 'Mahesh Kumara',     '+94 75 567 8901', '2026-05-12'),
(6, 'WP-CAD-9988', 'Hyundai',    'Tucson 2022',  'Niroshan Bandara',  '+94 72 678 9012', '2026-06-18');
SET IDENTITY_INSERT dbo.vehicle OFF;
GO

-- Table 9: Service Bays
SET IDENTITY_INSERT dbo.service_bay ON;
INSERT INTO dbo.service_bay (id, bay_name, status) VALUES
(1, 'Bay 1 - Quick Service',      'Available'),
(2, 'Bay 2 - Full Service',       'Occupied'),
(3, 'Bay 3 - Wheel & Suspension', 'Available'),
(4, 'Bay 4 - Diagnostic & Repair','Under Maintenance');
SET IDENTITY_INSERT dbo.service_bay OFF;
GO

-- Table 10: Staff Members
SET IDENTITY_INSERT dbo.staff ON;
INSERT INTO dbo.staff (id, user_id, name, role, contact_details, assigned_station, employment_status) VALUES
(1, 2, 'Sunil Perera',       'Manager',    '+94 77 111 2233', 'Management Office',    'Active'),
(2, NULL, 'Achala Gunaratne', 'HR Officer', '+94 71 222 3344', 'HR Department',        'Active'),
(3, 4, 'Nimal Jayawardena',  'Mechanic',   '+94 76 333 4455', 'Bay 2 - Full Service', 'Active'),
(4, NULL, 'Dhanushka Prasad', 'Mechanic',   '+94 70 444 5566', 'Bay 1 - Quick Service', 'Active'),
(5, NULL, 'Ruwan Silva',      'Attendant',  '+94 75 555 6677', 'Forecourt Island 1',   'Active'),
(6, 3, 'Kamal Fernando',     'Cashier',    '+94 72 666 7788', 'Main POS Counter',      'Active');
SET IDENTITY_INSERT dbo.staff OFF;
GO

-- Table 11: Service Bookings
SET IDENTITY_INSERT dbo.service_booking ON;
INSERT INTO dbo.service_booking (id, vehicle_id, license_plate, customer_name, service_date, time_slot, service_type, status, notes) VALUES
(1, 1, 'WP-CAA-1234', 'Kasun Dias',      CAST(CURRENT_TIMESTAMP AS DATE), '09:00 AM', 'Full Service',   'In Progress', 'Engine vibration check required'),
(2, 2, 'WP-CAB-5678', 'Samantha Silva',  CAST(CURRENT_TIMESTAMP AS DATE), '11:00 AM', 'Oil Change',     'Completed',   'Castrol 5W-30 synthetic applied'),
(3, 3, 'WP-KX-9012',  'Praveen Fonseka', DATEADD(DAY, 1, CAST(CURRENT_TIMESTAMP AS DATE)), '01:00 PM', 'Full Service',   'Pending',     '100,000 km periodic maintenance'),
(4, 4, 'WP-KV-3412',  'Dilshan Silva',   DATEADD(DAY, 2, CAST(CURRENT_TIMESTAMP AS DATE)), '10:00 AM', 'Inspection',     'Pending',     'Pre-journey safety inspection'),
(5, 5, 'CP-HG-7821',  'Mahesh Kumara',   DATEADD(DAY, -3, CAST(CURRENT_TIMESTAMP AS DATE)),'02:00 PM', 'General Repair', 'Completed',   'Brake pad replacement completed');
SET IDENTITY_INSERT dbo.service_booking OFF;
GO

-- Table 12: Job Cards
SET IDENTITY_INSERT dbo.job_card ON;
INSERT INTO dbo.job_card (id, reference_number, license_plate, customer_name, mechanic_id, mechanic_name, bay_id, bay_name, service_type, status, mechanic_notes, parts_used) VALUES
(1, 1, 'WP-CAA-1234', 'Kasun Dias',    3, 'Nimal Jayawardena', 2, 'Bay 2 - Full Service',  'Full Service',    'In Progress', 'Drained transmission fluid; replacing air & oil filters.', '1x Toyota Oil Filter, 1x Air Filter'),
(2, 5, 'CP-HG-7821',  'Mahesh Kumara', 4, 'Dhanushka Prasad',  1, 'Bay 1 - Quick Service', 'General Repair',  'Completed',   'Fitted front brake pads, road-tested successfully.',       '1x Ceramic Brake Pads (Front Set)');
SET IDENTITY_INSERT dbo.job_card OFF;
GO

-- Table 13: Customer Feedback
SET IDENTITY_INSERT dbo.customer_feedback ON;
INSERT INTO dbo.customer_feedback (id, job_card_id, customer_name, service_type, rating, comments, needs_attention, submission_date) VALUES
(1, 2,    'Mahesh Kumara',     'General Repair', 5, 'Brakes feel very responsive now. Excellent work and quick delivery!', 0, DATEADD(DAY, -2, CURRENT_TIMESTAMP)),
(2, NULL, 'Samantha Silva',   'Oil Change',     4, 'Professional service, clean lounge with Wi-Fi while waiting.',        0, DATEADD(DAY, -1, CURRENT_TIMESTAMP)),
(3, NULL, 'Anura Dissanayake','Quick Wash',     2, 'Under-chassis wash was missed during rush hour. Needs attention.',      1, DATEADD(HOUR, -4, CURRENT_TIMESTAMP));
SET IDENTITY_INSERT dbo.customer_feedback OFF;
GO

-- Table 14: Invoices
SET IDENTITY_INSERT dbo.invoice ON;
INSERT INTO dbo.invoice (id, reference_number, invoice_name, customer_name, invoice_date, due_date, gross_total_amount, convenience_fee, net_total, amount_paid, status, customer_username, license_plate, customer_type, payment_method, overdue_penalty_rate, cash_tendered) VALUES
(1, 1, 'INV-2026-001', 'Advance Payment: Full Service', 'BKG-1', 'Kasun Dias',       CAST(CURRENT_TIMESTAMP AS DATE), DATEADD(DAY, 1, CAST(CURRENT_TIMESTAMP AS DATE)),   5000.0, 0.0,  5000.0,  5000.0, 'PAID'),
(2, 2, 'INV-2026-002', 'Routine Oil Change Package',    'BKG-2', 'Samantha Silva',   DATEADD(DAY, -1, CAST(CURRENT_TIMESTAMP AS DATE)), CAST(CURRENT_TIMESTAMP AS DATE),  15600.0, 0.0, 15600.0, 15600.0, 'PAID'),
(3, 5, 'INV-2026-003', 'Brake Pad Replacement Service', 'BKG-5', 'Mahesh Kumara',   DATEADD(DAY, -3, CAST(CURRENT_TIMESTAMP AS DATE)), DATEADD(DAY, -1, CAST(CURRENT_TIMESTAMP AS DATE)), 12500.0, 0.0, 12500.0,  6000.0, 'PARTIAL'),
(4, 3, 'INV-2026-004', 'Advance Payment: 100k Service', 'BKG-3', 'Praveen Fonseka',  CAST(CURRENT_TIMESTAMP AS DATE), DATEADD(DAY, 2, CAST(CURRENT_TIMESTAMP AS DATE)),   5000.0, 0.0,  5000.0,     0.0, 'OVERDUE');
SET IDENTITY_INSERT dbo.invoice OFF;
GO

-- Table 15: Payment Records
SET IDENTITY_INSERT dbo.payment_record ON;
INSERT INTO dbo.payment_record (id, reference_number, payment_date, paid_occupant, payment_method, amount) VALUES
(1, 1, CAST(CURRENT_TIMESTAMP AS DATE),                    'Kasun Dias',     'Cash',  5000.0),
(2, 2, DATEADD(DAY, -1, CAST(CURRENT_TIMESTAMP AS DATE)), 'Samantha Silva', 'Card',  15600.0),
(3, 3, DATEADD(DAY, -3, CAST(CURRENT_TIMESTAMP AS DATE)), 'Mahesh Kumara',   'Cash',  6000.0);
SET IDENTITY_INSERT dbo.payment_record OFF;
GO

-- Table 16: Shift Schedules
SET IDENTITY_INSERT dbo.shift_schedule ON;
INSERT INTO dbo.shift_schedule (id, staff_id, staff_name, shift_date, shift_timing, station, status) VALUES
(1, 1, 'Sunil Perera',      CAST(CURRENT_TIMESTAMP AS DATE), 'Morning (08:00 - 16:00)', 'Management Office',    'Scheduled'),
(2, 3, 'Nimal Jayawardena', CAST(CURRENT_TIMESTAMP AS DATE), 'Morning (08:00 - 16:00)', 'Bay 2 - Full Service', 'Scheduled'),
(3, 4, 'Dhanushka Prasad',  CAST(CURRENT_TIMESTAMP AS DATE), 'Evening (16:00 - 00:00)', 'Bay 1 - Quick Service', 'Scheduled'),
(4, 5, 'Ruwan Silva',       CAST(CURRENT_TIMESTAMP AS DATE), 'Morning (08:00 - 16:00)', 'Forecourt Island 1',   'Scheduled'),
(5, 6, 'Kamal Fernando',    CAST(CURRENT_TIMESTAMP AS DATE), 'Morning (08:00 - 16:00)', 'Main POS Counter',     'Scheduled');
SET IDENTITY_INSERT dbo.shift_schedule OFF;
GO

-- Table 17: Attendance Records
SET IDENTITY_INSERT dbo.attendance_record ON;
INSERT INTO dbo.attendance_record (id, staff_id, staff_name, attendance_date, clock_in_time, clock_out_time, status) VALUES
(1, 1, 'Sunil Perera',      CAST(CURRENT_TIMESTAMP AS DATE), '07:55:00', NULL, 'Present'),
(2, 3, 'Nimal Jayawardena', CAST(CURRENT_TIMESTAMP AS DATE), '08:02:00', NULL, 'Present'),
(3, 5, 'Ruwan Silva',       CAST(CURRENT_TIMESTAMP AS DATE), '07:50:00', NULL, 'Present'),
(4, 6, 'Kamal Fernando',    CAST(CURRENT_TIMESTAMP AS DATE), '08:00:00', NULL, 'Present');
SET IDENTITY_INSERT dbo.attendance_record OFF;
GO

-- Table 18: Leave Requests
SET IDENTITY_INSERT dbo.leave_request ON;
INSERT INTO dbo.leave_request (id, staff_id, staff_name, leave_type, start_date, end_date, reason, status) VALUES
(1, 4, 'Dhanushka Prasad',  'Sick Leave',   DATEADD(DAY, -5, CAST(CURRENT_TIMESTAMP AS DATE)), DATEADD(DAY, -4, CAST(CURRENT_TIMESTAMP AS DATE)), 'Viral fever with medical certificate', 'Approved'),
(2, 5, 'Ruwan Silva',       'Annual Leave', DATEADD(DAY, 7, CAST(CURRENT_TIMESTAMP AS DATE)),  DATEADD(DAY, 9, CAST(CURRENT_TIMESTAMP AS DATE)),  'Family wedding in Kandy',               'Pending'),
(3, 6, 'Kamal Fernando',    'Casual Leave', DATEADD(DAY, 14, CAST(CURRENT_TIMESTAMP AS DATE)), DATEADD(DAY, 14, CAST(CURRENT_TIMESTAMP AS DATE)), 'Personal bank affairs',               'Pending');
SET IDENTITY_INSERT dbo.leave_request OFF;
GO

-- Table 19: Notifications
SET IDENTITY_INSERT dbo.notification ON;
INSERT INTO dbo.notification (id, user_id, title, message, type, status, created_date) VALUES
(1, 1, 'Low Stock Alert',   'Super Diesel tank level is at 850L (below minimum 1000L warning). Reorder suggested.', 'Alert',   'Unread', DATEADD(HOUR, -5, CURRENT_TIMESTAMP)),
(2, 1, 'Booking Confirmed', 'Service booked for Kasun Dias (Vehicle: WP-CAA-1234) on today at 09:00 AM.',             'Booking', 'Unread', DATEADD(HOUR, -4, CURRENT_TIMESTAMP)),
(3, 1, 'Customer Review',   'Low rating (2/5) received for Quick Wash from customer Anura Dissanayake.',              'Alert',   'Unread', DATEADD(HOUR, -2, CURRENT_TIMESTAMP)),
(4, 2, 'Job Completed',     'Mechanic Dhanushka completed General Repair on vehicle CP-HG-7821.',                     'Service', 'Read',   DATEADD(HOUR, -1, CURRENT_TIMESTAMP));
SET IDENTITY_INSERT dbo.notification OFF;
GO

-- Re-enable Foreign Key Constraints with strict verification
EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL";
GO
