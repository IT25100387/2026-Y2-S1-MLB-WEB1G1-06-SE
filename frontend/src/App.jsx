import React from 'react';
import {ValidationProvider} from './components/Validation';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Landing from './pages/Landing';
import DashboardLayout from './pages/DashboardLayout';
import CustomerDashboardLayout from './pages/CustomerDashboardLayout';
import DashboardOverview from './pages/Dashboard';
import Bookings from './pages/admin/Bookings';
import Vehicles from './pages/admin/Vehicles';
import ServiceHistory from './pages/admin/ServiceHistory';

import JobCards from './pages/admin/workshop/JobCards';
import BaySchedule from './pages/admin/workshop/BaySchedule';
import MechanicSchedule from './pages/admin/workshop/MechanicSchedule';
import FuelTanks from './pages/admin/inventory/FuelTanks';
import SpareParts from './pages/admin/inventory/SpareParts';
import Suppliers from './pages/admin/inventory/Suppliers';

import FuelSales from './pages/admin/fuel/FuelSales';
import FuelOperationsLayout from './pages/admin/fuel/FuelOperationsLayout';
import FuelPumpSettings from './pages/admin/fuel/FuelPumpSettings';
import FuelDailyCollection from './pages/admin/fuel/FuelDailyCollection';
import FuelPumpHistory from './pages/admin/fuel/FuelPumpHistory';
import FuelPrices from './pages/admin/fuel/FuelPrices';
import FuelInventoryStates from './pages/admin/fuel/FuelInventoryStates';
import SparePartOffers from './pages/admin/offers/SparePartOffers';
import ServiceOffers from './pages/admin/offers/ServiceOffers';
import ServiceCatalog from './pages/admin/offers/ServiceCatalog';
import Payments from './pages/admin/Payments';
import Invoices from './pages/admin/Invoices';
import {InvoiceDetails} from './components/BillingPages';

import UserManagement from './pages/admin/users/UserManagement';
import CustomerDirectory from './pages/admin/users/CustomerDirectory';
import AuditLogs from './pages/admin/users/AuditLogs';

import StaffDirectory from './pages/admin/hr/StaffDirectory';
import ShiftsAttendance from './pages/admin/hr/ShiftsAttendance';
import LeaveRequests from './pages/admin/hr/LeaveRequests';

import AdminCustomerFeedback from './pages/admin/support/CustomerFeedback';
import SupportTickets from './pages/admin/support/SupportTickets';

import CashierDashboard from './pages/cashier/CashierDashboard';
import MechanicDashboard from './pages/mechanic/MechanicDashboard';
import AccountSettings from './pages/AccountSettings';

import CustomerGarage from './pages/customer/CustomerGarage';
import CustomerServiceCatalog from './pages/customer/CustomerServiceCatalog';
import CustomerServiceBooking from './pages/customer/CustomerServiceBooking';
import CustomerAppointments from './pages/customer/CustomerAppointments';
import CustomerInvoices from './pages/customer/CustomerInvoices';
import CustomerInvoiceDetails from './pages/customer/CustomerInvoiceDetails';
import CustomerPayments from './pages/customer/CustomerPayments';
import CustomerSupport from './pages/customer/CustomerSupport';
import CustomerFeedback from './pages/customer/CustomerFeedback';
import CustomerVehicleDetails from './pages/customer/CustomerVehicleDetails';
import CustomerServiceHistory from './pages/customer/CustomerServiceHistory';
import CustomerProfile from './pages/customer/CustomerProfile';
import CustomerDashboard from './pages/customer/CustomerDashboard';
import CustomerStore from './pages/customer/CustomerStore';
import CustomerTracking from './pages/customer/CustomerTracking';
import CustomerFuel from './pages/customer/CustomerFuel';
import AuthPage from './pages/AuthPage';
import Notifications from './pages/Notifications';
import StaffSchedule from './pages/StaffSchedule';
import FeedbackHost from './components/FeedbackHost';
import {AuthProvider,RequireRole} from './context/AuthContext';
import {useAuth} from './context/AuthContext';
import {CashierVehicles,CashierCatalog,CashierInvoices,CashierInvoiceDetails} from './pages/cashier/CashierPages';
import {NotificationProvider} from './context/NotificationContext';

