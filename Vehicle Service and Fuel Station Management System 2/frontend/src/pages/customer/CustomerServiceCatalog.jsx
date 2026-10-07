import {useEffect,useState} from 'react';
import {Link,useSearchParams} from 'react-router-dom';
import {motion,useReducedMotion} from 'framer-motion';
import {api} from '../../lib/api';
import {SearchBar,money} from '../../components/DataUI';
import {OfferMark,CatalogPhoto,CatalogPagination} from '../../components/CustomerCatalogUI';

export default function CustomerServiceCatalog(){
  const [services,setServices]=useState([]),[loading,setLoading]=useState(true),[error,setError]=useState(''),[query,setQuery]=useState(''),[category,setCategory]=useState('All'),[page,setPage]=useState(1),[size,setSize]=useState(15);
  const [params,setParams]=useSearchParams(),reduced=useReducedMotion(),offersOnly=params.get('offers')==='true';
  const load=async()=>{try{setServices(await api('/api/v1/customer/services'));setError('');}catch(e){setError(e.message);}finally{setLoading(false);}};
  useEffect(()=>{load();},[]);
  const categories=['All',...new Set(services.map(service=>service.category).filter(Boolean))];
  const rows=services.filter(service=>(category==='All'||service.category===category)&&(!offersOnly||service.discountPercentage>0)&&`${service.name} ${service.category} ${service.shortDescription}`.toLowerCase().includes(query.toLowerCase()));
  const current=Math.min(page,Math.max(1,Math.ceil(rows.length/size)));
  return <div className="customer-catalog"><div className="customer-page-header"><p className="customer-eyebrow">Care for every mile</p><h1>Service catalog</h1><p>Find the right care for your vehicle. Explore our packages, see current offers and choose your next appointment.</p></div>
    <div className="customer-catalog-toolbar"><SearchBar onSearch={value=>{setQuery(value);setPage(1);}}/><button className={`customer-filter ${offersOnly?'is-active':''}`} aria-pressed={offersOnly} onClick={()=>{setParams(offersOnly?{}:{offers:'true'});setPage(1);}}><i aria-hidden="true" className="fa-solid fa-tag"/>Offers only</button></div>
    <div className="customer-category-tabs">{categories.map(value=><button className={category===value?'is-active':''} aria-pressed={category===value} key={value} onClick={()=>{setCategory(value);setPage(1);}}>{value}</button>)}</div>
    {error&&<p role="alert" className="customer-error">{error}<button onClick={load}>Retry</button></p>}
    {loading?<p className="customer-loading">Loading services...</p>:rows.length?<><div className="customer-catalog-cards">{rows.slice((current-1)*size,current*size).map((service,index)=><motion.article className="customer-product-card customer-service-card" key={service.id} initial={reduced?false:{opacity:0,y:10}} animate={{opacity:1,y:0}} transition={{duration:.2,delay:index*.03}}>
      <div className="customer-product-media"><CatalogPhoto src={service.imageUrl||service.photoUrl} fallback="/landing-service-repairs.jpg" alt={service.imageUrl||service.photoUrl?service.name:'service workshop'}/><OfferMark discount={service.discountPercentage}/>{service.popular&&<span className="customer-popular-mark"><i aria-hidden="true" className="fa-solid fa-star"/>Popular</span>}</div>
      <div className="customer-product-copy"><p className="customer-product-category">{service.category||'Vehicle care'}<span><i aria-hidden="true" className="fa-regular fa-clock"/>{service.estimatedDuration||'Ask our team'}</span></p><h2>{service.name}</h2><p>{service.shortDescription}</p>{service.highlights?.length>0&&<ul>{service.highlights.slice(0,4).map((highlight,i)=><li key={i}><i aria-hidden="true" className="fa-solid fa-check"/>{highlight}</li>)}</ul>}<div className="customer-product-bottom"><div><small>Estimated price</small><strong>Rs. {money(service.offerPrice??service.estimatedCost)}</strong>{service.discountPercentage>0&&<del>Rs. {money(service.estimatedCost)}</del>}</div><Link className="btn-accent customer-card-button" to={`/customer/book?service=${service.id}`} aria-label={'Book '+service.name}>Book service<i aria-hidden="true" className="fa-solid fa-arrow-right"/></Link></div></div>
    </motion.article>)}</div><CatalogPagination total={rows.length} page={current} setPage={setPage} size={size} setSize={setSize}/></>:!error&&<div className="customer-empty">No services match your filters. Try another category or turn off Offers only.</div>}
  </div>;
}
