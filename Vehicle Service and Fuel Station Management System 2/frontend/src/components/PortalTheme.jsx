import {createContext,useContext,Children,cloneElement,isValidElement} from 'react';

const PortalThemeContext=createContext(false);
export const usePortalTheme=()=>useContext(PortalThemeContext);

// Only the four opted-in role layouts receive this presentation layer.
export default function PortalTheme({children}){
  return <PortalThemeContext.Provider value={true}><div className="unified-portal">{children}</div></PortalThemeContext.Provider>;
}

export function RecordPagination({name='Records',total,page,size,onPage,onSize}){
  const pages=Math.max(1,Math.ceil(total/size));
  return <nav className="admin-pagination portal-pagination" aria-label={name+' pagination'}><div><span>Showing {total?(page-1)*size+1:0} &ndash; {Math.min(page*size,total)} of {total}</span><span>Rows:</span>{[10,15,20].map(value=><button key={value} aria-label={value+' rows per page'} aria-pressed={size===value} onClick={()=>onSize(value)}>{value}</button>)}</div><div><button aria-label="Previous page" disabled={page<=1} onClick={()=>onPage(page-1)}>Prev</button><button aria-label="Next page" disabled={page>=pages} onClick={()=>onPage(page+1)}>Next</button></div></nav>;
}

// Keep the original handlers and accessible names; give icon-only table actions
// visible labels without changing the actions available on any record.
export function typedTableActions(nodes){
  return Children.map(nodes,node=>{
    if(!isValidElement(node))return node;
    const children=Children.toArray(node.props.children);
    if(node.type==='button'||node.type==='a'||node.props.to){
      const iconOnly=children.length===1&&isValidElement(children[0])&&children[0].type==='i';
      const label=node.props['aria-label'];
      const shortLabel=label?.match(/^(Edit|Delete|Cancel|View|Book|Reschedule|Deactivate|Reactivate)\b/i)?.[1]||(label==='Toggle employment status'?'Change status':label);
      return cloneElement(node,{className:[node.props.className,'portal-table-action'].filter(Boolean).join(' ')},iconOnly&&shortLabel?shortLabel:node.props.children);
    }
    return node.props.children?cloneElement(node,{},typedTableActions(node.props.children)):node;
  });
}
