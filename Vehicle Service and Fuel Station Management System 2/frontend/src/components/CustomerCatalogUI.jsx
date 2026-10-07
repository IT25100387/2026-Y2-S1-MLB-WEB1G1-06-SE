import {useState} from 'react';
import {RecordPagination} from './PortalTheme';

export function OfferMark({discount}) {
  return Number(discount)>0?<span className="customer-offer-mark"><i aria-hidden="true" className="fa-solid fa-tag"/>Offer · {discount}% off</span>:null;
}

export function CatalogPhoto({src,fallback,alt}) {
  const [failed,setFailed]=useState(false);
  return <div className="customer-catalog-photo"><img src={!failed&&src?src:fallback} alt={src&&!failed?alt:'FuelCore '+alt+' collection'} loading="lazy" onError={()=>setFailed(true)}/></div>;
}

export function CatalogPagination({total,page,setPage,size,setSize}) {
  return <RecordPagination name="Catalog" total={total} page={page} size={size} onPage={setPage} onSize={value=>{setSize(value);setPage(1);}}/>;
}
