import {useState} from 'react';
import {motion} from 'framer-motion';
import {QuickDeck} from '../../components/DataUI';
import {ServiceHeader,ServiceFilters} from './ManagerServiceUI';
import useInventoryWorkspace from './inventory/useInventoryWorkspace';
import {InventoryBadge,InventoryMessages} from './inventory/InventoryUI';
import InventoryForm from './inventory/InventoryForm';
import {inventoryState,litres} from './inventory/inventoryValues';

export default function ManagerFuelTanks(){
  const workspace=useInventoryWorkspace('fuel'),[draft,setDraft]=useState(''),[query,setQuery]=useState(''),[status,setStatus]=useState('');
  const filters=<ServiceFilters label="Search tanks" draft={draft} setDraft={setDraft} query={query} onSearch={setQuery} status={status} onStatus={setStatus} statuses={[...new Set(['Optimal','Low Stock',...workspace.rows.map(row=>inventoryState('fuel',row.status))])]}><button className="btn-accent manager-service-primary" onClick={()=>workspace.setSelected({})}>Add tank</button></ServiceFilters>;
  const tanks=workspace.rows.filter(tank=>(!status||inventoryState('fuel',tank.status)===status)&&`${tank.fuelType} UST-${tank.id} ${tank.supplier||''}`.toLowerCase().includes(query.toLowerCase()));
  return <div className="manager-inventory manager-inventory-fuel"><ServiceHeader kicker="Inventory infrastructure" title="Fuel tanks.">{filters}</ServiceHeader><InventoryMessages workspace={workspace}/><QuickDeck>{filters}</QuickDeck>{workspace.loading?<p className="manager-inventory-empty" role="status">Loading fuel tanks...</p>:!tanks.length?<p className="manager-inventory-empty">NO RECORDS FOUND</p>:<div className="manager-tank-grid">{tanks.map((tank,index)=>{
    const percentage=Number(tank.maxCapacityLitres)>0?100*Number(tank.currentStockLitres)/Number(tank.maxCapacityLitres):0,fill=Math.max(0,Math.min(100,percentage));
    return <motion.section className="manager-tank-card" aria-label={tank.fuelType} key={tank.id} initial={{opacity:0,y:5}} animate={{opacity:1,y:0}} transition={{duration:.2,delay:index*.03}}><div className="manager-tank-top"><span>UST-{tank.id}</span><InventoryBadge kind="fuel" value={tank.status}/></div><h2>{tank.fuelType}</h2><div className="manager-tank-volume"><strong>{litres(tank.currentStockLitres)} <small>L</small></strong><span>{litres(tank.maxCapacityLitres)} L Max</span></div><div className={'manager-tank-meter '+(/Low|Critical/i.test(tank.status)?'is-low':'')} role="meter" aria-label={tank.fuelType+' tank level'} aria-valuemin={0} aria-valuemax={100} aria-valuenow={fill} aria-valuetext={litres(tank.currentStockLitres)+' litres, '+percentage.toFixed(1)+' percent full'}><span style={{width:fill+'%'}}/></div><div className="manager-tank-level"><span>{percentage.toFixed(1)}% full</span><span>Warning at {litres(tank.minStockWarning)} L</span></div>{tank.supplier&&<p className="manager-tank-supplier">Supplier: {tank.supplier}</p>}<div className="manager-tank-actions"><button className="manager-service-action" onClick={()=>workspace.setSelected({...tank})}>Edit</button><button className="manager-service-action" disabled={workspace.deleting!==null} onClick={()=>workspace.erase(tank)}>{workspace.deleting===tank.id?'Deleting…':'Delete'}</button></div></motion.section>;
  })}</div>}{workspace.selected&&<InventoryForm key={workspace.selected.id+'-'+workspace.selected.version} kind="fuel" initial={workspace.selected} workspace={workspace}/>}</div>;
}
