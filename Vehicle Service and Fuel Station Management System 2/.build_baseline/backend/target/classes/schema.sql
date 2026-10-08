-- ============================================================================
-- Vehicle Service and Fuel Station Management System
-- Microsoft SQL Server Database Schema Script
-- Target Database: fuelstationdb
-- ============================================================================

USE fuelstationdb;
GO

-- ----------------------------------------------------------------------------
-- 1. DROP EXISTING FOREIGN KEYS AND TABLES IF RECREATING
-- ----------------------------------------------------------------------------
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += N'ALTER TABLE ' + QUOTENAME(OBJECT_SCHEMA_NAME(parent_object_id)) 
    + N'.' + QUOTENAME(OBJECT_NAME(parent_object_id)) 
    + N' DROP CONSTRAINT ' + QUOTENAME(name) + N';' + CHAR(13)
FROM sys.foreign_keys;
EXEC sp_executesql @sql;
GO

IF OBJECT_ID('dbo.notification', 'U') IS NOT NULL DROP TABLE dbo.notification;
IF OBJECT_ID('dbo.customer_feedback', 'U') IS NOT NULL DROP TABLE dbo.customer_feedback;
IF OBJECT_ID('dbo.payment_record', 'U') IS NOT NULL DROP TABLE dbo.payment_record;
IF OBJECT_ID('dbo.invoice', 'U') IS NOT NULL DROP TABLE dbo.invoice;
IF OBJECT_ID('dbo.job_card', 'U') IS NOT NULL DROP TABLE dbo.job_card;
IF OBJECT_ID('dbo.service_booking', 'U') IS NOT NULL DROP TABLE dbo.service_booking;
IF OBJECT_ID('dbo.vehicle', 'U') IS NOT NULL DROP TABLE dbo.vehicle;
IF OBJECT_ID('dbo.service_bay', 'U') IS NOT NULL DROP TABLE dbo.service_bay;
IF OBJECT_ID('dbo.leave_request', 'U') IS NOT NULL DROP TABLE dbo.leave_request;
IF OBJECT_ID('dbo.attendance_record', 'U') IS NOT NULL DROP TABLE dbo.attendance_record;
IF OBJECT_ID('dbo.shift_schedule', 'U') IS NOT NULL DROP TABLE dbo.shift_schedule;
IF OBJECT_ID('dbo.staff', 'U') IS NOT NULL DROP TABLE dbo.staff;
IF OBJECT_ID('dbo.fuel_sale', 'U') IS NOT NULL DROP TABLE dbo.fuel_sale;
IF OBJECT_ID('dbo.fuel_pump', 'U') IS NOT NULL DROP TABLE dbo.fuel_pump;
IF OBJECT_ID('dbo.fuel_price', 'U') IS NOT NULL DROP TABLE dbo.fuel_price;
IF OBJECT_ID('dbo.spare_part', 'U') IS NOT NULL DROP TABLE dbo.spare_part;
IF OBJECT_ID('dbo.fuel_inventory', 'U') IS NOT NULL DROP TABLE dbo.fuel_inventory;
IF OBJECT_ID('dbo.supplier', 'U') IS NOT NULL DROP TABLE dbo.supplier;
IF OBJECT_ID('dbo.app_user', 'U') IS NOT NULL DROP TABLE dbo.app_user;
GO

-- ----------------------------------------------------------------------------
-- 2. CREATE TABLES WITH PRIMARY KEYS AND CONSTRAINTS
-- ----------------------------------------------------------------------------

