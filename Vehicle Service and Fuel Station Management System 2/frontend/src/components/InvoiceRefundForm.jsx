import {useState} from 'react';
import EntityForm from './EntityForm';
import {money} from './DataUI';
import {requestKey} from '../lib/api';
import {canRefundInvoice,refundInvoice} from '../lib/refunds';

export default function InvoiceRefundForm({invoice,payment,onRefunded}){
  const [key]=useState(requestKey);
  const maximum=payment?Math.min(invoice.amountPaid,payment.amount-(payment.refundedAmount||0)):invoice.amountPaid;
  if(!canRefundInvoice(invoice))return <p role="alert">Only fully paid invoices with no previous refund can be refunded.</p>;
  return <div><div className="manager-invoice-payment-balance"><span>Available to refund</span><strong>Rs. {money(maximum)}</strong></div>
    <p className="text-sm text-gray-400 mb-5">One refund is allowed per invoice. You can edit the amount; a partial refund also uses this one refund.</p>
    <EntityForm initial={{requestKey:key,amount:Number(maximum).toFixed(2),paymentMethod:'CASH'}} submitLabel="Confirm refund" fields={[
      {key:'amount',label:'Refund amount (LKR)',type:'number',min:.01,max:maximum,decimals:2,placeholder:'e.g. 1500.00'},
      {key:'paymentMethod',label:'Refund method',options:['CASH','CARD'],maxRows:2},
      {key:'reason',label:'Refund reason',type:'textarea',minLength:3,maxLength:255,placeholder:'e.g. Customer returned an unused part'}
    ]} onSave={async form=>onRefunded(await refundInvoice(invoice,form,payment))}/>
  </div>;
}
