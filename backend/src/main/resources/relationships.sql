-- ============================================================================
-- Vehicle Service and Fuel Station Management System
-- Microsoft SQL Server Database Relationships Script
-- Target Database: fuelstationdb
-- Purpose: Explicitly define, document, and enforce Foreign Key Constraints
-- ============================================================================

USE fuelstationdb;
GO

-- ----------------------------------------------------------------------------
-- MODULE 1: VEHICLE MANAGEMENT & SERVICE BOOKINGS
-- ----------------------------------------------------------------------------

-- Relationship 1: A Vehicle can have multiple Service Bookings (1 to Many)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_service_booking_vehicle')
BEGIN
    ALTER TABLE dbo.service_booking
    ADD CONSTRAINT FK_service_booking_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES dbo.vehicle(id)
    ON DELETE SET NULL;
END
GO

-- ----------------------------------------------------------------------------
-- MODULE 2: WORKSHOP & SERVICE MANAGEMENT
-- ----------------------------------------------------------------------------

-- Relationship 2: A Service Booking is linked to a Job Card (1 to 1 / 1 to Many)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_job_card_booking')
BEGIN
    ALTER TABLE dbo.job_card
    ADD CONSTRAINT FK_job_card_booking
    FOREIGN KEY (booking_id) REFERENCES dbo.service_booking(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 3: A Job Card is assigned to a Staff member (Mechanic) (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_job_card_mechanic')
BEGIN
    ALTER TABLE dbo.job_card
    ADD CONSTRAINT FK_job_card_mechanic
    FOREIGN KEY (mechanic_id) REFERENCES dbo.staff(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 4: A Job Card occupies a Service Bay (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_job_card_bay')
BEGIN
    ALTER TABLE dbo.job_card
    ADD CONSTRAINT FK_job_card_bay
    FOREIGN KEY (bay_id) REFERENCES dbo.service_bay(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 5: Customer Feedback references a completed Job Card (Many to 1 / 1 to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_customer_feedback_job_card')
BEGIN
    ALTER TABLE dbo.customer_feedback
    ADD CONSTRAINT FK_customer_feedback_job_card
    FOREIGN KEY (job_card_id) REFERENCES dbo.job_card(id)
    ON DELETE SET NULL;
END
GO

-- ----------------------------------------------------------------------------
-- MODULE 3: BILLING, PAYMENTS & SUPPLIERS
-- ----------------------------------------------------------------------------

-- Relationship 6: An Invoice can be tied to a Service Booking (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_invoice_booking')
BEGIN
    ALTER TABLE dbo.invoice
    ADD CONSTRAINT FK_invoice_booking
    FOREIGN KEY (booking_id) REFERENCES dbo.service_booking(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 7: An Invoice has multiple Payment Records (Split / Partial Payments) (1 to Many)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_payment_record_invoice')
BEGIN
    ALTER TABLE dbo.payment_record
    ADD CONSTRAINT FK_payment_record_invoice
    FOREIGN KEY (invoice_id) REFERENCES dbo.invoice(id)
    ON DELETE CASCADE;
END
GO

-- ----------------------------------------------------------------------------
-- MODULE 4: FUEL STATION & INVENTORY MANAGEMENT
-- ----------------------------------------------------------------------------

-- Relationship 8: Fuel Inventory tanks are supplied by a Supplier Vendor (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_fuel_inventory_supplier')
BEGIN
    ALTER TABLE dbo.fuel_inventory
    ADD CONSTRAINT FK_fuel_inventory_supplier
    FOREIGN KEY (supplier_id) REFERENCES dbo.supplier(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 9: Spare Parts are supplied by a Supplier Vendor (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_spare_part_supplier')
BEGIN
    ALTER TABLE dbo.spare_part
    ADD CONSTRAINT FK_spare_part_supplier
    FOREIGN KEY (supplier_id) REFERENCES dbo.supplier(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 10: A Fuel Sale is dispensed from a physical Fuel Pump (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_fuel_sale_pump')
BEGIN
    ALTER TABLE dbo.fuel_sale
    ADD CONSTRAINT FK_fuel_sale_pump
    FOREIGN KEY (pump_id) REFERENCES dbo.fuel_pump(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 11: A Fuel Sale draws stock from a Fuel Inventory Tank (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_fuel_sale_inventory')
BEGIN
    ALTER TABLE dbo.fuel_sale
    ADD CONSTRAINT FK_fuel_sale_inventory
    FOREIGN KEY (inventory_id) REFERENCES dbo.fuel_inventory(id)
    ON DELETE SET NULL;
END
GO

-- ----------------------------------------------------------------------------
-- MODULE 5: HUMAN RESOURCES, SHIFTS & ATTENDANCE
-- ----------------------------------------------------------------------------

-- Relationship 12: A Staff member can have a linked User Account (1 to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_staff_user')
BEGIN
    ALTER TABLE dbo.staff
    ADD CONSTRAINT FK_staff_user
    FOREIGN KEY (user_id) REFERENCES dbo.app_user(id)
    ON DELETE SET NULL;
END
GO

-- Relationship 13: Shift Schedules belong to a Staff member (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_shift_schedule_staff')
BEGIN
    ALTER TABLE dbo.shift_schedule
    ADD CONSTRAINT FK_shift_schedule_staff
    FOREIGN KEY (staff_id) REFERENCES dbo.staff(id)
    ON DELETE CASCADE;
END
GO

-- Relationship 14: Daily Attendance records belong to a Staff member (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_attendance_record_staff')
BEGIN
    ALTER TABLE dbo.attendance_record
    ADD CONSTRAINT FK_attendance_record_staff
    FOREIGN KEY (staff_id) REFERENCES dbo.staff(id)
    ON DELETE CASCADE;
END
GO

-- Relationship 15: Leave Requests are submitted by a Staff member (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_leave_request_staff')
BEGIN
    ALTER TABLE dbo.leave_request
    ADD CONSTRAINT FK_leave_request_staff
    FOREIGN KEY (staff_id) REFERENCES dbo.staff(id)
    ON DELETE CASCADE;
END
GO

-- ----------------------------------------------------------------------------
-- MODULE 6: USER & NOTIFICATION RELATIONS
-- ----------------------------------------------------------------------------

-- Relationship 16: System Notifications can target a specific User (Many to 1)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_notification_user')
BEGIN
    ALTER TABLE dbo.notification
    ADD CONSTRAINT FK_notification_user
    FOREIGN KEY (user_id) REFERENCES dbo.app_user(id)
    ON DELETE SET NULL;
END
GO
