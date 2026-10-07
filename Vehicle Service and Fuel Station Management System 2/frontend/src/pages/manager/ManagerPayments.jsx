import {Input,Select} from '../../components/Validation';
import {useCallback,useEffect,useState} from 'react';
import {Link} from 'react-router-dom';
import {motion} from 'framer-motion';
import {api,notice} from '../../lib/api';
import {Badge,Modal,QuickDeck,money} from '../../components/DataUI';
import {exportCSV} from '../../components/BillingPages';
import EntityForm from '../../components/EntityForm';
import './ManagerInvoices.css';
import './ManagerPayments.css';
import {useLiveRefresh} from '../../lib/useLiveRefresh';

const statusLabels={SUCCESS:'Success',PENDING:'Pending',FAILED:'Failed',REFUNDED:'Refunded',PARTIALLY_REFUNDED:'Partially refunded'};
const methodLabels={CASH:'Cash',CARD:'Credit / Debit Card',QR:'QR / Transfer',BANK:'Bank Transfer',CHEQUE:'Cheque',OTHER:'Other'};
const methodKey=value=>String(value??'').trim().toUpperCase();
const retained=payment=>Math.max(0,(payment.amount||0)-(payment.refundedAmount||0));
const canRefund=payment=>['SUCCESS','PAID','PARTIALLY_REFUNDED'].includes(payment.status)&&retained(payment)>0&&Boolean(payment.invoiceNumber);

function PaymentFilters({draft,setDraft,query,onSearch,status,onStatus,method,onMethod,statuses,methods,onExport,compact=false}){
  const active=Boolean(query||draft||status||method);
  return <div className={`manager-payment-filters${compact?' is-compact':''}`}>
    <div className="manager-invoice-primary-filters">
      <form noValidate className="manager-invoice-search" onSubmit={event=>{event.preventDefault();onSearch(draft.trim());}}>
        <Input aria-label="Search ledger" placeholder="Search ledger..." className="input-dark" value={draft} onChange={event=>setDraft(event.target.value)}/>
        {draft&&<button type="button" aria-label="Clear search text" onClick={()=>{setDraft('');onSearch('');}}><i aria-hidden="true" className="fa-solid fa-xmark"/></button>}
        <button type="submit" aria-label="Search"><i aria-hidden="true" className="fa-solid fa-magnifying-glass"/></button>
      </form>
      <div className="manager-invoice-select"><Select aria-label="Filter status" className="input-dark" value={status} onChange={event=>onStatus(event.target.value)}><option value="">All Status</option>{statuses.map(value=><option key={value} value={value}>{statusLabels[value]||value}</option>)}</Select><i aria-hidden="true" className="fa-solid fa-chevron-down"/></div>
      <div className="manager-invoice-select"><Select validationKey="method" aria-label="Filter method" className="input-dark" value={method} onChange={event=>onMethod(event.target.value)}><option value="">All Methods</option>{methods.map(value=><option key={value} value={value}>{methodLabels[value]||value}</option>)}</Select><i aria-hidden="true" className="fa-solid fa-chevron-down"/></div>
    </div>
    {(active||compact)&&<div className="manager-payment-filter-actions">{active&&<button type="button" className="manager-invoice-reset" onClick={()=>{setDraft('');onSearch('');onStatus('');onMethod('');}}><i aria-hidden="true" className="fa-solid fa-rotate-left"/>Reset filters</button>}{compact&&<button className="btn-accent manager-invoice-export" onClick={onExport}>Export CSV</button>}</div>}
  </div>;
}

