import {Input,Select} from '../../components/Validation';
import {useCallback,useEffect,useRef,useState} from 'react';
import {Link,useParams} from 'react-router-dom';
import {api} from '../../lib/api';
import {money} from '../../components/DataUI';
import {CashierPhoto as ItemPhoto} from './CashierPhoto';
import {downloadReceipt,exportCSV} from '../../components/BillingPages';
import {ServiceHeader,ServiceFilters,ServiceTable} from '../manager/ManagerServiceUI';
import {CashierModal,CashierState} from './CashierUI';
import InvoicePaymentForm from '../../components/InvoicePaymentForm';
import PaymentReceipt from '../../components/PaymentReceipt';
import InvoiceRefundForm from '../../components/InvoiceRefundForm';
import {canRefundInvoice} from '../../lib/refunds';

function useRecords(path){
 const [data,setData]=useState(null),[error,setError]=useState(''),request=useRef(0);
 const load=useCallback(async()=>{const current=++request.current;try{const result=await api(path);if(current===request.current){setData(result);setError('');}}catch(e){if(current===request.current)setError(e.message);}},[path]);
 useEffect(()=>{setData(null);setError('');load();window.addEventListener('fuelcore:refresh',load);return()=>{request.current++;window.removeEventListener('fuelcore:refresh',load);};},[load]);
 return {data,error,load};
}
const detail=(label,value)=><div key={label}><dt>{label}</dt><dd>{value==null||value===''?'Unavailable':value}</dd></div>;
const payable=invoice=>invoice.balanceDue>0&&!['VOID','REFUNDED','PARTIALLY_REFUNDED','PAID'].includes(invoice.status);
const matches=(row,query,fields)=>fields.some(key=>String(row[key]??'').toLowerCase().includes(query.toLowerCase()));
function Errors({error,load}){return error&&<p className="cashier-error" role="alert">{error} <button onClick={load}>Retry</button></p>;}
export function CashierVehicles(){
 const {data,error,load}=useRecords('/api/pos/vehicles'),[draft,setDraft]=useState(''),[query,setQuery]=useState(''),[selected,setSelected]=useState(null);
 const columns=[
  {key:'licensePlate',label:'License plate',width:'15%',render:r=><strong>{r.licensePlate}</strong>},
  {key:'make',label:'Make',width:'11%',render:r=><strong>{r.make}</strong>},
  {key:'model',label:'Model',width:'11%',render:r=><small>{r.model}</small>},
  {label:'Owner',width:'19%',render:r=><div className="cashier-table-inline"><strong>{r.ownerName}</strong><small>{r.ownerUsername?'Customer account: '+r.ownerUsername:'Registered vehicle without account'}</small></div>},
  {label:'Contact',width:'14%',key:'ownerContact'},{label:'Enrolled',width:'11%',key:'registeredDate'},
  {label:'Actions',width:'14%',render:r=><button className="manager-service-action" onClick={()=>setSelected(r)}>View details</button>}
 ];
 const fields=['licensePlate','make','model','ownerName','ownerUsername','ownerContact'];
 return <div className="cashier-directory"><ServiceHeader kicker="Counter directory" title="Registered vehicles."><Link className="cashier-secondary" to="/dashboard/pos">Back to point of sale</Link></ServiceHeader><p className="cashier-muted">Vehicles with and without a customer account are listed here.</p><ServiceFilters label="Search vehicles" draft={draft} setDraft={setDraft} query={query} onSearch={setQuery}/><Errors error={error} load={load}/><ServiceTable rows={(data||[]).filter(row=>matches(row,query,fields))} columns={columns} loading={!data&&!error} name="Vehicle directory"/>{selected&&<CashierModal title="Vehicle details" onClose={()=>setSelected(null)}><dl className="cashier-details-grid">{[['Registration',selected.licensePlate],['Make',selected.make],['Model',selected.model],['Owner',selected.ownerName],['Customer account',selected.ownerUsername||'No customer account'],['Contact',selected.ownerContact],['Registered',selected.registeredDate],['Year',selected.manufactureYear],['Fuel type',selected.fuelType],['Mileage',selected.mileage],['Engine number',selected.engineNumber],['Chassis number',selected.chassisNumber]].map(([label,value])=>detail(label,value))}</dl></CashierModal>}</div>;
}
export function CashierCatalog(){
 const {data,error,load}=useRecords('/api/pos/catalog'),[draft,setDraft]=useState(''),[query,setQuery]=useState(''),[offers,setOffers]=useState(false),[status,setStatus]=useState(''),[category,setCategory]=useState(''),[page,setPage]=useState(1);
 const categories=[...new Set((data||[]).map(r=>r.category).filter(Boolean))];
 const rows=(data||[]).filter(r=>matches(r,query,['name','category','shortDescription'])&&(!offers||r.discountPercentage>0)&&(!category||r.category===category)&&(!status||(r.active?'Active':'Inactive')===status)),pages=Math.max(1,Math.ceil(rows.length/12)),current=Math.min(page,pages);
 return <div><ServiceHeader kicker="Prices and active offers" title="Service catalog."><Link className="cashier-secondary" to="/dashboard/pos">Back to point of sale</Link></ServiceHeader><p className="cashier-muted">View service prices, details and current offers. Bill an existing service using its reference at the POS.</p><ServiceFilters label="Search services" draft={draft} setDraft={setDraft} query={query} onSearch={value=>{setQuery(value);setPage(1);}} status={status} onStatus={value=>{setStatus(value);setPage(1);}} statuses={['Active','Inactive']}><Select className="input-dark" aria-label="Filter category" value={category} onChange={e=>{setCategory(e.target.value);setPage(1);}}><option value="">All categories</option>{categories.map(value=><option key={value}>{value}</option>)}</Select><label className="cashier-qr-confirm"><Input type="checkbox" checked={offers} onChange={e=>{setOffers(e.target.checked);setPage(1);}}/>Offers only</label></ServiceFilters><Errors error={error} load={load}/>{!data&&!error?<p className="cashier-empty">Loading services...</p>:<div className="cashier-card-grid">{rows.slice((current-1)*12,current*12).map(r=><article className="cashier-product" key={r.id}><div className="cashier-product-photo"><ItemPhoto src={r.imageUrl} kind="service" alt={r.name}/>{r.popular&&<span className="cashier-popular-mark">Popular service</span>}{r.discountPercentage>0&&<span className="cashier-offer-mark">{r.discountPercentage}% OFF</span>}</div><div className="cashier-product-copy"><p>{r.category} <i aria-hidden="true" className={'fa-solid '+(r.icon||'fa-wrench')}/></p><h2>{r.name}</h2><p>{r.shortDescription||'No description recorded.'}</p><small>Estimated duration: {r.estimatedDuration||'Ask the workshop'}</small><CashierState value={r.active?'Active':'Inactive'}/>{r.highlights?.length>0&&<ul className="cashier-service-highlights">{r.highlights.map((text,index)=><li key={index}>{text}</li>)}</ul>}<div className="cashier-product-bottom"><div>{r.discountPercentage>0&&<del>Rs. {money(r.estimatedCost)}</del>}<strong>Rs. {money(r.price)}</strong></div><span>View only</span></div></div></article>)}</div>}{data&&!rows.length&&<p className="cashier-empty">No services match these filters.</p>}<nav className="cashier-card-pagination" aria-label="Service catalog pagination"><span>{rows.length} services</span><button disabled={current===1} onClick={()=>setPage(current-1)}>Prev</button><span>{current} / {pages}</span><button disabled={current===pages} onClick={()=>setPage(current+1)}>Next</button></nav></div>;
}
export function CashierInvoices(){
 const [period,setPeriod]=useState('today'),[paymentInvoice,setPaymentInvoice]=useState(null),[paymentError,setPaymentError]=useState(''),[paymentReceipt,setPaymentReceipt]=useState(null);
 const openPayment=async row=>{try{const latest=await api('/api/billing/invoices/'+row.id);if(!payable(latest.invoice))throw new Error('This invoice has no payable balance');setPaymentInvoice(latest.invoice);setPaymentError('');}catch(error){setPaymentError(error.message);}};
 const {data,error,load}=useRecords(period==='today'?'/api/pos/invoices/today':'/api/billing/invoices');
 const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[status,setStatus]=useState(''),[type,setType]=useState('');
 const [dates,setDates]=useState({issueFrom:'',issueTo:'',dueFrom:'',dueTo:''});
 useEffect(()=>{
  const focus=()=>load();window.addEventListener('focus',focus);
  const timer=period==='today'&&data?.nextRefreshAt&&data?.serverTime?setTimeout(load,Math.max(100,Date.parse(data.nextRefreshAt)-Date.parse(data.serverTime))):null;
  return()=>{if(timer!==null)clearTimeout(timer);window.removeEventListener('focus',focus);};
 },[data,load,period]);
 const dateError=(dates.issueFrom&&dates.issueTo&&dates.issueFrom>dates.issueTo)||(dates.dueFrom&&dates.dueTo&&dates.dueFrom>dates.dueTo);
 const within=(value,from,to)=>(!from&&!to)||Boolean(value&&(!from||value>=from)&&(!to||value<=to));
 const rows=(data?.invoices||[]).filter(r=>!dateError&&(!status||r.status===status)&&(!type||r.invoiceType===type)&&within(r.invoiceDate,dates.issueFrom,dates.issueTo)&&within(r.dueDate,dates.dueFrom,dates.dueTo)&&matches(r,query,['invoiceNumber','referenceNumber','customerName','customerUsername','licensePlate','invoiceName']));
 const total=r=>r.netTotalWithPenalty??r.netTotal??0;
 const columns=[
  {label:'Invoice ID',width:'17%',render:r=><div className="cashier-table-inline"><Link to={'/dashboard/invoices/'+r.id}>{r.invoiceNumber}</Link>{r.referenceNumber&&<small>Ref: {r.referenceNumber}</small>}</div>},
  {label:'Type',width:'11%',render:r=>r.invoiceType==='SERVICE'?'Service':r.invoiceType==='SPARE_PART'?'Spare part':r.invoiceType},
  {label:'Customer',width:'17%',key:'customerName'},
  {label:'Issue date',width:'12%',key:'invoiceDate'},
  {label:'Due date',width:'12%',render:r=>r.dueDate||'After completion'},
  {label:'Net total',width:'14%',render:r=><div className="cashier-table-inline"><strong>Rs. {money(total(r))}</strong><small>Paid: Rs. {money(r.amountPaid)}</small></div>},
  {label:'Status',width:'12%',render:r=><CashierState value={r.status}/>},
  {label:'Actions',width:'10%',render:r=><div className="cashier-table-inline"><button className="manager-service-action" disabled={!payable(r)} onClick={()=>openPayment(r)}>Pay</button><Link className="manager-service-action" to={'/dashboard/invoices/'+r.id}>View</Link></div>}
 ];
 return <div className="cashier-directory">
  <ServiceHeader kicker="Billing" title="Invoices."><button className="btn-accent" onClick={()=>exportCSV(rows.map(r=>({invoice:r.invoiceNumber,reference:r.referenceNumber||'',type:r.invoiceType,customer:r.customerName,issued:r.invoiceDate,due:r.dueDate,total:total(r),paid:r.amountPaid,status:r.status})),'cashier-invoices-'+(period==='today'?(data?.stationDate||'today'):'all')+'.csv')}>Export CSV</button></ServiceHeader>
  <p className="cashier-muted">{period==='today'?`${data?.stationDate||'Today'} - Sri Lanka time. Invoices issued or processed at the counter today. This view refreshes at midnight.`:'All saved invoices, including earlier counter and service invoices.'}</p>
  <ServiceFilters label="Search invoices" draft={draft} setDraft={setDraft} query={query} onSearch={setQuery} status={status} onStatus={setStatus} statuses={[...new Set((data?.invoices||[]).map(r=>r.status))]}>
   <Select className="input-dark" aria-label="Invoice type" value={type} onChange={e=>setType(e.target.value)}><option value="">All types</option><option value="SERVICE">Service</option><option value="SPARE_PART">Spare part</option></Select>
   <Select className="input-dark" aria-label="Invoice period" value={period} onChange={e=>setPeriod(e.target.value)}><option value="today">Today</option><option value="all">All invoices</option></Select>
  </ServiceFilters>
  <div className="cashier-invoice-dates">{[['issue','Issue'],['due','Due']].map(([key,label])=><div key={key} role="group" aria-label={label+' date range'}><span>{label}:</span><label>From<Input validationKey={key+'From'} type="date" className="input-dark" aria-label={label+' date from'} value={dates[key+'From']} onChange={e=>setDates({...dates,[key+'From']:e.target.value})}/></label><label>To<Input validationKey={key+'To'} min={dates[key+'From']||'1900-01-01'} type="date" className="input-dark" aria-label={label+' date to'} value={dates[key+'To']} onChange={e=>setDates({...dates,[key+'To']:e.target.value})}/></label></div>)}{Object.values(dates).some(Boolean)&&<button className="manager-service-action" onClick={()=>setDates({issueFrom:'',issueTo:'',dueFrom:'',dueTo:''})}>Clear dates</button>}</div>
  {dateError&&<p className="cashier-error" role="alert">A date range must end on or after its starting date.</p>}
  <Errors error={error||paymentError} load={load}/><ServiceTable rows={rows} columns={columns} name="Invoices" loading={!data&&!error}/>{paymentInvoice&&<CashierModal title="Process payment" onClose={()=>setPaymentInvoice(null)}><InvoicePaymentForm invoice={paymentInvoice} onPaid={async receipt=>{setPaymentInvoice(null);setPaymentReceipt(receipt);await load();}}/></CashierModal>}{paymentReceipt&&<PaymentReceipt receipt={paymentReceipt} onClose={()=>setPaymentReceipt(null)}/>}
 </div>;
}
export function CashierInvoiceDetails(){
 const {id}=useParams(),{data,error,load}=useRecords('/api/billing/invoices/'+encodeURIComponent(id)),[downloadError,setDownloadError]=useState(''),[paying,setPaying]=useState(false),[refunding,setRefunding]=useState(false),[paymentReceipt,setPaymentReceipt]=useState(null),inv=data?.invoice;
 if(!inv)return <div><Errors error={error} load={load}/>{!error&&<p>Loading invoice...</p>}</div>;
 return <div><ServiceHeader kicker="Invoice details" title={inv.invoiceNumber}><Link className="cashier-secondary" to="/dashboard/invoices">Invoices</Link><button className="btn-accent" onClick={async()=>{try{await downloadReceipt('/api/billing/invoices/'+id+'/receipt',inv.invoiceNumber+'.pdf');}catch(e){setDownloadError(e.message);}}}>Download receipt</button></ServiceHeader><Errors error={error||downloadError} load={load}/><div className="cashier-details-layout"><section className="cashier-details-card"><CashierState value={inv.status}/><dl className="cashier-details-grid">{[['Customer',inv.customerName],['Customer type',inv.customerType?.replaceAll('_',' ')],['Reference',inv.referenceNumber||'No service reference'],['Vehicle',inv.licensePlate],['Issue date',inv.invoiceDate],['Due date',inv.dueDate||'Payment period starts after completion']].map(([label,value])=>detail(label,value))}</dl><ServiceTable rows={(inv.lineItems||[]).map((r,index)=>({...r,id:index}))} columns={[{label:'Item',width:'45%',key:'itemName'},{label:'Quantity',width:'15%',key:'quantity'},{label:'Unit price',width:'17%',render:r=>'Rs. '+money(r.unitPrice)},{label:'Total',width:'18%',render:r=>'Rs. '+money(r.totalAmount)}]} name="Invoice items"/></section><aside className="cashier-details-card"><h2>Payment summary</h2><dl className="cashier-receipt-totals">{[['Invoice total',inv.netTotalWithPenalty],['Paid',inv.amountPaid],['Remaining balance',inv.balanceDue]].map(([label,value])=><div key={label}><dt>{label}</dt><dd>Rs. {money(value)}</dd></div>)}</dl>{inv.referenceNumber&&inv.balanceDue>0&&!['VOID','REFUNDED','PARTIALLY_REFUNDED'].includes(inv.status)&&<Link className="btn-accent" to={'/dashboard/pos?reference='+encodeURIComponent(inv.referenceNumber)}>Open service at POS</Link>}{payable(inv)&&<button className="btn-accent" onClick={()=>setPaying(true)}>Pay invoice</button>}{canRefundInvoice(inv)&&<button className="cashier-secondary" onClick={()=>setRefunding(true)}>Refund invoice</button>}<h3>Recorded payments</h3>{(data.payments||[]).map(p=><p key={p.id}>{p.paymentDate} · {p.paymentMethod} · Rs. {money(p.amount)} <CashierState value={p.status}/></p>)}{!data.payments?.length&&<p className="cashier-muted">No payment recorded.</p>}</aside></div>{refunding&&<CashierModal title="Refund invoice" onClose={()=>setRefunding(false)}><InvoiceRefundForm invoice={inv} onRefunded={async receipt=>{setRefunding(false);setPaymentReceipt(receipt);await load();}}/></CashierModal>}{paying&&<CashierModal title="Process payment" onClose={()=>setPaying(false)}><InvoicePaymentForm invoice={inv} onPaid={async receipt=>{setPaying(false);setPaymentReceipt(receipt);await load();}}/></CashierModal>}{paymentReceipt&&<PaymentReceipt receipt={paymentReceipt} onClose={()=>setPaymentReceipt(null)}/>}</div>;
}
