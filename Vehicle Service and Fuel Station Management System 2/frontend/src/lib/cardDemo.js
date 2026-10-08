export const DEMO_CARD_NUMBER='4111111111111111';

const stationDate=date=>Object.fromEntries(new Intl.DateTimeFormat('en',{timeZone:'Asia/Colombo',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(date).filter(part=>part.type!=='literal').map(part=>[part.type,part.value]));
export function testCardDetails(date=new Date()){
  const today=stationDate(date);
  return {cardHolder:'Demo Customer',cardNumber:'4111 1111 1111 1111',expiry:'12/'+String(Number(today.year)+4).slice(-2),securityCode:'123'};
}
export function sanitizeDemoCard(field,value,final=false){
  if(field==='cardHolder'){
    const name=value.replace(/[^\p{L}\p{M} ]/gu,'').replace(/^ +/,'').replace(/ +/g,' ').slice(0,60);
    return final?name.trim():name;
  }
  const digits=value.replace(/\D/g,'').slice(0,field==='cardNumber'?16:field==='expiry'?4:3);
  return field==='cardNumber'?digits.replace(/(.{4})(?=.)/g,'$1 '):field==='expiry'&&digits.length>2?digits.slice(0,2)+'/'+digits.slice(2):digits;
}
export function validateDemoCard(form,date=new Date()){
  const errors={},number=(form.cardNumber||'').replace(/ /g,'');
  if(!/^[\p{L}\p{M}]+(?: [\p{L}\p{M}]+)*$/u.test(form.cardHolder||'')||(form.cardHolder||'').length<2||(form.cardHolder||'').length>60)errors.cardHolder='Use 2–60 letters with single spaces between names.';
  const checksum=[...number].reverse().reduce((sum,digit,index)=>{let n=Number(digit);if(index%2)n=n*2>9?n*2-9:n*2;return sum+n;},0);
  if(!/^4\d{15}$/.test(number)||checksum%10)errors.cardNumber='Enter a valid 16-digit Visa test card number.';
  else if(number!==DEMO_CARD_NUMBER)errors.cardNumber='Use the demo card 4111 1111 1111 1111.';
  const match=/^(0[1-9]|1[0-2])\/(\d{2})$/.exec(form.expiry||''),today=stationDate(date);
  if(!match)errors.expiry='Enter a valid expiry in MM/YY format.';
  else {
    const year=2000+Number(match[2]),month=Number(match[1]),currentYear=Number(today.year);
    if(year<currentYear||year===currentYear&&month<Number(today.month))errors.expiry='The card expiry must be this month or later.';
    else if(year>currentYear+20)errors.expiry='Enter an expiry within the next 20 years.';
  }
  if(!/^\d{3}$/.test(form.securityCode||''))errors.securityCode='Enter a 3-digit security code.';
  return errors;
}

// A missing gateway may fall back to a preview only on this local demo UI.
export function localCardDemoAllowed(settings,hostname=typeof window==='undefined'?'':window.location.hostname){
  return import.meta.env?.VITE_CARD_DEMO_ENABLED!=='false'&&settings.configured!==true&&['localhost','127.0.0.1','::1','[::1]'].includes(hostname);
}
export function demoReceiptFromQuote(quote,amount,key,date=new Date()){
  const total=Number(quote.total),balance=Number(quote.balance),paid=Number(quote.paid||0),original=quote.invoice||{};
  if(!Number.isFinite(amount)||amount<=0||!Number.isFinite(total)||!Number.isFinite(balance)||amount>balance+.001)throw new Error('Payment must be within the outstanding balance.');
  if(!quote.referenceNumber&&amount<balance-.001)throw new Error('Spare parts require full payment.');
  if(quote.finalized===false&&amount>=balance-.001)throw new Error('Complete the service before full settlement.');
  const today=stationDate(date),paymentDate=today.year+'-'+today.month+'-'+today.day,remaining=Math.round(Math.max(0,balance-amount)*100)/100;
  const invoice={...original,id:original.id??null,invoiceNumber:original.invoiceNumber||'DEMO-'+key.slice(0,8).toUpperCase(),invoiceType:original.invoiceType||(quote.referenceNumber?'SERVICE':'SPARE_PART'),referenceNumber:quote.referenceNumber||null,customerName:original.customerName||'Guest Customer',invoiceDate:original.invoiceDate||quote.issueDate||paymentDate,dueDate:original.dueDate||quote.dueDate||(quote.referenceNumber?null:original.invoiceDate||quote.issueDate||paymentDate),paymentMethod:'CARD',netTotal:total,netTotalWithPenalty:total,amountPaid:Math.round((paid+amount)*100)/100,balanceDue:remaining,status:remaining<=.001?'PAID':'PARTIAL',lineItems:(quote.lines||original.lineItems||[]).map(line=>({...line,totalAmount:line.totalAmount??Math.round(line.unitPrice*line.quantity*100)/100}))};
  return {completed:true,demo:true,localDemo:true,state:'DEMO_COMPLETED',invoice,paymentAmount:amount,paymentMethod:'CARD',paymentDate,changeDue:0,offerSavings:quote.offerSavings||0};
}
export function demoReceiptFromInvoice(invoice,amount,key){
  if(!invoice||['VOID','REFUNDED','PARTIALLY_REFUNDED'].includes(invoice.status))throw new Error('Choose an invoice with an outstanding balance.');
  return demoReceiptFromQuote({invoice,total:invoice.netTotalWithPenalty??invoice.netTotal,balance:invoice.balanceDue,paid:invoice.amountPaid,referenceNumber:invoice.referenceNumber||(invoice.invoiceType==='SERVICE'?'SERVICE':null),finalized:invoice.finalized,lines:invoice.lineItems},amount,key);
}
