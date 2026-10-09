import {useCallback,useEffect,useRef,useState} from 'react';
import {api,apiFetch,confirmAction,notice} from '../../../lib/api';

export default function useCatalogWorkspace(root,offerKind){
  const [data,setData]=useState({services:[],offers:[],spareParts:[]}),[loading,setLoading]=useState(true),[error,setError]=useState(''),[selected,setSelected]=useState(null),[action,setAction]=useState(null),requests=useRef(0),pending=useRef(false);
  const load=useCallback(async()=>{
    const request=++requests.current;
    try{const feed=await api(root);if(offerKind==='services'){const catalog=await api('/api/services');feed.services=catalog.services;}if(request!==requests.current)return;setData(feed);setError('');}catch(error){if(request===requests.current)setError(error.message);}finally{if(request===requests.current)setLoading(false);}
  },[root,offerKind]);
  useEffect(()=>{load();window.addEventListener('fuelcore:refresh',load);return()=>{requests.current++;window.removeEventListener('fuelcore:refresh',load);};},[load]);
  const save=async body=>{await apiFetch(root+(selected.id?'/update/'+selected.id:'/add'),{method:'POST',body});setSelected(null);await load();notice('Changes saved');};
  const act=async(row,type)=>{
    if(pending.current)return;pending.current=true;setAction(row.id);setError('');
    try{if(type==='delete'&&!await confirmAction('Delete '+(row.name||'this offer')+'? Linked records remain protected.'))return;await api(root+'/'+type+'/'+row.id,{method:'POST'});await load();notice(type==='delete'?'Record deleted':'Status updated');}catch(error){setError(error.message);}finally{pending.current=false;setAction(null);}
  };
  return {data,loading,error,selected,setSelected,load,save,act,action};
}
