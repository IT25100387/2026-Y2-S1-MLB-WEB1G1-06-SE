import {useAuth} from '../../../context/AuthContext';
import AdminAccounts from '../AdminAccounts';
import CrudPage from '../../../components/CrudPage';
import {Badge} from '../../../components/DataUI';
export default function Customers(){const {account}=useAuth();if(account?.role==='ROLE_ADMIN')return <AdminAccounts customer/>;return <CrudPage title="Customer directory" endpoint="/api/v1/customers" rowsKey="customers" columns={[{key:'username',label:'Account',width:'15%'},{key:'fullName',label:'Customer',width:'20%'},{key:'phoneNumber',label:'Phone',width:'15%'},{key:'email',label:'Email',width:'20%'},{label:'State',width:'10%',render:u=><Badge value={u.status}/>}]} />;}
