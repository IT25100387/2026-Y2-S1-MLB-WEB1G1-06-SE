import {useEffect,useRef} from 'react';

// Refresh visible job/billing pages after server-side cancellation or refund,
// preserving the user's filters and any open form.
export function useLiveRefresh(refresh,enabled=true){
  const latest=useRef(refresh);
  useEffect(()=>{latest.current=refresh;},[refresh]);
  useEffect(()=>{
    if(!enabled)return;
    const update=()=>{if(!document.hidden)Promise.resolve(latest.current()).catch(()=>{});};
    const timer=setInterval(update,20000);
    window.addEventListener('focus',update);window.addEventListener('fuelcore:refresh',update);
    return()=>{clearInterval(timer);window.removeEventListener('focus',update);window.removeEventListener('fuelcore:refresh',update);};
  },[enabled]);
}
