import {Input,Select} from '../../components/Validation';
import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,notice,requestKey} from '../../lib/api';
import {Badge,Modal,QuickDeck,money} from '../../components/DataUI';
import {exportCSV} from '../../components/BillingPages';
import EntityForm from '../../components/EntityForm';
import {collectCardPayment} from '../../lib/cardPayments';
import PaymentReceipt from '../../components/PaymentReceipt';
import './ManagerInvoices.css';
import {useLiveRefresh} from '../../lib/useLiveRefresh';

const emptyFilters={status:'',type:'',issueFrom:'',issueTo:'',dueFrom:'',dueTo:''};
const typeLabels={SERVICE:'Service',SPARE_PART:'Spare Part',FUEL:'Fuel (archived)'};
const statusLabels={PENDING:'Pending',PARTIAL:'Partial',PAID:'Paid',OVERDUE:'Overdue',REFUNDED:'Refunded',PARTIALLY_REFUNDED:'Partially refunded',VOID:'Void'};
const invoiceTotal=invoice=>invoice.netTotalWithPenalty??invoice.netTotal??0;
const canPay=invoice=>invoice.balanceDue>0&&!['VOID','REFUNDED','PARTIALLY_REFUNDED','PAID'].includes(invoice.status);

function InvoiceFilters({draft,setDraft,query,onSearch,filters,setFilters,statuses,types,onExport,compact=false}){
  const active=Boolean(query||draft||Object.values(filters).some(Boolean));
  const update=(key,value)=>setFilters(current=>({...current,[key]:value}));
  const select=(key,label,values,labels)=><div className="manager-invoice-select"><Select aria-label={label} className="input-dark" value={filters[key]} onChange={event=>update(key,event.target.value)}><option value="">{key==='status'?'All Status':'All Types'}</option>{values.map(value=><option key={value} value={value}>{labels[value]||value}</option>)}</Select><i aria-hidden="true" className="fa-solid fa-chevron-down"/></div>;
  return <div className={`manager-invoice-filters${compact?' is-compact':''}`}>
    <div className="manager-invoice-primary-filters">
      <form noValidate className="manager-invoice-search" onSubmit={event=>{event.preventDefault();onSearch(draft.trim());}}>
        <Input aria-label="Search invoices" placeholder="Search invoices..." className="input-dark" value={draft} onChange={event=>setDraft(event.target.value)}/>
        {draft&&<button type="button" aria-label="Clear search text" onClick={()=>{setDraft('');onSearch('');}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}
        <button type="submit" aria-label="Search"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button>
      </form>
      {select('status','Filter status',statuses,statusLabels)}
      {select('type','Filter type',types,typeLabels)}
    </div>
    <div className="manager-invoice-date-filters">
      {[['issue','Issue'],['due','Due']].map(([key,label])=><div className="manager-invoice-date-range" key={key} role="group" aria-label={`${label} date range`}>
        <span>{label}:</span>
        <Input validationKey={key+'From'} type="date" aria-label={`${label} date from`} className="input-dark" value={filters[key+'From']} onChange={event=>update(key+'From',event.target.value)}/>
        <span aria-hidden="true">&ndash;</span>
        <Input validationKey={key+'To'} min={filters[key+'From']||'1900-01-01'} type="date" aria-label={`${label} date to`} className="input-dark" value={filters[key+'To']} onChange={event=>update(key+'To',event.target.value)}/>
      </div>)}
      {active&&<button type="button" className="manager-invoice-reset" onClick={()=>{setDraft('');onSearch('');setFilters(emptyFilters);}}><i aria-hidden="true" className="fa-solid fa-rotate-left"/>Reset filters</button>}
      {compact&&<button className="btn-accent manager-invoice-export" onClick={onExport}>Export CSV</button>}
    </div>
  </div>;
}

export default function ManagerInvoices(){
  const [rows,setRows]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[filters,setFilters]=useState(emptyFilters);
  const [size,setSize]=useState(10),[page,setPage]=useState(1),[selected,setSelected]=useState(null),[opening,setOpening]=useState(null),[paymentReceipt,setPaymentReceipt]=useState(null);
  const load=useCallback(async()=>{try{const data=await api('/api/billing/invoices');setRows(data.invoices||[]);setError('');}catch(error){setError(error.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();},[load]);
  useLiveRefresh(load);
  const changeQuery=value=>{setQuery(value);setPage(1);};
  const changeFilters=value=>{setFilters(value);setPage(1);};
  const dateError=(filters.issueFrom&&filters.issueTo&&filters.issueFrom>filters.issueTo)||(filters.dueFrom&&filters.dueTo&&filters.dueFrom>filters.dueTo);
  const within=(value,from,to)=>(!from&&!to)||Boolean(value&&(!from||value>=from)&&(!to||value<=to));
  const filtered=rows.filter(invoice=>!dateError&&(!filters.status||invoice.status===filters.status)&&(!filters.type||invoice.invoiceType===filters.type)
    &&within(invoice.invoiceDate,filters.issueFrom,filters.issueTo)&&within(invoice.dueDate,filters.dueFrom,filters.dueTo)
    &&[invoice.invoiceNumber,invoice.referenceNumber,invoice.customerName,invoice.customerUsername,invoice.licensePlate,invoice.invoiceName].some(value=>String(value??'').toLowerCase().includes(query.toLowerCase())));
  const totalPages=Math.max(1,Math.ceil(filtered.length/size)),current=Math.min(page,totalPages);
  const exportInvoices=()=>exportCSV(filtered.map(invoice=>({
    'Invoice ID':invoice.invoiceNumber,'Service reference':invoice.referenceNumber||'',Type:typeLabels[invoice.invoiceType]||invoice.invoiceType,
    Customer:invoice.customerName||invoice.customerUsername||'Walk-in','Issue date':invoice.invoiceDate,'Due date':invoice.dueDate||'',
    'Net total LKR':invoiceTotal(invoice),'Paid LKR':invoice.amountPaid||0,'Refunded LKR':invoice.refundedAmount||0,'Balance LKR':invoice.balanceDue||0,Status:invoice.status
  })),'invoices.csv');
  const openPayment=async invoice=>{
    setOpening(invoice.id);
    try{const data=await api(`/api/billing/invoices/${invoice.id}`);if(!canPay(data.invoice)){await load();notice('This invoice has no payable balance.');return;}setSelected({...data.invoice,requestKey:requestKey()});}
    catch(error){setError(error.message);}finally{setOpening(null);}
  };
  const provisional=selected?.invoiceType==='SERVICE'&&!selected?.finalized;
  const pay=async form=>{
    if(provisional&&form.amount>=selected.balanceDue-.01)throw new Error('Full settlement is available after service completion. Enter an advance or partial amount.');
    const result=form.paymentMethod==='CARD'?await collectCardPayment({invoiceNumber:selected.invoiceNumber,amount:form.amount},form.requestKey,undefined,{invoice:selected}):await api('/api/billing/pay',{method:'POST',headers:{'Idempotency-Key':form.requestKey},body:JSON.stringify({...form,invoiceNumber:selected.invoiceNumber})});
    setSelected(null);setPaymentReceipt({...result,paymentAmount:form.amount,paymentMethod:form.paymentMethod});await load();notice(result.demo?'Demo receipt ready — no money collected':'Payment recorded');
  };
  const filterProps={draft,setDraft,query,onSearch:changeQuery,filters,setFilters:changeFilters,onExport:exportInvoices,
    statuses:[...new Set([...Object.keys(statusLabels),...rows.map(row=>row.status).filter(Boolean)])],
    types:[...new Set(['SERVICE','SPARE_PART',...rows.map(row=>row.invoiceType).filter(Boolean)])]};
  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-invoices">
    <div className="manager-invoice-heading"><div><p className="manager-invoice-kicker">Billing &amp; payments</p><h1>Manage invoices.</h1><p className="manager-invoice-balance">Outstanding balance: LKR {money(rows.reduce((sum,row)=>sum+(row.balanceDue||0),0))}</p></div><button className="btn-accent manager-invoice-export" disabled={!filtered.length} onClick={exportInvoices}>Export CSV</button></div>
    {error&&<p role="alert" className="text-brand-accent mb-4">{error} <button onClick={load}>Retry</button></p>}
    <InvoiceFilters {...filterProps}/>
    <QuickDeck><InvoiceFilters {...filterProps} compact/></QuickDeck>
    {dateError&&<p role="alert" className="text-brand-accent text-sm mt-4">A date range must end on or after its starting date.</p>}
    <div className="manager-invoice-table overflow-x-auto"><table className="w-full table-fixed text-sm"><colgroup>{[4,15,9,13,10,10,15,14,10].map((width,index)=><col key={index} style={{width:width+'%'}}/>)}</colgroup>
      <thead><tr>{['#','Invoice ID','Type','Customer','Issue date','Due date','Net total','Status','Actions'].map(label=><th key={label} className="text-left">{label}</th>)}</tr></thead>
      <tbody>{!loading&&filtered.slice((current-1)*size,current*size).map((invoice,index)=><motion.tr key={invoice.id} initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2,delay:index*.03}}>
        <td className="font-mono text-gray-500">{(current-1)*size+index+1}</td>
        <td><Link className="manager-invoice-number" to={`/dashboard/invoices/${invoice.id}`}>{invoice.invoiceNumber}</Link>{invoice.referenceNumber&&<span className="manager-invoice-reference">Ref: {invoice.referenceNumber}</span>}</td>
        <td>{typeLabels[invoice.invoiceType]||invoice.invoiceType||'Unspecified'}</td>
        <td>{invoice.customerName||invoice.customerUsername||'Walk-in'}</td>
        <td className="font-mono text-gray-400">{invoice.invoiceDate||'Unavailable'}</td>
        <td className="font-mono text-gray-400">{invoice.dueDate||(invoice.invoiceType==='SERVICE'&&!invoice.finalized?'After completion':'Unavailable')}</td>
        <td><strong className="manager-invoice-total">Rs. {money(invoiceTotal(invoice))}</strong>{invoice.amountPaid>0&&<span className="manager-invoice-paid">Paid: Rs. {money(invoice.amountPaid)}</span>}<span className="manager-invoice-remaining">Balance: Rs. {money(invoice.balanceDue)}</span></td>
        <td><Badge value={invoice.status}/></td>
        <td><div className="manager-invoice-actions"><button className="manager-invoice-action" disabled={!canPay(invoice)||opening!==null} onClick={()=>openPayment(invoice)}>{opening===invoice.id?'Opening...':'Pay'}</button><Link className="manager-invoice-action" to={`/dashboard/invoices/${invoice.id}`}>View</Link></div></td>
      </motion.tr>)}{(loading||!filtered.length)&&<tr><td colSpan={9} className="manager-invoice-empty">{loading?'Loading invoices...':'NO INVOICES MATCH YOUR CRITERIA'}</td></tr>}</tbody>
    </table></div>
    <nav aria-label="Invoice pagination" className="manager-invoice-pagination"><div className="manager-invoice-page-info"><span>Showing {filtered.length?(current-1)*size+1:0} &ndash; {Math.min(current*size,filtered.length)} of {filtered.length}</span><div className="manager-invoice-row-options"><span>Rows:</span>{[10,15,20].map(value=><button key={value} aria-label={`${value} rows per page`} aria-pressed={value===size} className={value===size?'is-active':''} onClick={()=>{setSize(value);setPage(1);}}>{value}</button>)}</div></div><div className="manager-invoice-page-buttons"><button aria-label="Previous page" disabled={current===1} onClick={()=>setPage(current-1)}>Prev</button><button aria-label="Next page" disabled={current===totalPages} onClick={()=>setPage(current+1)}>Next</button></div></nav>
    {selected&&<Modal title="Process payment" onClose={()=>setSelected(null)}><p className="manager-invoice-payment-id">{selected.invoiceNumber}{selected.referenceNumber&&<span>Ref: {selected.referenceNumber}</span>}</p><div className="manager-invoice-payment-balance"><span>Balance due</span><strong>Rs. {money(selected.balanceDue)}</strong></div>{provisional&&<p className="text-blue-400 text-sm mb-5">This service invoice is provisional. Advance and partial payments are available; full settlement is available after service completion.</p>}<EntityForm key={selected.id} submitLabel="Confirm payment" initial={{requestKey:selected.requestKey,amount:provisional?Math.max(0,(selected.advanceAmountDue||0)-(selected.amountPaid||0))||'':selected.balanceDue,paymentMethod:'CASH'}} fields={[{key:'amount',label:'Amount to pay (LKR)',type:'number',min:.01,max:selected.balanceDue},{key:'paymentMethod',label:'Payment method',options:['CASH','CARD','QR'],maxRows:3}]} onSave={pay}/><button className="manager-invoice-action mt-4" onClick={()=>setSelected(null)}>Cancel</button></Modal>}
  {paymentReceipt&&<PaymentReceipt receipt={paymentReceipt} onClose={()=>setPaymentReceipt(null)}/>}
  </motion.div>;
}