-- Table 1: System Users (Authentication & Authorization)
CREATE TABLE dbo.app_user (
    id BIGINT IDENTITY(1,1) NOT NULL,
    username NVARCHAR(50) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(100) NOT NULL,
    role NVARCHAR(50) NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT DF_app_user_status DEFAULT 'Active',
    created_at DATETIME2 NOT NULL CONSTRAINT DF_app_user_created DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_app_user PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_app_user_username UNIQUE (username),
    CONSTRAINT CK_app_user_role CHECK (role IN ('Admin', 'Manager', 'Cashier', 'Mechanic', 'Customer', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_CASHIER', 'ROLE_MECHANIC', 'ROLE_CUSTOMER')),
    CONSTRAINT CK_app_user_status CHECK (status IN ('Active', 'Deactivated', 'Inactive', 'ACTIVE', 'DEACTIVATED', 'INACTIVE'))
);

-- Table 2: Suppliers (Fuel & Spare Parts Vendors)
CREATE TABLE dbo.supplier (
    id BIGINT IDENTITY(1,1) NOT NULL,
    supplier_name NVARCHAR(100) NOT NULL,
    company NVARCHAR(100) NOT NULL,
    contact_number NVARCHAR(20) NULL,
    email NVARCHAR(100) NULL,
    fuel_types_supplied NVARCHAR(255) NULL,
    delivery_schedule NVARCHAR(100) NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_supplier_status DEFAULT 'Active',
    CONSTRAINT PK_supplier PRIMARY KEY CLUSTERED (id)
);

-- Table 3: Fuel Inventory (Underground Storage Tanks)
CREATE TABLE dbo.fuel_inventory (
    id BIGINT IDENTITY(1,1) NOT NULL,
    fuel_type NVARCHAR(50) NOT NULL,
    current_stock_litres FLOAT NOT NULL CONSTRAINT DF_fuel_inventory_current DEFAULT 0.0,
    min_stock_warning FLOAT NOT NULL CONSTRAINT DF_fuel_inventory_min DEFAULT 1000.0,
    max_capacity_litres FLOAT NOT NULL CONSTRAINT DF_fuel_inventory_max DEFAULT 10000.0,
    supplier NVARCHAR(100) NULL,
    supplier_id BIGINT NULL,
    last_delivery_date DATETIME2 NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_fuel_inventory_status DEFAULT 'Normal',
    CONSTRAINT PK_fuel_inventory PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_fuel_inventory_fuel_type UNIQUE (fuel_type),
    CONSTRAINT CK_fuel_inventory_stock CHECK (current_stock_litres >= 0),
    CONSTRAINT CK_fuel_inventory_capacity CHECK (max_capacity_litres >= current_stock_litres)
);

-- Table 4: Fuel Pricing
CREATE TABLE dbo.fuel_price (
    id BIGINT IDENTITY(1,1) NOT NULL,
    fuel_type NVARCHAR(50) NOT NULL,
    current_price FLOAT NOT NULL,
    previous_price FLOAT NULL,
    last_updated DATETIME2 NOT NULL CONSTRAINT DF_fuel_price_updated DEFAULT CURRENT_TIMESTAMP,
    updated_by NVARCHAR(100) NULL,
    CONSTRAINT PK_fuel_price PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_fuel_price_positive CHECK (current_price >= 0)
);

-- Table 5: Fuel Dispensing Pumps
CREATE TABLE dbo.fuel_pump (
    id BIGINT IDENTITY(1,1) NOT NULL,
    pump_name NVARCHAR(50) NOT NULL,
    pump_number NVARCHAR(50) NOT NULL,
    fuel_type NVARCHAR(50) NOT NULL,
    location NVARCHAR(100) NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_fuel_pump_status DEFAULT 'Active',
    assigned_operator NVARCHAR(100) NULL,
    current_meter_reading FLOAT NOT NULL CONSTRAINT DF_fuel_pump_meter DEFAULT 0.0,
    opening_reading FLOAT NOT NULL CONSTRAINT DF_fuel_pump_opening DEFAULT 0.0,
    closing_reading FLOAT NOT NULL CONSTRAINT DF_fuel_pump_closing DEFAULT 0.0,
    todays_sales FLOAT NOT NULL CONSTRAINT DF_fuel_pump_sales DEFAULT 0.0,
    CONSTRAINT PK_fuel_pump PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_fuel_pump_pump_number UNIQUE (pump_number),
    CONSTRAINT CK_fuel_pump_status CHECK (status IN ('Active', 'Maintenance', 'Disabled'))
);

-- Table 6: Fuel Sales (Point-of-Sale Transactions)
CREATE TABLE dbo.fuel_sale (
    id BIGINT IDENTITY(1,1) NOT NULL,
    receipt_number NVARCHAR(50) NOT NULL,
    sale_date DATETIME2 NOT NULL CONSTRAINT DF_fuel_sale_date DEFAULT CURRENT_TIMESTAMP,
    pump_id BIGINT NULL,
    pump_name NVARCHAR(50) NULL,
    fuel_type NVARCHAR(50) NOT NULL,
    inventory_id BIGINT NULL,
    quantity_litres FLOAT NOT NULL,
    current_price FLOAT NOT NULL,
    total_amount FLOAT NOT NULL,
    customer_name NVARCHAR(100) NULL,
    vehicle_number NVARCHAR(50) NULL,
    payment_method NVARCHAR(50) NOT NULL CONSTRAINT DF_fuel_sale_pay_method DEFAULT 'Cash',
    cashier NVARCHAR(100) NULL,
    CONSTRAINT PK_fuel_sale PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_fuel_sale_receipt_number UNIQUE (receipt_number),
    CONSTRAINT CK_fuel_sale_quantity CHECK (quantity_litres > 0),
    CONSTRAINT CK_fuel_sale_total CHECK (total_amount >= 0)
);

-- Table 7: Spare Parts Inventory
CREATE TABLE dbo.spare_part (
    id BIGINT IDENTITY(1,1) NOT NULL,
    part_name NVARCHAR(100) NOT NULL,
    category NVARCHAR(50) NOT NULL,
    stock_quantity INT NOT NULL CONSTRAINT DF_spare_part_stock DEFAULT 0,
    min_stock_warning INT NOT NULL CONSTRAINT DF_spare_part_min DEFAULT 5,
    buying_price FLOAT NOT NULL CONSTRAINT DF_spare_part_buy DEFAULT 0.0,
    selling_price FLOAT NOT NULL CONSTRAINT DF_spare_part_sell DEFAULT 0.0,
    supplier NVARCHAR(100) NULL,
    supplier_id BIGINT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_spare_part_status DEFAULT 'In Stock',
    CONSTRAINT PK_spare_part PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_spare_part_stock CHECK (stock_quantity >= 0),
    CONSTRAINT CK_spare_part_prices CHECK (buying_price >= 0 AND selling_price >= 0)
);

-- Table 8: Registered Customer Vehicles
CREATE TABLE dbo.vehicle (
    id BIGINT IDENTITY(1,1) NOT NULL,
    license_plate NVARCHAR(20) NOT NULL,
    make NVARCHAR(50) NOT NULL,
    model NVARCHAR(50) NOT NULL,
    owner_name NVARCHAR(100) NOT NULL,
    owner_contact NVARCHAR(50) NOT NULL,
    registered_date NVARCHAR(50) NOT NULL,
    CONSTRAINT PK_vehicle PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_vehicle_license_plate UNIQUE (license_plate)
);

-- Table 9: Service Bays (Workshop Physical Bays)
CREATE TABLE dbo.service_bay (
    id BIGINT IDENTITY(1,1) NOT NULL,
    bay_name NVARCHAR(50) NOT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_service_bay_status DEFAULT 'Available',
    CONSTRAINT PK_service_bay PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_service_bay_bay_name UNIQUE (bay_name),
    CONSTRAINT CK_service_bay_status CHECK (status IN ('Available', 'Occupied', 'Under Maintenance'))
);

-- Table 10: Staff Directory (Human Resources)
CREATE TABLE dbo.staff (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NULL,
    name NVARCHAR(100) NOT NULL,
    role NVARCHAR(50) NOT NULL,
    contact_details NVARCHAR(100) NULL,
    assigned_station NVARCHAR(100) NULL,
    employment_status NVARCHAR(50) NOT NULL CONSTRAINT DF_staff_status DEFAULT 'Active',
    CONSTRAINT PK_staff PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_staff_status CHECK (employment_status IN ('Active', 'On Leave', 'Inactive'))
);

-- Table 11: Service Bookings (Appointments)
CREATE TABLE dbo.service_booking (
    id BIGINT IDENTITY(1,1) NOT NULL,
    vehicle_id BIGINT NULL,
    license_plate NVARCHAR(20) NOT NULL,
    customer_name NVARCHAR(100) NOT NULL,
    service_date DATE NOT NULL,
    time_slot NVARCHAR(50) NOT NULL,
    service_type NVARCHAR(50) NOT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_service_booking_status DEFAULT 'Pending',
    notes NVARCHAR(500) NULL,
    CONSTRAINT PK_service_booking PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_service_booking_status CHECK (status IN ('Pending', 'Approved', 'Assigned', 'In Progress', 'Completed', 'Cancelled', 'PENDING', 'APPROVED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

-- Table 12: Workshop Job Cards
CREATE TABLE dbo.job_card (
    id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NULL,
    license_plate NVARCHAR(20) NULL,
    customer_name NVARCHAR(100) NULL,
    mechanic_id BIGINT NULL,
    mechanic_name NVARCHAR(100) NULL,
    bay_id BIGINT NULL,
    bay_name NVARCHAR(50) NULL,
    service_type NVARCHAR(50) NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_job_card_status DEFAULT 'In Progress',
    mechanic_notes NVARCHAR(MAX) NULL,
    parts_used NVARCHAR(MAX) NULL,
    CONSTRAINT PK_job_card PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_job_card_status CHECK (status IN ('Pending', 'In Progress', 'Waiting for Parts', 'Quality Check', 'Completed'))
);

-- Table 13: Customer Feedback & Reviews
CREATE TABLE dbo.customer_feedback (
    id BIGINT IDENTITY(1,1) NOT NULL,
    job_card_id BIGINT NULL,
    customer_name NVARCHAR(100) NOT NULL,
    service_type NVARCHAR(50) NOT NULL,
    rating INT NOT NULL,
    comments NVARCHAR(MAX) NULL,
    needs_attention BIT NOT NULL CONSTRAINT DF_customer_feedback_attention DEFAULT 0,
    submission_date DATETIME2 NOT NULL CONSTRAINT DF_customer_feedback_date DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_customer_feedback PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_customer_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Table 14: Billing Invoices
CREATE TABLE dbo.invoice (
    id BIGINT IDENTITY(1,1) NOT NULL,
    booking_id BIGINT NULL,
    invoice_no NVARCHAR(50) NOT NULL,
    invoice_name NVARCHAR(100) NOT NULL,
    reference_no NVARCHAR(50) NULL,
    customer_name NVARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    gross_total_amount FLOAT NOT NULL,
    convenience_fee FLOAT NOT NULL CONSTRAINT DF_invoice_fee DEFAULT 0.0,
    net_total FLOAT NOT NULL,
    amount_paid FLOAT NOT NULL CONSTRAINT DF_invoice_paid DEFAULT 0.0,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_invoice_status DEFAULT 'PENDING',
    CONSTRAINT PK_invoice PRIMARY KEY CLUSTERED (id),
    CONSTRAINT UQ_invoice_invoice_no UNIQUE (invoice_no),
    CONSTRAINT CK_invoice_status CHECK (status IN ('PAID', 'OVERDUE', 'PARTIAL', 'PENDING'))
);

-- Table 15: Payment Records (Split & Full Payments)
CREATE TABLE dbo.payment_record (
    id BIGINT IDENTITY(1,1) NOT NULL,
    invoice_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    paid_occupant NVARCHAR(100) NOT NULL,
    payment_method NVARCHAR(50) NOT NULL CONSTRAINT DF_payment_record_method DEFAULT 'Cash',
    amount FLOAT NOT NULL,
    CONSTRAINT PK_payment_record PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_payment_record_amount CHECK (amount > 0)
);

-- Table 16: Staff Shift Schedules
CREATE TABLE dbo.shift_schedule (
    id BIGINT IDENTITY(1,1) NOT NULL,
    staff_id BIGINT NOT NULL,
    staff_name NVARCHAR(100) NULL,
    shift_date DATE NOT NULL,
    shift_timing NVARCHAR(50) NOT NULL,
    station NVARCHAR(100) NOT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_shift_schedule_status DEFAULT 'Scheduled',
    CONSTRAINT PK_shift_schedule PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_shift_schedule_status CHECK (status IN ('Scheduled', 'Completed', 'Cancelled'))
);

-- Table 17: Staff Attendance Records (Clock-In / Clock-Out)
CREATE TABLE dbo.attendance_record (
    id BIGINT IDENTITY(1,1) NOT NULL,
    staff_id BIGINT NOT NULL,
    staff_name NVARCHAR(100) NULL,
    attendance_date DATE NOT NULL,
    clock_in_time TIME NOT NULL,
    clock_out_time TIME NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_attendance_record_status DEFAULT 'Present',
    CONSTRAINT PK_attendance_record PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_attendance_record_status CHECK (status IN ('Present', 'Absent', 'Late'))
);

-- Table 18: Staff Leave Requests
CREATE TABLE dbo.leave_request (
    id BIGINT IDENTITY(1,1) NOT NULL,
    staff_id BIGINT NOT NULL,
    staff_name NVARCHAR(100) NULL,
    leave_type NVARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason NVARCHAR(500) NOT NULL,
    status NVARCHAR(50) NOT NULL CONSTRAINT DF_leave_request_status DEFAULT 'Pending',
    CONSTRAINT PK_leave_request PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_leave_request_status CHECK (status IN ('Pending', 'Approved', 'Rejected')),
    CONSTRAINT CK_leave_request_dates CHECK (end_date >= start_date)
);

-- Table 19: System Notifications & Alerts
CREATE TABLE dbo.notification (
    id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NULL,
    title NVARCHAR(100) NOT NULL,
    message NVARCHAR(500) NOT NULL,
    type NVARCHAR(50) NOT NULL,
    status NVARCHAR(20) NOT NULL CONSTRAINT DF_notification_status DEFAULT 'Unread',
    created_date DATETIME2 NOT NULL CONSTRAINT DF_notification_date DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT PK_notification PRIMARY KEY CLUSTERED (id),
    CONSTRAINT CK_notification_status CHECK (status IN ('Unread', 'Read'))
);
GO

-- ----------------------------------------------------------------------------
-- 3. PERFORMANCE INDEXES
-- ----------------------------------------------------------------------------
CREATE NONCLUSTERED INDEX IX_service_booking_vehicle ON dbo.service_booking(vehicle_id);
CREATE NONCLUSTERED INDEX IX_service_booking_date_slot ON dbo.service_booking(service_date, time_slot);
CREATE NONCLUSTERED INDEX IX_job_card_booking ON dbo.job_card(booking_id);
CREATE NONCLUSTERED INDEX IX_job_card_mechanic ON dbo.job_card(mechanic_id);
CREATE NONCLUSTERED INDEX IX_job_card_bay ON dbo.job_card(bay_id);
CREATE NONCLUSTERED INDEX IX_payment_record_invoice ON dbo.payment_record(invoice_id);
CREATE NONCLUSTERED INDEX IX_fuel_sale_pump ON dbo.fuel_sale(pump_id);
CREATE NONCLUSTERED INDEX IX_fuel_sale_date ON dbo.fuel_sale(sale_date);
CREATE NONCLUSTERED INDEX IX_attendance_staff_date ON dbo.attendance_record(staff_id, attendance_date);
CREATE NONCLUSTERED INDEX IX_shift_staff_date ON dbo.shift_schedule(staff_id, shift_date);
CREATE NONCLUSTERED INDEX IX_leave_staff ON dbo.leave_request(staff_id);
GO
