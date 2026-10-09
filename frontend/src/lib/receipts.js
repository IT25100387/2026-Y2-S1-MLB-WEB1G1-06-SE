import {apiFetch} from './api';
const money=value=>new Intl.NumberFormat('en-LK',{minimumFractionDigits:2,maximumFractionDigits:2}).format(value||0);
export async function downloadReceipt(path,name){const response=await apiFetch(path);const url=URL.createObjectURL(await response.blob());const link=document.createElement('a');link.href=url;link.download=name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}

// Build the print document with text nodes so invoice content cannot inject HTML.
export function printPaymentReceipt(receipt){
  const popup=window.open('','_blank');
  if(!popup)throw new Error('Allow the receipt window to open, then select Print receipt again.');
  popup.opener=null;
  const doc=popup.document,invoice=receipt.invoice,refund=Boolean(receipt.refund);
  doc.title=(receipt.demo?'DEMO · ':'')+invoice.invoiceNumber+' receipt';
  const style=doc.createElement('style');style.textContent='body{font:14px Arial,sans-serif;color:#111;margin:30px;max-width:720px}h1{font-size:24px}table{border-collapse:collapse;width:100%;margin:24px 0}th,td{text-align:left;padding:10px 0;border-bottom:1px solid #ddd}td:last-child,th:last-child{text-align:right}dl{display:grid;grid-template-columns:1fr 1fr;gap:10px}dd{text-align:right;margin:0}button{padding:10px 20px}@media print{button{display:none}body{margin:0}@page{margin:18mm}}';doc.head.appendChild(style);
  const add=(parent,tag,text)=>{const node=doc.createElement(tag);node.textContent=text;parent.appendChild(node);return node;};
  add(doc.body,'h1',(receipt.demo?'DEMO · ':'')+'FuelCore · '+(refund?'Refund statement':receipt.demo?'Payment statement':'Payment receipt'));if(receipt.demo)add(doc.body,'p',refund?'DEMO REFUND — No money refunded. The original invoice remains unchanged.':'DEMO RECEIPT — No money collected. This preview does not settle the invoice.');add(doc.body,'p','Invoice: '+invoice.invoiceNumber);
  if(invoice.referenceNumber)add(doc.body,'p','Service reference: '+invoice.referenceNumber);
  if(invoice.customerName)add(doc.body,'p','Customer: '+invoice.customerName);
  add(doc.body,'p',(refund?'Refund date: ':'Payment date: ')+(receipt.record?.paymentDate||receipt.paymentDate||invoice.invoiceDate));
  if(refund)add(doc.body,'p','Refund reason: '+receipt.refundReason);
  const table=add(doc.body,'table',''),head=add(table,'tr','');for(const title of ['Item','Quantity','Total (LKR)'])add(head,'th',title);
  for(const item of invoice.lineItems||[]){const row=add(table,'tr','');add(row,'td',item.itemName);add(row,'td',String(item.quantity));add(row,'td',money(item.totalAmount));}
  const totals=add(doc.body,'dl','');
  for(const [label,value] of [[refund?'Refund method':'Payment method',receipt.paymentMethod||receipt.record?.paymentMethod||invoice.paymentMethod],[refund?'This refund':'This payment','Rs. '+money(receipt.refundAmount??receipt.paymentAmount??receipt.record?.amount)],['Invoice total','Rs. '+money(invoice.netTotalWithPenalty??invoice.netTotal)],['Payments retained','Rs. '+money(invoice.amountPaid)],['Remaining balance','Rs. '+money(invoice.balanceDue)],...(!refund?[['Change','Rs. '+money(receipt.changeDue)]]:[])]){add(totals,'dt',label);add(totals,'dd',String(value||''));}
  const button=add(doc.body,'button',receipt.demo||refund?'Print statement':'Print receipt');button.onclick=()=>popup.print();popup.focus();popup.requestAnimationFrame(()=>popup.print());
}

