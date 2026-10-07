import {motion,useReducedMotion} from 'framer-motion';
import {useFuelOperations} from './FuelOperationsContext';

export default function FuelPageShell({section,title,description,children,requiresFeed=true}){
  const {data,errors,load}=useFuelOperations(),reduced=useReducedMotion();
  return <motion.div key={section} className="manager-fuel-page" data-fuel-section={section} initial={reduced?false:{opacity:0,y:6}} animate={{opacity:1,y:0}} transition={{duration:.2}}>
    <div className="manager-fuel-heading"><p>Fuel operations</p><h1>{title}</h1>{description&&<div className="manager-fuel-description">{description}</div>}</div>
    {requiresFeed&&errors.load&&<div className="manager-fuel-error" role="alert">{errors.load}<button onClick={load}>Retry</button></div>}
    {requiresFeed&&!data?<p role="status" className="manager-fuel-description">{errors.load?'Figures could not be loaded. Please retry.':'Loading fuel operations...'}</p>:children}
  </motion.div>;
}