export default function ManagerPayments(){
  const [rows,setRows]=useState([]),[total,setTotal]=useState(0),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [draft,setDraft]=useState(''),[query,setQuery]=useState(''),[status,setStatus]=useState(''),[method,setMethod]=useState('');
  const [page,setPage]=useState(1),[size,setSize]=useState(10),[selected,setSelected]=useState(null),[opening,setOpening]=useState(null);
  const load=useCallback(async()=>{try{const data=await api('/api/billing/payments');setRows(data.payments||[]);setTotal(data.totalSettled??0);setError('');return data.payments||[];}catch(error){setError(error.message);throw error;}finally{setLoading(false);}},[]);
  useEffect(()=>{load().catch(()=>{});},[load]);
  useLiveRefresh(load);
  const changeQuery=value=>{setQuery(value);setPage(1);};
  const changeStatus=value=>{setStatus(value);setPage(1);};
  const changeMethod=value=>{setMethod(value);setPage(1);};
  const filtered=rows.filter(payment=>(!status||payment.status===status)&&(!method||methodKey(payment.paymentMethod)===method)
    &&[payment.invoiceNumber,payment.referenceNumber,payment.paidOccupant,payment.customerUsername,payment.notes].some(value=>String(value??'').toLowerCase().includes(query.toLowerCase())));
  const totalPages=Math.max(1,Math.ceil(filtered.length/size)),current=Math.min(page,totalPages);
  const exportPayments=()=>exportCSV(filtered.map(payment=>({
    'Payment ID':payment.id,'Invoice number':payment.invoiceNumber||'','Service reference':payment.referenceNumber||'',Date:payment.paymentDate||'',
    Customer:payment.paidOccupant||payment.customerUsername||'Walk-in',Method:methodLabels[methodKey(payment.paymentMethod)]||payment.paymentMethod||'',
    'Amount LKR':payment.amount||0,'Refunded LKR':payment.refundedAmount||0,'Retained LKR':retained(payment),Status:payment.status,Notes:payment.notes||''
  })),'payments.csv');
  const openRefund=async payment=>{
    setOpening(payment.id);
    try{const latest=(await load()).find(row=>row.id===payment.id);if(!latest||!canRefund(latest)){notice('This payment has no refundable amount.');return;}setSelected(latest);}
    catch{}finally{setOpening(null);}
  };
  const refund=async form=>{
    await api(`/api/billing/refund-payment/${selected.id}?reason=${encodeURIComponent(form.reason.trim())}`,{method:'POST'});
    setSelected(null);notice('Payment refund recorded');await load().catch(()=>{});
  };
  const filterProps={draft,setDraft,query,onSearch:changeQuery,status,onStatus:changeStatus,method,onMethod:changeMethod,onExport:exportPayments,
    statuses:[...new Set([...Object.keys(statusLabels),...rows.map(row=>row.status).filter(Boolean)])],
    methods:[...new Set([...Object.keys(methodLabels),...rows.map(row=>methodKey(row.paymentMethod)).filter(Boolean)])]};
  return <motion.div initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2}} className="manager-payments">
    <div className="manager-invoice-heading"><div><p className="manager-invoice-kicker">Billing &amp; payments</p><h1>Payment settlements.</h1><p className="manager-invoice-balance">Collected after refunds: LKR {money(total)}</p></div><button className="btn-accent manager-invoice-export" disabled={!filtered.length} onClick={exportPayments}>Export CSV</button></div>
    {error&&<p role="alert" className="text-brand-accent mb-4">{error} <button onClick={()=>load().catch(()=>{})}>Retry</button></p>}
    <PaymentFilters {...filterProps}/><QuickDeck><PaymentFilters {...filterProps} compact/></QuickDeck>
    <div className="manager-invoice-table manager-payment-table overflow-x-auto"><table className="w-full table-fixed text-sm"><colgroup>{[4,22,12,16,12,13,12,9].map((width,index)=><col key={index} style={{width:width+'%'}}/>)}</colgroup><thead><tr>{['#','Ref / Invoice no','Date','Customer','Method','Amount','Status','Actions'].map(label=><th key={label} className="text-left">{label}</th>)}</tr></thead>
      <tbody>{!loading&&filtered.slice((current-1)*size,current*size).map((payment,index)=><motion.tr key={payment.id} initial={{opacity:0}} animate={{opacity:1}} transition={{duration:.2,delay:index*.03}}>
        <td className="font-mono text-gray-500">{(current-1)*size+index+1}</td>
        <td>{payment.invoiceId?<Link className="manager-invoice-number" to={`/dashboard/invoices/${payment.invoiceId}`}>{payment.invoiceNumber||payment.referenceNumber||'Unlinked payment'}</Link>:<span className="manager-invoice-number">{payment.invoiceNumber||payment.referenceNumber||'Unlinked payment'}</span>}{payment.invoiceNumber&&payment.referenceNumber&&payment.invoiceNumber!==payment.referenceNumber&&<span className="manager-invoice-reference">Ref: {payment.referenceNumber}</span>}</td>
        <td className="font-mono text-gray-400">{payment.paymentDate||'Unavailable'}</td>
        <td>{payment.paidOccupant||payment.customerUsername||'Walk-in'}</td>
        <td className="text-gray-400">{methodLabels[methodKey(payment.paymentMethod)]||payment.paymentMethod||'Unspecified'}</td>
        <td><strong className="manager-invoice-total">Rs. {money(payment.amount)}</strong>{payment.refundedAmount>0&&<><span className="manager-payment-refunded">Refunded: Rs. {money(payment.refundedAmount)}</span><span className="manager-invoice-remaining">Retained: Rs. {money(retained(payment))}</span></>}</td>
        <td><Badge value={payment.status?.replaceAll('_',' ')}/></td>
        <td><div className="manager-invoice-actions"><button className="manager-invoice-action" disabled={!canRefund(payment)||opening!==null} onClick={()=>openRefund(payment)}>{opening===payment.id?'Opening...':'Refund'}</button>{payment.invoiceId&&<Link className="manager-invoice-action" to={`/dashboard/invoices/${payment.invoiceId}`}>View</Link>}</div></td>
      </motion.tr>)}{(loading||!filtered.length)&&<tr><td colSpan={8} className="manager-invoice-empty">{loading?'Loading payments...':'NO PAYMENTS MATCH YOUR CRITERIA'}</td></tr>}</tbody></table></div>
    <nav aria-label="Payment pagination" className="manager-invoice-pagination"><div className="manager-invoice-page-info"><span>Showing {filtered.length?(current-1)*size+1:0} &ndash; {Math.min(current*size,filtered.length)} of {filtered.length}</span><div className="manager-invoice-row-options"><span>Rows:</span>{[10,15,20].map(value=><button key={value} aria-label={`${value} rows per page`} aria-pressed={value===size} className={value===size?'is-active':''} onClick={()=>{setSize(value);setPage(1);}}>{value}</button>)}</div></div><div className="manager-invoice-page-buttons"><button aria-label="Previous page" disabled={current===1} onClick={()=>setPage(current-1)}>Prev</button><button aria-label="Next page" disabled={current===totalPages} onClick={()=>setPage(current+1)}>Next</button></div></nav>
    {selected&&<Modal title="Refund payment" onClose={()=>setSelected(null)}><p className="manager-invoice-payment-id">{selected.invoiceNumber}{selected.referenceNumber&&<span>Ref: {selected.referenceNumber}</span>}</p><div className="manager-invoice-payment-balance"><span>Amount to refund</span><strong>Rs. {money(retained(selected))}</strong></div><p className="text-gray-400 text-sm mb-5">Refund the remaining amount of this payment to {selected.paidOccupant||selected.customerUsername||'the customer'}. The invoice and payment history will be updated.</p><EntityForm key={selected.id} submitLabel="Confirm refund" fields={[{key:'reason',label:'Refund reason',type:'textarea'}]} onSave={refund}/><button className="manager-invoice-action mt-4" onClick={()=>setSelected(null)}>Cancel</button></Modal>}
  </motion.div>;
}