function CashierView({cashier,children}){const {account}=useAuth();return account?.role==='ROLE_CASHIER'?cashier:children;}
function App() {
  return (
    <Router>
      <ValidationProvider><AuthProvider><NotificationProvider><FeedbackHost/><Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<AuthPage mode="login"/>}/>
        <Route path="/signup" element={<AuthPage mode="signup"/>}/>
        <Route path="/forgot-password" element={<AuthPage mode="forgot"/>}/>
        <Route path="/reset-password" element={<AuthPage mode="reset"/>}/>
        
        {/* Dashboard Routes wrapped in the new Layout */}
        <Route path="/dashboard" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER","ROLE_CASHIER","ROLE_MECHANIC"]}><DashboardLayout /></RequireRole>}>
          <Route index element={<DashboardOverview />} />
          
          {/* Workshop Routes */}
          <Route path="workshop/job-cards" element={<RequireRole roles={["ROLE_MANAGER"]}><JobCards /></RequireRole>} />
          <Route path="workshop/bays" element={<RequireRole roles={["ROLE_MANAGER"]}><BaySchedule /></RequireRole>} />
          <Route path="workshop/mechanics" element={<RequireRole roles={["ROLE_MANAGER"]}><MechanicSchedule /></RequireRole>} />
          
          {/* Mechanic Routes */}
          <Route path="attendance" element={<RequireRole roles={["ROLE_MANAGER","ROLE_CASHIER","ROLE_MECHANIC"]}><Navigate to="/dashboard/profile/shifts" replace/></RequireRole>} />
          <Route path="profile/shifts" element={<RequireRole roles={["ROLE_MANAGER","ROLE_CASHIER","ROLE_MECHANIC"]}><StaffSchedule/></RequireRole>} />
          <Route path="profile/leave" element={<RequireRole roles={["ROLE_MANAGER","ROLE_CASHIER","ROLE_MECHANIC"]}><StaffSchedule/></RequireRole>} />
          <Route path="mechanic-workspace" element={<RequireRole roles={["ROLE_MECHANIC"]}><MechanicDashboard /></RequireRole>} />
          
          <Route path="bookings" element={<RequireRole roles={["ROLE_MANAGER", "ROLE_CASHIER"]}><Bookings /></RequireRole>} />
          <Route path="vehicles" element={<RequireRole roles={["ROLE_MANAGER", "ROLE_CASHIER"]}><CashierView cashier={<CashierVehicles/>}><Vehicles /></CashierView></RequireRole>} />
          <Route path="service-history" element={<RequireRole roles={["ROLE_MANAGER", "ROLE_CASHIER"]}><ServiceHistory /></RequireRole>} />
          
          {/* Inventory Routes */}
          <Route path="inventory/fuel" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><FuelTanks /></RequireRole>} />
          <Route path="inventory/parts" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><SpareParts /></RequireRole>} />
          <Route path="inventory/suppliers" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><Suppliers /></RequireRole>} />

          {/* Fuel Operations Routes */}
          <Route element={<RequireRole roles={["ROLE_MANAGER"]}><FuelOperationsLayout /></RequireRole>}>
            <Route path="fuel/sales" element={<FuelSales />} />
            <Route path="fuel/pumps" element={<FuelPumpSettings />} />
            <Route path="fuel/collection" element={<FuelDailyCollection />} />
            <Route path="fuel/history" element={<FuelPumpHistory />} />
          </Route>
          <Route path="fuel/prices" element={<RequireRole roles={["ROLE_MANAGER", "ROLE_CASHIER"]}><FuelPrices /></RequireRole>} />
          <Route path="fuel/inventory" element={<RequireRole roles={["ROLE_MANAGER"]}><FuelInventoryStates /></RequireRole>} />
          <Route path="offers/spare-parts" element={<RequireRole roles={["ROLE_ADMIN", "ROLE_MANAGER"]}><SparePartOffers /></RequireRole>} />
          <Route path="offers/services" element={<RequireRole roles={["ROLE_ADMIN", "ROLE_MANAGER"]}><ServiceOffers /></RequireRole>} />
          <Route path="offers/service-catalog" element={<RequireRole roles={["ROLE_ADMIN", "ROLE_MANAGER"]}><ServiceCatalog /></RequireRole>} />
          
          <Route path="invoices" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER", "ROLE_CASHIER"]}><CashierView cashier={<CashierInvoices/>}><Invoices /></CashierView></RequireRole>} />
          <Route path="invoices/:id" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER","ROLE_CASHIER"]}><CashierView cashier={<CashierInvoiceDetails/>}><InvoiceDetails/></CashierView></RequireRole>}/>
          <Route path="payments" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><Payments /></RequireRole>} />
          <Route path="cashier/customers" element={<RequireRole roles={["ROLE_CASHIER"]}><Navigate to="/dashboard/pos" replace/></RequireRole>} />
          <Route path="cashier/catalog" element={<RequireRole roles={["ROLE_CASHIER"]}><CashierCatalog/></RequireRole>} />
          <Route path="pos" element={<RequireRole roles={["ROLE_CASHIER"]}><CashierDashboard /></RequireRole>} />

          {/* Governance & Users Routes */}
          <Route path="users" element={<RequireRole roles={["ROLE_ADMIN"]}><UserManagement /></RequireRole>} />
          <Route path="customers" element={<RequireRole roles={["ROLE_ADMIN", "ROLE_MANAGER"]}><CustomerDirectory /></RequireRole>} />
          <Route path="audit-logs" element={<RequireRole roles={["ROLE_ADMIN"]}><AuditLogs /></RequireRole>} />

          {/* HR & Shifts Routes */}
          <Route path="hr/staff" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><StaffDirectory /></RequireRole>} />
          <Route path="hr/shifts" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><ShiftsAttendance /></RequireRole>} />
          <Route path="hr/leave" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><LeaveRequests /></RequireRole>} />
          
          {/* Support Routes */}
          <Route path="support/feedback" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><AdminCustomerFeedback /></RequireRole>} />
          <Route path="support/tickets" element={<RequireRole roles={["ROLE_ADMIN","ROLE_MANAGER"]}><SupportTickets /></RequireRole>} />

          {/* Account Settings Routes */}
          <Route path="profile" element={<AccountSettings />} />
          <Route path="profile/edit" element={<AccountSettings />} />
          <Route path="profile/security" element={<AccountSettings />} />
          <Route path="settings" element={<AccountSettings />} />
          <Route path="notifications" element={<Notifications />} />
        </Route>

        {/* Customer Portal Routes */}
        <Route path="/customer" element={<RequireRole roles={["ROLE_CUSTOMER"]}><CustomerDashboardLayout /></RequireRole>}>
          <Route index element={<CustomerDashboard />} />
          <Route path="vehicles" element={<CustomerGarage />} />
          <Route path="vehicles/:id" element={<CustomerVehicleDetails />} />
          
          <Route path="book" element={<CustomerServiceBooking />} />
          <Route path="catalog" element={<CustomerServiceCatalog />} />
          <Route path="track/:referenceNumber" element={<CustomerTracking/>} />
          <Route path="appointments" element={<CustomerAppointments />} />
          <Route path="service-history" element={<CustomerServiceHistory />} />
          <Route path="invoices" element={<CustomerInvoices />} />
          <Route path="invoices/:invoiceNumber" element={<CustomerInvoiceDetails />} />
          <Route path="payments" element={<CustomerPayments />} />
          <Route path="store" element={<CustomerStore />} />
          <Route path="fuel" element={<CustomerFuel />} />
          <Route path="offers" element={<Navigate to="/customer/catalog?offers=true" replace/>} />
          <Route path="support" element={<CustomerSupport />} />
          <Route path="feedback" element={<CustomerFeedback />} />
          <Route path="profile" element={<CustomerProfile />} />

          {/* Customer Account Settings */}
          <Route path="profile/edit" element={<CustomerProfile />} />
          <Route path="profile/security" element={<CustomerProfile />} />
          <Route path="settings" element={<CustomerProfile />} />
          <Route path="notifications" element={<Notifications />} />
        </Route>
        <Route path="*" element={<div className="p-12 text-center">Page not found. <a href="/">Return home</a></div>}/>
      </Routes></NotificationProvider></AuthProvider></ValidationProvider>
    </Router>
  );
}

export default App;
