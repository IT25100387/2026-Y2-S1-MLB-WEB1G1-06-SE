import {useState} from 'react';
import EntityForm from './EntityForm';
import {api,notice,requestKey} from '../lib/api';
import {collectCardPayment} from '../lib/cardPayments';

export default function InvoicePaymentForm({invoice,onPaid}){
  const [key]=useState(requestKey),provisional=invoice.invoiceType==='SERVICE'&&!invoice.finalized;
  const save=async form=>{
    if(provisional&&form.amount>=invoice.balanceDue-.001)throw new Error('Full settlement is available after service completion. Enter an advance or partial amount.');
    const result=form.paymentMethod==='CARD'?await collectCardPayment({invoiceNumber:invoice.invoiceNumber,amount:form.amount},key,undefined,{invoice}):await api('/api/billing/pay',{method:'POST',headers:{'Idempotency-Key':key},body:JSON.stringify({...form,invoiceNumber:invoice.invoiceNumber})});
    notice(result.demo?'Demo receipt ready — no money collected':'Payment recorded');await onPaid({...result,paymentAmount:form.amount,paymentMethod:form.paymentMethod});
  };
  return <><p className="cashier-note">{invoice.invoiceNumber} · Balance: Rs. {invoice.balanceDue.toFixed(2)}</p>{provisional&&<p className="cashier-note">Advance and partial payments are available. Complete the service before full settlement.</p>}<EntityForm initial={{amount:provisional?Math.max(0,(invoice.advanceAmountDue||0)-(invoice.amountPaid||0))||'':invoice.balanceDue,paymentMethod:'CASH'}} submitLabel="Confirm payment" fields={[{key:'amount',label:'Amount to pay (LKR)',type:'number',min:.01,max:invoice.balanceDue},{key:'paymentMethod',label:'Payment method',options:['CASH','CARD','QR'],maxRows:3}]} onSave={save}/></>;
}
