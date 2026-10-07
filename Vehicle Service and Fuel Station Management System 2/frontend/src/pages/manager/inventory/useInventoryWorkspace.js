import {useCallback,useEffect,useRef,useState} from 'react';
import {api,apiFetch,confirmAction,notice} from '../../../lib/api';

export default function useInventoryWorkspace(kind){
  const root='/api/inventory/'+kind,rowsKey=kind==='fuel'?'inventories':kind;
  const [rows,setRows]=useState([]),[related,setRelated]=useState({}),[loading,setLoading]=useState(true),[error,setError]=useState(''),[relatedError,setRelatedError]=useState('');
  const [selected,setSelected]=useState(null),[deleting,setDeleting]=useState(null),[actionError,setActionError]=useState('');
  const requests=useRef(0),deletePending=useRef(false);
  const load=useCallback(async()=>{
    const request=++requests.current;
    const auxiliary=kind==='suppliers'?'/api/inventory/fuel':'/api/inventory/suppliers';
    const [main,lookup]=await Promise.allSettled([api(root),api(auxiliary)]);
    if(request!==requests.current)return;
    if(main.status==='fulfilled'){setRows(main.value[rowsKey]);setError('');}else setError(main.reason.message);
    setRelated(old=>({...old,...(main.status==='fulfilled'&&kind==='suppliers'?{parts:main.value.parts||[]}:{}),...(lookup.status==='fulfilled'?lookup.value:{})}));
    setRelatedError(lookup.status==='fulfilled'?'':lookup.reason.message);setLoading(false);
  },[kind,root,rowsKey]);
  useEffect(()=>{load();window.addEventListener('fuelcore:refresh',load);return()=>{requests.current++;window.removeEventListener('fuelcore:refresh',load);};},[load]);
  const save=async form=>{
    const url=root+(selected.id?'/update/'+selected.id:'/add');
    if(form instanceof FormData)await apiFetch(url,{method:'POST',body:form});
    else await api(url,{method:'POST',body:JSON.stringify(form)});
    setSelected(null);setActionError('');await load();notice('Changes saved');
  };
  const reloadSelected=async()=>{
    const feed=await api(root),fresh=feed[rowsKey].find(row=>row.id===selected.id);
    if(!fresh)throw new Error('This record is no longer available. Close the form and refresh the page.');
    setSelected({...fresh});notice('Latest saved record loaded. Review the values before saving.');
  };
  const erase=async row=>{
    if(deletePending.current)return;
    deletePending.current=true;setDeleting(row.id);setActionError('');
    try{
      const name=row.fuelType||row.partName||row.supplierName;
      if(!(await confirmAction(`Delete ${name}? Linked inventory, pump, job, invoice and offer records remain protected.`)))return;
      await api(root+'/delete/'+row.id,{method:'POST'});await load();notice('Record deleted');
    }catch(error){setActionError(error.message);}
    finally{deletePending.current=false;setDeleting(null);}
  };
  return {rows,related,loading,error,relatedError,actionError,load,selected,setSelected,save,reloadSelected,erase,deleting};
}
