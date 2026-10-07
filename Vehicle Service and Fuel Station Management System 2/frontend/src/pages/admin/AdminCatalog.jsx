import ManagerOffers from '../manager/ManagerOffers';
import ManagerServiceCatalog from '../manager/ManagerServiceCatalog';
// Reuse corrected original card/form workflows; Manager pages stay unchanged.
export function AdminOffers(props){return <div className="admin-catalog"><ManagerOffers {...props}/></div>;}
export default function AdminCatalog(){return <div className="admin-catalog"><ManagerServiceCatalog/></div>;}
