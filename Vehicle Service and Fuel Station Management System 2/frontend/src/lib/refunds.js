import {api,requestKey} from './api';
import {cardConfiguration} from './cardPayments';
import {localCardDemoAllowed} from './cardDemo';
import {openDemoCardCheckout} from './demoCheckout';

export function fullyPaidRefundable(invoice){
  return Boolean(invoice&&invoice.status==='PAID'&&Number(invoice.balanceDue)<=.01&&Number(invoice.amountPaid)>0&&!(Number(invoice.refundedAmount)>0));
}
const demoKey=invoice=>'fuelcore.demo.refund.'+(localStorage.getItem('accountUsername')||'')+'.'+invoice.invoiceNumber;
export function hasDemoRefund(invoice){return Boolean(invoice&&sessionStorage.getItem(demoKey(invoice)));}
export function canRefundInvoice(invoice){return fullyPaidRefundable(invoice)&&!hasDemoRefund(invoice);}

export async function refundInvoice(invoice,form,payment){
  // Re-read the accessible invoice before opening checkout or recording cash.
  const current=(await api('/api/billing/invoices/'+invoice.id)).invoice;
  if(!canRefundInvoice(current))throw new Error(hasDemoRefund(current)?'A demo refund has already been completed for this invoice.':'Only fully paid invoices with no previous refund can be refunded.');
  const amount=Number(form.amount),reason=String(form.reason||'').trim().replace(/\s+/g,' '),method=form.paymentMethod;
  const maximum=payment?Math.min(current.amountPaid,Number(payment.amount)-Number(payment.refundedAmount||0)):Number(current.amountPaid);
  if(!Number.isFinite(amount)||amount<.01||amount>maximum||Math.abs(amount*100-Math.round(amount*100))>.000001)throw new Error('Enter a refund amount from LKR 0.01 to the available amount, with at most two decimal places.');
  if(reason.length<3||reason.length>255)throw new Error('Use 3–255 characters for the refund reason.');
  if(!['CASH','CARD'].includes(method))throw new Error('Select Cash or Card for a refund.');
  const key=form.requestKey||requestKey(),date=new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  const statement={completed:true,refund:true,refundAmount:amount,paymentAmount:amount,paymentMethod:method,refundReason:reason,paymentDate:date};
  if(method==='CARD'){
    const settings=await cardConfiguration('/api/payments/card/configuration');
    if(settings.mode==='DEMO'||!settings.configured&&localCardDemoAllowed(settings)){
      const retained=Math.round((current.amountPaid-amount)*100)/100;
      const receipt={...statement,demo:true,localDemo:true,invoice:{...current,amountPaid:retained,refundedAmount:amount,balanceDue:0,status:retained>0?'PARTIALLY_REFUNDED':'REFUNDED'}};
      const result=await openDemoCardCheckout({operation:'refund',amount,localDemo:true,receipt});
      // One preview per invoice in this signed-in browser session. Real records
      // remain unchanged, and the server independently enforces real refunds.
      sessionStorage.setItem(demoKey(current),JSON.stringify({amount,requestKey:key}));
      window.dispatchEvent(new Event('fuelcore:refresh'));
      return result;
    }
  }
  const path=payment?'/api/billing/refund-payment/'+payment.id:'/api/billing/refund/'+current.id;
  const query=new URLSearchParams({amount:String(amount),reason,paymentMethod:method});
  const result=await api(path+'?'+query,{method:'POST',headers:{'Idempotency-Key':key}});
  const updated=result.invoice||(await api('/api/billing/invoices/'+current.id)).invoice;
  return {...statement,...result,invoice:updated};
}
