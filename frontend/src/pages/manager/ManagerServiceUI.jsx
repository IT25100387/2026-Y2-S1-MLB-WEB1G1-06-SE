import {Input,Select} from '../../components/Validation';
import {useState} from 'react';
import {Link} from 'react-router-dom';
import {motion} from 'framer-motion';
import {Badge,money} from '../../components/DataUI';
import './ManagerService.css';
import {usePortalTheme} from '../../components/PortalTheme';
import {exportTableToCsv,getPaginationRange,formatTimeRange} from '../../lib/tableUtils';

export function ServiceHeader({kicker,title,subtext,actions,children}){
  return <div className="manager-service-heading">
    <div>
      <p className="manager-service-kicker">{kicker}</p>
      <h1>{title}</h1>
      {subtext&&<p className="manager-service-balance">{subtext}</p>}
    </div>
    <div className="manager-service-heading-actions">
      {actions}
      {children}
    </div>
  </div>;
}
export function ServiceFilters({label,draft,setDraft,query,onSearch,status='',onStatus,statuses=[],children}){
  return <div className="manager-service-filters"><form noValidate className="manager-service-search" onSubmit={event=>{event.preventDefault();onSearch(draft.trim());}}><Input aria-label={label} placeholder={label?label+'...':'Search...'} className="input-dark" value={draft} onChange={event=>setDraft(event.target.value)}/>{draft&&<button type="button" aria-label="Clear search text" onClick={()=>{setDraft('');onSearch('');}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}<button type="submit" aria-label="Search"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button></form>{onStatus&&<div className="manager-service-select"><Select aria-label="Filter status" className="input-dark" value={status} onChange={event=>onStatus(event.target.value)}><option value="">All Status</option>{statuses.map(value=><option key={value}>{value}</option>)}</Select><i aria-hidden="true" className="fa-solid fa-chevron-down"/></div>}{(draft||query||status)&&<button className="manager-service-action" onClick={()=>{setDraft('');onSearch('');onStatus?.('');}}>Reset filters</button>}{children}</div>;
}
export function ServiceTable({rows,columns,loading=false,name='Records',empty='NO RECORDS FOUND'}){
  const themed=usePortalTheme(),signature=rows.map(row=>row.id).join('|');
  const [paging,setPaging]=useState({signature,size:themed?10:15,page:1});
  const size=paging.size,totalPages=Math.max(1,Math.ceil(rows.length/size)),current=Math.min(signature===paging.signature?paging.page:1,totalPages);
  const pageRange=getPaginationRange(current,totalPages);
  return <><div className="manager-service-table overflow-x-auto"><table className="w-full table-fixed text-sm"><colgroup><col style={{width:'5%'}}/>{columns.map(column=><col key={column.label} style={{width:column.width}}/>)}</colgroup><thead><tr><th>#</th>{columns.map(column=><th key={column.label}>{column.label}</th>)}</tr></thead><tbody>{!loading&&rows.slice((current-1)*size,current*size).map((row,index)=><motion.tr key={row.id} initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2,delay:index*.03}}><td className="font-mono text-gray-500">{(current-1)*size+index+1}</td>{columns.map(column=><td key={column.label}>{column.render?column.render(row):row[column.key]??'Unavailable'}</td>)}</motion.tr>)}{(loading||!rows.length)&&<tr><td colSpan={columns.length+1} className="manager-service-empty">{loading?'Loading records...':empty}</td></tr>}</tbody></table></div><nav aria-label={name+' pagination'} className="manager-service-pagination"><div className="manager-service-page-info"><span>Showing {rows.length?(current-1)*size+1:0} &ndash; {Math.min(current*size,rows.length)} of {rows.length}</span><label className="manager-service-row-select-wrap"><span>Rows:</span><select className="manager-row-select" aria-label="Rows per page" value={size} onChange={e=>setPaging({signature,size:Number(e.target.value),page:1})}>{[10,15,20,50].map(val=><option key={val} value={val}>{val}</option>)}</select></label></div><div className="manager-service-page-buttons"><button aria-label="Previous page" disabled={current===1} onClick={()=>setPaging({signature,size,page:current-1})}>Prev</button>{pageRange.map((p,idx)=>p==='...'?<span key={`dots-${idx}`} className="manager-page-ellipsis">&hellip;</span>:<button key={p} aria-label={`Page ${p}`} aria-current={current===p?'page':undefined} className={`manager-page-num ${current===p?'is-active':''}`} onClick={()=>setPaging({signature,size,page:p})}>{p}</button>)}<button aria-label="Next page" disabled={current===totalPages} onClick={()=>setPaging({signature,size,page:current+1})}>Next</button></div></nav></>;
}
export function BookingDetails({booking:b}){
  return <><div className="manager-service-detail-ref"><strong>{b.referenceNumber||'Reference unavailable'}</strong>{b.invoiceId&&<Link to={`/dashboard/invoices/${b.invoiceId}`}>{b.invoiceNumber||'View invoice'}</Link>}</div><Badge value={b.status}/><dl className="manager-service-detail-grid">{[['Vehicle plate',b.licensePlate],['Customer',b.customerName],['Customer type',b.customerType?.replaceAll('_',' ')],['Contact',b.customerPhone],['Service package',b.serviceType],['Service date',b.serviceDate],['Time',formatTimeRange(b.timeSlot,b.estimatedDuration)],['Estimated cost',b.estimatedCost!=null?'LKR '+money(b.estimatedCost):null]].map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value||'Unavailable'}</dd></div>)}</dl><div className="manager-service-detail-notes"><h3>Booking notes</h3><p>{b.notes||'No booking notes'}</p></div></>;
}
