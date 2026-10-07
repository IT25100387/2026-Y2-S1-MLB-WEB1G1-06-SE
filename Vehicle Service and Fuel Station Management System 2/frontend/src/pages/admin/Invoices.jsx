import {InvoiceList} from '../../components/BillingPages';
import {useAuth} from '../../context/AuthContext';
import ManagerInvoices from '../manager/ManagerInvoices';
export default function Page(){const {account}=useAuth();return ['ROLE_ADMIN','ROLE_MANAGER'].includes(account?.role)?<ManagerInvoices/>:<InvoiceList customer={false}/>;}
