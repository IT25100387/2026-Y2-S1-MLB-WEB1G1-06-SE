import {VehicleList} from '../../components/VehiclePages';
import {useAuth} from '../../context/AuthContext';
import ManagerVehicles from '../manager/ManagerVehicles';
export default function Page(){const {account}=useAuth();return account?.role==='ROLE_MANAGER'?<ManagerVehicles/>:<VehicleList customer={false}/>;}
