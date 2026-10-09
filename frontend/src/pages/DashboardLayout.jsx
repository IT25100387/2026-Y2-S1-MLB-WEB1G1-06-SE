import PortalLayout from '../components/PortalLayout';
import ManagerPortalLayout from '../components/ManagerPortalLayout';
import MechanicPortalLayout from '../components/MechanicPortalLayout';
import {useAuth} from '../context/AuthContext';
import AdminPortalLayout from '../components/AdminPortalLayout';
import CashierPortalLayout from '../components/CashierPortalLayout';
import PortalTheme from '../components/PortalTheme';
export default function DashboardLayout(){const {account}=useAuth();if(account?.role==='ROLE_CASHIER')return <CashierPortalLayout/>;let layout=<PortalLayout/>;if(account?.role==='ROLE_MANAGER')layout=<ManagerPortalLayout/>;else if(account?.role==='ROLE_MECHANIC')layout=<MechanicPortalLayout/>;else if(account?.role==='ROLE_ADMIN')layout=<AdminPortalLayout/>;return <PortalTheme>{layout}</PortalTheme>;}
