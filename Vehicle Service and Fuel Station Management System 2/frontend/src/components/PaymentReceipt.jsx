import {useState} from 'react';
import {Modal,Badge,money} from './DataUI';
import {downloadReceipt,printPaymentReceipt} from '../lib/receipts';
import './PaymentReceipt.css';

export default function PaymentReceipt({receipt,onClose,customer=false}){
  const [error,setError]=useState('');
  const run=async action=>{try{setError('');await action();}catch(error){setError(error.message);}};
  const invoice=receipt.invoice;
  const refund=receipt.refund;
  const method=receipt.paymentMethod||receipt.record?.paymentMethod||invoice.paymentMethod;
  return <Modal title={refund?'Refund statement ready':receipt.demo?'Demo receipt ready':'Receipt ready'} onClose={onClose}><div className="payment-receipt-content">
    <div className="payment-receipt-success"><span className="payment-receipt-check"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 12 4 4 8-8"/></svg></span><h3>{refund?(receipt.demo?'Demo refund completed':'Refund completed'):receipt.demo?'Demo payment completed':'Payment completed'}</h3><p>Your {receipt.demo||refund?'statement':'receipt'} is ready to print.</p></div>
    <div className="payment-receipt-amount"><span>{refund?(receipt.demo?'Test refund':'Amount refunded'):receipt.demo?'Test payment':'Payment received'}</span><strong><small>LKR</small> {money(receipt.refundAmount??receipt.paymentAmount??receipt.record?.amount)}</strong></div>
    <dl className="payment-receipt-details"><div><dt>Invoice</dt><dd>{invoice.invoiceNumber}</dd></div><div><dt>{refund?'Refund method':'Payment method'}</dt><dd>{method==='CARD'?'Card':method==='CASH'?'Cash':method}</dd></div><div><dt>{refund?'Payments retained':receipt.demo?'Preview balance':'Remaining balance'}</dt><dd>LKR {money(refund?invoice.amountPaid:invoice.balanceDue)}</dd></div><div><dt>Status</dt><dd><Badge value={receipt.demo?'DEMO':invoice.status}/></dd></div></dl>
    {receipt.demo&&<p className="payment-receipt-demo-note">Demonstration only. No money {refund?'refunded':'collected'}; the original invoice remains unchanged.</p>}{error&&<p className="payment-receipt-error" role="alert">{error}</p>}
    <div className="payment-receipt-actions"><button className="btn-accent" onClick={()=>run(()=>printPaymentReceipt(receipt))}>{receipt.demo||refund?'Print statement':'Print receipt'}</button>{!receipt.demo&&!refund&&<button className="payment-receipt-secondary" onClick={()=>run(()=>downloadReceipt(customer?'/api/v1/customer/invoices/'+encodeURIComponent(invoice.invoiceNumber)+'/receipt':'/api/billing/invoices/'+invoice.id+'/receipt',invoice.invoiceNumber+'.pdf'))}>Download receipt</button>}<button className="payment-receipt-secondary" onClick={onClose}>Done</button></div>
  </div></Modal>;
}
