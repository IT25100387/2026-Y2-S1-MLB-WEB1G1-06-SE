import {useRef,useState} from 'react';
import {api} from '../lib/api';
import {sanitizeDemoCard,testCardDetails,validateDemoCard} from '../lib/cardDemo';
import {RequiredMark} from './Validation';
import {CashierModal} from '../pages/cashier/CashierUI';
import {money} from './DataUI';
import './DemoCardCheckout.css';

const fields=[['cardHolder','Cardholder name','e.g. Demo Customer',60,'Cardholder name'],['cardNumber','Test card number','4111 1111 1111 1111',19,'Card number'],['expiry','Test expiry','MM/YY',5,'Expiry date'],['securityCode','Test security code','e.g. 123',3,'Security code']];
function CardIcon(){return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><rect x="2.5" y="4.5" width="19" height="15" rx="3"/><path d="M3 10h18M6 15h4"/></svg>;}
function LockIcon(){return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/></svg>;}
export default function DemoCardCheckout({checkout,path,onFinish,onCancel}){
  const refund=checkout.operation==='refund';
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[form,setForm]=useState({cardHolder:'',cardNumber:'',expiry:'',securityCode:''}),[errors,setErrors]=useState({});
  const box=useRef(null),touched=useRef(new Set());
  const change=(field,value,final=false)=>{
    const next={...form,[field]:sanitizeDemoCard(field,value,final)};setForm(next);setError('');
    if(final)touched.current.add(field);
    if(touched.current.has(field))setErrors(old=>({...old,[field]:validateDemoCard(next)[field]}));
  };
  const complete=async event=>{
    event.preventDefault();if(busy)return;
    const clean={...form,cardHolder:sanitizeDemoCard('cardHolder',form.cardHolder,true)},issues=validateDemoCard(clean);setForm(clean);setErrors(issues);fields.forEach(([field])=>touched.current.add(field));
    if(Object.keys(issues).length){box.current.elements.namedItem(Object.keys(issues)[0])?.focus();return;}
    setBusy(true);setError('');
    try{
      // Card details remain in this form; neither storage nor the API receives them.
      const result=checkout.localDemo?checkout.receipt:await api(path+'/demo-complete',{method:'POST'});
      if(!result?.completed||!result.demo)throw new Error('This demo checkout is closed. Cancel it and start another preview.');
      onFinish(result);
    }catch(error){setError(error.message);setBusy(false);}
  };
  const cancel=async()=>{if(busy)return;setBusy(true);try{if(!checkout.localDemo)await api(path+'/cancel-unprocessed',{method:'POST'});onCancel();}catch(error){setError(error.message);setBusy(false);}};
  return <div className="demo-card-checkout"><CashierModal title={refund?'Demo card refund':'Demo card checkout'} busy={busy} onClose={cancel}>
    <div className="demo-checkout-layout">
      <section className="demo-checkout-entry" aria-label="Card details">
        <div className="demo-checkout-heading"><span className="demo-checkout-icon"><CardIcon/></span><div><h3>{refund?'Refund to card':'Pay by card'}</h3><p>Enter your test card details to continue.</p></div></div>
        <form ref={box} noValidate autoComplete="off" onSubmit={complete}>
          <div className="demo-checkout-form-heading"><span>Card details</span><button type="button" disabled={busy} onClick={()=>{setForm(testCardDetails());setErrors({});setError('');}}>Use test details</button></div>
          <fieldset className="demo-card-fields" disabled={busy}>
            {fields.map(([field,label,placeholder,maxLength,visibleLabel])=><label key={field} className={'demo-card-field demo-card-field-'+field}><span className="demo-card-field-label">{visibleLabel}<RequiredMark/></span><span className="demo-card-input-wrap"><input name={field} aria-label={label} type={field==='securityCode'?'password':'text'} inputMode={field==='cardHolder'?'text':'numeric'} autoComplete="off" required aria-required="true" maxLength={maxLength} placeholder={placeholder} value={form[field]} aria-invalid={Boolean(errors[field])} aria-describedby={errors[field]?'demo-'+field+'-error':field==='securityCode'?'demo-security-hint':undefined} onChange={event=>change(field,event.target.value)} onBlur={event=>change(field,event.target.value,true)}/>{field==='cardNumber'&&<span className="demo-card-input-icon"><CardIcon/></span>}</span>{field==='securityCode'&&!errors[field]&&<small id="demo-security-hint">3 digits on the back of the card</small>}{errors[field]&&<span id={'demo-'+field+'-error'} className="demo-field-error" role="alert">{errors[field]}</span>}</label>)}
          </fieldset>
          {error&&<p className="demo-field-error demo-form-error" role="alert">{error}</p>}
          <button type="submit" className="demo-card-pay" disabled={busy}><LockIcon/>{busy?'Preparing statement…':refund?'Complete demo refund':'Complete demo payment'}</button>
          <div className="demo-card-footer"><span>{refund?'No transfer':'No charge'} · Test mode</span><button type="button" disabled={busy} onClick={cancel}>{refund?'Cancel refund':'Cancel payment'}</button></div>
        </form>
      </section>
      <aside className="demo-payment-summary" aria-label="Payment summary">
        <div className="demo-summary-brand">FUEL<span>CORE</span><span className="demo-test-badge">DEMO</span></div>
        <p className="demo-summary-caption">{refund?'Amount to refund':'Amount to pay'}</p><div className="demo-summary-amount"><small>LKR</small><strong>{money(checkout.amount)}</strong></div>
        <div className="demo-summary-card" aria-hidden="true"><div><span>FUELCORE</span><span>TEST CARD</span></div><span className="demo-card-chip"/><p>•••• &nbsp; •••• &nbsp; •••• &nbsp; 1111</p><footer><span>DEMO CUSTOMER</span><strong>VISA</strong></footer></div>
        <dl className="demo-summary-details"><div><dt>Payment method</dt><dd>Card</dd></div>{checkout.receipt?.invoice?.invoiceNumber&&<div><dt>Invoice</dt><dd>{checkout.receipt.invoice.invoiceNumber}</dd></div>}<div><dt>Next step</dt><dd>Print statement</dd></div></dl>
        <p className="demo-summary-note">This is a demonstration. {refund?'No money is refunded.':'No money is collected.'}</p>
      </aside>
    </div>
  </CashierModal></div>;
}

