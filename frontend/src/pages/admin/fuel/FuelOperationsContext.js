import {createContext,useContext} from 'react';

export const FuelOperationsContext=createContext(null);
export function useFuelOperations(){
  const context=useContext(FuelOperationsContext);
  if(!context)throw new Error('Fuel operations must be opened inside the fuel workspace');
  return context;
}
