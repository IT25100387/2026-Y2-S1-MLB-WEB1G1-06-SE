import {useState} from 'react';
import {motion,useReducedMotion} from 'framer-motion';
import {ItemPhoto} from '../../../components/ItemPhoto';
import '../ManagerService.css';
import './ManagerCatalog.css';

export function CatalogMedia({src,kind,name,discount,popular}){return <div className="manager-catalog-media"><ItemPhoto src={src} kind={kind} alt={name}/>{discount>0&&<span className="manager-catalog-discount">{discount}% off</span>}{popular&&<span className="manager-catalog-popular"><i aria-hidden="true" className="fa-solid fa-star"/>Popular</span>}</div>;}
export function CatalogCards({rows,name,loading,children}){
  const signature=rows.map(row=>row.id).join('|'),[paging,setPaging]=useState({signature,size:15,page:1}),reduced=useReducedMotion();
  const size=paging.size,current=Math.min(signature===paging.signature?paging.page:1,Math.max(1,Math.ceil(rows.length/size))),pages=Math.max(1,Math.ceil(rows.length/size));
  return <>{loading?<p role="status" className="manager-inventory-empty">Loading items...</p>:!rows.length?<p className="manager-inventory-empty">No items match your filters.</p>:<div className="manager-catalog-cards">{rows.slice((current-1)*size,current*size).map((item,index)=><motion.article key={item.id} className="manager-item-card" initial={reduced?false:{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{duration:.2,delay:index*.025}}>{children(item)}</motion.article>)}</div>}<nav aria-label={name+' pagination'} className="manager-service-pagination"><div className="manager-service-page-info"><span>Showing {rows.length?(current-1)*size+1:0} – {Math.min(current*size,rows.length)} of {rows.length}</span><div className="manager-service-row-options"><span>Rows:</span>{[10,15,20].map(value=><button key={value} aria-label={`${value} rows per page`} aria-pressed={size===value} className={size===value?'is-active':''} onClick={()=>setPaging({signature,size:value,page:1})}>{value}</button>)}</div></div><div className="manager-service-page-buttons"><button aria-label="Previous page" disabled={current===1} onClick={()=>setPaging({signature,size,page:current-1})}>Prev</button><button aria-label="Next page" disabled={current===pages} onClick={()=>setPaging({signature,size,page:current+1})}>Next</button></div></nav></>;
}
