import {useCallback,useEffect,useRef,useState} from 'react';
import {Link} from 'react-router-dom';
import {useReducedMotion} from 'framer-motion';
import {api} from '../lib/api';
import {money} from './DataUI';
import {ItemPhoto} from './ItemPhoto';
import './GarageOffers.css';

function shuffle(items){
  const result=[...items];
  for(let i=result.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[result[i],result[j]]=[result[j],result[i]];}
  return result;
}
const keyFor=offer=>offer.type+':'+offer.name;
const stepFor=node=>node?.children[1]?node.children[1].offsetLeft-node.children[0].offsetLeft:node?.clientWidth||0;

export default function GarageOffers(){
  const [offers,setOffers]=useState([]),[index,setIndex]=useState(0),[loading,setLoading]=useState(true),[error,setError]=useState('');
  const [paused,setPaused]=useState(false),[hovered,setHovered]=useState(false),[focused,setFocused]=useState(false),[hidden,setHidden]=useState(document.hidden);
  const interacting=hovered||focused;
  const viewport=useRef(null),settling=useRef(null),position=useRef(0),signature=useRef(''),reduced=useReducedMotion();
  const load=useCallback(async()=>{
    try{
      // Existing catalog feeds supply each item's saved photo; no pricing or offer rules change.
      const [data,services,parts]=await Promise.all([api('/api/v1/customer/offers'),api('/api/v1/customer/services').catch(()=>[]),api('/api/v1/customer/store').catch(()=>[])]);
      const items=[...(data.serviceOffers||[]).map(offer=>({...offer,type:'service',imageUrl:offer.imageUrl||services.find(item=>item.name===offer.name)?.imageUrl})),...(data.sparePartOffers||[]).map(offer=>({...offer,type:'part',imageUrl:offer.imageUrl||parts.find(item=>item.partName===offer.name)?.imageUrl}))];
      const next=JSON.stringify(items);
      if(signature.current!==next){signature.current=next;position.current=0;setIndex(0);setOffers(shuffle(items));}
      setError('');
    }catch(e){setError(e.message);}finally{setLoading(false);}
  },[]);
  useEffect(()=>{
    load();const visibility=()=>setHidden(document.hidden);
    window.addEventListener('focus',load);window.addEventListener('fuelcore:refresh',load);document.addEventListener('visibilitychange',visibility);
    return()=>{window.removeEventListener('focus',load);window.removeEventListener('fuelcore:refresh',load);document.removeEventListener('visibilitychange',visibility);clearTimeout(settling.current);};
  },[load]);
  useEffect(()=>{
    const node=viewport.current;if(!node||!offers.length)return;
    const align=()=>node.scrollTo({left:(offers.length>1?offers.length+position.current:0)*stepFor(node),behavior:'instant'});
    align();const observer=new ResizeObserver(align);observer.observe(node);
    return()=>observer.disconnect();
  },[offers,error,loading]);
  const move=useCallback(direction=>{
    const node=viewport.current;if(!node||offers.length<2)return;
    const step=stepFor(node);node.scrollTo({left:(Math.round(node.scrollLeft/step)+direction)*step,behavior:reduced?'instant':'smooth'});
  },[offers.length,reduced]);
  useEffect(()=>{
    if(offers.length<2||paused||interacting||hidden||reduced)return;
    const timer=setInterval(()=>move(1),3500);return()=>clearInterval(timer);
  },[offers.length,paused,interacting,hidden,reduced,move]);
  const scroll=()=>{
    clearTimeout(settling.current);
    settling.current=setTimeout(()=>{
      const node=viewport.current;if(!node||offers.length<2)return;
      const step=stepFor(node),raw=Math.round(node.scrollLeft/step),current=((raw%offers.length)+offers.length)%offers.length;
      position.current=current;setIndex(current);
      if(raw<offers.length||raw>=2*offers.length)node.scrollTo({left:(offers.length+current)*step,behavior:'instant'});
    },140);
  };
  const cycles=offers.length>1?[0,1,2]:[1];
  return <section className="garage-offers garage-card-offers" aria-labelledby="garage-offers-title" aria-roledescription="carousel"
    onMouseEnter={()=>setHovered(true)} onMouseLeave={()=>setHovered(false)} onFocusCapture={()=>setFocused(true)} onBlurCapture={e=>{if(!e.currentTarget.contains(e.relatedTarget))setFocused(false);}}>
    <div className="garage-section-heading"><div><p className="garage-kicker">A little extra value</p><h2 id="garage-offers-title">Offers for your next visit.</h2></div><div className="garage-catalog-links"><Link to="/customer/catalog?offers=true" className="garage-text-link">Service offers</Link><Link to="/customer/store?offers=true" className="garage-text-link">Part offers <i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link></div></div>
    {error?<div className="garage-offer-empty" role="alert">{error} <button onClick={load}>Retry offers</button></div>:loading?<p className="garage-offer-empty">Loading current offers...</p>:!offers.length?<div className="garage-offer-empty"><i aria-hidden="true" className="fa-solid fa-tags"/><div><strong>Your next offer is on its way.</strong><p>There are no active offers right now. Browse our services and parts anytime.</p></div><Link to="/customer/catalog">Explore services</Link></div>:<>
      <div ref={viewport} className="garage-offer-cards" onScroll={scroll} style={{'--offer-total':Math.min(4,offers.length)}}>
        {cycles.flatMap(cycle=>offers.map((offer,itemIndex)=><article className="garage-offer-card" key={cycle+':'+itemIndex+':'+keyFor(offer)} data-offer-index={itemIndex} data-offer-type={offer.type} data-offer-copy={cycle!==1||undefined} aria-hidden={cycle!==1||undefined}>
          <div className="garage-offer-media"><ItemPhoto src={offer.imageUrl} kind={offer.type==='service'?'service':'part'} alt={cycle===1?offer.name:''}/><span className="garage-offer-badge">{offer.discountPercentage}% OFF</span></div>
          <div className="garage-offer-card-copy"><p className="garage-offer-kind"><i aria-hidden="true" className={'fa-solid '+(offer.icon||'fa-gears')}/>{offer.type==='service'?'Service offer':'Spare part offer'}<span>{offer.category}</span></p><h3>{offer.name}</h3><p className="garage-offer-description">{offer.description}</p>
            <div className="garage-offer-card-prices"><del>Rs. {money(offer.originalPrice)}</del><strong>Rs. {money(offer.newPrice)}</strong></div>
            <Link tabIndex={cycle===1?0:-1} className="garage-offer-card-action" to={offer.type==='service'?'/customer/catalog?offers=true':'/customer/store?offers=true'}>{offer.type==='service'?'Explore service':'Shop parts'}<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link>
          </div>
        </article>))}
      </div>
      <div className="garage-offer-controls"><span>{index+1} / {offers.length} offers</span>{offers.length>1&&<div><button aria-label="Previous offer" onClick={()=>move(-1)}><i aria-hidden="true" className="fa-solid fa-arrow-left"/></button><button aria-label={paused?'Resume offers':'Pause offers'} onClick={()=>setPaused(value=>!value)}><i aria-hidden="true" className={`fa-solid ${paused?'fa-play':'fa-pause'}`}/></button><button aria-label="Next offer" onClick={()=>move(1)}><i aria-hidden="true" className="fa-solid fa-arrow-right"/></button></div>}</div>
    </>}
  </section>;
}
