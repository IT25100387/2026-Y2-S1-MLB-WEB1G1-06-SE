import {ServiceHistory} from '../../components/VehiclePages';
import {useAuth} from '../../context/AuthContext';
import ManagerServiceHistory from '../manager/ManagerServiceHistory';
export default function Page(){const {account}=useAuth();return account?.role==='ROLE_MANAGER'?<ManagerServiceHistory/>:<ServiceHistory customer={false}/>;}
