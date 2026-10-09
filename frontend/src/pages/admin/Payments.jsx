import {PaymentList} from '../../components/BillingPages';
import {useAuth} from '../../context/AuthContext';
import ManagerPayments from '../manager/ManagerPayments';
export default function Page(){const {account}=useAuth();return ['ROLE_ADMIN','ROLE_MANAGER'].includes(account?.role)?<ManagerPayments/>:<PaymentList customer={false}/>;}
