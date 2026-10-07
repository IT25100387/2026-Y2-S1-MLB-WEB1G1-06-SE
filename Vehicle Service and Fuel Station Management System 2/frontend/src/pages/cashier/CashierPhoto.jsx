import {useState} from 'react';
export function CashierPhoto({src,kind='part',alt=''}){const [failed,setFailed]=useState('');return <img className="item-photo" src={src&&failed!==src?src:'/cashier-'+(kind==='service'?'service':'part')+'-placeholder.svg'} alt={alt} loading="lazy" onError={()=>{if(src)setFailed(src);}}/>;}
