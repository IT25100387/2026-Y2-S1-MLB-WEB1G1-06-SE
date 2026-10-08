import {api,requestKey} from './api';
import {openDemoCardCheckout} from './demoCheckout';
import {demoReceiptFromInvoice,demoReceiptFromQuote,localCardDemoAllowed} from './cardDemo';

export async function cardConfiguration(endpoint){
  const local=localCardDemoAllowed({});
  try{return await api(endpoint,{silentStatuses:local?[403,404]:[]});}
  catch(error){
    if(local&&[403,404].includes(error.status)){
      // Older station servers may deny the new gateway route. A signed-in user
      // can still preview checkout locally; this grants no server permissions.
      const account=await api('/api/auth/me');
      if(['ROLE_ADMIN','ROLE_MANAGER','ROLE_CASHIER','ROLE_CUSTOMER'].includes(account.role))return {configured:false};
    }
    throw error;
  }
}

let sdk;
export function loadCardSDK(){
  if(window.payhere)return Promise.resolve();
  if(!sdk)sdk=new Promise((resolve,reject)=>{
    const script=document.createElement('script');script.src='https://www.payhere.lk/lib/payhere-2.0.js';script.async=true;
    script.onload=()=>{if(window.payhere)resolve();else{sdk=null;script.remove();reject(new Error('Card gateway did not load'));}};
    script.onerror=()=>{sdk=null;script.remove();reject(new Error('Cannot connect to the card gateway. Retry when the connection is available.'));};
    document.head.appendChild(script);
  });
  return sdk;
}

// A browser callback is only a hint. Only a signed notification can settle the ledger.
export async function collectCardPayment(body,key,endpoint='/api/payments/card/start',preview={}){
  const storageKey='fuelcore.card.'+(body.invoiceNumber||'store.'+body.partId);
  let saved=JSON.parse(sessionStorage.getItem(storageKey)||'null');
  if(saved&&JSON.stringify(saved.body)!==JSON.stringify(body))throw new Error('A previous card checkout needs verification. Retry its original amount before starting another payment.');
  if(!saved){
    const settings=await cardConfiguration('/api/payments/card/configuration');
    if(!settings.configured&&localCardDemoAllowed(settings)){
      const id=requestKey(),receipt=preview.invoice?demoReceiptFromInvoice(preview.invoice,body.amount,id):demoReceiptFromQuote(preview.quote,preview.quote?.balance,id);
      return openDemoCardCheckout({amount:receipt.paymentAmount,localDemo:true,receipt});
    }
    if(!settings.configured)throw new Error('Configure PayHere and the station guest billing profile before taking a card payment.');
    if(settings.mode!=='DEMO')await loadCardSDK();saved={key:settings.mode==='DEMO'?requestKey():key,body,endpoint,phase:'STARTING'};sessionStorage.setItem(storageKey,JSON.stringify(saved));
  }
  const statusPath='/api/payments/card/'+encodeURIComponent(saved.key);
  if(saved.phase==='STARTING'){
    let result;
    try{result=await api(saved.endpoint,{method:'POST',headers:{'Idempotency-Key':saved.key},body:JSON.stringify(saved.body)});}
    catch(error){if(error.status&&error.status<500)sessionStorage.removeItem(storageKey);throw error;}
    if(result.completed){sessionStorage.removeItem(storageKey);return result;}
    if(result.demoCheckout){saved.phase='DEMO';saved.demo=result;sessionStorage.setItem(storageKey,JSON.stringify(saved));}
    if(result.payment){
      await loadCardSDK();
      saved.phase='OPENED';sessionStorage.setItem(storageKey,JSON.stringify(saved));
      window.payhere.onCompleted=()=>{};
      const dismissed=()=>api(statusPath+'/cancel-unprocessed',{method:'POST'}).catch(()=>{});
      window.payhere.onDismissed=dismissed;window.payhere.onError=dismissed;
      window.payhere.startPayment(result.payment);
    }
  }
  if(saved.phase==='DEMO'){
    const recorded=await api(statusPath);
    if(recorded.completed){sessionStorage.removeItem(storageKey);return recorded;}
    if(['FAILED','CANCELLED'].includes(recorded.state)){sessionStorage.removeItem(storageKey);throw new Error('Demo checkout cancelled. No money was collected.');}
    try{const result=await openDemoCardCheckout(saved.demo,statusPath);sessionStorage.removeItem(storageKey);return result;}
    catch(error){if(error.message.startsWith('Demo checkout cancelled'))sessionStorage.removeItem(storageKey);throw error;}
  }
  const deadline=Date.now()+16*60*1000;
  while(Date.now()<deadline){
    let result;
    try{result=await api(statusPath);}catch(error){throw new Error(error.message+' Retry to check this checkout; do not collect another payment.');}
    if(result.completed){sessionStorage.removeItem(storageKey);return result;}
    if(['FAILED','CANCELLED'].includes(result.state)){sessionStorage.removeItem(storageKey);throw new Error(result.message||'The gateway did not collect this payment.');}
    if(result.state==='REVIEW')throw new Error(result.message||'This card payment needs verification with PayHere.');
    await new Promise(resolve=>setTimeout(resolve,2000));
  }
  throw new Error('Card confirmation is delayed. Retry to check the existing checkout before collecting another payment.');
}
