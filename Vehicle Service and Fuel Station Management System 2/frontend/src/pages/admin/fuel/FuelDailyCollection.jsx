import {money} from '../../../components/DataUI';
import {ServiceTable} from '../../manager/ManagerServiceUI';
import {useFuelOperations} from './FuelOperationsContext';
import FuelPageShell from './FuelPageShell';

export default function FuelDailyCollection(){
  const {data}=useFuelOperations();
  return <FuelPageShell section="daily-collection" title="Combined daily collection." description="Confirmed figures from every pump, brought together by station day.">
    {data&&<section id="daily-collection"><p className="manager-fuel-description">Current station day: <strong>{data.stationDay}</strong> · 05:00 to 05:00 · Asia/Colombo</p><div className="manager-fuel-summary">{[['Daily collection',data.dailyCollection,true],['Daily litres',data.dailyLitres,false],['Collection difference',data.dailyDifference,true]].map(([label,value,currency])=><div key={label}><p>{label}</p><strong>{currency?'Rs. ':''}{money(value)}{!currency?' L':''}</strong></div>)}</div><ServiceTable name="Daily collection" rows={[...data.collectionHistory].sort((a,b)=>b.date.localeCompare(a.date)).map(row=>({...row,id:row.date}))} columns={[{key:'date',label:'Station day',width:'30%'},{key:'confirmedPumps',label:'Pumps confirmed',width:'30%'},{key:'collectedAmount',label:'Collected',width:'35%',render:row=>`Rs. ${money(row.collectedAmount)}`}]} /></section>}
  </FuelPageShell>;
}
