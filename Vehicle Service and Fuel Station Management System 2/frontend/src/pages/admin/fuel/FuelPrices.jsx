import {fuelGrades} from '../../../lib/fieldOptions';
import AdminFuelPrices from '../AdminFuelPrices';
import {useAuth} from '../../../context/AuthContext';
import CrudPage from '../../../components/CrudPage';
import {money} from '../../../components/DataUI';
export default function FuelPrices(){const {account}=useAuth(),editable=account?.role==='ROLE_MANAGER',root='/api/fuel-operations/prices';if(account?.role==='ROLE_ADMIN')return <AdminFuelPrices/>;return <CrudPage title="Fuel prices" endpoint={root} rowsKey="prices" create={editable?{url:root+'/add'}:undefined} update={editable?id=>({url:root+'/update/'+id}):undefined} remove={editable?id=>({url:root+'/delete/'+id,method:'POST'}):undefined} fields={[{key:'fuelType',label:'Fuel type',options:fuelGrades,allowCustom:true},{key:'currentPrice',label:'Price per litre (LKR)',type:'number',min:.01}]} columns={[{key:'fuelType',label:'Fuel',width:'20%'},{label:'Current LKR',width:'20%',render:p=>money(p.currentPrice)},{label:'Previous LKR',width:'15%',render:p=>money(p.previousPrice)},{key:'lastUpdated',label:'Updated',width:'15%'},{key:'updatedBy',label:'By',width:'10%'}]}/>;}
