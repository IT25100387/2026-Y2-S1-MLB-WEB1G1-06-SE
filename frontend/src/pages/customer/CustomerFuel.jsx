import {useCallback,useEffect,useState} from 'react';
import {api} from '../../lib/api';
import {money} from '../../components/DataUI';
export default function CustomerFuel(){
  const [prices,setPrices]=useState([]),[error,setError]=useState(''),[loading,setLoading]=useState(true);
  const load=useCallback(async()=>{try{const data=await api('/api/v1/customer/fuel');setPrices(data.prices);setError('');}catch(e){setError(e.message);}finally{setLoading(false);}},[]);
  useEffect(()=>{load();window.addEventListener('focus',load);return()=>window.removeEventListener('focus',load);},[load]);
  return <div><div className="customer-page-header"><p className="customer-eyebrow">Ready for the road</p><h1>Fuel prices</h1><p>Today's station prices. Choose your fuel and plan your next stop.</p></div>{error&&<p role="alert" className="customer-error">{error}<button onClick={load}>Retry</button></p>}{loading?<p className="customer-loading">Loading fuel prices...</p>:prices.length?<div className="customer-fuel-prices">{prices.map(price=><article key={price.id}><span><i aria-hidden="true" className="fa-solid fa-gas-pump"/></span><h2>{price.fuelType}</h2><strong>Rs. {money(price.currentPrice)}</strong><p>per litre</p></article>)}</div>:!error&&<p className="customer-empty">Fuel prices are not available right now. Please check again shortly.</p>}</div>;
}
